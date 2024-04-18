package it.unibo.comunication

import arrow.core.Either

/**
 * Represents something that needs to be initialized and finalized.
 */
interface Initializable {

    /**
     * Performs the operations needed before using the object
     */
    suspend fun initialize(): Either<ProtocolError, Unit>
    /**
     * Performs the closing operations on the object
     */
    fun finalize(): Either<ProtocolError, Unit>
}