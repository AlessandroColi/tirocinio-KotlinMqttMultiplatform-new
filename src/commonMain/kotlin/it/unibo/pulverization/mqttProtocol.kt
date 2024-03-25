package it.unibo.pulverization

import MQTTClient
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import mqtt.MQTTVersion
import mqtt.packets.Qos
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asSharedFlow
import mqtt.Subscription
import mqtt.packets.mqttv5.MQTT5Properties
import mqtt.packets.mqttv5.ReasonCode
import mqtt.packets.mqttv5.SubscriptionOptions

/**
 * Represents the MQTT protocol.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class MqttProtocol(
    private val host: String = "localhost",
    private val port: Int = 1884,
    private val username: String? = null,
    private val password: String? = null,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : Protocol {
    private val logger = KotlinLogging.logger("MqttProtocol")
    private val scope = CoroutineScope(coroutineDispatcher + Job())

    private val registeredTopics = mutableMapOf<Pair<Entity, Entity>, String>()
    private val topicChannels = mutableMapOf<String, MutableSharedFlow<ByteArray>>()
    private lateinit var listenerJob: Job
    private lateinit var client : MQTTClient
    override suspend fun setupChannel(source: Entity, destination: Entity) {
        logger.debug { "Setting up channel for entity $source" }
        registeredTopics += (source to destination) to toTopics(source, destination)
        registeredTopics += (destination to source) to toTopics(destination, source)
        topicChannels += toTopics(source, destination) to MutableSharedFlow(1)
        topicChannels += toTopics(destination, source) to MutableSharedFlow(1)
    }

    override suspend fun writeToChannel(from: Entity, to: Entity, message: ByteArray): Either<ProtocolError, Unit> = coroutineScope {
        either {
            val topic = registeredTopics[Pair(from, to)]
            logger.debug { "Writing message $message to topic $topic" }

            ensureNotNull(topic) { ProtocolError.EntityNotRegistered(to) }

                Either.catch { client.publish(
                    retain = true,
                    Qos.EXACTLY_ONCE,
                    topic,
                    message.toUByteArray(),
                    MQTT5Properties(
                        serverKeepAlive = 5000u,
                        retainAvailable = 1u,
                    )
                )
                }.mapLeft { ProtocolError.ProtocolException(it) }
        }
    }

    override fun readFromChannel(from: Entity, to: Entity): Either<ProtocolError, Flow<ByteArray>> = either {
        val candidateTopic = ensureNotNull(registeredTopics[Pair(from, to)]) { ProtocolError.EntityNotRegistered(from) }
        val channel = ensureNotNull(topicChannels[candidateTopic]) { ProtocolError.EntityNotRegistered(from) }
        logger.debug { "Reading from topic $candidateTopic" }
        channel.asSharedFlow()
    }

    suspend fun initialize(): Either<ProtocolError, Unit> = coroutineScope {
        either {
            Either.catch {
                client = MQTTClient(
                    MQTTVersion.MQTT5,
                    host,
                    port,
                    tls = null,
                    userName = username,
                    password = password?.encodeToByteArray()?.toUByteArray(),
                    keepAlive = 5000,
                    cleanStart = false,
                ){
                    logger.debug { "New message arrived on topic $it.topicName" }
                    requireNotNull(it.payload) { "Message cannot be null" }
                    topicChannels[it.topicName]?.tryEmit(it.payload!!.toByteArray())
                }

                client.subscribe(listOf(
                    Subscription("MqttProtocol_Test/#",
                        SubscriptionOptions(qos = Qos.EXACTLY_ONCE))))

                while(!client.connackReceived){
                    delay(50)  // avoid blocking the cpu
                    client.step()
                }

            }.mapLeft { ProtocolError.ProtocolException(it) }.bind()

            listenerJob = scope.launch {
                logger.debug { "client setup" }
                client.run()
            }
        }
    }

    fun finalize(): Either<ProtocolError, Unit>  {
        client.disconnect(ReasonCode.SUCCESS)
        scope.coroutineContext.cancelChildren()
        logger.debug { "client finalized" }
        return Unit.right()
    }


    /**
     * Creates the MQTT topic for the given entity.
     * @return the topic where the entity will receive messages.
     */
    private fun toTopics(source: Entity, destination: Entity): String {
        return if (source.id != null && destination.id != null) {
            "MqttProtocol_Test/${source.entityName}/${destination.entityName}/${destination.id}"
        } else {
            "MqttProtocol_Test/${source.entityName}/${destination.entityName}"
        }
    }
}