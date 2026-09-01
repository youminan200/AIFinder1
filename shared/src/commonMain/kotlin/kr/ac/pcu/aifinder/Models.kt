package kr.ac.pcu.aifinder

import kotlinx.serialization.Serializable

@Serializable
data class RoomArea(
    val id: Int,
    var name: String,
    val photoUri: String? = null // NEW: Room area background photo
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
    val userId: String? = null, // Link items to logged-in user
    val additionalPhotoUris: List<String> = emptyList(), // NEW: Support multiple photos for an item
    val pinX: Float? = null, // NEW: relative placement on area photo
    val pinY: Float? = null, // NEW: relative placement on area photo
    val expiryTimestamp: Long? = null, // NEW: expiration / checking reminder
    val tags: List<String> = emptyList() // NEW: categories/tags
)

@Serializable
data class User(
    val id: String,
    val username: String,
    val passwordHash: String = "",
    val email: String,
    val profilePhotoUri: String? = null, // NEW: User avatar profile picture
    val displayName: String = "", // NEW: User display name
    val sharedGroupCode: String? = null // NEW: Linked shared space group code
)

@Serializable
data class SyncRequest(
    val userId: String,
    val items: List<ItemRecord>
)

@Serializable
data class ChecklistItem(
    val id: String = "",
    val name: String,
    var checked: Boolean = false
)

@Serializable
data class ServerResponse(
    val success: Boolean,
    val message: String? = null,
    val user: User? = null
)
