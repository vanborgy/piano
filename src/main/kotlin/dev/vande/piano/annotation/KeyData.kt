package dev.vande.piano.annotation

/**
 * @author soren@tranquil.cc
 * @project Piano
 * @at 11/8/2023
 */

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KeyData(val name: String = "", val cacheDuration: Long = 10000) //10 second default cache time