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
    import kotlin.random.Random

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
        private lateinit var client: MqttClient

        actual override suspend fun setupChannel(
            source: Entity,
            destination: Entity
        ) {
            logger.debug { "-Setting up channel for entity $source to $destination" }
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
            logger.debug {"-Writing message ${message.decodeToString()} to topic $topic" } //todo go back to message

            ensureNotNull(topic) { ProtocolError.EntityNotRegistered(to) }

            Either.catch { client.publish(
                topic,
                message.decodeToString(),
                options = js("{ qos: 2, retain: true }")

            )   }.mapLeft { ProtocolError.ProtocolException(it) }
        }

        actual override fun readFromChannel(
            from: Entity,
            to: Entity
        ): Either<ProtocolError, Flow<ByteArray>> = either {
            val candidateTopic = ensureNotNull(registeredTopics[Pair(from, to)])
                { ProtocolError.EntityNotRegistered(from) }
            val channel = ensureNotNull(topicChannels[candidateTopic])
                { ProtocolError.EntityNotRegistered(from) }
            logger.debug {"-Reading from topic $candidateTopic" }
            channel.asSharedFlow()
        }
        actual override suspend fun initialize(): Either<ProtocolError, Unit> = either {
            Either.catch {
                logger.debug {"-entering init"}
                val options = js("{" +
                        "  protocolId: 'MQTT'," +
                        "  protocolVersion: 5," +
                        "  clean: false," +
                        "}")
                options.username = this@MqttProtocol.username
                options.password = this@MqttProtocol.password
                options.clientId = Random.nextInt()
                logger.debug {"-attempting to connect"}
                client = connect("mqtt://${host}:${port}", options = options)

                logger.debug {"-waiting to connect"}

                client.on("connect") { _,_,_ ->
                    logger.debug {"-client initialized"}
                    client.subscribe("${mainTopic}/#",
                        options = js("{ qos: 2 }")
                    ) { err: dynamic, granted: dynamic ->
                        if (!err as Boolean) {
                            logger.debug {"-Subscribed to topic: ${granted.topic}" }
                        } else {
                            logger.debug {"-Subscribe did not work: ${err.message}" }
                        }
                    }
                }

                client.on("error") { error: dynamic ->
                    logger.debug {"-Error connecting to MQTT broker: ${error.message}"}
                }
                client.on("message"){
                        topic: String, payload: dynamic, _ ->
                    //payload is a js buffer, so it needs to be converted
                    val msg = ByteArray(payload.length as Int)
                    for (i in 0 until payload.length as Int) {
                        msg[i] = payload[i] as Byte
                    }

                    logger.debug {"-Received message on topic $topic: ${msg.decodeToString()}"}
                    topicChannels[topic]?.tryEmit(msg)
                }
            }
        }

        actual override fun finalize(): Either<ProtocolError, Unit> {
            client.end( force = true , options = js("{reasonCode : 0x00};"))
            logger.debug {"-client finalized" }
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
