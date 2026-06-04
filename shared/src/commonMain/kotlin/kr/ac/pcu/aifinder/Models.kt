package kr.ac.pcu.aifinder

import kotlinx.serialization.Serializable

@Serializable
data class RoomArea(
    val id: Int,
    var name: String
)

@Serializable
data class ItemRecord(
    val id: String,
    val name: String,
    val areaId: Int,
    var areaName: String,
    val timestamp: Long,
    val photoUri: String?,
    val boundingBox: String?, // Format: "left,top,right,bottom"
    var isFavorite: Boolean = false
)
