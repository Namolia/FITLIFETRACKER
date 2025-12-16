package com.example.fitlifetracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(
    onGoRegister: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Giriş", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-posta") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Şifre") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isBlank() || pass.isBlank()) {
                    Toast.makeText(ctx, "E-posta ve şifre boş olamaz", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email.trim(), pass)
                    .addOnSuccessListener {
                        Toast.makeText(ctx, "Giriş başarılı", Toast.LENGTH_SHORT).show()
                        onLoginSuccess(email.trim())
                    }
                    .addOnFailureListener {
                        Toast.makeText(ctx, "Giriş hatası: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Giriş Yap")
        }

        TextButton(onClick = onGoRegister, modifier = Modifier.fillMaxWidth()) {
            Text("Hesabın yok mu? Kayıt ol")
        }
    }
}

@Composable
fun RegisterScreen(
    onGoLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Kayıt", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Ad Soyad") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-posta") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Şifre") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isBlank() || email.isBlank() || pass.isBlank()) {
                    Toast.makeText(ctx, "Tüm alanları doldur", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val auth = FirebaseAuth.getInstance()
                val db = FirebaseFirestore.getInstance()

                auth.createUserWithEmailAndPassword(email.trim(), pass)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener

                        val userMap = hashMapOf(
                            "name" to name.trim(),
                            "email" to email.trim(),
                            "role" to "user",
                            "active" to true
                        )

                        db.collection("users").document(uid).set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(ctx, "Kayıt başarılı", Toast.LENGTH_SHORT).show()
                                onGoLogin()
                            }
                            .addOnFailureListener {
                                Toast.makeText(ctx, "DB hatası: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(ctx, "Kayıt hatası: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kayıt Ol")
        }

        TextButton(onClick = onGoLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Zaten hesabın var mı? Giriş yap")
        }
    }
}
