package ru.stankin.mj.testutils

import org.jboss.shrinkwrap.api.ArchivePath
import org.jboss.shrinkwrap.api.Filter
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by nickl-mac on 01.04.17.
 */

fun isTestClass(arhivePath: String): Boolean {
    val urlClassLoader = (Thread.currentThread().contextClassLoader as? URLClassLoader) ?: return false
    return urlClassLoader.urLs.filter { it.path.contains("/target/test-classes") }.any {
        val path = it.path + arhivePath
        return Files.exists(Paths.get(path))
    }
}

fun notTests() = Filter<ArchivePath> { !isTestClass(it.get()) }