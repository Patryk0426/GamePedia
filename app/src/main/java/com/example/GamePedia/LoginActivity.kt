package com.example.GamePedia

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.GamePedia.database.DatabaseHelper
import com.example.GamePedia.database.User
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = DatabaseHelper(this)

        val title: TextView = findViewById(R.id.loginTitle)
        val usernameInput: TextInputEditText = findViewById(R.id.usernameInput)
        val passwordInput: TextInputEditText = findViewById(R.id.passwordInput)
        val btnAction: MaterialButton = findViewById(R.id.btnLogin)
        val btnSwitch: TextView = findViewById(R.id.btnSwitchMode)

        btnSwitch.setOnClickListener {
            isLoginMode = !isLoginMode
            if (isLoginMode) {
                title.text = "Witaj w GamePedia"
                btnAction.text = "ZALOGUJ SIĘ"
                btnSwitch.text = "Nie masz konta? Zarejestruj się"
            } else {
                title.text = "Utwórz konto"
                btnAction.text = "ZAREJESTRUJ SIĘ"
                btnSwitch.text = "Masz już konto? Zaloguj się"
            }
        }

        btnAction.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    if (isLoginMode) {
                        val user = dbHelper.getUserByUsername(username)
                        if (user != null && user.passwordHash == password) {
                            user
                        } else null
                    } else {
                        val existingUser = dbHelper.getUserByUsername(username)
                        if (existingUser != null) {
                            "EXISTS"
                        } else {
                            val newUser = User(username = username, email = "", passwordHash = password)
                            dbHelper.insertUser(newUser)
                            dbHelper.getUserByUsername(username)
                        }
                    }
                }

                when (result) {
                    is User -> {
                        val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                            putExtra("USER_ID", result.id)
                            putExtra("USERNAME", result.username)
                        }
                        startActivity(intent)
                        finish()
                    }
                    "EXISTS" -> {
                        Toast.makeText(this@LoginActivity, "Użytkownik już istnieje", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        if (isLoginMode) {
                            Toast.makeText(this@LoginActivity, "Błędne dane logowania", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
