package com.markbowen.placebook.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import com.markbowen.placebook.model.Bookmark


    // The @Dao annotation tells Room that this is a Data Access Object
    @Dao
    interface BookmarkDao {

        // loadAll() uses the @Query annotation to define an SQL statement to read all of
        //the bookmarks from the database and return them as a List of Bookmarks.

        @Query("SELECT * FROM Bookmark ORDER BY name")
        fun loadAll(): LiveData<List<Bookmark>>

        // the @Query annotation is..
        //..used to tell Room how to retrieve a single Bookmark. This method loads a..
        //..Bookmark based on the bookmarkId.

        @Query("SELECT * FROM Bookmark WHERE id = :bookmarkId")
        fun loadBookmark(bookmarkId: Long): Bookmark
        @Query("SELECT * FROM Bookmark WHERE id = :bookmarkId")
        fun loadLiveBookmark(bookmarkId: Long): LiveData<Bookmark>

        // The @Insert annotation is used to define insertBookmark(). This saves a single Bookmark

        @Insert(onConflict = IGNORE)
        fun insertBookmark(bookmark: Bookmark): Long
        // The @Update annotation is used to define updateBookmark().
        @Update(onConflict = REPLACE)
        fun updateBookmark(bookmark: Bookmark)
        // delete bookmark
        @Delete
        fun deleteBookmark(bookmark: Bookmark)
    }
