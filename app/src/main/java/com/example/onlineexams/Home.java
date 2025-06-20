package com.example.onlineexams;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Home screen activity that welcomes the user,
 * shows their stats, and provides navigation to
 * create or take tests.
 */
public class Home extends AppCompatActivity {

    private String userUID;
    private String firstName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Firebase reference and show loading dialog
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        ProgressDialog progressDialog = new ProgressDialog(Home.this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Retrieve user UID from intent extras
        Bundle b = getIntent().getExtras();
        if (b != null && b.containsKey("User UID")) {
            userUID = b.getString("User UID");
        } else {
            Toast.makeText(this, "User ID missing. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // UI references
        TextView name = findViewById(R.id.name);
        TextView total_questions = findViewById(R.id.total_questions);
        TextView total_points = findViewById(R.id.total_points);
        Button startTest = findViewById(R.id.startTest);
        Button createTest = findViewById(R.id.createTest);
        RelativeLayout solvedTests = findViewById(R.id.solvedTests);
        RelativeLayout your_tests = findViewById(R.id.your_tests);
        EditText test_title = findViewById(R.id.test_title);
        EditText start_test_id = findViewById(R.id.start_test_id);
        ImageView signout = findViewById(R.id.signout);

        // Listen for data changes in Firebase and populate UI with user info
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot userRef = snapshot.child("Users").child(userUID);

                // Set user's first name
                Object firstNameObj = userRef.child("First Name").getValue();
                if (firstNameObj != null) {
                    firstName = firstNameObj.toString();
                } else {
                    firstName = "User";
                }

                // Load and display total points
                if (userRef.hasChild("Total Points")) {
                    String totalPoints = userRef.child("Total Points").getValue().toString();
                    int points = Integer.parseInt(totalPoints);
                    total_points.setText(String.format("%03d", points));  // Zero-padded display
                }

                // Load and display total questions solved
                if (userRef.hasChild("Total Questions")) {
                    String totalQuestions = userRef.child("Total Questions").getValue().toString();
                    int questions = Integer.parseInt(totalQuestions);
                    total_questions.setText(String.format("%03d", questions)); // Zero-padded display
                }

                // Show welcome message
                name.setText("Welcome " + firstName + "!");
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Home.this, "Can't connect", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        };

        // Attach listener to Firebase
        database.addValueEventListener(listener);

        // Sign out logic
        signout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(Home.this, MainActivity.class);
            startActivity(i);
            finish();
        });

        // Create test button logic
        createTest.setOnClickListener(v -> {
            if (test_title.getText().toString().equals("")) {
                test_title.setError("Test title cannot be empty");
                return;
            }
            Intent i = new Intent(Home.this, TestEditor.class);
            i.putExtra("Test Title", test_title.getText().toString());
            test_title.setText("");
            startActivity(i);
        });

        // Start test by ID logic
        startTest.setOnClickListener(v -> {
            if (start_test_id.getText().toString().equals("")) {
                start_test_id.setError("Test ID cannot be empty");
                return;
            }
            Intent i = new Intent(Home.this, Test.class);
            i.putExtra("Test ID", start_test_id.getText().toString());
            start_test_id.setText("");
            startActivity(i);
        });

        // View solved tests
        solvedTests.setOnClickListener(v -> {
            Intent i = new Intent(Home.this, ListTests.class);
            i.putExtra("Operation", "List Solved Tests");
            startActivity(i);
        });

        // View created tests
        your_tests.setOnClickListener(v -> {
            Intent i = new Intent(Home.this, ListTests.class);
            i.putExtra("Operation", "List Created Tests");
            startActivity(i);
        });
    }
}