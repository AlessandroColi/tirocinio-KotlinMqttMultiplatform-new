package it.unibo.comunication

import arrow.core.Either
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * Represents the MQTT protocol.
 */
expect class MqttProtocol(
    host: String = "localhost",
    port: Int = 1884,
    username: String? = null,
    password: String? = null,
    mainTopic: String = "MqttProtocol_Test",
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) : Protocol {

    override suspend fun setupChannel(source: Entity, destination: Entity)
    override suspend fun writeToChannel(
        from: Entity,
        to: Entity,
        message: ByteArray
    ): Either<ProtocolError, Unit>

    override fun readFromChannel(
        from: Entity,
        to: Entity
    ): Either<ProtocolError, Flow<ByteArray>>

    override suspend fun initialize(): Either<ProtocolError, Unit>
    override fun finalize(): Either<ProtocolError, Unit>


}