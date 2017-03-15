package ru.stankin.mj.testutils

import io.kotlintest.matchers.BeWrapper

/**
 * Created by nickl on 15.03.17.
 */
object Matchers {

    infix fun <T> BeWrapper<T>.ne(notexpected: Any): Unit {
        if (value == notexpected)
            throw AssertionError("$value is $notexpected but should not be")
    }


}