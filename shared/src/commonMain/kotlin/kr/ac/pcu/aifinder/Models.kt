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
    var isFavorite: Boolean = false,
    val userId: String? = null // Link items to logged-in user
)

@Serializable
data class User(
    val id: String,
    val username: String,
    val passwordHash: String = "",
    val email: String
)

@Serializable
data class SyncRequest(
    val userId: String,
    val items: List<ItemRecord>
)

@Serializable
data class ServerResponse(
    val success: Boolean,
    val message: String? = null,
    val user: User? = null
)
