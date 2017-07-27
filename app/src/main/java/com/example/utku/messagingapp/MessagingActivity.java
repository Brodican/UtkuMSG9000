package com.example.utku.messagingapp;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
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
import android.widget.ProgressBar;
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

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.sql.DriverManager.println;

public class MessagingActivity extends AppCompatActivity {

    /*Tag for logs*/
    private static final String TAG = "MainActivity";
    private static final int SIGN_IN_REQUEST_CODE = 1;

    /*Declare FirebaseAuth, AuthStateListener objects*/
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    /*Declaring FirebaseAnalytics*/
    private FirebaseAnalytics mFirebaseAnalytics;

    private FirebaseListAdapter<Message> mAdapter;

    private ProgressBar mLoadingIndicator; // Progress bar for when loading messages
    private ListView mMessageList;

    public int msgCount = 0; // Count number of messages loaded

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        FloatingActionButton floaterButt = (FloatingActionButton) findViewById(R.id.send_msg);

        mLoadingIndicator = (ProgressBar) findViewById(R.id.loading_messages_indicator);

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

            /*Display toast to welcome user, since they are signed in
            (only if savedInstanceState is null, so user is not toasted on rotation)*/
            if (savedInstanceState == null) {
                Toast.makeText(this, "Come in " + FirebaseAuth
                                .getInstance()
                                .getCurrentUser()
                                .getDisplayName(),
                        Toast.LENGTH_LONG).show();
                new LoadMsgBackground().execute();
            }
            else {

            /*AsyncTask to load chat in background*/
                new LoadMsgBackground().execute();
//            displayChatMessages();
            }

        }
    }

    public void testSignOut() {

        AuthUI.getInstance().signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MessagingActivity.this, "Debug_Signout", Toast.LENGTH_SHORT).show();
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
                new LoadMsgBackground().execute();
            } else {
                Toast.makeText(this, "Sign in unsuccessful", Toast.LENGTH_SHORT).show();

                finish(); // Quit app when unsuccessful
            }
        }
    }

//    public class GithubQueryTask extends AsyncTask<String, Void, String> {
//
////        @Override
////        protected String doInBackground(String... strings) {
////            return null;
////        }
////
////        @Override
////        protected void onPreExecute() {
////            super.onPreExecute();
////            mLoadingIndicator.setVisibility(View.VISIBLE);
////        }
////
////        @Override
////        protected void onPostExecute(String githubSearchResults) {
////            mLoadingIndicator.setVisibility(View.INVISIBLE);
////            if (githubSearchResults != null && !githubSearchResults.equals("")) {
////                showJsonDataView();
////                mSearchResultsTextView.setText(githubSearchResults);
////            } else {
////                showErrorMessage();
////            }
////        }
//    }

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
                            Toast.makeText(MessagingActivity.this,
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

    public class LoadMsgBackground extends AsyncTask<String, Void, String[]> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected String[] doInBackground(String... params) {

            if (params.length == 0) {
                displayChatMessages();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] weatherData) {

        }
    }

    private void displayChatMessages() {

        mMessageList = (ListView) findViewById(R.id.list_of_messages);

        mAdapter = new FirebaseListAdapter<Message>(this, Message.class, R.layout.message,
                FirebaseDatabase.getInstance().getReference()) {
            @Override
            protected void populateView(View v, Message model, int position) {
                TextView textTVs = v.findViewById(R.id.message_self);
//                TextView timeTVs = v.findViewById(R.id.time);
                TextView nameTVs = v.findViewById(R.id.username_self);

                TextView textTV = v.findViewById(R.id.message);
//                TextView timeTV = v.findViewById(R.id.time);
                TextView nameTV = v.findViewById(R.id.username);

                String currentName = null;

                // If the message is by the current user, message should be at right
                try { // Name may be empty
                    currentName = FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getDisplayName();
                } catch (Exception e) {

                }
                try {
                    if (currentName.equals(model.getUser())) { // If current user, empty left
                        textTVs.setVisibility(View.VISIBLE); // Change visibility so message box not seen
                        textTVs.setText(model.getText());
                        nameTVs.setText(model.getUser());
                        textTV.setText("");
                        nameTV.setText("");
                        textTV.setVisibility(View.INVISIBLE);

                    } else { // Else, empty right
                        textTV.setVisibility(View.VISIBLE);
                        textTV.setText(model.getText());
                        nameTV.setText(model.getUser());
                        textTVs.setText("");
                        nameTVs.setText("");
                        textTVs.setVisibility(View.INVISIBLE);
                    }
                } catch (Exception e) { // Name may be empty
                    textTV = v.findViewById(R.id.message);
                    nameTV = v.findViewById(R.id.username);
                }

                // TODO(1) set random colour for people

//                timeTV.setText(DateFormat.format("dd-MM-yyyy (HH:mm:ss)",
//                        model.getMsgTime());
                if (position == mAdapter.getCount()-1) {
                    mLoadingIndicator.setVisibility(View.INVISIBLE);
                }
            }
        };
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMessageList.setAdapter(mAdapter);
            }
        });
    }
}
