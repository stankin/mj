package ru.stankin.mj.rested.security

import com.fasterxml.jackson.databind.node.ArrayNode
import com.github.scribejava.core.model.OAuth2AccessToken
import org.pac4j.oauth.client.VkClient
import org.pac4j.oauth.profile.JsonHelper
import org.pac4j.oauth.profile.vk.VkProfile

class MyVkClient(key: String, secret: String) : VkClient(key, secret) {

    init {
        name = "VkClient"
    }

    override fun getProfileUrl(accessToken: OAuth2AccessToken?): String = super.getProfileUrl(accessToken) + "&v=5.73"

    override fun extractUserProfile(body: String?): VkProfile = VkProfile().apply {
        JsonHelper.getFirstNode(body)?.let { json ->
            val array = json.get("response") as ArrayNode
            val userNode = array.get(0)
            setId(JsonHelper.getElement(userNode, "id"))
            for (attribute in attributesDefinition.primaryAttributes) {
                addAttribute(attribute, JsonHelper.getElement(userNode, attribute))
            }
        }
    }
}