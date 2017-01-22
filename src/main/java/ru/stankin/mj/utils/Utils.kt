package ru.stankin.mj.utils

import org.sql2o.Connection
import org.sql2o.ResultSetIterable
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

fun <T> toStream(resultSet: ResultSetIterable<T>, connection: Connection): Stream<T> {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(resultSet.iterator(),
            Spliterator.DISTINCT or Spliterator.NONNULL or
                    Spliterator.CONCURRENT or Spliterator.IMMUTABLE
    ), false).onClose {
        resultSet.close()
        connection.close()
    }
}