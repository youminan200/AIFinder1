package kr.ac.pcu.aifinder

expect object FirebaseBackend {
    suspend fun registerUser(user: User): ServerResponse
    suspend fun authenticate(username: String, passwordHash: String): ServerResponse
    suspend fun syncItems(userId: String, items: List<ItemRecord>): Boolean
    suspend fun loadItems(userId: String): List<ItemRecord>
    suspend fun updateUserProfile(user: User): Boolean
}

