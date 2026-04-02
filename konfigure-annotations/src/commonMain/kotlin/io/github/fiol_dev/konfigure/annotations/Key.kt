package io.github.fiol_dev.konfigure.annotations

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Key(val name: String)
