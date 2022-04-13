package com.markbowen.placebook.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.markbowen.placebook.db.BookmarkDao
import com.markbowen.placebook.db.PlaceBookDatabase
import com.markbowen.placebook.model.Bookmark


    // a constructor that passes in an object named..
    //..context. A Context object is required
    class BookmarkRepo(context: Context) {

        // Two properties are defined that BookmarkRepo will use for its data source.

        private val db = PlaceBookDatabase.getInstance(context)
        private val bookmarkDao: BookmarkDao = db.bookmarkDao()

        // This method returns the unique id of the newly saved Bookmark

        fun addBookmark(bookmark: Bookmark): Long? {
            val newId = bookmarkDao.insertBookmark(bookmark)
            bookmark.id = newId
            return newId
        }

        fun createBookmark(): Bookmark {
            return Bookmark()
        }
        // Create the allBookmarks property that returns a LiveData list of all Bookmarks in the Repository
        val allBookmarks: LiveData<List<Bookmark>>
            get() {
                return bookmarkDao.loadAll()
            }
    }
