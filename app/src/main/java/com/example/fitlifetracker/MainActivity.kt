package com.example.fitlifetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.fitlifetracker.ui.theme.FitLifeTrackerTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FitLifeTrackerTheme {
                val auth = FirebaseAuth.getInstance()

                // Ekranlar: login / register / user_home / admin_home
                var screen by remember { mutableStateOf("login") }
                var selectedPlan by remember { mutableStateOf<Plan?>(null) }
                var currentEmail by remember { mutableStateOf("") }

                // Uygulama açılınca: daha önce giriş yapan varsa role göre yönlendir
                LaunchedEffect(Unit) {
                    val user = auth.currentUser
                    if (user != null) {
                        currentEmail = user.email ?: ""
                        decideHomeByRole(
                            uid = user.uid,
                            onAdmin = { screen = "admin_home" },
                            onUser = { screen = "user_home" },
                            onError = { screen = "login" }
                        )
                    }
                }

                when (screen) {
                    "login" -> LoginScreen(
                        onGoRegister = { screen = "register" },
                        onLoginSuccess = { email ->
                            currentEmail = email
                            val u = auth.currentUser
                            if (u != null) {
                                decideHomeByRole(
                                    uid = u.uid,
                                    onAdmin = { screen = "admin_home" },
                                    onUser = { screen = "user_home" },
                                    onError = { screen = "login" }
                                )
                            } else {
                                screen = "login"
                            }
                        }
                    )

                    "register" -> RegisterScreen(
                        onGoLogin = { screen = "login" }
                    )

                    "user_home" -> UserHomeScreen(
                        email = currentEmail,
                        onGoPlans = { screen = "user_plans" },
                        onGoMotivation = { screen = "motivation" },
                        onGoWeather = { screen = "weather" },
                        onGoCart = { screen = "cart" }, // ✅ EKLENDİ
                        onLogout = {
                            auth.signOut()
                            screen = "login"
                        }
                    )

                    "user_plans" -> UserPlansScreen(
                        onBack = { screen = "user_home" },
                        onSelectPlan = { p ->
                            selectedPlan = p
                            screen = "plan_detail"
                        }
                    )

                    "plan_detail" -> {
                        val p = selectedPlan
                        if (p == null) {
                            screen = "user_plans"
                        } else {
                            UserPlanDetailScreen(
                                plan = p,
                                onGoCart = { screen = "cart" },
                                onGoProgram = { screen = "user_program" },
                                onBack = { screen = "user_plans" }
                            )
                        }
                    }

                    "user_program" -> {
                        val p = selectedPlan
                        if (p == null) {
                            screen = "user_plans"
                        } else {
                            UserProgramScreen(plan = p, onBack = { screen = "plan_detail" })
                        }
                    }

                    "cart" -> CartScreen(
                        onBack = { screen = "user_home" } // ✅ daha mantıklı geri dönüş
                    )

                    // ✅ Motivasyon
                    "motivation" -> QuoteScreen(
                        onBack = { screen = "user_home" }
                    )

                    "weather" -> WeatherScreen(
                        onBack = { screen = "user_home" }
                    )

                    "admin_home" -> AdminHomeScreen(
                        email = currentEmail,
                        onGoPlans = { screen = "admin_plans" },
                        onGoOrders = { screen = "admin_orders" },
                        onLogout = {
                            auth.signOut()
                            screen = "login"
                        }
                    )

                    "admin_plans" -> AdminPlansScreen(
                        onBack = { screen = "admin_home" },
                        onEditProgram = { p ->
                            selectedPlan = p
                            screen = "admin_plan_program"
                        }
                    )

                    "admin_plan_program" -> {
                        val p = selectedPlan
                        if (p == null) {
                            screen = "admin_plans"
                        } else {
                            AdminPlanProgramScreen(plan = p, onBack = { screen = "admin_plans" })
                        }
                    }

                    "admin_orders" -> AdminOrdersScreen(
                        onBack = { screen = "admin_home" }
                    )

                    // (Opsiyonel) Eski route
                    "quote" -> QuoteScreen(onBack = { screen = "user_home" })

                    // ✅ Emniyet
                    else -> screen = "login"
                }
            }
        }
    }
}

/**
 * Firestore: users/{uid} dokümanından
 * - role: "admin" veya "user"
 * - active: true/false
 * okur ve ona göre yönlendirir.
 */
fun decideHomeByRole(
    uid: String,
    onAdmin: () -> Unit,
    onUser: () -> Unit,
    onError: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(uid).get()
        .addOnSuccessListener { doc ->
            val role = doc.getString("role") ?: "user"
            val active = doc.getBoolean("active") ?: true

            if (!active) {
                onError()
                return@addOnSuccessListener
            }

            if (role == "admin") onAdmin() else onUser()
        }
        .addOnFailureListener {
            onError()
        }
}
