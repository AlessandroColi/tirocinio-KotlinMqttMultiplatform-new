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
            println( "Setting up channel for entity $source to $destination" )
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
            println( "Writing message ${message.decodeToString()} to topic $topic" ) //todo go back to message

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
            println( "Reading from topic $candidateTopic" )
            channel.asSharedFlow()
        }
        actual override suspend fun initialize(): Either<ProtocolError, Unit> = either {
            Either.catch {
                println("entering init")
                @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                mqtt = require("mqtt") as MqttJs

                val options = js("{}")
                options.username = this@MqttProtocol.username
                options.password = this@MqttProtocol.password


                client = mqtt.connect("mqtt://${host}:${port}", options = options)
                client.on("connect") { _ ->
                    println("client initialized")
                    client.subscribe("${mainTopic}/#",
                        options = js("{ qos: 2 }")
                    ) { err: dynamic, granted: dynamic ->
                        if (!err as Boolean) {
                            println( "Subscribed to topic: ${granted.topic}" )
                        } else {
                            println( "Subscribe did not work: ${err.message}" )
                        }
                    }
                }

                client.on("error") { error: dynamic ->
                    println("Error connecting to MQTT broker: ${error.message}")
                }
                client.on("message"){
                        (topic: String, message: dynamic, _) ->
                    val msg = message.toString()
                    println("Received message on topic $topic: $msg")
                    topicChannels[topic as String]?.tryEmit(msg.encodeToByteArray())
                }
            }
        }

        actual override fun finalize(): Either<ProtocolError, Unit> {
            client.end( options = object {
                val reasonCode = 0x00
            })
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
