package ru.stankin.mj.model

/**
 * Created by nickl on 16.01.17.
 */
data class ModulesUpdateStat(@JvmField var added: Int, @JvmField var updated: Int, @JvmField var deleted: Int) {

    operator fun plus(o: ModulesUpdateStat): ModulesUpdateStat = ModulesUpdateStat(added + o.added, updated + o.updated, deleted + o.deleted)

    operator fun plusAssign(o: ModulesUpdateStat): Unit {
        added += o.added
        updated += o.updated
        deleted += o.deleted
    }

}