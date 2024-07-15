package com.example.inox

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.inox.adapter.SpinnerAdapter
import com.example.inox.model.Movies
import com.example.inox.model.Response
import com.example.inox.model.Schedules
import com.example.inox.model.Theatres
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE: Int by lazy { 1001 }
    private var movieList: List<Movies> = mutableListOf()
    private var selectedDate = ""
    private var selectedMovieId = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        setContentView(R.layout.activity_main)
        val movieSpinner: Spinner = findViewById(R.id.movieSpinner)
        val dateSpinner: Spinner = findViewById(R.id.dateSpinner)
        val cinemaSpinner: Spinner = findViewById(R.id.cinemaSpinner)
        val timingSpinner: Spinner = findViewById(R.id.timingSpinner)

        lifecycleScope.launch {
            openMovieListoptn(movieSpinner, cinemaSpinner, dateSpinner, timingSpinner)
        }

        Log.d("checkData", "onCreate: " + movieSpinner.selectedItem.toString())

    }

    private suspend fun openMovieListoptn(
        movieSpinner: Spinner,
        cinemaSpinner: Spinner,
        dateSpinner: Spinner,
        timingSpinner: Spinner
    ) {
        val openMovieListoptnHandler = CoroutineExceptionHandler { _, throwable ->
            println("error found in openMovieListoptn coroutine: $throwable")
        }
        val movieNameList = getMovieName(movieSpinner)
        CoroutineScope(Dispatchers.Main + openMovieListoptnHandler).launch {
            val adapter = SpinnerAdapter(application, movieNameList, 0)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            movieSpinner.adapter = adapter
            movieSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position == 0) {
                        return
                    }
                    val selectedItem = parent?.getItemAtPosition(position).toString()
                    Toast.makeText(applicationContext, selectedItem, Toast.LENGTH_SHORT).show()
                    lifecycleScope.launch {
                        selectedMovieId = movieList[position - 1].filmcommonId
                        getDateOption(
                            cinemaSpinner,
                            dateSpinner,
                            movieList[position - 1],
                            timingSpinner
                        )
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }
        }
    }

    private suspend fun getDateOption(
        cinemaSpinner: Spinner,
        dateSpinner: Spinner,
        selectedMovieItem: Movies,
        timingSpinner: Spinner
    ) {
        val getDateOptionHandler = CoroutineExceptionHandler { _, throwable ->
            println("error found in getDateOption coroutine: $throwable")
        }
        val movieId = selectedMovieItem.filmcommonId
        val res = CoroutineScope(Dispatchers.IO + getDateOptionHandler).async { getResponse() }
        val dateList = mutableListOf<Schedules>()
        val avaliableDate = mutableListOf("Select Date")
        val response = res.await()
        for (schedule in response.schedules) {
            for (show in schedule.showTimings) {
                if (movieId == show[1]) {
                    dateList.add(schedule)
                }
            }
        }

        for (date in dateList) {
            if (!avaliableDate.contains(date.day)) avaliableDate.add(date.day)
        }

        CoroutineScope(Dispatchers.Main + getDateOptionHandler).launch {
            Log.d("checkData", "onCreate222: $avaliableDate")
            val adapter = SpinnerAdapter(application, avaliableDate, 0)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dateSpinner.adapter = adapter
            dateSpinner.visibility = View.VISIBLE
            cinemaSpinner.visibility = View.GONE
            timingSpinner.visibility = View.GONE

            dateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position == 0) {
                        return
                    }
                    val item = parent?.getItemAtPosition(position).toString()
                    Toast.makeText(applicationContext, item, Toast.LENGTH_SHORT).show()
                    lifecycleScope.launch {
                        selectedDate = avaliableDate[position]
                        getCinemaOption(timingSpinner,cinemaSpinner, response, avaliableDate[position], movieId)
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }

        }

    }

    private fun getCinemaOption(
        timingSpinner: Spinner,
        cinemaSpinner: Spinner,
        response: Response,
        selectedDate: String,
        selectedMovieId: String
    ) {
        val theaterList = mutableListOf<Theatres>()
        val theaterNameList = mutableListOf<String>("Select Cinema")
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        /*  If the user denies the permission and checks the "Don't ask again" option,
          further calls to requestPermissions() will automatically return PERMISSION_DENIED without prompting the user.*/
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    for (theatre in response.theatres) {
                        val distance = calculateDistance(
                            theatre.Lat.toDouble(),
                            theatre.Long.toDouble(),
                            latitude,
                            longitude
                        )
                        theaterList.add(theatre)
                        theaterNameList.add(theatre.TheatreName + " (" + distance.toInt() + " km away)")
                    }
                }
            }

        val adapter = SpinnerAdapter(application, theaterNameList, 0)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cinemaSpinner.adapter = adapter
        cinemaSpinner.visibility = View.VISIBLE
        timingSpinner.visibility = View.GONE

        cinemaSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == 0) {
                    return
                }
                val item = parent?.getItemAtPosition(position).toString()
                Toast.makeText(applicationContext, item, Toast.LENGTH_SHORT).show()
                lifecycleScope.launch {
                    getTimeOptn(timingSpinner, selectedMovieId ,selectedDate, theaterList[position-1].PosTheatreID,response)
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }
    }


    private fun getTimeOptn(timingSpinner: Spinner, selectedMovieId: String, selectedDate: String, selectedTheaterId: String, response: Response) {
        val timeList = mutableListOf<String>("Select Time")
        val schedules = response.schedules
        for (schedule in schedules) {
            if (schedule.day == this.selectedDate) {
                for (show in schedule.showTimings) {
                    if (selectedTheaterId == show[8] && this.selectedMovieId == show[1]) {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val time = inputFormat.parse(show[2])?.let { outputFormat.format(it) }
                        if (time != null) {
                            timeList.add("$time • ${show[5]}")
                        }
                    }
                }
            }
        }
        val adapter = SpinnerAdapter(application, timeList, 0)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timingSpinner.adapter = adapter
        timingSpinner.visibility = View.VISIBLE

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val permissionResultHandler = CoroutineExceptionHandler { _, throwable ->
            println("error found in onRequestPermissionsResult coroutine: $throwable")
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                CoroutineScope(Dispatchers.Main + permissionResultHandler).launch {
                    val response = async { getResponse() }.await()
                    getCinemaOption(
                        findViewById(R.id.timingSpinner),
                        findViewById(R.id.cinemaSpinner),
                        response,
                        selectedDate,
                        selectedMovieId
                    )
                }
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    Toast.makeText(
                        applicationContext,
                        "Please grant location permission",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadiusKm = 6371

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                sin(dLon / 2) * sin(dLon / 2) * cos(lat1Rad) * cos(lat2Rad)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }

    private suspend fun getMovieName(spinner: Spinner): List<String> {
        val movieNameList: MutableList<String> = mutableListOf("Select Movie")
        val res = getResponse()
        movieList = res.movieDetails
        for ((i, movie) in res.movieDetails.withIndex()) {
            Log.d("getcinemacheck", "onCreate:$i : ${movie.filmName}")
            val movieLanguages = {
                val languageList = mutableListOf<String>()
                for (language in movie.languages) {
                    languageList.add(language.language)
                }
                languageList
            }
            movieNameList.add(movie.filmName + " (" + movieLanguages().joinToString(", ") + ")")
        }
        return movieNameList
    }

    private suspend fun getResponse(): Response {
        val getResponseHandler = CoroutineExceptionHandler { _, throwable ->
            println("error found in getResponse coroutine: $throwable")
        }
        val response = CoroutineScope(Dispatchers.IO + getResponseHandler).async {
            readJsonFromAssets(application, "getcinema.json")
        }
        val gson = Gson()
        val jsonObject = response.await()
        val res = gson.fromJson(jsonObject.toString(), Response::class.java)
        return res
    }

    fun readJsonFromAssets(context: Context, fileName: String): JSONObject? {
        return try {
            val inputStream = context.assets.open(fileName)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            JSONObject(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


}