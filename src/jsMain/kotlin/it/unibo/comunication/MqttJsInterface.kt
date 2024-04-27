@file:Suppress("REDUNDANT_NULLABLE")
package it.unibo.comunication
/**
 * js function to import a library, only used for mqtt.js in this project.
 */
external fun require(module: String): dynamic
/**
 * represents a mqtt client.
 */
external interface MqttJsClient {
    /**
     * publish a message on the given topic.
     * @param topic the topic.
     * @param message the message.
     * @param options the message options (qos, retain, ...)
     * @param callback the function to be called upon complete.
     */
    fun publish(
        topic: String,
        message: dynamic,
        options: dynamic? = definedExternally,
        callback: ((err:dynamic) -> Unit)? = definedExternally
    )
    /**
     * subscribe to the given topic.
     * @param topic the topic.
     * @param options the message options (qos, retain, ...).
     * @param callback the function to be executed upon disconnect and connect.
     */
    fun subscribe(topic: String, options: dynamic? = definedExternally,
                  callback: ((err: dynamic, granted: dynamic) -> Unit)? = definedExternally)
    /**
     * setups what to do on certain events.
     * @param event the trigger for the callback function (message,connect,...).
     * @param callback the function to be executed after the given event.
     */
    fun on(event: String, callback: (args: Array<dynamic>) -> Unit)
    /**
     * closes the client.
     * @param force the end needs to be forced or not
     * @param callback the function to be called upon complete.
     */
    fun end(force: Boolean? = definedExternally, callback: (() -> Unit)? = definedExternally)
}

/**
 * the connection to the broker.
 */
external interface MqttJs {
    /**
     * connect to the broker.
     * @return the client [MqttJsClient].
     */
    fun connect(host: String, options: dynamic? = definedExternally): MqttJsClient
}
