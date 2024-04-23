package it.unibo.comunication

/**
 * js function to import a library, only used for mqtt.js in this project.
 */
external fun require(module: String): MqttJs

/**
 * represents a mqtt client.
 */
external interface MqttJsClient {
    /**
     * setups what to do on cenrtain events.
     * @param event the trigger for the callback function (message,connect,...).
     * @param callback the function to be executed after the given event.
     */
    fun on(event: String, callback: (String, dynamic, dynamic) -> Unit)

    /**
     * subscribe to the given topic.
     * @param topic the topic.
     * @param options the message options (qous, retain, ...).
     * @param callback the funtion to be executed upon disconnect and connect.
     */
    fun subscribe(topic: String, options: dynamic, callback: (err: dynamic, granted:dynamic) -> Unit)

    /**
     * publish a message on the given topic.
     * @param topic the topic.
     * @param message the message.
     * @param options the message options (qous, retain, ...).
     */
    fun publishAsync(topic: String, message: dynamic,
                options: dynamic  = definedExternally )

    /**
     * closes the client.
     */
    fun end()
}

/**
 * connection options for the mqtt broker.
 */
external interface MqttJsOptions {
    /**
     * the username of the client.
     */
    var username: String

    /**
     * the password.
     */
    var password: String

    /**
     * cleanstart of the connection.
     */
    var clean : Boolean
}

/**
 * represents the connection to the mqtt broker in js.
 */
external interface MqttJs {
    /**
     * connect to the broker.
     * @return the client [MqttJsClient].
     */
    fun connect(url: String, port: Int,  options: dynamic = definedExternally): MqttJsClient
}

