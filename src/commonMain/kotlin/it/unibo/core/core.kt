package it.unibo.core

import it.unibo.comunication.Entity
import it.unibo.comunication.MqttProtocol
import it.unibo.comunication.Protocol
import it.unibo.comunication.ProtocolError
import it.unibo.gui.SimpleGui
import it.unibo.gui.gui
import kotlinx.coroutines.*


class core {

    private val source = Entity("esp32")
    private val dest = Entity("backend")
    private var gui: gui = SimpleGui()
    private var mqtt: Protocol = MqttProtocol(host = "broker.mqtt-dashboard.com", mainTopic = "RiverMonitoring")

    suspend fun init(){
        mqtt.initialize()
        mqtt.setupChannel(source,dest)
    }

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
    val app = core()
    println("launching app")
    runBlocking {
        app.init()
        app.run()
    }
    println("leaving app")
}
