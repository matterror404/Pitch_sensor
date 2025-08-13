package com.example.sensor

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MainActivity : Activity(), SensorEventListener {

    private lateinit var tvPitch: TextView
    private lateinit var tvHori: TextView

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null

    // 替换成你电脑的 IP（通常是热点的）
    private val SERVER_IP = "192.168.137.1"
    private val SERVER_PORT = 9876

    private val udpScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var socket: DatagramSocket
    private lateinit var serverAddr: InetAddress

    private var latestPitchDeg: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvPitch = findViewById(R.id.tvPitch)


        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        // 启动 UDP 后台发送线程
        udpScope.launch {
            socket = DatagramSocket()
            serverAddr = InetAddress.getByName(SERVER_IP)

            while (isActive) {
                val msg = "%.1f".format(latestPitchDeg).toByteArray()
                val pkt = DatagramPacket(msg, msg.size, serverAddr, SERVER_PORT)
                socket.send(pkt)
                delay(5)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        udpScope.cancel()
        if (::socket.isInitialized) socket.close()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientations = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientations)

            val pitchDeg = Math.abs(Math.toDegrees(orientations[2].toDouble()).toFloat())
            latestPitchDeg = pitchDeg

            runOnUiThread {
                tvPitch.text = "Pitch: %.1f°".format(pitchDeg)
            }
        }
    }
}
