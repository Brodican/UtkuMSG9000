package com.example.utku.messagingapp;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.sql.DriverManager.println;

public class MainActivity extends AppCompatActivity {

    /*Tag for logs*/
    private static final String TAG = "MainActivity";
    private static final int SIGN_IN_REQUEST_CODE = 1;

    /*Declare FirebaseAuth, AuthStateListener objects*/
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    /*Declaring FirebaseAnalytics*/
    private FirebaseAnalytics mFirebaseAnalytics;

    private FirebaseListAdapter<Message> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        FloatingActionButton floaterButt = (FloatingActionButton) findViewById(R.id.send_msg);

        floaterButt.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                EditText input = (EditText) findViewById(R.id.text_input);

                FirebaseDatabase.getInstance()
                        .getReference()
                        .push() // Means a key is auto-generated
                        .setValue(new Message(input.getText().toString(), // Makes Message object with message and user
                                FirebaseAuth.getInstance()
                        .getCurrentUser()
                        .getDisplayName()));
                input.setText("");
            }
        });

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivityForResult(
                    AuthUI
                            .getInstance()
                            .createSignInIntentBuilder()
                            .build(),
                    SIGN_IN_REQUEST_CODE
                    );
        } else {

            /*Display toast to welcome user, since they are signed in*/
            Toast.makeText(this, "Come in " + FirebaseAuth
                            .getInstance()
                            .getCurrentUser()
                            .getDisplayName(),
                    Toast.LENGTH_LONG).show();

            /*Load the chat*/
            displayChatMessages();
        }
    }

    public void testSignOut() {

        AuthUI.getInstance().signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MainActivity.this, "Debug_Signout", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    public void debug_SignOut(View view) { testSignOut(); }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SIGN_IN_REQUEST_CODE) { // If sign in request occurs
            if (resultCode == RESULT_OK) { // If successful
                Toast.makeText(this, "Sign in successful", Toast.LENGTH_SHORT).show();
                displayChatMessages();
            } else {
                Toast.makeText(this, "Sign in unsuccessful", Toast.LENGTH_SHORT).show();

                finish(); // Quit app when unsuccessful
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_sign_out) {
            AuthUI.getInstance().signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(MainActivity.this,
                                    "Signout successful",
                                    Toast.LENGTH_SHORT).show();
                            new CountDownTimer(1000, 1000) {
                                public void onFinish() {
                                    // When timer is finished
                                    // Execute your code here
                                    finish();
                                }
                                public void onTick(long millisUntilFinished) {
                                    // millisUntilFinished    The amount of time until finished.
                                }
                            }.start();
                        }
                    });
        }
        return true;
    }

    private void displayChatMessages() {
        ListView messageList = (ListView) findViewById(R.id.list_of_messages);

        mAdapter = new FirebaseListAdapter<Message>(this, Message.class, R.layout.message,
                FirebaseDatabase.getInstance().getReference()) {
            @Override
            protected void populateView(View v, Message model, int position) {
                TextView textTV = v.findViewById(R.id.message);
                TextView timeTV = v.findViewById(R.id.time);
                TextView nameTV = v.findViewById(R.id.username);
                // TODO(1) set random colour for people
                textTV.setText(model.getText());
                nameTV.setText(model.getUser());

//                timeTV.setText(DateFormat.format("dd-MM-yyyy (HH:mm:ss)",
//                        model.getMsgTime());
            }
        };
        messageList.setAdapter(mAdapter);
    }


}
