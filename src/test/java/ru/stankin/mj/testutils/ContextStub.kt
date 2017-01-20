package ru.stankin.mj.testutils

import java.util.*
import javax.naming.*

/**
 * Created by nickl on 13.01.17.
 */
class ContextStub(var src: MutableMap<String, Any>):Context {

    constructor():this(mutableMapOf())

    override fun listBindings(name: Name?): NamingEnumeration<Binding> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun listBindings(name: String?): NamingEnumeration<Binding> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun destroySubcontext(name: Name?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun destroySubcontext(name: String?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun list(name: Name?): NamingEnumeration<NameClassPair> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun list(name: String?): NamingEnumeration<NameClassPair> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun composeName(name: Name?, prefix: Name?): Name {
        throw UnsupportedOperationException("not implemented")
    }

    override fun composeName(name: String?, prefix: String?): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun createSubcontext(name: Name?): Context {
        throw UnsupportedOperationException("not implemented")
    }

    override fun createSubcontext(name: String?): Context {
        throw UnsupportedOperationException("not implemented")
    }

    override fun rename(oldName: Name?, newName: Name?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun rename(oldName: String?, newName: String?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun addToEnvironment(propName: String?, propVal: Any?): Any {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getNameParser(name: Name?): NameParser {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getNameParser(name: String?): NameParser {
        throw UnsupportedOperationException("not implemented")
    }

    override fun close() {
        throw UnsupportedOperationException("not implemented")
    }

    override fun lookup(name: Name?): Any {
        throw UnsupportedOperationException("not implemented")
    }

    override fun lookup(name: String?): Any = src[name]?: throw NoSuchElementException(name)


    override fun getNameInNamespace(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun rebind(name: Name?, obj: Any?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun rebind(name: String?, obj: Any?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun unbind(name: Name?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun unbind(name: String?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getEnvironment(): Hashtable<*, *> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun bind(name: Name?, obj: Any?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun bind(name: String?, obj: Any?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun removeFromEnvironment(propName: String?): Any {
        throw UnsupportedOperationException("not implemented")
    }

    override fun lookupLink(name: Name?): Any {
        throw UnsupportedOperationException("not implemented")
    }

    override fun lookupLink(name: String?): Any {
        throw UnsupportedOperationException("not implemented")
    }
}