package ru.stankin.mj.utils

import org.apache.logging.log4j.LogManager
import org.jboss.resteasy.spi.BadRequestException
import org.jboss.resteasy.spi.HttpRequest
import org.jboss.resteasy.spi.validation.GeneralValidator
import ru.stankin.mj.utils.restutils.uriBuilder
import java.lang.reflect.Method
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.FormParam
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ContextResolver
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.kotlinFunction

/**
 * Created by nickl on 21.03.17.
 */

class KotlinRestValidator : ContextResolver<GeneralValidator> {
    override fun getContext(type: Class<*>?): GeneralValidator = NullValidator()
}

class NullValidator : GeneralValidator {

    private val log = LogManager.getLogger(NullValidator::class.java)

    override fun validate(request: HttpRequest?, `object`: Any?, vararg groups: Class<*>?) {
    }

    override fun isMethodValidatable(method: Method?): Boolean {
        log.debug("isMethodValidatable: $method")
        return true
    }

    override fun checkViolations(request: HttpRequest?) {
    }

    override fun validateAllParameters(request: HttpRequest, `object`: Any, method: Method, parameterValues: Array<out Any?>, vararg groups: Class<*>?) {
        for ((i, param) in (method.kotlinFunction?.valueParameters ?: emptyList()).withIndex()) {
            if (!param.type.isMarkedNullable && parameterValues[i] == null) {
                val paramname = param.findAnnotation<QueryParam>()?.value
                        ?: param.findAnnotation<FormParam>()?.value
                        ?: param.name
                        ?: "param$i"

                throw IllegalRestParameterNullability(paramname)
            }
        }
    }

    override fun validateReturnValue(request: HttpRequest?, `object`: Any?, method: Method?, returnValue: Any?, vararg groups: Class<*>?) {

    }

    override fun isValidatable(clazz: Class<*>?): Boolean {
        log.debug("isValidatable: $clazz ")
        return true
    }
}

class IllegalRestParameterNullability(val paramName: String) : BadRequestException("param '$paramName' cant be null")

class RedirectAwareBadRequestMapper : ExceptionMapper<BadRequestException> {

    private val log = LogManager.getLogger(RedirectAwareBadRequestMapper::class.java)

    @Context
    private lateinit var request: HttpServletRequest;

    override fun toResponse(cause: BadRequestException): Response? {
        log.debug("RedirectAwareBadRequestMapper: $cause")

        val redirect = request.getParameter("redirect_uri")
        return if (redirect != null)
            Response.temporaryRedirect(uriBuilder(redirect) {
                queryParam("error", cause.message)
            }).build()
        else {
            Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN_TYPE)
                    .entity(cause.message)
                    .build()
        }
    }
}