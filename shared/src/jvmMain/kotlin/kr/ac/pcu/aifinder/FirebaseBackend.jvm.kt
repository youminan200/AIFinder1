package kr.ac.pcu.aifinder

actual object FirebaseBackend {
    actual suspend fun registerUser(user: User): ServerResponse {
        return ServerResponse(success = false, message = "Desktop not supported yet")
    }

    actual suspend fun authenticate(username: String, passwordHash: String): ServerResponse {
         return ServerResponse(success = false, message = "Desktop not supported yet")
    }

    actual suspend fun syncItems(userId: String, items: List<ItemRecord>): Boolean {
        return false
    }

    actual suspend fun loadItems(userId: String): List<ItemRecord> {
        return emptyList()
    }

    actual suspend fun updateUserProfile(user: User): Boolean {
        return false
    }
}
