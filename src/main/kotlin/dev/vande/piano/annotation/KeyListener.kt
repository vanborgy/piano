package dev.vande.piano.annotation

/**
 * @author soren@tranquil.cc
 * @project Piano
 * @at 11/10/2023
 */

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class KeyListener(val scope: String = "", val avoidGlobal: Boolean = false)
