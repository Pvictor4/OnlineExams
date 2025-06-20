package com.example.onlineexams;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Result extends AppCompatActivity {

    private Question[] data;
    private String uid;
    private String testID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        testID = getIntent().getStringExtra("Test ID");
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if(getIntent().hasExtra("User UID"))
            uid  = getIntent().getStringExtra("User UID");

        TextView title = findViewById(R.id.title);
        ListView listView = findViewById(R.id.listview);
        TextView total = findViewById(R.id.total);

        // Try to get the title from Intent extras
        String testTitle = getIntent().getStringExtra("Test Title");
        if (testTitle != null) {
            title.setText(testTitle);
        }

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("Tests").hasChild(testID)) {
                    DataSnapshot ansRef = snapshot.child("Tests").child(testID).child("Answers").child(uid);
                    DataSnapshot qRef = snapshot.child("Tests").child(testID);


                    if (testTitle == null) {
                        Object titleObj = qRef.child("Title").getValue();
                        if (titleObj != null) {
                            title.setText(titleObj.toString());
                        } else {
                            title.setText("[Deleted Test]");
                        }
                    }

                    Object totalQuestionsObj = qRef.child("Total Questions").getValue();
                    if (totalQuestionsObj == null) {
                        Toast.makeText(Result.this, "Total Questions not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    int num = Integer.parseInt(totalQuestionsObj.toString());
                    data = new Question[num];
                    int correctAns = 0;

                    for (int i = 0; i < num; i++) {
                        DataSnapshot qRef2 = qRef.child("Questions").child(String.valueOf(i));
                        Question question = new Question();

                        Object questionText = qRef2.child("Question").getValue();
                        Object opt1 = qRef2.child("Option 1").getValue();
                        Object opt2 = qRef2.child("Option 2").getValue();
                        Object opt3 = qRef2.child("Option 3").getValue();
                        Object opt4 = qRef2.child("Option 4").getValue();
                        Object ansObj = qRef2.child("Ans").getValue();
                        Object selectedAnsObj = ansRef.child(String.valueOf(i + 1)).getValue();

                        if (questionText == null || opt1 == null || opt2 == null || opt3 == null || opt4 == null || ansObj == null || selectedAnsObj == null) {
                            Toast.makeText(Result.this, "Missing data for question " + (i+1), Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        question.setQuestion(questionText.toString());
                        question.setOption1(opt1.toString());
                        question.setOption2(opt2.toString());
                        question.setOption3(opt3.toString());
                        question.setOption4(opt4.toString());
                        question.setSelectedAnswer(Integer.parseInt(selectedAnsObj.toString()));

                        int ans = Integer.parseInt(ansObj.toString());
                        question.setCorrectAnswer(ans);

                        if(ans == question.getSelectedAnswer()) correctAns++;
                        data[i] = question;
                    }

                    total.setText("Total " + correctAns + "/" + data.length);

                    ListAdapter listAdapter = new ListAdapter(data);
                    listView.setAdapter(listAdapter);
                } else {
                    Toast.makeText(Result.this, "Test not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Result.this, "Can't connect", Toast.LENGTH_SHORT).show();
            }
        };
        database.addValueEventListener(listener);
    }

    public class ListAdapter extends BaseAdapter {
        Question[] arr;

        ListAdapter(Question[] arr2){
            arr= arr2;
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
            View v = inflater.inflate(R.layout.question,null);

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
            TextView result =v.findViewById(R.id.result);

            switch (data[i].getSelectedAnswer()){
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
            }

            option1.setEnabled(false);
            option2.setEnabled(false);
            option3.setEnabled(false);
            option4.setEnabled(false);

            result.setVisibility(View.VISIBLE);

            if(data[i].getSelectedAnswer() == data[i].getCorrectAnswer()){
                result.setBackgroundResource(R.drawable.green_background);
                result.setTextColor(ContextCompat.getColor(Result.this,R.color.green_dark));
                result.setText("Correct Answer");
            }else{
                result.setBackgroundResource(R.drawable.red_background);
                result.setTextColor(ContextCompat.getColor(Result.this,R.color.red_dark));
                result.setText("Wrong Answer");

                switch (data[i].getCorrectAnswer()){
                    case 1:
                        option1.setBackgroundResource(R.drawable.green_background);
                        break;
                    case 2:
                        option2.setBackgroundResource(R.drawable.green_background);
                        break;
                    case 3:
                        option3.setBackgroundResource(R.drawable.green_background);
                        break;
                    case 4:
                        option4.setBackgroundResource(R.drawable.green_background);
                        break;
                }
            }

            return v;
        }
    }
}