package io.github.fioldev.konfigure.annotations

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Key(val name: String)
