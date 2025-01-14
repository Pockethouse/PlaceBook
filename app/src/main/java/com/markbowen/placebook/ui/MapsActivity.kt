package com.markbowen.placebook.ui


import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.markbowen.placebook.R
import com.markbowen.placebook.adapter.BookmarkInfoWindowAdapter
import com.markbowen.placebook.adapter.BookmarkListAdapter
import com.markbowen.placebook.databinding.ActivityMapsBinding
import com.markbowen.placebook.viewModel.MapsViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var placesClient: PlacesClient
    private lateinit var databinding: ActivityMapsBinding
    private lateinit var bookmarkListAdapter: BookmarkListAdapter
    private var markers = HashMap<Long, Marker>()


    //Fuse locator utilizes hardware to get location
    private lateinit var fusedLocationClient:
            FusedLocationProviderClient
//declaring a private member to hold the MapsViewModel
    private val mapsViewModel by viewModels<MapsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        databinding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(databinding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupLocationClient()
        setupToolbar()
        setupPlacesClient()
        setupNavigationDrawer()
    }

    private fun setupToolbar() {
        setSupportActionBar(databinding.mainMapView.toolbar)
        val toggle = ActionBarDrawerToggle(
            this, databinding.drawerLayout,
            databinding.mainMapView.toolbar,
            R.string.open_drawer, R.string.close_drawer)
        toggle.syncState()
        
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMapListeners()
        createBookmarkObserver()
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
        showProgress()
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
            Place.Field.LAT_LNG,
            Place.Field.TYPES)
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
                    hideProgress()
                }
            }
    }
    //This method sets up the adapter for the bookmark recycler view
    private fun setupNavigationDrawer() {
        val layoutManager = LinearLayoutManager(this)
        databinding.drawerViewMaps.bookmarkRecyclerView.layoutManager = layoutManager
        bookmarkListAdapter = BookmarkListAdapter(null, this)
        databinding.drawerViewMaps.bookmarkRecyclerView.adapter = bookmarkListAdapter
    }

    private fun displayPoiGetPhotoStep(place: Place) {

        val photoMetadata = place
            .getPhotoMetadatas()?.get(0)
        if (photoMetadata == null) {
            displayPoiDisplayStep(place, null)
            return
        }

        val photoRequest = FetchPhotoRequest
            .builder(photoMetadata)
            .setMaxWidth(resources.getDimensionPixelSize(
                R.dimen.default_image_width))
            .setMaxHeight(resources.getDimensionPixelSize(
                R.dimen.default_image_height))
            .build()

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
                hideProgress()
            }
    }
//This method creates a new bookmark from a location, and then it starts the
//bookmark details Activity 2 allow editing
    private fun newBookmark(latLng: LatLng) {
        GlobalScope.launch {
            val bookmarkId = mapsViewModel.addBookmark(latLng)
            bookmarkId?.let {
                startBookmarkDetails(it)
            }
        }
    }

    private fun setupMapListeners() {
        mMap.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
        mMap.setOnPoiClickListener {
            displayPoi(it)
            mMap.setOnInfoWindowClickListener {
                handleInfoWindowClick(it)

                databinding.mainMapView.fab.setOnClickListener {
                    searchAtCurrentLocation()

                    mMap.setOnMapLongClickListener { latLng ->
                        newBookmark(latLng)
                    }
                }
            }


        }
    }
