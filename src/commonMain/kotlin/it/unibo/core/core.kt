package it.unibo.core

import arrow.core.right
import it.unibo.comunication.Entity
import it.unibo.comunication.MqttProtocol
import it.unibo.comunication.Protocol
import it.unibo.gui.gui

class core {

    private lateinit var gui: gui
    private lateinit var mqtt: Protocol
    private val source = Entity("from")
    private val dest = Entity("to")

    private suspend fun init(){
        mqtt.initialize()
        mqtt.setupChannel(source,dest)
//        TODO init gui
    }

    private fun run(){
    }

    suspend fun main(){
        init()
        run()
    }

}