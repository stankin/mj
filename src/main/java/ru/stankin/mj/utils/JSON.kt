package ru.stankin.mj.utils

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Created by nickl on 13.01.17.
 */
object JSON {

    val objectMapper = ObjectMapper()

    fun asJson(any: Any) = objectMapper.writeValueAsString(any)
    inline fun <reified T : Any> read(str: String): T = objectMapper.readValue(str, T::class.java)


}