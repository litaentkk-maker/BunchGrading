package com.palmharvest.pro.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palmharvest.pro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit
) {
    var isRegistering by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray50)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                // Header Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Primary600)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            color = White.copy(alpha = 0.2f)
                        ) {
                            // Placeholder for logo
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = "Logo",
                                modifier = Modifier.padding(12.dp),
                                tint = White
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "PalmHarvest Pro",
                            style = MaterialTheme.typography.displaySmall,
                            color = White
                        )
                        Text(
                            text = "Harvester Management",
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary100
                        )
                    }
                }

                // Form Section
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRegistering) "Create Account" else "Welcome Back",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Gray900
                    )
                    Text(
                        text = if (isRegistering) "Join our harvesting network today" else "Sign in to manage your harvests",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray500,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    if (isRegistering) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name", style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Gray400) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary600,
                                unfocusedBorderColor = Gray200
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address", style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Gray400) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary600,
                            unfocusedBorderColor = Gray200
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Password", style = MaterialTheme.typography.labelLarge, color = Gray500)
                            if (!isRegistering) {
                                TextButton(onClick = { /* Handle Forgot Password */ }) {
                                    Text("Forgot?", style = MaterialTheme.typography.labelLarge, color = Primary600)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Gray400) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary600,
                                unfocusedBorderColor = Gray200
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (isRegistering) onRegister(email, password, name)
                            else onLogin(email, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary600)
                    ) {
                        Text(
                            text = if (isRegistering) "Create Account" else "Sign In",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { isRegistering = !isRegistering }) {
                        Text(
                            text = if (isRegistering) "Already have an account? Sign In" else "Don't have an account? Register",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray500
                        )
                    }
                }
            }
        }
    }
}
