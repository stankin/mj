package ru.stankin.mj.rested.security

import com.fasterxml.jackson.databind.JsonNode
import com.github.scribejava.core.builder.api.DefaultApi20
import com.github.scribejava.core.model.OAuth2AccessToken

import org.apache.logging.log4j.LogManager

import org.pac4j.core.profile.converter.Converters

import org.pac4j.oauth.client.OAuth20Client
import org.pac4j.oauth.config.OAuth20Configuration
import org.pac4j.oauth.profile.JsonHelper
import org.pac4j.oauth.profile.OAuth20Profile
import org.pac4j.oauth.profile.definition.OAuth20ProfileDefinition


/**
 * Created by nickl on 01.01.17.
 */
class YandexClient() : OAuth20Client<OAuth20Profile>() {

    constructor(key: String, secret: String) : this() {
        this.configuration = YandexProfileDefinition.Configuration
        this.key = key
        this.secret = secret
    }

}


object YandexProfileDefinition : OAuth20ProfileDefinition<OAuth20Profile, OAuth20Configuration>() {

    private val log = LogManager.getLogger(this.javaClass)

    override fun extractUserProfile(body: String?): OAuth20Profile = YandexProfile(JsonHelper.getFirstNode(body))

    override fun getProfileUrl(accessToken: OAuth2AccessToken, configuration: OAuth20Configuration): String =
            "https://login.yandex.ru/info?format=json&oauth_token=" + accessToken.accessToken


    val FIRST_NAME = "first_name"
    val last_name = "last_name"
    val display_name = "display_name"
    val emails = "emails"
    val default_email = "default_email"
    val real_name = "real_name"
    val birthday = "birthday"
    val login = "login"
    val sex = "sex"
    val id = "id"

    init {
        primary(FIRST_NAME, Converters.STRING)
        primary(last_name, Converters.STRING)
        primary(display_name, Converters.STRING)
        primary(emails, Converters.STRING)
        primary(default_email, Converters.STRING)
        primary(real_name, Converters.STRING)
        primary(birthday, Converters.STRING)
        primary(login, Converters.STRING)
        primary(sex, Converters.STRING)
        primary(id, Converters.STRING)
    }

    class YandexProfile : OAuth20Profile {
        constructor(id: String, attributes: Map<String, Any>) {
            setId(id)
            clientName = "YandexProfile"
            for ((k, v) in attributes) {
                addAttribute(k, v)
            }
        }

        constructor(jsonNode: JsonNode) {
            id = JsonHelper.getElement(jsonNode, "id") as String
            for (attribute in primaryAttributes) {
                addAttribute(attribute, JsonHelper.getElement(jsonNode, attribute))
            }
        }

        override fun getEmail(): String = getAttribute("display_name", String::class.java)
    }

    object Configuration : OAuth20Configuration() {
        init {
            api = object : DefaultApi20() {

                override fun getAuthorizationBaseUrl(): String = "https://oauth.yandex.ru/authorize"

                override fun getAccessTokenEndpoint(): String = "https://oauth.yandex.ru/token"

            }
            profileDefinition = YandexProfileDefinition
        }
    }
}
