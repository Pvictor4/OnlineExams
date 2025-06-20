package com.example.onlineexams;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
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

public class Test extends AppCompatActivity {

    private Question[] data;
    private String testID;
    private String uid;
    private int oldTotalPoints = 0;
    private int oldTotalQuestions = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        testID = getIntent().getStringExtra("Test ID");
        if (testID == null || testID.isEmpty()) {
            Toast.makeText(this, "Test ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ListView listView = findViewById(R.id.listview);
        Button submit = findViewById(R.id.submit);
        TextView title = findViewById(R.id.title);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("Tests").hasChild(testID)) {
                    DataSnapshot ref = snapshot.child("Tests").child(testID);

                    Object titleObj = ref.child("Title").getValue();
                    title.setText(titleObj != null ? titleObj.toString() : "No Title");

                    Object totalQObj = ref.child("Total Questions").getValue();
                    int num = 0;
                    if (totalQObj != null) {
                        try {
                            num = Integer.parseInt(totalQObj.toString());
                        } catch (NumberFormatException e) {
                            num = 0;
                        }
                    }

                    data = new Question[num];

                    for (int i = 0; i < num; i++) {
                        DataSnapshot qRef = ref.child("Questions").child(String.valueOf(i));
                        Question question = new Question();

                        Object qText = qRef.child("Question").getValue();
                        question.setQuestion(qText != null ? qText.toString() : "");

                        Object opt1 = qRef.child("Option 1").getValue();
                        Object opt2 = qRef.child("Option 2").getValue();
                        Object opt3 = qRef.child("Option 3").getValue();
                        Object opt4 = qRef.child("Option 4").getValue();

                        question.setOption1(opt1 != null ? opt1.toString() : "");
                        question.setOption2(opt2 != null ? opt2.toString() : "");
                        question.setOption3(opt3 != null ? opt3.toString() : "");
                        question.setOption4(opt4 != null ? opt4.toString() : "");

                        Object ansObj = qRef.child("Ans").getValue();
                        int ans = 0;
                        if (ansObj != null) {
                            try {
                                ans = Integer.parseInt(ansObj.toString());
                            } catch (NumberFormatException e) {
                                ans = 0;
                            }
                        }
                        question.setCorrectAnswer(ans);
                        data[i] = question;
                    }

                    ListAdapter listAdapter = new ListAdapter(data);
                    listView.setAdapter(listAdapter);

                    DataSnapshot ref2 = snapshot.child("Users").child(uid);

                    if (ref2.hasChild("Total Points")) {
                        Object pointsObj = ref2.child("Total Points").getValue();
                        if (pointsObj != null) {
                            try {
                                oldTotalPoints = Integer.parseInt(pointsObj.toString());
                            } catch (NumberFormatException e) {
                                oldTotalPoints = 0;
                            }
                        }
                    }

                    if (ref2.hasChild("Total Questions")) {
                        Object questionsObj = ref2.child("Total Questions").getValue();
                        if (questionsObj != null) {
                            try {
                                oldTotalQuestions = Integer.parseInt(questionsObj.toString());
                            } catch (NumberFormatException e) {
                                oldTotalQuestions = 0;
                            }
                        }
                    }
                } else {
                    Toast.makeText(Test.this, "Test not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Test.this, "Can't connect", Toast.LENGTH_SHORT).show();
            }
        };
        database.addValueEventListener(listener);

        submit.setOnClickListener(v -> {
            if (data == null || data.length == 0) {
                Toast.makeText(Test.this, "No questions loaded", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference ref = database.child("Tests").child(testID)
                    .child("Answers").child(uid);

            int totalPoints = oldTotalPoints;
            int points = 0;
            for (int i = 0; i < data.length; i++) {
                int selectedAnswer = data[i].getSelectedAnswer();
                ref.child(String.valueOf((i + 1))).setValue(selectedAnswer);

                if (selectedAnswer == data[i].getCorrectAnswer()) {
                    totalPoints++;
                    points++;
                }
            }

            ref.child("Points").setValue(points);

            int totalQuestions = oldTotalQuestions + data.length;
            database.child("Users").child(uid).child("Total Points").setValue(totalPoints);
            database.child("Users").child(uid).child("Total Questions").setValue(totalQuestions);
            database.child("Users").child(uid).child("Tests Solved").child(testID).setValue("");

            // Fixed keys here and also pass test title
            Intent i = new Intent(Test.this, Result.class);
            i.putExtra("Test ID", testID);  // exact key as expected by Result
            TextView titleView = findViewById(R.id.title);
            String testTitle = titleView.getText().toString();
            i.putExtra("Test Title", testTitle);
            startActivity(i);
            finish();
        });
    }

    public class ListAdapter extends BaseAdapter {
        Question[] arr;

        ListAdapter(Question[] arr2) {
            arr = arr2;
        }

        @Override
        public int getCount() {
            return arr.length;
        }

        @Override
        public Object getItem(int i) {
            return arr[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = getLayoutInflater();
            View v = inflater.inflate(R.layout.question, null);

            TextView question = v.findViewById(R.id.question);
            RadioButton option1 = v.findViewById(R.id.option1);
            RadioButton option2 = v.findViewById(R.id.option2);
            RadioButton option3 = v.findViewById(R.id.option3);
            RadioButton option4 = v.findViewById(R.id.option4);

            question.setText(data[i].getQuestion());
            option1.setText(data[i].getOption1());
            option2.setText(data[i].getOption2());
            option3.setText(data[i].getOption3());
            option4.setText(data[i].getOption4());

            option1.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
                if (isChecked) data[i].setSelectedAnswer(1);
            });

            option2.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
                if (isChecked) data[i].setSelectedAnswer(2);
            });

            option3.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
                if (isChecked) data[i].setSelectedAnswer(3);
            });

            option4.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
                if (isChecked) data[i].setSelectedAnswer(4);
            });

            switch (data[i].getSelectedAnswer()) {
                case 1:
                    option1.setChecked(true);
                    break;
                case 2:
                    option2.setChecked(true);
                    break;
                case 3:
                    option3.setChecked(true);
                    break;
                case 4:
                    option4.setChecked(true);
                    break;
                default:
                    option1.setChecked(false);
                    option2.setChecked(false);
                    option3.setChecked(false);
                    option4.setChecked(false);
                    break;
            }

            return v;
        }
    }
}