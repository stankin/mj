package ru.stankin.mj.utils.restutils

import java.net.URI
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

/**
 * Created by nickl on 15.03.17.
 */


fun uriBuilder(uri: URI, config: UriBuilder.() -> Unit): URI = withBuilder(UriBuilder.fromUri(uri), config)

fun uriBuilder(uri: String, config: UriBuilder.() -> Unit): URI = withBuilder(UriBuilder.fromUri(uri), config)

fun redirect(uri: String, config: UriBuilder.() -> Unit) = Response.temporaryRedirect(uriBuilder(uri, config)).build()

inline fun withBuilder(builder: UriBuilder, config: UriBuilder.() -> Unit) = builder.apply { config() }.build()

fun HttpServletRequest.baseRedirect(path: String) = Response.temporaryRedirect(basedUrl(path)).build()

fun HttpServletRequest.baseRedirect(path: String, config: UriBuilder.() -> Unit) =
        Response.temporaryRedirect(uriBuilder(basedUrl(path), config)).build()

fun HttpServletRequest.basedUrl(path: String) = URI(requestURL.removeSuffix(requestURI).toString() + contextPath + path)


fun badRequest(message: String): Response = responseMessage(Response.Status.BAD_REQUEST, message)

fun responseMessage(status: Response.Status, message: String) = Response.status(status).entity(mapOf("message" to message)).build()

