package dev.vande.piano

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.rabbitmq.client.*
import dev.vande.piano.annotation.KeyData
import dev.vande.piano.annotation.KeyListener
import dev.vande.piano.container.ListenerContainer
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

/**
 * @author soren@tranquil.cc
 * @project Piano
 * @at 11/8/2023
 */
object Piano {

    var host: String = "localhost"
    var queueName: String = "piano_messages"
    var gson = GsonBuilder().serializeNulls().create()

    private lateinit var channel: Channel
    private lateinit var listenerRegistry : MutableMap<String, ListenerContainer>

    fun setup(lambda: Piano.() -> Unit) {
        lambda.invoke(this)

        bind(host)
    }

    private fun bind(host: String) {
        val channel = ConnectionFactory().let {
            it.host = host
            it
        }.newConnection().createChannel()

        channel.queueDeclare(queueName, false, false, false, null)
        channel.exchangeDeclare("piano", "fanout")
        channel.queueBind(queueName, "piano", "")

        channel.basicConsume(queueName, true, DeliverCallback { _, delivery ->
            val json = gson.toJsonTree(String(delivery.body, Charsets.UTF_8)).asJsonObject

            val payload = json["payload"].asString

            listenerRegistry[payload]?.handleMessage(json)
        }, CancelCallback {  })

        this.channel = channel
        this.listenerRegistry = hashMapOf<String, ListenerContainer>()
    }

    fun Key.play() {
        playKey(this)
    }

    fun playKey(iKey: Key) {
        val name = iKey::class.findAnnotation<KeyData>()?.let { if (it.name == "") iKey::class.simpleName else it.name }

        listenerRegistry[name]?.listeners!!.values.flatten().forEach {
            it.second.call(it.first, iKey)
        }
    }

    fun Key.broadcast(scope: String? = null) {
        broadcastKey(this, scope)
    }

    fun broadcastKey(iKey: Key, scope: String? = null) {
        val keyData = iKey::class.findAnnotation<KeyData>() ?: return
        val name = keyData.let { if (it.name == "") iKey::class.simpleName else it.name }
        val cacheTime = keyData.cacheDuration
        val json = JsonObject()

        json.addProperty("payload", name)

        if (scope != null) json.addProperty("scope", scope)

        json.addProperty("data", gson.toJson(iKey))

        val props = AMQP.BasicProperties.Builder()
            .expiration(cacheTime.toString())
            .build()

        this.channel.basicPublish("piano", "", props, json.toString().toByteArray(Charsets.UTF_8))
    }

    fun registerListeners(any: Any) {
        any::class.declaredFunctions
            .filter {
                it.hasAnnotation<KeyListener>()
                && it.parameters.size == 1
                && it.parameters[0].type.isSubtypeOf(typeOf<Key>())
            }
            .forEach { func ->
                val kClass = func.parameters[0].type.classifier as KClass<Key>
                val keyData = kClass.findAnnotation<KeyData>() ?: return

                val name = keyData.let { if (it.name == "") kClass.simpleName else it.name }!!

                listenerRegistry.computeIfAbsent(name) { ListenerContainer(kClass) }.registerListener(any, func)
            }
    }

}