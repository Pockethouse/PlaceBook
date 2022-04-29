package com.markbowen.placebook.adapter
import android.app.Activity
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.markbowen.placebook.databinding.ContentBookmarkInfoBinding
import com.markbowen.placebook.ui.MapsActivity
import com.markbowen.placebook.viewModel.MapsViewModel


class BookmarkInfoWindowAdapter(val context: Activity) :
    GoogleMap.InfoWindowAdapter {

    private val binding = ContentBookmarkInfoBinding.inflate(context.layoutInflater)

    override fun getInfoWindow(marker: Marker): View? {

        return null
    }

    override fun getInfoContents(marker: Marker): View? {
        binding.title.text = marker.title ?: ""
        binding.phone.text = marker.snippet ?: ""


        val imageView = binding.photo

        //The when statement is used to run conditional code based on the class type of..
        //..marker.tag

        when (marker.tag) {
            // If marker.tag is a MapsActivity.PlaceInfo, you set the imageView bitmap
            //directly from the PlaceInfo.image object.
            is MapsActivity.PlaceInfo -> {
                imageView.setImageBitmap(
                    (marker.tag as MapsActivity.PlaceInfo).image)
            }
            // If marker.tag is a MapsViewModel.BookmarkMarkerView, you set the imageView
            //bitmap from the BookmarkMarkerView.
            is MapsViewModel.BookmarkView -> {
                val bookMarkview = marker.tag as
                        MapsViewModel.BookmarkView
                // Set imageView bitmap here
                imageView.setImageBitmap(bookMarkview.getImage(context))
            }
        }
        return binding.root
    }
}