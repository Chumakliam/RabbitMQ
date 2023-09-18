package com.example.adarabbitmq01

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.rabbitmq.client.*


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.sse.*
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.CountDownLatch

class MainActivity : AppCompatActivity() {
    val oC_Client = OkHttpClient()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun C_SETxRabbitMQ() {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val oFactory = ConnectionFactory()
                    oFactory.host = "27.254.239.245"
                    oFactory.port = 5672
                    oFactory.username = "Admin"
                    oFactory.password = "Admin"
                    oFactory.virtualHost = "test"
                    val oConnection = oFactory.newConnection()
                    val nQueue = 5000

                    if (oConnection.isOpen) {
                        Log.d("Main", "Connected successfully!")
                        val oChannel = oConnection.createChannel()
                        oChannel.queueDeclare("testQueue", false, false, false, null)

                        val oLatch = CountDownLatch(nQueue)
                        val oConsumer = object : DefaultConsumer(oChannel) {
                            override fun handleDelivery(
                                consumerTag: String?,
                                envelope: Envelope?,
                                properties: AMQP.BasicProperties?,
                                body: ByteArray?
                            )
                            {
                                val tMessage = body?.toString(Charset.defaultCharset())

                                if (tMessage != null) {
                                Log.d("Main", "handleDelivery: "+tMessage.toString())
                                 tMessage
                                }
                                oLatch.countDown()
                            }
                        }
                        oChannel.basicConsume("testQueue", true, oConsumer)
                        for (nI in 1..nQueue) {
                            val tMessage = "$nI"
                            oChannel.basicPublish(
                                "",
                                "testQueue",
                                null,
                                tMessage.toByteArray(Charset.defaultCharset())
                            )
                        }
                        oLatch.await()
                        oChannel.close()
                        oConnection.close()
                    } else {
                        Log.d("Main", "Failed to connect!")
                    }
                } catch (e: Exception) {
                    Log.d("Main", "Exception")
                    e.printStackTrace()
                }
            }
        }
    }

    fun C_SETxConnectSSELoop() {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val oRequest = Request.Builder()
                    .url("https://postman-echo.com/server-events/3")
                    .build()

                val oListener = object : EventSourceListener() {
                    override fun onOpen(eventSource: EventSource, response: Response) {
                        Log.d("Main", "onOpen: ")
                    }

                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {
                        Log.d("Main", type+"  onEvent: "+data)
                        if(type.equals("notification")){
                            Log.d("Main", "Update request received!!!!!!!")
                            eventSource.cancel()
                        }

                    }
                    override fun onClosed(eventSource: EventSource) {
                        Log.d("Main", "onClosed: ")
                        C_SETxConnectSSELoop()
                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?
                    ) {
                        if (t != null) {
                            Log.d("Manin", "onFailure: "+t.message)
                        }
                    }
                }
            EventSources.createFactory(oC_Client).newEventSource(oRequest, oListener)
            }
        }
    }

    fun C_SETxSSE(){

        val oRequest = Request.Builder()
            .url("https://postman-echo.com/server-events/5")
            .build()

        oC_Client.newCall(oRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(poCall: Call, poResponse: Response) {
                poResponse.use {
                    if (poResponse.isSuccessful) {
                        Log.d("onResponse", poResponse.body!!.string())
                    }
                }
            }
        })
    }
    fun C_SEToButton(view: View) {
        C_SETxSSE()
//        C_SETxConnectSSELoop()
    }
}