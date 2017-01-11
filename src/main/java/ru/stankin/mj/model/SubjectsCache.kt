package ru.stankin.mj.model

import org.apache.logging.log4j.LogManager
import org.sql2o.Connection
import java.util.*


open class SubjectsCache(private val connection: Connection) {

    private val log = LogManager.getLogger(SubjectsCache::class.java);

    private val moduleSubjectCache = HashMap<Int, Subject>()
    private val subjectIdentityCache = HashMap<Int, Subject>()


    @Synchronized
    fun moduleSubject(moduleId: Int): Subject =
            moduleSubjectCache.computeIfAbsent(moduleId, { moduleId ->
                val subject0 = connection
                        .createQuery("SELECT * from subjects WHERE subjects.id in (SELECT subject_id FROM modules WHERE modules.id = :id)")
                        .addParameter("id", moduleId)
                        .throwOnMappingFailure(false)
                        .executeAndFetchFirst(Subject::class.java) ?: throw NoSuchElementException("no subject for module ${moduleId}")

                log.trace("loaded for moduleid {} subject is {}", moduleId, subject0)

                val subject = subjectIdentityCache.computeIfAbsent(subject0.id, { i -> subject0 })

                val allModules = connection
                        .createQuery("select id FROM modules WHERE subject_id = :id")
                        .addParameter("id", subject.id)
                        .executeScalarList(Int::class.java)

                for (m in allModules) {
                    moduleSubjectCache.put(m, subject)
                }

                subject
            })

    fun loadSubject(module: Module): Module {
        module.subject = moduleSubject(module.id)
        return module
    }


}