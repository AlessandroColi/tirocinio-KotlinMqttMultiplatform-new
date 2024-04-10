package it.unibo.core

import arrow.core.Either
import arrow.core.right
import it.unibo.comunication.Entity
import it.unibo.comunication.MqttProtocol
import it.unibo.comunication.Protocol
import it.unibo.comunication.ProtocolError
import it.unibo.gui.SimpleGui
import it.unibo.gui.gui

class core {

    private val source = Entity("from")
    private val dest = Entity("to")
    private var gui: gui = SimpleGui()
    private var mqtt: Protocol = MqttProtocol()

    private suspend fun init(){
        mqtt.initialize()
        mqtt.setupChannel(source,dest)
    }

    private suspend fun run(){
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

    suspend fun main(){
        init()
        run()
    }

}