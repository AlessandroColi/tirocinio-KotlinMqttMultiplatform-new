package it.unibo.comunication

import arrow.core.Either
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

/**
 * Represents the MQTT protocol.
 */
actual class MqttProtocol actual constructor(
    host: String,
    port: Int,
    username: String?,
    password: String?,
    mainTopic: String,
    coroutineDispatcher: CoroutineDispatcher
) : Protocol {
    actual override suspend fun setupChannel(
        source: Entity,
        destination: Entity
    ) {
    }

    actual override suspend fun writeToChannel(
        from: Entity,
        to: Entity,
        message: ByteArray
    ): Either<ProtocolError, Unit> {
        TODO("Not yet implemented")
    }

    actual override fun readFromChannel(
        from: Entity,
        to: Entity
    ): Either<ProtocolError, Flow<ByteArray>> {
        TODO("Not yet implemented")
    }

    actual override suspend fun initialize(): Either<ProtocolError, Unit> {
        TODO("Not yet implemented")
    }

    actual override fun finalize(): Either<ProtocolError, Unit> {
        TODO("Not yet implemented")
    }


}