package ru.stankin.mj

/**
 * Created by nickl on 26.01.17.
 */
class UserActionException : Exception {
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(message: String) : super(message)
    constructor() : super()
}