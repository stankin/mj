package ru.stankin.mj.model

import org.apache.logging.log4j.LogManager
import ru.stankin.mj.utils.stream
import java.io.InputStream
import java.util.*
import java.util.stream.Stream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import kotlin.coroutines.experimental.buildIterator

/**
 * Created by nickl on 09.03.17.
 */
object StudentsXML {

    val markTypes = mapOf(1 to "М1", 2 to "М2", 3 to "З", 5 to "Э", 4 to "К")

    private val log = LogManager.getLogger(StudentsXML::class.java)


    private operator fun ArrayDeque<Pair<String, Map<String, String>>>.get(elem: String): Map<String, String> {
        return find { it.first == elem }!!.second
    }

    fun readStudentsFromXML(input: InputStream, semester: String): Stream<Student> {

        val factory = XMLInputFactory.newInstance();
        val reader =
                factory.createXMLStreamReader(input);

        data class SubjData(val group: String, val name: String, val factor: String)


        return stream(buildIterator {

            val ctxStack = ArrayDeque<Pair<String, Map<String, String>>>()

            val subjectsCache = HashMap<SubjData, Subject>()

            fun subject(name: String, factor: String): Subject? {
                val subjData = SubjData(ctxStack["group"]["name"]!!, name, factor)
                return subjectsCache.computeIfAbsent(subjData,
                        { subjData -> Subject(semester, subjData.group, subjData.name, subjData.factor.replace(',', '.').toDouble()) })

            }


            var curStudent: Student? = null;

            while (reader.hasNext()) {
                when (reader.next()) {
                    XMLStreamConstants.START_ELEMENT -> try {
                        val attributes = (0 until reader.attributeCount).associateBy({ reader.getAttributeLocalName(it) }, { reader.getAttributeValue(it) })
                        ctxStack.push(reader.localName to attributes)

                        when (reader.localName) {
                            "student" ->
                                curStudent = Student(
                                        attributes["stud_id"],
                                        ctxStack["group"]["name"],
                                        attributes["surame"],
                                        attributes["firstname"],
                                        attributes["patronymic"]
                                )

                            "exam" ->
                                curStudent!!.modules.add(
                                        Module(subject(ctxStack["discipline"]["name"]!!, ctxStack["discipline"]["factor"]!!),
                                                curStudent.id,
                                                attributes["type"]?.toIntOrNull()?.let { markTypes[it] } ?: "",
                                                attributes["mark"]!!.toInt(),
                                                -1
                                        ))

                        }
                    } catch (e: Exception) {
                        throw RuntimeException("excetion processing ${reader.localName} for student ${curStudent?.cardid}", e)
                    }

                    XMLStreamConstants.END_ELEMENT -> {
                        ctxStack.pop()
                        when (reader.localName) {
                            "student" -> {
                                yield(curStudent!!)
                                curStudent = null
                            }
                        }
                    }

                }

            }


            log.debug("subjectsCache.size = ${subjectsCache.size}")

        }).onClose { reader.close() }

    }


}
