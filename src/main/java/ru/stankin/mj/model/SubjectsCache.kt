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

//open class PersistingSubjectsCache constructor(val persist: (data: SubjData) -> Subject) {
//
//    //constructor(loader:  (String, String, String, Double) -> Subject) : this({e -> loader(e.semester, e.group, e.name, e.factor)})
//
//    private val moduleSubjectCache = HashMap<SubjData, Subject>()
//
//
//    @Synchronized
//    fun persisted(semester: String, group: String, name: String, factor: Double): Subject {
//        val data = SubjData(semester, group, name, factor)
//        return moduleSubjectCache.computeIfAbsent(data, persist)
//    }
//
//    fun persisted(unsafeSubject: Subject) = persisted(unsafeSubject.getSemester(),
//            unsafeSubject.getStgroup(),
//            unsafeSubject.getTitle(), unsafeSubject.getFactor())
//
//
//    data class SubjData(val semester: String, val group: String, val name: String, val factor: Double)
//
//}