package com.example.testapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.testapp.data.model.User
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val users = db.collection("users")

    fun login(
        username: String,
        password: String,
        callback: (Boolean, User?, String) -> Unit
    ) {
        users.whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    try {
                        Log.d("LOGIN_DEBUG", "Raw doc: ${doc.data}")

                        val user = doc.toObject(User::class.java)
                        if (user == null) {
                            callback(false, null, "Error fetching user")
                            return@addOnSuccessListener
                        }

                        if (user.password == password) {
                            callback(true, user, "Login successful")
                        } else {
                            callback(false, null, "Password is incorrect")
                        }
                    } catch (e: Exception) {
                        Log.e("LOGIN_ERROR", "Exception parsing user object", e)
                        callback(false, null, "Error fetching user")
                    }
                } else {
                    callback(false, null, "No user found with this username")
                }
            }
            .addOnFailureListener {
                Log.e("LOGIN_ERROR", "Firestore error", it)
                callback(false, null, "Error fetching user: ${it.message}")
            }
    }

    fun signup(
        username: String,
        fullName: String,
        password: String,
        callback: (Boolean, User?, String) -> Unit
    ) {
        val cleanUsername = username.trim().lowercase()

        users.whereEqualTo("username", cleanUsername)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    Log.d("SIGNUP_DEBUG", "User already exists: $cleanUsername")
                    callback(false, null, "User already exists with this username")
                } else {
                    val newUser = User(
                        username = cleanUsername,
                        fullName = fullName.trim(),
                        password = password,
                        pieces = listOf<Int>() // or remove if not needed
                    )

                    users.add(newUser)
                        .addOnSuccessListener { documentRef ->
                            Log.d("SIGNUP_DEBUG", "User added: ${documentRef.id} in collection: ${documentRef.path}")
                            callback(true, newUser, "Signup successful")
                        }
                        .addOnFailureListener { e ->
                            Log.e("SIGNUP_ERROR", "Error adding user", e)
                            callback(false, null, "Error creating user: ${e.message}")
                        }
                }
            }
            .addOnFailureListener {
                Log.e("SIGNUP_ERROR", "Firestore error", it)
                callback(false, null, "Error checking user: ${it.message}")
            }
    }

}
