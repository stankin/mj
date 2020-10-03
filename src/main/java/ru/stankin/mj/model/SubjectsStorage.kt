package ru.stankin.mj.model

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import org.sql2o.Sql2o
import ru.stankin.mj.utils.ThreadLocalTransaction
import java.util.*
import java.util.concurrent.TimeUnit
import javax.enterprise.inject.Default
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by nickl on 22.01.17.
 */
@Singleton
class SubjectsStorage @Inject constructor(private val sql2o: Sql2o) {

    private val subjectsCacheById = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .build<Int, Subject>(CacheLoader.from { id ->
                sql2o.open(ThreadLocalTransaction.get()).use { connection ->
                    connection
                            .createQuery("SELECT * FROM subjects WHERE subjects.id = :id")
                            .addParameter("id", id)
                            .throwOnMappingFailure(false)
                            .executeAndFetchFirst(Subject::class.java) ?: throw NoSuchElementException("no subject for id ${id}")

                }
            })

    fun byId(id: Int): Subject = subjectsCacheById.get(id)

    fun loadSubject(module: Module) = module.apply { subject = subjectsCacheById.get(this.subjectId) }

    private data class SubjData(val semester: String, val group: String, val name: String, val factor: Double)

    private val subjectsCacheBySubjData = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .build<SubjData, Subject>(CacheLoader.from { inf ->  getOrCreateSubject(inf!!)})

    fun persistedSubject(un: Subject) = subjectsCacheBySubjData.get(SubjData(un.semester, un.stgroup, un.title, un.factor))

    private fun getOrCreateSubject(subjData: SubjData): Subject {

        sql2o.beginTransaction().use { connection ->
            val subject = connection.createQuery("SELECT * FROM subjects WHERE semester = :semester AND stgroup = :group AND subjects.title = :title LIMIT 1")
                    .addParameter("semester", subjData.semester)
                    .addParameter("group", subjData.group)
                    .addParameter("title", subjData.name)
                    .executeAndFetchFirst(Subject::class.java)
            if (subject != null && Math.abs(subject.factor - subjData.factor) < 0.001) {
                return subject
            } else if (subject == null) {
                val id = connection
                        .createQuery("INSERT INTO subjects (factor, stgroup, title, semester) VALUES (:factor, :group, :title, :semester)")
                        .addParameter("semester", subjData.semester)
                        .addParameter("group", subjData.group)
                        .addParameter("title", subjData.name)
                        .addParameter("factor", subjData.factor)
                        .executeUpdate().getKey<Int>(Int::class.java)
                connection.commit()
                return Subject(id, subjData.semester, subjData.group, subjData.name, subjData.factor)
            } else {
                connection
                        .createQuery("UPDATE subjects SET factor = :factor WHERE id = :id")
                        .addParameter("factor", subjData.factor)
                        .addParameter("id", subject.id)
                        .executeUpdate()
                subject.factor = subjData.factor
                connection.commit()
                return subject
            }
        }
    }

}