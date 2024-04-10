package it.unibo.comunication

import arrow.core.Either

interface Initializable {

    suspend fun initialize(): Either<ProtocolError, Unit>
    fun finalize(): Either<ProtocolError, Unit>
}
