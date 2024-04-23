package it.unibo.comunication

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Represents the MQTT protocol.
 */
@Suppress("unused")

actual class MqttProtocol actual constructor(
    private val host: String,
    private val port: Int,
    private val username: String?,
    private val password: String?,
    private val mainTopic: String,
    private val coroutineDispatcher: CoroutineDispatcher
) : Protocol {

    private val logger = KotlinLogging.logger("MqttProtocol")

    private val registeredTopics = mutableMapOf<Pair<Entity, Entity>, String>()
    private val topicChannels = mutableMapOf<String, MutableSharedFlow<ByteArray>>()
    private lateinit var client : MqttJsClient
    private lateinit var mqtt : MqttJs

    actual override suspend fun setupChannel(
        source: Entity,
        destination: Entity
    ) {
        println( "Setting up channel for entity $source" )
        registeredTopics += (source to destination) to toTopics(source, destination)
        registeredTopics += (destination to source) to toTopics(destination, source)
        topicChannels += toTopics(source, destination) to MutableSharedFlow(1)
        topicChannels += toTopics(destination, source) to MutableSharedFlow(1)

    }

    actual override suspend fun writeToChannel(
        from: Entity,
        to: Entity,
        message: ByteArray
    ): Either<ProtocolError, Unit>  = either {
        val topic = registeredTopics[Pair(from, to)]
        println( "Writing message $message to topic $topic" )

        ensureNotNull(topic) { ProtocolError.EntityNotRegistered(to) }

        Either.catch { client.publishAsync(
            topic,
            message,
            options = object {
                var qos = 2
                var retain = true
            }
        )
        }.mapLeft { ProtocolError.ProtocolException(it) }
    }

    actual override fun readFromChannel(
        from: Entity,
        to: Entity
    ): Either<ProtocolError, Flow<ByteArray>> = either {
        val candidateTopic = ensureNotNull(registeredTopics[Pair(from, to)]) { ProtocolError.EntityNotRegistered(from) }
        val channel = ensureNotNull(topicChannels[candidateTopic]) { ProtocolError.EntityNotRegistered(from) }
        println( "Reading from topic $candidateTopic" )
        channel.asSharedFlow()
    }
    actual override suspend fun initialize(): Either<ProtocolError, Unit> = either {
        Either.catch {
            mqtt = require("mqtt")

            client = mqtt.connect(host, port, username?.takeIf { it.isNotEmpty() }?.let { safeUsername ->
                password?.takeIf { it.isNotEmpty() }?.let { safePassword ->
                    object : MqttJsOptions {
                        override var username: String = safeUsername
                        override var password: String = safePassword
                        override var clean: Boolean = false
                    }
                }
            })
            client.on("connect") { _,_,_ ->
                client.subscribe("${mainTopic}/#",
                    options = object {
                        val qos = 2
                    }) { err: dynamic, granted: dynamic ->
                    if (err != null) {
                        println( "disconnected: ${err.message}" )
                    } else {
                        println( "Subscribed to topic: ${granted.topic}" )
                    }
                }
            }
            //TODO forse vanno messi gli on() anche per le altre casistiche?
            client.on("message"){
                topic: String, message: dynamic, packet: dynamic ->
                logger.debug{"Received message on topic $topic: $message"}
                topicChannels[topic]?.tryEmit(packet.payload.toByteArray()) //va bene ?
            }
        }
    }

    actual override fun finalize(): Either<ProtocolError, Unit> {
        client.end()
        println( "client finalized" )
        return Unit.right()
    }

    //todo comune a tutte le piattaforme, la posso mettere in common?
    private fun toTopics(source: Entity, destination: Entity): String {
        return if (source.id != null && destination.id != null) {
            "${mainTopic}/${source.entityName}/${destination.entityName}/${destination.id}"
        } else {
            "${mainTopic}/${source.entityName}/${destination.entityName}"
        }
    }
}
