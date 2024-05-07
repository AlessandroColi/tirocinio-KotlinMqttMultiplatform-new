@file:Suppress("REDUNDANT_NULLABLE")
@file:JsModule("mqtt")
package it.unibo.comunication
/**
 * represents a mqtt client.
 */
external class MqttClient {
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
     * setups what to do on certain events, this is to be used for the message event.
     * @param event the trigger for the callback function (message,connect,...).
     * @param callback the function to be executed after the given event.
     */
    fun on(event: String, callback: (String, dynamic, dynamic) -> Unit)

    /**
     * setups what to do on certain events, this is to be used for the other events.
     * @param event the trigger for the callback function (message,connect,...).
     * @param callback the function to be executed after the given event.
     */
    fun on(event: String, callback: (dynamic) -> Unit)

    /**
     * closes the client.
     * @param force the end needs to be forced or not
     * @param callback the function to be called upon complete.
     */
    fun end(force: Boolean? = definedExternally, options: dynamic? = definedExternally,
            callback: (() -> Unit)? = definedExternally)
}

/**
 * connect to the broker.
 * @return the client [MqttClient].
 */
external fun connect(brokerUrl: String, options: dynamic? = definedExternally): MqttClient
