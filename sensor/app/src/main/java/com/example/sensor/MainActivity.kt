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

    private val SERVER_IP = "xx.xx.xx.xx" // replaced with computer IP
    private val SERVER_PORT = 9876 // replace with the port used in computer

    private val udpScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var socket: DatagramSocket
    private lateinit var serverAddr: InetAddress

    private var latestPitchDeg: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvPitch = findViewById(R.id.tvPitch)
        tvHori = findViewById(R.id.tvHori)


        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        // Send angle data
        udpScope.launch {
            socket = DatagramSocket()
            serverAddr = InetAddress.getByName(SERVER_IP)

            while (isActive) {
                val msg = "%.1f".format(latestPitchDeg).toByteArray()
                val pkt = DatagramPacket(msg, msg.size, serverAddr, SERVER_PORT)
                socket.send(pkt)
                delay(150) //
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

            val pitchDeg = Math.abs(Math.toDegrees(orientations[2].toDouble()).toFloat()) //read angle
            latestPitchDeg = pitchDeg

            val HoriDeg = Math.toDegrees(orientations[1].toDouble()).toFloat()
            latestPitchDeg = pitchDeg

            runOnUiThread {
                tvPitch.text = "Pitch: %.1f°".format(pitchDeg)
                tvHori.text = "Horizon: %.1f°".format(HoriDeg)
            }
        }
    }
}
