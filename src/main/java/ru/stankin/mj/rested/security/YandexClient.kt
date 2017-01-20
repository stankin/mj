package ru.stankin.mj.rested.security

import com.github.scribejava.core.builder.api.BaseApi
import com.github.scribejava.core.builder.api.DefaultApi20
import com.github.scribejava.core.model.OAuth2AccessToken
import com.github.scribejava.core.model.OAuthConfig
import com.github.scribejava.core.oauth.OAuth20Service
import org.apache.logging.log4j.LogManager
import org.pac4j.core.profile.AttributesDefinition
import org.pac4j.core.profile.converter.Converters
import org.pac4j.oauth.client.BaseOAuth20Client
import org.pac4j.oauth.profile.JsonHelper
import org.pac4j.oauth.profile.OAuth20Profile
import org.pac4j.oauth.profile.converter.JsonListConverter

/**
 * Created by nickl on 01.01.17.
 */
class YandexClient() : BaseOAuth20Client<YandexProfile>() {

    private val log = LogManager.getLogger(this.javaClass)

    constructor(key: String, secret: String) : this() {
        this.key = key
        this.secret = secret
    }

    override fun getApi(): BaseApi<OAuth20Service> = YandexApi

    override fun hasOAuthGrantType(): Boolean = true

    override fun extractUserProfile(body: String?): YandexProfile {
        log.debug("extractUserProfile for body: $body")
        return YandexProfile().let { profile ->
            JsonHelper.getFirstNode(body)?.run {
                profile.setId(JsonHelper.getElement(this, "id"))
                for (attribute in profile.attributesDefinition.primaryAttributes) {
                    profile.addAttribute(attribute, JsonHelper.getElement(this, attribute))
                }
            }
            profile
        }
    }

    override fun getProfileUrl(accessToken: OAuth2AccessToken): String =
            "https://login.yandex.ru/info?format=json&oauth_token=" + accessToken.accessToken

}


class YandexProfile() : OAuth20Profile() {

    constructor(id: String, attributes: Map<String, Any>) : this() {
        setId(id)
        clientName = "YandexProfile"
        for ((k, v) in attributes) {
            addAttribute(k, v)
        }
    }

    override fun getAttributesDefinition(): AttributesDefinition = YandexAttributesDefinition

    override fun getEmail(): String = getAttribute("display_name", String::class.java)
}

object YandexAttributesDefinition : AttributesDefinition() {

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
        primary(emails, JsonListConverter(String::class.java, Array<String>::class.java))
        primary(default_email, Converters.STRING)
        primary(real_name, Converters.STRING)
        primary(birthday, Converters.STRING)
        primary(login, Converters.STRING)
        primary(sex, Converters.STRING)
        primary(id, Converters.STRING)
    }


}

private object YandexApi : DefaultApi20() {

    private val AUTHORIZE_URL = "https://oauth.yandex.ru/authorize?response_type=code&client_id=%s"

    override fun getAuthorizationUrl(config: OAuthConfig): String {
        return String.format(AUTHORIZE_URL, config.apiKey)
    }

    override fun getAccessTokenEndpoint(): String = "https://oauth.yandex.ru/token"

}