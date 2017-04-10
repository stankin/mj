package ru.stankin.mj.model

import org.apache.logging.log4j.LogManager
import org.sql2o.Sql2o
import ru.stankin.mj.utils.ThreadLocalTransaction.tlTransaction
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by nickl on 08.04.17.
 */
@Singleton
class ModulesNotificator @Inject constructor(private val sql2o: Sql2o, private val modulesStorage: ModulesStorage) {

    private val logger = LogManager.getLogger(ModulesNotificator::class.java)

    private val timer: Timer

    init {
        logger.debug("created")
        timer = Timer("modules-notificator-timer", true)
    }


    @Volatile
    private var scheduled: NotificationTask? = null

    var notificationDelay = 30 * 60 * 60 * 1000L

    @Synchronized
    fun onNewModules(@Observes transaction: Transaction) {
        logger.debug("gotTransaction $transaction")
        scheduled?.cancel()
        scheduled = NotificationTask(transaction)
        timer.schedule(scheduled, notificationDelay)
    }


    private inner class NotificationTask(val transaction: Transaction) : TimerTask() {

        override fun run() {
            if (cancelled)
                return

            try {

                sql2o.tlTransaction { connection ->
                    connection.createQuery("""INSERT INTO lasttransactionnotifications(user_id, lastnotifiedtransaction)
  SELECT id,:prev  FROM users LEFT JOIN lasttransactionnotifications
      ON users.id = user_id WHERE lastnotifiedtransaction ISNULL ;""")
                            .addParameter("prev", transaction.id - 1)
                            .executeUpdate()
                            .commit()
                }


                val studentsToNotify = sql2o.open().use { connection ->
                    connection.createQuery("""SELECT DISTINCT student_id,lastnotifiedtransaction FROM moduleshistory LEFT JOIN lasttransactionnotifications
    ON student_id = user_id
WHERE transaction > lastnotifiedtransaction and transaction <= :current""")
                            .addParameter("current", transaction.id)
                            .executeAndFetch(LastNotificationInfo::class.java)
                }


                for ((studentId, lastnotifiedtransaction) in studentsToNotify) {
                    modulesStorage.getStudentModulesChangesFromRange(studentId, lastnotifiedtransaction, transaction.id)
                }



            } catch (e: Exception) {
                logger.error("error notifying transaction $transaction", e)
            }


        }

        @Volatile
        private var cancelled: Boolean = false

        override fun cancel(): Boolean {
            cancelled = true
            return super.cancel()
        }


    }

    private data class LastNotificationInfo(val student_id:Long,val lastnotifiedtransaction:Long)

}