package com.example.myweatherapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class ThreeDayForecastActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_three_day_forecast)

        title = intent.getStringExtra(("buttonText"))

        val temperatureData = intent?.getSerializableExtra("temperatureData") as ArrayList<Double>?
        val humidityData = intent.getIntegerArrayListExtra("humidityData") ?: ArrayList()
        val cloudCoverData = intent.getIntegerArrayListExtra("cloudCoverData") ?: ArrayList()
        val interval = intent.getIntExtra("interval", 0)
        Log.d("salami", "salami")
        Log.d("temperatureData", temperatureData.toString())
        Log.d("humidityData", humidityData.toString())
        Log.d("cloudCoverData", cloudCoverData.toString())

        if (temperatureData != null) {
            updateGridItems(
                listOf(R.id.temperature0000, R.id.temperature0400, R.id.temperature0800, R.id.temperature1200, R.id.temperature1600, R.id.temperature2000),
                temperatureData,
                R.id.temperatureAvg,
                4
            )
        }

        if (humidityData != null) {
            updateGridItems(
                listOf(R.id.humidity0000, R.id.humidity0400, R.id.humidity0800, R.id.humidity1200, R.id.humidity1600, R.id.humidity2000),
                humidityData,
                R.id.humidityAvg,
                4
            )
        }

        if (cloudCoverData != null) {
            updateGridItems(
                listOf(R.id.cloud0000, R.id.cloud0400, R.id.cloud0800, R.id.cloud1200, R.id.cloud1600, R.id.cloud2000),
                cloudCoverData,
                R.id.cloudAvg,
                4
            )
        }
    }

    private fun updateGridItems(timeIds: List<Int>, dataList: List<Any>, averageId: Int, interval: Int) {
        for (i in timeIds.indices) {
            val index = i * interval
            val textView = findViewById<TextView>(timeIds[i])
            textView.text = if (dataList.isNotEmpty()) "${dataList[index]}" else ""
        }

        val average = if (dataList.size >= 24) {
            val values = dataList.subList(0, 24)
            values.map { it.toString().toDouble() }.average()
        } else {
            "N/A"
        }

        findViewById<TextView>(averageId).text = String.format("%.2f", average)
    }
}
