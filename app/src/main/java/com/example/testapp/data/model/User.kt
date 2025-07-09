package com.example.testapp.data.model

data class User(
    val username: String = "",
    val fullName: String = "",
    val password: String = "",
    val pieces: List<Int> = emptyList()
)
