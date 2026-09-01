package kr.ac.pcu.aifinder

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

actual object FirebaseBackend {
    private fun getDatabaseInstance(): FirebaseDatabase {
        return FirebaseDatabase.getInstance("https://aifinder-3d776-default-rtdb.firebaseio.com")
    }


        actual suspend fun registerUser(user: User): ServerResponse {
        return try {
            val trimmedEmail = user.email.trim()
            val trimmedUsername = user.username.trim()
            val email = if (trimmedEmail.contains("@")) trimmedEmail else "${trimmedUsername}@aifinder.com"
            android.util.Log.d("FirebaseBackend", "Attempting to register user: username=$trimmedUsername, email=$email")
            val authResult = FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, user.passwordHash).await()
            val uid = authResult.user?.uid ?: user.id
            android.util.Log.d("FirebaseBackend", "Auth user created successfully: uid=$uid")
            val firestoreUser = user.copy(id = uid, email = email)
            
            val userMap = mapOf(
                "id" to firestoreUser.id,
                "username" to firestoreUser.username,
                "passwordHash" to firestoreUser.passwordHash,
                "email" to firestoreUser.email,
                "displayName" to firestoreUser.displayName
            )
            val database = getDatabaseInstance()
            android.util.Log.d("FirebaseBackend", "Saving profile to database path: users/$uid/profile")
            database.getReference("users").child(firestoreUser.id).child("profile").setValue(userMap).await()
            android.util.Log.d("FirebaseBackend", "Profile saved successfully")
            ServerResponse(success = true, message = "회원가입 성공", user = firestoreUser)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseBackend", "Registration failed with exception", e)
            e.printStackTrace()
            ServerResponse(success = false, message = e.message ?: "회원가입 실패")
        }
    }

    actual suspend fun authenticate(username: String, passwordHash: String): ServerResponse {
        return try {
            val trimmedUsername = username.trim()
            val email = if (trimmedUsername.contains("@")) trimmedUsername else "${trimmedUsername}@aifinder.com"
            android.util.Log.d("FirebaseBackend", "Attempting login: email=$email")
            val authResult = FirebaseAuth.getInstance().signInWithEmailAndPassword(email, passwordHash).await()
            val uid = authResult.user?.uid ?: return ServerResponse(success = false, message = "인증 실패")
            android.util.Log.d("FirebaseBackend", "Auth login success: uid=$uid")
            
            android.util.Log.d("FirebaseBackend", "Fetching profile for uid: $uid")
            val snapshot = getDatabaseInstance().getReference("users").child(uid).child("profile").get().await()
            
            val fetchedUsername = snapshot.child("username").getValue(String::class.java) ?: username
            val fetchedEmail = snapshot.child("email").getValue(String::class.java) ?: email
            val fetchedDisplayName = snapshot.child("displayName").getValue(String::class.java) ?: ""
            val user = User(id = uid, username = fetchedUsername, passwordHash = passwordHash, email = fetchedEmail, displayName = fetchedDisplayName)
            android.util.Log.d("FirebaseBackend", "Login completed successfully for: ${user.username}")
            ServerResponse(success = true, message = "로그인 성공", user = user)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseBackend", "Authentication failed with exception", e)
            e.printStackTrace()
            ServerResponse(success = false, message = "로그인 실패: ${e.message}")
        }
    }

    actual suspend fun syncItems(userId: String, items: List<ItemRecord>): Boolean {
        return try {
            val itemsRef = getDatabaseInstance().getReference("users").child(userId).child("items")
            
            val updates = mutableMapOf<String, Any>()
            items.forEach { item ->
                val itemMap = mapOf(
                    "id" to item.id,
                    "name" to item.name,
                    "areaId" to item.areaId,
                    "areaName" to item.areaName,
                    "timestamp" to item.timestamp,
                    "photoUri" to (item.photoUri ?: ""),
                    "boundingBox" to (item.boundingBox ?: ""),
                    "isFavorite" to item.isFavorite,
                    "userId" to (item.userId ?: userId)
                )
                updates[item.id] = itemMap
            }
            itemsRef.setValue(updates).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    actual suspend fun loadItems(userId: String): List<ItemRecord> {
        return try {
            val snapshot = getDatabaseInstance().getReference("users").child(userId).child("items").get().await()
            val list = mutableListOf<ItemRecord>()
            for (child in snapshot.children) {
                list.add(ItemRecord(
                    id = child.child("id").getValue(String::class.java) ?: "",
                    name = child.child("name").getValue(String::class.java) ?: "",
                    areaId = child.child("areaId").getValue(Int::class.java) ?: 0,
                    areaName = child.child("areaName").getValue(String::class.java) ?: "",
                    timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L,
                    photoUri = child.child("photoUri").getValue(String::class.java)?.takeIf { it.isNotEmpty() },
                    boundingBox = child.child("boundingBox").getValue(String::class.java)?.takeIf { it.isNotEmpty() },
                    isFavorite = child.child("isFavorite").getValue(Boolean::class.java) ?: false,
                    userId = child.child("userId").getValue(String::class.java)
                ))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    actual suspend fun updateUserProfile(user: User): Boolean {
        return try {
            val database = getDatabaseInstance()
            val userMap = mapOf(
                "id" to user.id,
                "username" to user.username,
                "passwordHash" to user.passwordHash,
                "email" to user.email,
                "displayName" to user.displayName
            )
            database.getReference("users").child(user.id).child("profile").setValue(userMap).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseBackend", "Profile update failed", e)
            false
        }
    }
}
