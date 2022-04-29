package com.markbowen.placebook.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.markbowen.placebook.model.Bookmark

//@Database annotation and defines an..
//..array of all entities used by the database.

@Database(entities = arrayOf(Bookmark::class), version = 3)
abstract class PlaceBookDatabase : RoomDatabase() {
    // The abstract method bookmarkDao is defined to return a DAO interface
    abstract fun bookmarkDao(): BookmarkDao
    //
    companion object {
        // 4
        private var instance: PlaceBookDatabase? = null
        // 5
        fun getInstance(context: Context): PlaceBookDatabase {
            if (instance == null) {
                // 6
                 instance = Room.databaseBuilder(context.applicationContext,
                    PlaceBookDatabase::class.java, "PlaceBook")
                    .fallbackToDestructiveMigration()
                    .build()
            }
            // 7
            return instance as PlaceBookDatabase
        }
    }
}