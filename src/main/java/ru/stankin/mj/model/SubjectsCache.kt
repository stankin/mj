package ru.stankin.mj.model

import org.apache.logging.log4j.LogManager
import org.sql2o.Connection
import java.util.*


open class SubjectsCache(private val connection: Connection) {

    private val log = LogManager.getLogger(SubjectsCache::class.java);

    private val moduleSubjectCache = HashMap<Int, Subject>()


    @Synchronized
    fun subject(subjectId: Int): Subject =
            moduleSubjectCache.computeIfAbsent(subjectId, { subjectId ->
                val subject0 = connection
                        .createQuery("SELECT * from subjects WHERE subjects.id = :id")
                        .addParameter("id", subjectId)
                        .throwOnMappingFailure(false)
                        .executeAndFetchFirst(Subject::class.java) ?: throw NoSuchElementException("no subject for id ${subjectId}")

                log.trace("loaded for subject id {} subject is {}", subjectId, subject0)

                subject0
            })

    fun loadSubject(module: Module): Module {
        module.subject = subject(module.subjectId)
        return module
    }


}