package com.example.myweatherapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity(), LocationListener {
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2
    private val temperatureArray = mutableListOf<Double>()
    private val humidityArray = mutableListOf<Int>()
    private val cloudCoverArray = mutableListOf<Int>()
    private var weatherData: WeatherData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Weather App"

        val forecastButtons = arrayOf(
            findViewById(R.id.day1Button),
            findViewById(R.id.day2Button),
            findViewById<Button>(R.id.day3Button)
        )

        forecastButtons[0].setOnClickListener {
            weatherData?.let { it1 -> showForecast(it1, 24, forecastButtons[0].text.toString()) }
        }

        forecastButtons[1].setOnClickListener {
            weatherData?.let { it1 -> showForecast(it1, 48, forecastButtons[1].text.toString()) }
        }

        forecastButtons[2].setOnClickListener {
            weatherData?.let { it1 -> showForecast(it1, 72, forecastButtons[2].text.toString()) }
        }

        val screenWidth = resources.displayMetrics.widthPixels
        val rectangleWidth = (screenWidth - 4 * 8) / 3

        for (button in forecastButtons) {
            val layoutParams = button.layoutParams
            layoutParams.width = rectangleWidth
            layoutParams.height = (rectangleWidth * 0.3).toInt()
            button.layoutParams = layoutParams
        }

        if (checkLocationPermission()) {
            getLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        return ContextCompat.checkSelfPermission(this, locationPermission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
    }

    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
    }

    override fun onLocationChanged(location: Location) {
        val currentCity = getCurrentCityName(this, location)
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val calendar = Calendar.getInstance()
        calendar.time = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(currentDate) ?: Date()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val day1Date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val day2Date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val day3Date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)

        runOnUiThread {
            findViewById<TextView>(R.id.cityTextView).text = "City: $currentCity"
            findViewById<TextView>(R.id.dateTextView).text = "Date: $currentDate"
            findViewById<TextView>(R.id.day1Button).text = "$day1Date"
            findViewById<TextView>(R.id.day2Button).text = "$day2Date"
            findViewById<TextView>(R.id.day3Button).text = "$day3Date"
        }

        makeAPIRequest(location.latitude, location.longitude)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentCityName(context: Context, location: Location): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val city = addresses[0].locality
                return city ?: "City not found"
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "City not found"
    }

    data class WeatherData(
        val latitude: Double,
        val longitude: Double,
        val hourly: HourlyData
    )

    data class HourlyData(
        val temperature_2m: List<Double>,
        val relative_humidity_2m: List<Int>,
        val cloud_cover: List<Int>
    )

    private fun parseJson(jsonResponse: String): WeatherData? {
        return try {
            val gson = Gson()
            gson.fromJson(jsonResponse, WeatherData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun makeAPIRequest(latitude: Double, longitude: Double) {
        val apiUrl = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&hourly=temperature_2m,relative_humidity_2m,cloud_cover"

        val hours = listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00")

        Thread {
            try {
                for (hour in hours) {
                    val url = URL("$apiUrl&hour=$hour")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"

                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    connection.disconnect()

                    // Handle the JSON response here
                    val jsonResponse = response.toString()
                    weatherData = parseJson(jsonResponse)

                    // Check if parsing was successful and data is not null
                    if (weatherData?.hourly != null) {
                        val temperatureData = weatherData?.hourly?.temperature_2m
                        val humidityData = weatherData?.hourly?.relative_humidity_2m
                        val cloudCoverData = weatherData?.hourly?.cloud_cover

                        if (temperatureData != null) {
                            temperatureArray.addAll(temperatureData)
                        }
                        if (humidityData != null) {
                            humidityArray.addAll(humidityData)
                        }
                        if (cloudCoverData != null) {
                            cloudCoverArray.addAll(cloudCoverData)
                        }
                    } else {
                        // Handle the case where parsing failed or data is null
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Failed to parse JSON or data is null",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                runOnUiThread {
                    updateGridItems(
                        listOf(R.id.temperature0000, R.id.temperature0400, R.id.temperature0800, R.id.temperature1200, R.id.temperature1600, R.id.temperature2000),
                        temperatureArray,
                        R.id.temperatureAvg
                    )

                    updateGridItems(
                        listOf(R.id.humidity0000, R.id.humidity0400, R.id.humidity0800, R.id.humidity1200, R.id.humidity1600, R.id.humidity2000),
                        humidityArray,
                        R.id.humidityAvg
                    )

                    updateGridItems(
                        listOf(R.id.cloud0000, R.id.cloud0400, R.id.cloud0800, R.id.cloud1200, R.id.cloud1600, R.id.cloud2000),
                        cloudCoverArray,
                        R.id.cloudAvg
                    )
                }
            } catch (e: Exception) {
                // Handle other exceptions here
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "An error occurred: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun updateGridItems(timeIds: List<Int>, dataList: List<Any>, averageId: Int) {
        for (i in timeIds.indices) {
            val index = i * 4
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

    private fun showForecast(weatherData: WeatherData, interval: Int, buttonText: String) {
        val intent = Intent(this, ThreeDayForecastActivity::class.java)
        intent.putExtra("interval", interval)

        val temperatureArray = weatherData.hourly.temperature_2m.subList(interval, interval + 24)
        val humidityArray = weatherData.hourly.relative_humidity_2m.subList(interval, interval + 24)
        val cloudArray = weatherData.hourly.cloud_cover.subList(interval, interval + 24)

        intent.putExtra("temperatureData", ArrayList(temperatureArray))
        intent.putExtra("humidityData", ArrayList(humidityArray))
        intent.putExtra("cloudCoverData", ArrayList(cloudArray))
        intent.putExtra("buttonText", buttonText)

        startActivity(intent)
    }
}
