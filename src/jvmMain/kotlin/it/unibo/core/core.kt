package it.unibo.core

import it.unibo.comunication.Entity
import it.unibo.comunication.MqttProtocol
import it.unibo.comunication.Protocol
import it.unibo.comunication.ProtocolError
import it.unibo.gui.SimpleGui
import it.unibo.gui.Gui
import kotlinx.coroutines.*

/**
 * runs the simple program to monitor the mqtt comms.
 */
class Core {

    private val source = Entity("esp32")
    private val dest = Entity("backend")
    private var gui: Gui = SimpleGui()
    private var mqtt: Protocol = MqttProtocol("test.mosquitto.org",1883, mainTopic = "RiverMonitoring")

    /**
     * initialize the components, to be called before [run].
     */
    suspend fun init(){
        mqtt.initialize()
        mqtt.setupChannel(source,dest)
    }

    /**
     * executes indefinitely the mqtt comms listener.
     */
    suspend fun run(){
        val res = mqtt.readFromChannel(source, dest)
        res.onLeft { protocolError ->
            when (protocolError) {
                is ProtocolError.EntityNotRegistered ->
                    throw IllegalArgumentException("Entity not registered: ${protocolError.entity}")
                is ProtocolError.ProtocolException ->
                    throw protocolError.exception
            }
        }
        res.onRight { flow ->
            flow.collect { msg ->
                gui.write("current distance: " + msg.map { it.toInt().toChar() }.joinToString(""))
            }
        }
    }
}

fun main() {
    val app = Core()
    println("launching app")
    runBlocking {
        app.init()
        app.run()
    }

    println("leaving app")
}