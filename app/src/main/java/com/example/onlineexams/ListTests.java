package com.example.onlineexams;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
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

import java.util.ArrayList;

/**
 * Activity to list different types of tests:
 * - Solved tests
 * - Created tests
 * - Grades for a particular test
 */
public class ListTests extends AppCompatActivity {

    // Operation type and flags
    private String oper;
    private boolean showGrade = false;
    private boolean solvedTests = false;
    private boolean createdTests = false;
    private boolean testGrades = false;

    // Firebase and UI
    private String uid;
    private ArrayList<String> ids;
    private ArrayList<String> grades;
    private String testID;
    private ListView listView;
    private TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_tests);

        // Initialize UI and Firebase references
        oper = getIntent().getStringExtra("Operation");
        title = findViewById(R.id.title);
        listView = findViewById(R.id.listview);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ids = new ArrayList<>();
        grades = new ArrayList<>();
        ArrayList<String> data = new ArrayList<>();

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        // Determine what type of list to show based on operation
        if ("List Solved Tests".equals(oper)) {
            showGrade = false;
            solvedTests = true;
        } else if ("List Created Tests".equals(oper)) {
            showGrade = false;
            createdTests = true;
        } else if ("List Test Grades".equals(oper)) {
            testID = getIntent().getStringExtra("Test ID");
            title.setText(testID);
            testGrades = true;
            showGrade = true;

            // Long press to copy test ID to clipboard
            title.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Test ID", testID);
                clipboard.setPrimaryClip(clip);
                return true;
            });
        }

        // Fetch solved tests by the user
        if (solvedTests) {
            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    DataSnapshot ds = snapshot.child("Users").child(uid).child("Tests Solved");
                    for (DataSnapshot f : ds.getChildren()) {
                        ids.add(f.getKey());
                        DataSnapshot titleSnap = snapshot.child("Tests").child(f.getKey()).child("Title");
                        data.add(titleSnap.exists() ? titleSnap.getValue().toString() : "[Deleted Test]");
                    }
                    listView.setAdapter(new ListAdapter(data));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ListTests.this, "Can't connect", Toast.LENGTH_SHORT).show();
                }
            });

            // Fetch tests created by the user
        } else if (createdTests) {
            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    DataSnapshot ds = snapshot.child("Users").child(uid).child("Tests Created");
                    for (DataSnapshot f : ds.getChildren()) {
                        ids.add(f.getKey());
                        DataSnapshot titleSnap = snapshot.child("Tests").child(f.getKey()).child("Title");
                        data.add(titleSnap.exists() ? titleSnap.getValue().toString() : "[Deleted Test]");
                    }
                    listView.setAdapter(new ListAdapter(data));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ListTests.this, "Can't connect", Toast.LENGTH_SHORT).show();
                }
            });

            // Fetch test submissions and grades
        } else if (testGrades) {
            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    DataSnapshot ds = snapshot.child("Tests").child(testID).child("Answers");
                    for (DataSnapshot f : ds.getChildren()) {
                        ids.add(f.getKey());

                        // Get user name
                        DataSnapshot userSnap = snapshot.child("Users").child(f.getKey());
                        String firstName = userSnap.child("First Name").getValue() != null
                                ? userSnap.child("First Name").getValue().toString() : "";
                        String lastName = userSnap.child("Last Name").getValue() != null
                                ? userSnap.child("Last Name").getValue().toString() : "";
                        data.add(firstName + " " + lastName);

                        // Get score
                        String points = snapshot.child("Tests").child(testID).child("Answers").child(f.getKey()).child("Points").getValue().toString();
                        String total = snapshot.child("Tests").child(testID).child("Total Questions").getValue().toString();
                        grades.add(points + "/" + total);
                    }
                    listView.setAdapter(new ListAdapter(data));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ListTests.this, "Can't connect", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Custom adapter to display test names and grades (if applicable).
     */
    public class ListAdapter extends BaseAdapter {
        ArrayList<String> arr;

        ListAdapter(ArrayList<String> arr2) {
            arr = arr2;
        }

        @Override
        public int getCount() {
            return arr.size();
        }

        @Override
        public Object getItem(int i) {
            return arr.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = getLayoutInflater();
            View v = inflater.inflate(R.layout.tests_listitem, null);

            TextView grade = v.findViewById(R.id.grade);
            TextView test = v.findViewById(R.id.test);
            RelativeLayout item = v.findViewById(R.id.item);

            test.setText(arr.get(i));

            // Show grade if required
            if (showGrade) {
                grade.setVisibility(View.VISIBLE);
                grade.setText(grades.get(i));
            } else {
                grade.setVisibility(View.GONE);
            }

            // Set click listener based on type
            if (solvedTests) {
                item.setOnClickListener(view1 -> {
                    Intent intent = new Intent(ListTests.this, Result.class);
                    intent.putExtra("Test ID", ids.get(i));
                    startActivity(intent);
                });
            } else if (createdTests) {
                item.setOnClickListener(view1 -> {
                    Intent intent = new Intent(ListTests.this, ListTests.class);
                    intent.putExtra("Operation", "List Test Grades");
                    intent.putExtra("Test ID", ids.get(i));
                    intent.putExtra("Test Title", arr.get(i));
                    startActivity(intent);
                });
            } else if (testGrades) {
                item.setOnClickListener(view1 -> {
                    Intent intent = new Intent(ListTests.this, Result.class);
                    intent.putExtra("Test ID", testID);
                    intent.putExtra("User UID", ids.get(i));
                    startActivity(intent);
                });
            }

            return v;
        }
    }
}