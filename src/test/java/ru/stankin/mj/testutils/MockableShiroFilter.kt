package ru.stankin.mj.testutils

import org.apache.logging.log4j.LogManager
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.subject.SimplePrincipalCollection
import org.apache.shiro.subject.support.SubjectThreadState
import org.apache.shiro.util.ThreadContext
import org.apache.shiro.web.mgt.WebSecurityManager
import org.apache.shiro.web.servlet.ShiroFilter
import org.apache.shiro.web.subject.WebSubject
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

class MockableShiroFilter : ShiroFilter() {

    private val log = LogManager.getLogger(MockableShiroFilter::class.java)

    override fun createSubject(request: ServletRequest?, response: ServletResponse?): WebSubject {
        log.debug("create subject")

        if (mockedPrincipals == null)
            return super.createSubject(request, response)

        val delegatingSubject = WebSubject.Builder(getSecurityManager(), request, response)
                .authenticated(true)
                .principals(mockedPrincipals)
                .buildSubject()

        return delegatingSubject as WebSubject

    }

    companion object {
        private var mockedPrincipals: PrincipalCollection? = null


        fun <T> runAs(vararg principals: Any, f: () -> T): T = runAs(SimplePrincipalCollection(principals.asList(), "MockableShiroFilter"), f)

        fun <T> runAs(principals: PrincipalCollection, f: () -> T): T {

            val prev = mockedPrincipals
            mockedPrincipals = principals

            try {
                return f()
            } finally {
                mockedPrincipals = prev
            }


        }


    }

}