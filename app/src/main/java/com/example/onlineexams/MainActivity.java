package com.example.onlineexams;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

/**
 * MainActivity handles user login functionality.
 * If the user is already authenticated, redirects them to the Home activity.
 * Allows new users to navigate to the Signup screen.
 */
public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Elements
        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);
        Button login = findViewById(R.id.login);
        TextView signup = findViewById(R.id.signup);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Redirect to Home if already signed in
            Intent i = new Intent(MainActivity.this, Home.class);
            i.putExtra("User UID", user.getUid());
            startActivity(i);
            finish(); // Prevent returning to login
        }

        // Handle Login Button Click
        login.setOnClickListener(view -> {
            // Show loading dialog
            ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Loading...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Run authentication in a separate thread
            Thread thread = new Thread(() -> {
                String em = email.getText().toString();
                String pass = password.getText().toString();

                // Perform sign-in with Firebase
                auth.signInWithEmailAndPassword(em, pass).addOnCompleteListener(MainActivity.this,
                        (OnCompleteListener<AuthResult>) task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user1 = auth.getCurrentUser();
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    // Redirect to Home activity
                                    Intent i = new Intent(MainActivity.this, Home.class);
                                    i.putExtra("User UID", user1.getUid());
                                    startActivity(i);
                                    finish();
                                });
                            } else {
                                // Show error toast if sign-in fails
                                Toast.makeText(MainActivity.this, "Operation Failed", Toast.LENGTH_SHORT).show();
                                runOnUiThread(progressDialog::dismiss);
                            }
                        });
            });

            thread.start(); // Start the thread
        });

        // Navigate to Signup Activity
        signup.setOnClickListener(view -> {
            Intent i = new Intent(MainActivity.this, Signup.class);
            startActivity(i);
            finish(); // Close MainActivity so user can’t go back
        });
    }
}