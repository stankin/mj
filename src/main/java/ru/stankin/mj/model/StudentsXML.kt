package ru.stankin.mj.model

import org.apache.logging.log4j.LogManager
import ru.stankin.mj.utils.stream
import java.io.InputStream
import java.util.*
import java.util.stream.Stream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

/**
 * Created by nickl on 09.03.17.
 */
object StudentsXML {
    val markTypes = mapOf(1 to "М1", 2 to "М2", 3 to "З", 5 to "Э", 4 to "К")
    private val log = LogManager.getLogger(StudentsXML::class.java)
    private fun markColor(attributes: Map<String, String>): Int {
        when (attributes["P"]) {
            "1" -> return 0x0000ff
        }
        when (attributes["status"]) {
            "0" -> return -1
            "1" -> return Module.PURPLE_MODULE
            "2" -> return Module.YELLOW_MODULE
        }
        return -1;
    }

    private operator fun ArrayDeque<Pair<String, Map<String, String>>>.get(elem: String): Map<String, String> {
        return find { it.first == elem }!!.second
    }

    private fun String.intRounded(): Int? = replace(',', '.').toFloatOrNull()?.let { Math.round(it) }
    fun readStudentsFromXML(input: InputStream): Stream<Student> {
        val factory = XMLInputFactory.newInstance();
        val reader =
                factory.createXMLStreamReader(input);
        data class SubjData(val group: String, val name: String, val factor: String)
        return stream(iterator {
            val ctxStack = ArrayDeque<Pair<String, Map<String, String>>>()
            val subjectsCache = HashMap<SubjData, Subject>()
            fun subject(name: String, factor: String): Subject {
                val subjData = SubjData(ctxStack["group"]["name"]!!, name, factor)
                val semester = ctxStack["sem"]["title"]
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
                                                attributes["mark"]!!.substringBefore(',').toInt(),
                                                markColor(attributes)
                                        ))
                        }
                    } catch (e: Exception) {
                        throw RuntimeException("exception processing ${reader.localName} for student ${curStudent?.cardid}", e)
                    }
                    XMLStreamConstants.END_ELEMENT -> {
                        when (reader.localName) {
                            "student" -> {
                                curStudent!!.modules.apply {
                                    add(Module(subject(MarksWorkbookReader.RATING, "0.0"), curStudent!!.id, "М1", ctxStack["student"]["rating"]?.intRounded() ?: 0, -1))
                                    add(Module(subject(MarksWorkbookReader.ACCOUMULATED_RATING, "0.0"), curStudent!!.id, "М1", ctxStack["student"]["accumRating"]?.intRounded() ?: 0, -1))
                                }
                                yield(curStudent)
                                curStudent = null
                            }
                        }
                        ctxStack.pop()
                    }
                }
            }
            log.debug("subjectsCache.size = ${subjectsCache.size}")
        }).onClose { reader.close() }
    }
}
