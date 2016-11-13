package ru.stankin.mj.ru.stankin.mj.rested

import kotlinx.support.jdk7.use
import org.junit.Assert
import org.junit.Test
import org.sql2o.Sql2o
import ru.stankin.mj.rested.Student

import java.sql.DriverManager
import java.sql.Statement

/**
 * Created by nickl on 12.11.16.
 */


class JDBCToolsTest {



    @Test
    fun sql2oMapping() {

        val sql2o = Sql2o("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "", "")

        val students = sql2o.open().use { conn ->

            conn.jdbcConnection.createStatement().use {
                createStudentTable(it)
                fillStudentTable(it)
            }


            //language=SQL
            conn.createQuery("SELECT CARDID, NAME, SURNAME FROM STUDENT").executeAndFetch(Student::class.java)
        }

        println("students = " + students)


        Assert.assertEquals(3, students.size)

    }



    private fun fillStudentTable(it: Statement): Boolean {
        return it.execute("""

    INSERT INTO PUBLIC.STUDENT (ID, CARDID, INITIALS, NAME, PASSWORD, PATRONYM, STGROUP, SURNAME) VALUES (2, '114004', 'В.П.', 'Василий', '114004', 'Петрович', 'ИДБ-14-01', 'Семенов');
    INSERT INTO PUBLIC.STUDENT (ID, CARDID, INITIALS, NAME, PASSWORD, PATRONYM, STGROUP, SURNAME) VALUES (3, '414130', 'А.В.', 'Андрей', '1111', 'Васильевич', 'ИДБ-14-01', 'Чапаев');
    INSERT INTO PUBLIC.STUDENT (ID, CARDID, INITIALS, NAME, PASSWORD, PATRONYM, STGROUP, SURNAME) VALUES (4, '114043', 'А.М.', 'Александр', '1111', 'Михайллович', 'ИДБ-14-01', 'Серов');

    """)
    }

    private fun createStudentTable(it: Statement) {
        it.execute("""
    CREATE TABLE STUDENT
    (
        ID INTEGER PRIMARY KEY NOT NULL,
        CARDID VARCHAR(255),
        INITIALS VARCHAR(255),
        NAME VARCHAR(255),
        PASSWORD VARCHAR(255),
        PATRONYM VARCHAR(255),
        STGROUP VARCHAR(255),
        SURNAME VARCHAR(255)
    );
    CREATE UNIQUE INDEX UK_TEW6YKYN1TXTJB9XF8XP1WTFT_INDEX_B ON STUDENT (CARDID);
    CREATE INDEX UK_TEW6YKYN1TXTJB9XF8XP1WTFT ON STUDENT (CARDID);
    CREATE INDEX UK_QYLFRW82TUIGT0BUGFXCERN8U ON STUDENT (STGROUP, SURNAME, INITIALS);""")
    }


}

