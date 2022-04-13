package com.markbowen.placebook.viewModel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.markbowen.placebook.model.Bookmark
import com.markbowen.placebook.repository.BookmarkRepo


// Inheriting from AndroidViewModel allows you to include...
//...the application context

class MapsViewModel(application: Application) :
    AndroidViewModel(application) {

    private val TAG = "MapsViewModel"

    private var bookmarks: LiveData<List<BookmarkMarkerView>>? =
        null
    // Create the BookmarkRepo object, passing in the application context

    private val bookmarkRepo: BookmarkRepo = BookmarkRepo(
        getApplication())

    // Declare the method addBookmarkFromPlace that takes in a Google Place and a..
    //..Bitmap image

    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {

        // Use BookmarkRepo.createBookmark() to create an empty Bookmark object and..
        //..then fill it in using the Place data.

        val bookmark = bookmarkRepo.createBookmark()
        bookmark.placeId = place.id
        bookmark.name = place.name.toString()
        bookmark.longitude = place.latLng?.longitude ?: 0.0
        bookmark.latitude = place.latLng?.latitude ?: 0.0
        bookmark.phone = place.phoneNumber.toString()
        bookmark.address = place.address.toString()

        // save the Bookmark to the repository and print out an info message

        val newId = bookmarkRepo.addBookmark(bookmark)
        Log.i(TAG, "New bookmark $newId added to the database.")
    }
    data class BookmarkMarkerView(
        var id: Long? = null,
        var location: LatLng = LatLng(0.0, 0.0))

    //This is a helper method that converts a Bookmark object from the repo into a..
    //...BookmarkMarkerView object

    private fun bookmarkToMarkerView(bookmark: Bookmark) =
        BookmarkMarkerView(
            bookmark.id,
            LatLng(bookmark.latitude, bookmark.longitude))
    private fun mapBookmarksToMarkerView() {
        // Use the Transformations class to dynamically map Bookmark objects
        bookmarks = Transformations.map(bookmarkRepo.allBookmarks)
        { repoBookmarks ->
            // Transformations.map provides you with a list of Bookmarks returned from the
            //..bookmark repo
            repoBookmarks.map { bookmark ->
                bookmarkToMarkerView(bookmark)
            }
        }
    }
    //This method returns the LiveData object that will be observed by MapsActivity.
    fun getBookmarkMarkerViews() :
            LiveData<List<BookmarkMarkerView>>? {
        if (bookmarks == null) {
            mapBookmarksToMarkerView()
        }
        return bookmarks
    }
}