package com.markbowen.placebook.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.google.android.libraries.places.api.model.Place
import com.markbowen.placebook.R
import com.markbowen.placebook.db.BookmarkDao
import com.markbowen.placebook.db.PlaceBookDatabase
import com.markbowen.placebook.model.Bookmark


    // a constructor that passes in an object named..
    //..context. A Context object is required
    class BookmarkRepo(private val context: Context) {

        // Two properties are defined that BookmarkRepo will use for its data source.
        private var allCategories: HashMap<String, Int> = buildCategories()
        private val db = PlaceBookDatabase.getInstance(context)
        private val bookmarkDao: BookmarkDao = db.bookmarkDao()
        private var categoryMap: HashMap<Place.Type, String> = buildCategoryMap()
        val categories: List<String> get() = ArrayList(allCategories.keys)

        // This method returns the unique id of the newly saved Bookmark

        fun addBookmark(bookmark: Bookmark): Long? {
            val newId = bookmarkDao.insertBookmark(bookmark)
            bookmark.id = newId
            return newId
        }

        fun deleteBookmark(bookmark: Bookmark) {
            bookmark.deleteImage(context)
            bookmarkDao.deleteBookmark(bookmark)
        }

        //updateBookmark() takes in a bookmark and saves it using the bookmark DAO.
        //getBookmark() takes in a bookmark ID and uses the bookmark DAO to load the
        //corresponding bookmark.

        fun updateBookmark(bookmark: Bookmark) {
            bookmarkDao.updateBookmark(bookmark)
        }
        fun getBookmark(bookmarkId: Long): Bookmark {
            return bookmarkDao.loadBookmark(bookmarkId)
        }

        fun createBookmark(): Bookmark {
            return Bookmark()
        }
        // Create the allBookmarks property that returns a LiveData list of all Bookmarks in the Repository
        val allBookmarks: LiveData<List<Bookmark>>
            get() {
                return bookmarkDao.loadAll()
            }
//This method returns a live bookmark from the bookmark DAO
        fun getLiveBookmark(bookmarkId: Long): LiveData<Bookmark> =
            bookmarkDao.loadLiveBookmark(bookmarkId)


        //you need a method that maps a Google Place type to a supported
        //PlaceBook category.
        //This builds a HashMap that relates Place types to category names

        private fun buildCategoryMap() : HashMap<Place.Type, String> {
            return hashMapOf(
                Place.Type.BAKERY to "Restaurant",
                Place.Type.BAR to "Restaurant",
                Place.Type.CAFE to "Restaurant",
                Place.Type.FOOD to "Restaurant",
                Place.Type.RESTAURANT to "Restaurant",
                Place.Type.MEAL_DELIVERY to "Restaurant",
                Place.Type.MEAL_TAKEAWAY to "Restaurant",
                Place.Type.GAS_STATION to "Gas",
                Place.Type.CLOTHING_STORE to "Shopping",
                Place.Type.DEPARTMENT_STORE to "Shopping",
                Place.Type.FURNITURE_STORE to "Shopping",
                Place.Type.GROCERY_OR_SUPERMARKET to "Shopping",
                Place.Type.HARDWARE_STORE to "Shopping",
                Place.Type.HOME_GOODS_STORE to "Shopping",
                Place.Type.JEWELRY_STORE to "Shopping",
                Place.Type.SHOE_STORE to "Shopping",
                Place.Type.SHOPPING_MALL to "Shopping",
                Place.Type.STORE to "Shopping",
                Place.Type.LODGING to "Lodging",
                Place.Type.ROOM to "Lodging"
            )
        }
        //This method takes in a Place type and converts it to a valid category
        fun placeTypeToCategory(placeType: Place.Type): String {
            var category = "Other"
            if (categoryMap.containsKey(placeType)) {
                category = categoryMap[placeType].toString()
            }
            return category
        }
        private fun buildCategories() : HashMap<String, Int> {
            return hashMapOf(
                "Gas" to R.drawable.ic_gas,
                "Lodging" to R.drawable.ic_lodging,
                "Other" to R.drawable.ic_other,
                "Restaurant" to R.drawable.ic_restaurant,
                "Shopping" to R.drawable.ic_shopping
            )
        }



        //his method provides a public method to convert a category name to a resource ID
        fun getCategoryResourceId(placeCategory: String): Int? {
            return allCategories[placeCategory]
        }


    }


