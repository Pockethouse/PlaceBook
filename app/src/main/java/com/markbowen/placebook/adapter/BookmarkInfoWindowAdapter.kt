package com.markbowen.placebook.adapter
import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.markbowen.placebook.databinding.ContentBookmarkInfoBinding
import com.markbowen.placebook.ui.MapsActivity

// 2
class BookmarkInfoWindowAdapter(context: Activity) :
    GoogleMap.InfoWindowAdapter {
    // 3
    private val binding = ContentBookmarkInfoBinding.inflate(context.layoutInflater)
    // 4
    override fun getInfoWindow(marker: Marker): View? {
        // This function is required, but can return null if
        // not replacing the entire info window
        return null
    }
    // 5
    override fun getInfoContents(marker: Marker): View? {
        binding.title.text = marker.title ?: ""
        binding.phone.text = marker.snippet ?: ""


        val imageView = binding.photo

        //You’re casting the marker.tag to a PlaceInfo object and then accessing the image
        //property to set it as the imageView bitmap.
        imageView.setImageBitmap((marker.tag as
                MapsActivity.PlaceInfo).image)
        return binding.root
    }
}