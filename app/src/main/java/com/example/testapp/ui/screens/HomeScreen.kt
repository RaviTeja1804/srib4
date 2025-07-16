package com.example.testapp.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testapp.data.model.User
import com.example.testapp.viewmodel.ImageGeneratorViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

// --- Light & Lively Color Palette ---
val LightBackground = Color(0xFFF5F8FA) // Very light blue-grey
val PrimaryAccent = Color(0xFF4285F4)   // Google Blue-ish
val SecondaryAccent = Color(0xFF0F4C81) // Deep blue for strong elements
val TextDark = Color(0xFF333333)        // Dark grey for main text
val TextLight = Color(0xFF666666)       // Medium grey for secondary text
val BorderLight = Color(0xFFD3DCE0)     // Light grey for borders

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

    // Background uses a subtle vertical gradient from light to even lighter
    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(LightBackground, Color(0xFFE0E6EB))
        )
    }

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
            // Firestore sorting by array size is not directly supported this way.
            // We fetch all users and sort client-side.
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allUsers = snapshot.documents.mapNotNull { it.toObject(User::class.java) }.sortedByDescending { it.pieces.size }
                }
            }
        onDispose { registration.remove() }
    }

    // --- Main Screen Layout ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush) // Apply light background gradient
            .verticalScroll(rememberScrollState())
            .padding(vertical = 40.dp, horizontal = 16.dp)
    ) {
        // Top Row: Greeting + Logout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hello, ${currentUser.fullName}!",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = SecondaryAccent // Darker accent for main greeting
            )
            TextButton(onClick = onLogoutClick) {
                Text(
                    text = "Logout",
                    color = PrimaryAccent, // Vibrant accent for logout button
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Puzzle Image Grid Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White) // Pure white for card background
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🤩 Your Monthly Puzzle",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SecondaryAccent // Consistent title color
                )
                Spacer(modifier = Modifier.height(16.dp))

                puzzleImage?.let { image ->
                    val pieceWidth = image.width / 4
                    val pieceHeight = image.height / 4

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .padding(horizontal = 4.dp),
                        userScrollEnabled = false,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        items(16) { index ->
                            val row = index / 4
                            val col = index % 4
                            val piece = Bitmap.createBitmap(image, col * pieceWidth, row * pieceHeight, pieceWidth, pieceHeight)
                            val hasPiece = index in currentUser.pieces

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(2.dp))
                                    .border(
                                        width = if (hasPiece) 0.5.dp else 1.dp,
                                        color = if (hasPiece) PrimaryAccent else BorderLight, // Accent for collected, subtle for others
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(if (!hasPiece) BorderLight.copy(alpha = 0.4f) else Color.Transparent) // Dims uncollected
                            ) {
                                if (hasPiece) {
                                    Image(
                                        bitmap = piece.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Placeholder for uncollected pieces
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Could add a faint pattern or icon here
                                    }
                                }
                            }
                        }
                    }
                } ?: run {
                    CircularProgressIndicator(color = PrimaryAccent)
                    Text("Loading puzzle...", style = MaterialTheme.typography.bodyMedium, color = TextLight)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        //🏆 Leaderboard Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🏆 Top Players",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SecondaryAccent
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    items(allUsers.size) { index ->
                        val u = allUsers[index]
                        val isCurrentUser = u.username == currentUser.username
                        val textColor = if (isCurrentUser) PrimaryAccent else TextDark // Highlight current user

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(
                                    color = if (isCurrentUser) PrimaryAccent.copy(alpha = 0.1f) else Color.Transparent, // Subtle background for current user
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${u.username}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                            Text(
                                text = "${u.pieces.size} pieces",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                        Divider(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = BorderLight
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Earn More Pieces Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "💸 Get More Pieces!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SecondaryAccent
                )
                Spacer(modifier = Modifier.height(12.dp))

                val paymentOptions = listOf(
                    "Pay Gas Bill",
                    "Electricity Bill",
                    "Credit Card Payment",
                    "UPI Recharge"
                )

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
                                        val updatedList = unlocked.toList().sorted()

                                        docRef.update("pieces", updatedList)
                                            .addOnSuccessListener {
                                                currentUser = currentUser.copy(pieces = updatedList)
                                                if (updatedList.size >= 16) {
                                                    Toast.makeText(context, "🎉 Congrats! You completed the puzzle!", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "You unlocked ${newPieces.size} new pieces!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Failed to update pieces: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Failed to find user: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent), // Accent color for buttons
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color.White, // White text on vibrant blue
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}