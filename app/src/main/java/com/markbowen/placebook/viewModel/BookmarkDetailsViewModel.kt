package com.markbowen.placebook.viewModel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.markbowen.placebook.model.Bookmark
import com.markbowen.placebook.repository.BookmarkRepo
import com.markbowen.placebook.util.ImageUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BookmarkDetailsViewModel(application: Application) :
    AndroidViewModel(application) {
    private val bookmarkRepo = BookmarkRepo(getApplication())
    //The bookmarkDetailsView property holds the LiveData<BookmarkDetailsView>
    private var bookmarkDetailsView: LiveData<BookmarkDetailsView>? = null


    //1. Define a new data class to hold the info required by the View class.
    //2. Define a LiveData property with the new data class.
    //3. Define a method to transform LiveData model data to LiveData view data.
    //4. Define a method to return the view data to the View
    data class BookmarkDetailsView(var id: Long? = null,
                                   var name: String = "",
                                   var phone: String = "",
                                   var address: String = "",
                                   var notes: String = "",
                                   var category: String = "",
                                   var longitude: Double = 0.0,
                                   var latitude: Double = 0.0,
                                   var placeId: String? = null) {
        fun getImage(context: Context) = id?.let {
            ImageUtils.loadBitmapFromFile(context,
                Bookmark.generateImageFilename(it))
        }
        //This takes in a Bitmap image and saves it to the associated image file for the current
        //BookmarkView
        fun setImage(context: Context, image: Bitmap) {
            id?.let {
                ImageUtils.saveBitmapToFile(context, image,
                    Bookmark.generateImageFilename(it))
            }
        }

    } //This method converts a Bookmark model to a BookmarkDetailsView model.
    private fun bookmarkToBookmarkView(bookmark: Bookmark):
            BookmarkDetailsView {
        return BookmarkDetailsView(
            bookmark.id,
            bookmark.name,
            bookmark.phone,
            bookmark.address,
            bookmark.notes,
            bookmark.category,
            bookmark.longitude,
            bookmark.latitude,
            bookmark.placeId
        )

    }
//This method takes a BookmarkDetailsView and returns a Bookmark with the
//updated parameters from the BookmarkDetailsView
    private fun bookmarkViewToBookmark(bookmarkView:
                                       BookmarkDetailsView): Bookmark? {
        val bookmark = bookmarkView.id?.let {
            bookmarkRepo.getBookmark(it)
        }
        if (bookmark != null) {
            bookmark.id = bookmarkView.id
            bookmark.name = bookmarkView.name
            bookmark.phone = bookmarkView.phone
            bookmark.address = bookmarkView.address
            bookmark.notes = bookmarkView.notes
            bookmark.category = bookmarkView.category
        }
        return bookmark
    }
    fun getCategoryResourceId(category: String): Int? {
        return bookmarkRepo.getCategoryResourceId(category)
    }

    fun updateBookmark(bookmarkView: BookmarkDetailsView) {
        // A coroutine is used to run the method in the background. This allows calls to be
        //made by the bookmark repo that accesses the database.
        GlobalScope.launch {
            // The BookmarkDetailsView is converted to a Bookmark.
            val bookmark = bookmarkViewToBookmark(bookmarkView)
            // If the bookmark is not null, it's updated in the bookmark repo. This updates the
            //bookmark in the database.
            bookmark?.let { bookmarkRepo.updateBookmark(it) }
        }
    }

    //get the live Bookmark from the BookmarkRepo and then transform it to the
    //live BookmarkDetailsView
    fun mapBookmarkToBookmarkView(bookmarkId: Long) {
        val bookmark = bookmarkRepo.getLiveBookmark(bookmarkId)
        bookmarkDetailsView = Transformations.map(bookmark)
        { repoBookmark ->
            repoBookmark?.let { repoBookmark ->
                bookmarkToBookmarkView(repoBookmark)
            }
        }
    }



//exposing a method to return a live bookmark
//View based on a bookmark ID
    fun getBookmark(bookmarkId: Long): LiveData<BookmarkDetailsView>? {
        if (bookmarkDetailsView == null) {
            mapBookmarkToBookmarkView(bookmarkId)
        }
        return bookmarkDetailsView
    }

    fun getCategories(): List<String> {
        return bookmarkRepo.categories
    }


    fun deleteBookmark(bookmarkDetailsView: BookmarkDetailsView) {
        GlobalScope.launch {
            val bookmark = bookmarkDetailsView.id?.let { bookmarkRepo.getBookmark(it)
            }
            bookmark?.let {
                bookmarkRepo.deleteBookmark(it)
            }
        }
    }
}