//disableUserInteraction() sets a flag on the main window to prevent user
//touches.
//enableUserInteraction() clears the flag set by disableUserInteraction()
    private fun disableUserInteraction() {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
    private fun enableUserInteraction() {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
//showProgress() makes the progress bar visible and disables user interaction.
//hideProgress() hides the progress bar and enables user interaction.
    private fun showProgress() {
        databinding.mainMapView.progressBar.visibility =
            ProgressBar.VISIBLE
        disableUserInteraction()
    }
    private fun hideProgress() {
        databinding.mainMapView.progressBar.visibility =
            ProgressBar.GONE
        enableUserInteraction()
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



    //This method handles the action when a user taps a place Info window.
    private fun handleInfoWindowClick(marker: Marker) {
        when (marker.tag) {
            is PlaceInfo -> {
                val placeInfo = (marker.tag as PlaceInfo)
                if (placeInfo.place != null && placeInfo.image != null) {
                    GlobalScope.launch {
                        mapsViewModel.addBookmarkFromPlace(placeInfo.place,
                            placeInfo.image)
                    }
                }
                marker.remove();
            }
            is MapsViewModel.BookmarkView -> {
                val bookmarkMarkerView = (marker.tag as
                        MapsViewModel.BookmarkView)
                marker.hideInfoWindow()
                bookmarkMarkerView.id?.let {
                    startBookmarkDetails(it)
                }
            }
        }
    }


    //This method observes changes to the BookmarkMarkerView objects
    private fun createBookmarkObserver() {
        // 1
        mapsViewModel.getBookmarkViews()?.observe(
            this) {
            // 2
            mMap.clear()
            markers.clear()
            // 3
            it?.let {
                displayAllBookmarks(it)
                bookmarkListAdapter.setBookmarkData(it)
            }
        }
    }
//This pans and zooms the map to center over a Location
    private fun updateMapToLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,
            16.0f))
    }

    //This is a helper method that adds a single blue marker to the map

    private fun addPlaceMarker(
        bookmark: MapsViewModel.BookmarkView): Marker? {
        val marker = mMap.addMarker(MarkerOptions()
            .position(bookmark.location)
            .title(bookmark.name)
            .snippet(bookmark.phone)
            .icon(bookmark.categoryResourceId?.let {
                BitmapDescriptorFactory.fromResource(it)
            })
            .alpha(0.8f))
        if (marker != null) {
            marker.tag = bookmark
            bookmark.id?.let { markers.put(it, marker) }
        }

        return marker
    }
    // This method walks through a list of BookmarkMarkerView objects and calls
    //..addPlaceMarker()

    private fun displayAllBookmarks(
        bookmarks: List<MapsViewModel.BookmarkView>) {
        bookmarks.forEach { addPlaceMarker(it) }
    }

    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?)
    {

        hideProgress()

        val marker = mMap.addMarker(MarkerOptions()
            .position(place.latLng as LatLng)
            .title(place.name)
            .snippet(place.phoneNumber)
        )
        marker?.tag = PlaceInfo(place, photo)
        marker?.showInfoWindow()


    }

    // This method uses requestPermissions() to prompt the user to grant or deny the
    //ACCESS_FINE_LOCATION permission

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION)

    }
    companion object {
        //This defines a key for storing the bookmark ID in the intent extras.
        const val EXTRA_BOOKMARK_ID = "com.markbowen.placebook.EXTRA_BOOKMARK_ID"
        private const val AUTOCOMPLETE_REQUEST_CODE = 2
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
        //You'll call this method when the user taps on an Info
    private fun startBookmarkDetails(bookmarkId: Long) {
        val intent = Intent(this, BookmarkDetailsActivity::class.java)
           // This adds the bookmarkId as an extra parameter on the Intent
            intent.putExtra(EXTRA_BOOKMARK_ID, bookmarkId)

        startActivity(intent)
    }

    fun moveToBookmark(bookmark: MapsViewModel.BookmarkView) {
        // Before zooming the bookmark, the navigation drawer is closed.

        databinding.drawerLayout.closeDrawer(databinding.drawerViewMaps.
        drawerView)
        // The markers HashMap is used to look up the Marker
        val marker = markers[bookmark.id]
        // If the marker is found, its Info window is shown.
        marker?.showInfoWindow()
        // A Location object is created from the bookmark, and updateMapToLocation
        val location = Location("")
        location.latitude = bookmark.location.latitude
        location.longitude = bookmark.location.longitude
        updateMapToLocation(location)
    }

    private fun searchAtCurrentLocation() {
        // define the fields, which informs the Autocomplete widget what attributes to
        //return for each place.
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS,
            Place.Field.TYPES)
        //  compute the bounds of the currently visible region of the map
        val bounds =
            RectangularBounds.newInstance(mMap.projection.visibleRegion.latLngBounds)
        try {
            // Autocomplete provides an IntentBuilder method to build up the Intent
            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, placeFields)
                .setLocationBias(bounds)
                .build(this)
            //  start the Activity and pass a request code of AUTOCOMPLETE_REQUEST_CODE
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        } catch (e: GooglePlayServicesRepairableException) {
            Toast.makeText(this, "Problems Searching",
                Toast.LENGTH_LONG).show()
        } catch (e: GooglePlayServicesNotAvailableException) {
            Toast.makeText(this, "Problems Searching. Google Play Not available", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        // First, you check the requestCode to make sure it matches the
        //AUTOCOMPLETE_REQUEST_CODE passed into startActivityForResult().
        when (requestCode) {
            AUTOCOMPLETE_REQUEST_CODE ->
                // If the resultCode indicates the user found a place, and the data is not null,
                //then you continue to process the results.
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // getPlaceFromIntent()  gets the actual place that was found by the user
                    val place = Autocomplete.getPlaceFromIntent(data)
                    // You convert the place latLng to a location and pass that to the existing
                    //updateMapToLocation method
                    val location = Location("")
                    location.latitude = place.latLng?.latitude ?: 0.0
                    location.longitude = place.latLng?.longitude ?: 0.0
                    updateMapToLocation(location)
                    showProgress()
                    // when the user tapped on a placestart at the displayPoiGetPhotoMetaDataStep() and
                    //pass it the found place. This loads the place photo and displays the place Info
                    //window
                    displayPoiGetPhotoStep(place)
                }
        }
    }

    // This defines a class with two properties to hold a Place and a Bitmap.
    class PlaceInfo(val place: Place? = null,
                    val image: Bitmap? = null)

}