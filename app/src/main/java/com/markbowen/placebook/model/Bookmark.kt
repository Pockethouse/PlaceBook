package com.markbowen.placebook.model

import androidx.room.Entity
import androidx.room.PrimaryKey


// . The @Entity annotation tells Room that this is a database entity class
@Entity
// constructor is defined using arguments for all properties with default values defined

//The id property is defined using the @PrimaryKey annotation. There must be at...
//...least one of these per Entity class.

data class Bookmark(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    // 4
    var placeId: String? = null,
    var name: String = "",
    var address: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var phone: String = ""
)