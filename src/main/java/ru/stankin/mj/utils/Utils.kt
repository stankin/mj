package ru.stankin.mj.utils

import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport


fun requireSysProp(name: String):String =
        System.getProperty(name) ?: throw NullPointerException("system property '${name}' should be defined")

fun <T> stream(sourceIterator: Iterator<T>): Stream<T> {
    return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(sourceIterator, Spliterator.ORDERED),
            false)
}