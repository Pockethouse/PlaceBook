package com.markbowen.placebook.ui


import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.markbowen.placebook.R
import com.markbowen.placebook.adapter.BookmarkInfoWindowAdapter
import com.markbowen.placebook.databinding.ActivityMapsBinding
import com.markbowen.placebook.viewModel.MapsViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var placesClient: PlacesClient


    //Fuse locator utilizes hardware to get location
    private lateinit var fusedLocationClient:
            FusedLocationProviderClient
//declaring a private member to hold the MapsViewModel
    private val mapsViewModel by viewModels<MapsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupLocationClient()
        setupPlacesClient()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMapListeners()
        createBookmarkMarkerObserver()
        getCurrentLocation()
        }

    private fun setupPlacesClient() {
        Places.initialize(applicationContext,
            getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)
    }
    private fun setupLocationClient() {
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
    }

    private fun displayPoi(pointOfInterest: PointOfInterest) {

        displayPoiGetPlaceStep(pointOfInterest)
    }

    private fun displayPoiGetPlaceStep(pointOfInterest: PointOfInterest) {
        val placeId = pointOfInterest.placeId
        // 2
        val placeFields = listOf(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG)
        // 3
        val request = FetchPlaceRequest
            .builder(placeId, placeFields)
            .build()
        // 4
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                // 5
                val place = response.place
                displayPoiGetPhotoStep(place)
            }.addOnFailureListener { exception ->
                // 6
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.e(TAG,
                        "Place not found: " +
                                exception.message + ", " +
                                "statusCode: " + statusCode)
                }
            }
    }


    private fun displayPoiGetPhotoStep(place: Place) {
        // 1
        val photoMetadata = place
            .getPhotoMetadatas()?.get(0) // 2
        if (photoMetadata == null) {
            displayPoiDisplayStep(place, null)
            return
        }
        // 3
        val photoRequest = FetchPhotoRequest
            .builder(photoMetadata)
            .setMaxWidth(resources.getDimensionPixelSize(
                R.dimen.default_image_width))
            .setMaxHeight(resources.getDimensionPixelSize(
                R.dimen.default_image_height))
            .build()
        // 4
        placesClient.fetchPhoto(photoRequest)
            .addOnSuccessListener { fetchPhotoResponse ->
                val bitmap = fetchPhotoResponse.bitmap
                displayPoiDisplayStep(place, bitmap)
            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.e(TAG,
                        "Place not found: " +
                                exception.message + ", " +
                                "statusCode: " + statusCode)
                }
            }
    }

    private fun setupMapListeners() {
        mMap.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
        mMap.setOnPoiClickListener {
            displayPoi(it)
            mMap.setOnInfoWindowClickListener {
                handleInfoWindowClick(it)
            }


        }
    }

    // define the callback method to handle the user’s response

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Log.e(TAG, "Location permission denied")
            }
        }
    }

    //This method handles taps on a place info window

    private fun handleInfoWindowClick(marker: Marker) {
        val placeInfo = (marker.tag as PlaceInfo)
        if (placeInfo.place != null) {
            GlobalScope.launch {
                mapsViewModel.addBookmarkFromPlace(placeInfo.place,placeInfo.image)
            }
        }
        marker.remove()
    }

    //This method observes changes to the BookmarkMarkerView objects
    private fun createBookmarkMarkerObserver() {
        // 1
        mapsViewModel.getBookmarkMarkerViews()?.observe(
            this) {
            // 2
            mMap.clear()
            // 3
            it?.let {
                displayAllBookmarks(it)
            }
        }
    }

    //This is a helper method that adds a single blue marker to the map

    private fun addPlaceMarker(
        bookmark: MapsViewModel.BookmarkMarkerView): Marker? {
        val marker = mMap.addMarker(MarkerOptions()
            .position(bookmark.location)
            .icon(BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_AZURE)).alpha(0.8f))
        if (marker != null) {
            marker.tag = bookmark
        }
        return marker
    }
    // This method walks through a list of BookmarkMarkerView objects and calls
    //..addPlaceMarker()

    private fun displayAllBookmarks(
        bookmarks: List<MapsViewModel.BookmarkMarkerView>) {
        bookmarks.forEach { addPlaceMarker(it) }
    }

    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?)
    {
        val marker = mMap.addMarker(MarkerOptions()
            .position(place.latLng as LatLng)
            .title(place.name)
            .snippet(place.phoneNumber)
        )
        marker?.tag = PlaceInfo(place, photo)


    }

    // This method uses requestPermissions() to prompt the user to grant or deny the
    //ACCESS_FINE_LOCATION permission

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION)

    }
    companion object {
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
    }
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        // Check if the ACCESS_FINE_LOCATION permission was granted
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            // not been granted, then requestLocationPermissions() called
            requestLocationPermissions()
        } else {

            mMap.isMyLocationEnabled = true
            // you request to be notified by adding AddOnCompleteListener
            fusedLocationClient.lastLocation.addOnCompleteListener {
                val location = it.result
                if (location != null) {
                    // , you create a LatLng object from location
                    val latLng = LatLng(location.latitude,
                        location.longitude)

                    // You use CameraUpdateFactory.newLatLngZoom() to create a CameraUpdate object

                    val update = CameraUpdateFactory.newLatLngZoom(latLng,
                        16.0f)
                    //u call moveCamera() on map to update the camera
                    mMap.moveCamera(update)
                } else {
                    // logTag to debug message "no location found"
                    Log.e(TAG, "No location found")
                }
            }
        }
    }
    // This defines a class with two properties to hold a Place and a Bitmap.
    class PlaceInfo(val place: Place? = null,
                    val image: Bitmap? = null)

}