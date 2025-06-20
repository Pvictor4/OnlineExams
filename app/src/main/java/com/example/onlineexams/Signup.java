package com.example.onlineexams;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Signup extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        EditText first_name = findViewById(R.id.first_name);
        EditText last_name = findViewById(R.id.last_name);
        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);
        EditText confirm_password = findViewById(R.id.confirm_password);
        Button signup = findViewById(R.id.signup);
        TextView login = findViewById(R.id.login);

        signup.setOnClickListener(view -> {
            String firstName = first_name.getText().toString().trim();
            String lastName = last_name.getText().toString().trim();
            String em = email.getText().toString().trim();
            String pass = password.getText().toString().trim();
            String confirmPass = confirm_password.getText().toString().trim();

            if (firstName.isEmpty() || lastName.isEmpty() || em.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(Signup.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirmPass)) {
                confirm_password.setError("Passwords do not match");
                return;
            }

            if (pass.length() < 6) {
                password.setError("Password must be at least 6 characters");
                return;
            }

            ProgressDialog progressDialog = new ProgressDialog(Signup.this);
            progressDialog.setMessage("Signing up...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            auth.createUserWithEmailAndPassword(em, pass)
                    .addOnCompleteListener(Signup.this, task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                DatabaseReference ref = database.child("Users").child(user.getUid());
                                ref.child("First Name").setValue(firstName);
                                ref.child("Last Name").setValue(lastName);

                                Intent i = new Intent(Signup.this, Home.class);
                                i.putExtra("User UID", user.getUid());  // <-- fixed key here
                                startActivity(i);
                                finish();
                            }
                        } else {
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(Signup.this, "Signup failed: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        login.setOnClickListener(view -> {
            Intent i = new Intent(Signup.this, MainActivity.class);
            startActivity(i);
            finish();
        });
    }
}