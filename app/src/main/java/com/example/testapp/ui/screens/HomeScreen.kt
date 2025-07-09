package com.example.testapp.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testapp.data.model.User
import com.example.testapp.viewmodel.ImageGeneratorViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

fun decodeBase64ToBitmap(base64: String): Bitmap {
    val imageBytes = Base64.decode(base64, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

@Composable
fun HomeScreen(
    user: User,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val usersRef = remember { db.collection("users") }
    val imageViewModel: ImageGeneratorViewModel = viewModel()

    var allUsers by remember { mutableStateOf(listOf<User>()) }
    var currentUser by remember { mutableStateOf(user) }
    var puzzleImage by remember { mutableStateOf<Bitmap?>(null) }

    // Load puzzle image from base64
    LaunchedEffect(Unit) {
        imageViewModel.getOrGenerateMonthlyImage { base64, _ ->
            base64?.let {
                puzzleImage = decodeBase64ToBitmap(it)
            }
        }
    }


    // Live leaderboard listener
    DisposableEffect(Unit) {
        val registration: ListenerRegistration = usersRef
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allUsers = snapshot.documents
                        .mapNotNull { it.toObject(User::class.java) }
                        .sortedByDescending { it.pieces.size }
                }
            }
        onDispose { registration.remove() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Row: Greeting + Logout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Welcome ${currentUser.fullName}", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onLogoutClick) {
                Text("Logout")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🤩 Puzzle Image Grid
        Text("🤩 Your Puzzle", style = MaterialTheme.typography.titleMedium)
        puzzleImage?.let { image ->
            val pieceSize = image.width / 4
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height((pieceSize * 4).dp)
            ) {
                items(16) { index ->
                    val row = index / 4
                    val col = index % 4
                    val piece = Bitmap.createBitmap(image, col * pieceSize, row * pieceSize, pieceSize, pieceSize)

                    Box(
                        modifier = Modifier
                            .padding(1.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                            .size(pieceSize.dp)
                    ) {
                        if (index in currentUser.pieces) {
                            Image(bitmap = piece.asImageBitmap(), contentDescription = null)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Leaderboard
        Text("🏆 Leaderboard", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.height(150.dp)) {
            items(allUsers.size) { index ->
                val u = allUsers[index]
                Text("${u.username}: ${u.pieces.size} pieces")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fake Payments
        Text("💸 Earn More Pieces", style = MaterialTheme.typography.titleMedium)
        val paymentOptions = listOf("Pay Gas Bill", "Electricity Bill", "Credit Card", "UPI Recharge")

        paymentOptions.forEach { label ->
            Button(
                onClick = {
                    val unlocked = currentUser.pieces.toMutableSet()
                    val available = (0..15).filterNot { it in unlocked }

                    if (available.isEmpty()) {
                        Toast.makeText(context, "🎉 You already have all pieces!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val newPieces = available.shuffled().take(2)
                    unlocked.addAll(newPieces)

                    usersRef
                        .whereEqualTo("username", currentUser.username)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            if (!snapshot.isEmpty) {
                                val docRef = snapshot.documents[0].reference
                                val updatedList = unlocked.toList()
                                docRef.update("pieces", updatedList)
                                    .addOnSuccessListener {
                                        currentUser = currentUser.copy(pieces = updatedList)
                                        if (updatedList.size >= 16) {
                                            Toast.makeText(context, "🎉 Congrats! You got all pieces!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "You earned ${newPieces.size} new pieces!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(label)
            }
        }
    }
}
