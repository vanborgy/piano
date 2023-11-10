package dev.vande.piano.container

import com.google.gson.JsonObject
import dev.vande.piano.Key
import dev.vande.piano.Piano
import dev.vande.piano.annotation.KeyListener
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

/**
 * @author soren@tranquil.cc
 * @project Piano
 * @at 11/10/2023
 */
class ListenerContainer(val kClass: KClass<Key>) {

    val listeners = hashMapOf<String?, MutableList<Pair<Any, KFunction<*>>>>()

    fun registerListener(any: Any, kFunction: KFunction<*>) {
        val keyListener = kFunction.findAnnotation<KeyListener>()!!
        val scope = keyListener.let { if (it.scope != "") it.scope else null }

        listeners.computeIfAbsent(scope) { arrayListOf() }.add(Pair(any, kFunction))
    }

    fun handleMessage(message: JsonObject) {
        val scope = message["scope"].asString ?: null
        val data = Piano.gson.fromJson(message["data"].asString, kClass.java)

        if (scope != null) listeners[scope]?.forEach { it.second.call(it.first, data) }
        else listeners.values.flatten()
            .filter { !it.second.findAnnotation<KeyListener>()!!.avoidGlobal }
            .forEach { it.second.call(it.first, data) }
    }

}