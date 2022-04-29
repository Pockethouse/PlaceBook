package com.markbowen.placebook.viewModel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.markbowen.placebook.model.Bookmark
import com.markbowen.placebook.repository.BookmarkRepo
import com.markbowen.placebook.util.ImageUtils


// Inheriting from AndroidViewModel allows you to include...
//...the application context

class MapsViewModel(application: Application) :
    AndroidViewModel(application) {

    private val TAG = "MapsViewModel"

    private var bookmarks: LiveData<List<BookmarkView>>? = null
    // Create the BookmarkRepo object, passing in the application context

    private val bookmarkRepo: BookmarkRepo = BookmarkRepo(getApplication())

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
        bookmark.category = getPlaceCategory(place)
        // save the Bookmark to the repository and print out an info message

        val newId = bookmarkRepo.addBookmark(bookmark)

        //It's important to call this after the bookmark is saved to the database so that the
        //bookmark has a unique ID assigned
        //update addBookmarkFromPlace() to call the new setImage() method
        image?.let { bookmark.setImage(it, getApplication()) }

        Log.i(TAG, "New bookmark $newId added to the database.")
    }
    data class BookmarkView(val id: Long? = null,
                            val location: LatLng = LatLng(0.0, 0.0),
                            val name: String = "",
                            val phone: String = "",
                            val categoryResourceId: Int? = null) {
        fun getImage(context: Context) = id?.let {
            ImageUtils.loadBitmapFromFile(context,
                Bookmark.generateImageFilename(it))
        }
    }


    //This is a helper method that converts a Bookmark object from the repo into a..
    //...BookmarkMarkerView object

    private fun bookmarkToBookmarkView(bookmark: Bookmark):
            BookmarkView {
        return BookmarkView(
            bookmark.id,
            LatLng(bookmark.latitude, bookmark.longitude),
            bookmark.name,
            bookmark.phone,
            bookmarkRepo.getCategoryResourceId(bookmark.category))
    }
    //This takes in a LatLng location and creates a new untitled bookmark at the given
    //location. Then, it returns the new bookmark ID to the calle
    fun addBookmark(latLng: LatLng) : Long? {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.name = "Untitled"
        bookmark.longitude = latLng.longitude
        bookmark.latitude = latLng.latitude
        bookmark.category = "Other"
        return bookmarkRepo.addBookmark(bookmark)
    }

    private fun mapBookmarksToBookmarkView() {
        // Use the Transformations class to dynamically map Bookmark objects
        bookmarks = Transformations.map(bookmarkRepo.allBookmarks)
        { repoBookmarks ->
            // Transformations.map provides you with a list of Bookmarks returned from the
            //..bookmark repo
            repoBookmarks.map { bookmark ->
                bookmarkToBookmarkView(bookmark)
            }
        }
    }
    //This method returns the LiveData object that will be observed by MapsActivity.
    fun getBookmarkViews() :
            LiveData<List<BookmarkView>>? {
        if (bookmarks == null) {
            mapBookmarksToBookmarkView()
        }
        return bookmarks
    }
    //This method converts a place type to a bookmark category.
    private fun getPlaceCategory(place: Place): String {
        // The category defaults to "Other" in case there’s no type assigned to the place
        var category = "Other"
        val types = place.types
        types?.let { placeTypes ->
            // The method first checks the placeTypes List to see if it’s populated
            if (placeTypes.size > 0) {
                // If so, you extract the first type from the List and call placeTypeToCategory() t
                val placeType = placeTypes[0]
                category = bookmarkRepo.placeTypeToCategory(placeType)
            }
        }
        // . Finally, you return the category
        return category
    }
}