package com.medtechbr.parkinsonwatch

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.provider.Settings
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.widget.Toast
import com.medtechbr.parkinsonwatch.R.id.text
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer
import java.util.*

class MainActivity : WearableActivity(), SensorEventListener {

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[1]
        val xArr = floatArrayOf(x)
        val arr = FloatArray2ByteArray(xArr)
        chatService?.write(arr)
    }

    fun FloatArray2ByteArray(values: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(4 * values.size)

        for (value in values) {
            buffer.putFloat(value)
        }

        return buffer.array()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    var chatService : BluetoothChatService? = null

    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {

            Log.e("Main","messaged")
            when (msg.what) {
                Constants.MESSAGE_STATE_CHANGE -> when (msg.arg1) {
                    BluetoothChatService.STATE_CONNECTED -> {
                        if (null != applicationContext)
                            Toast.makeText(applicationContext, "connected", Toast.LENGTH_SHORT).show()
                    }
                }
                Constants.MESSAGE_WRITE -> {
                    val writeBuf = msg.obj as ByteArray
                    // construct a string from the buffer
                    val writeMessage = String(writeBuf)
                }
                Constants.MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    // construct a string from the valid bytes in the buffer
                    val readMessage = String(readBuf, 0, msg.arg1)
                    //text.text = readMessage

                }
                Constants.MESSAGE_DEVICE_NAME -> {
                    // save the connected device's name
                    val mConnectedDeviceName = msg.data.getString(Constants.DEVICE_NAME)
                    if (null != applicationContext) {
                        Toast.makeText(applicationContext, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show()
                    }
                }
                Constants.MESSAGE_TOAST -> if (null != applicationContext) {
                    Toast.makeText(applicationContext, msg.data.getString(Constants.TOAST),  Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    var flashing = false
    var active = false
    var t = Timer()

    override fun onPause() {
        super.onPause()
        setFlash(true)
        t.cancel()
        t = Timer()
    }
    fun vibrate(v: View) {

        if (active) {
            active = false
            setFlash(true)
            t.cancel()
            t = Timer()
            return
        }
        active = true
        t.scheduleAtFixedRate(object: TimerTask(){

            var counter = 0;
            override fun run() {
                runOnUiThread {

                    flashing = !flashing
                    setFlash(flashing)

                }
                //toggleFlash()
                /*counter++;
                if (counter==4) {
                    this.cancel()
                    active = false
                }*/
            }
        }, 0, 600)
    }

    public override fun onResume() {
        super.onResume()

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (chatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (chatService?.getState() === BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                chatService?.start()
            }
        }
    }

    fun setFlash(toFlash: Boolean) {
        if(!toFlash) {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(300)
        }
        val layout = window.attributes
        layout.screenBrightness = if (toFlash) 0.0f else 1.0f
        val white = Color.argb(255,255,255,255)
        val black = Color.argb(255,0,0,0)
        frameLayout.foregroundTintList = if (toFlash) ColorStateList.valueOf(black) else ColorStateList.valueOf(white)
        window.attributes = layout
        frameLayout.foreground
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(FLAG_KEEP_SCREEN_ON)
        chatService = BluetoothChatService(applicationContext, mHandler)

        val sensorManager: SensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, sensor, 40000)
        setContentView(R.layout.activity_main)

        //text.text="heys"

        // Enables Always-on
        setAmbientEnabled()
    }
}
