package com.example.utku.messagingapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static com.google.gson.internal.bind.TypeAdapters.UUID;
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

    /*FirebaseStorage instance*/
    FirebaseStorage storage = FirebaseStorage.getInstance();
    // Create a storage reference from our app
    StorageReference storageRef = storage.getReference();

    private FirebaseListAdapter<Message> mAdapter;

    // Member variable Views
    private ProgressBar mLoadingIndicator; // Progress bar for when loading messages
    private ListView mMessageList;
    private ImageView mTestImage;

    private Uri imageUri;

    public static String downloadUrl = null;

    // Keeps track of whether file is being uploaded, so button may not be pressed again while file is uploading
    public boolean letButtonBePressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        FloatingActionButton floaterButt = (FloatingActionButton) findViewById(R.id.send_msg);

        mLoadingIndicator = (ProgressBar) findViewById(R.id.loading_messages_indicator);

        mTestImage = (ImageView) findViewById(R.id.test_getImage);

        // Make image invisible until just before upload
        mTestImage.setVisibility(View.INVISIBLE);

        // Make downloadUrl null so upload is not attempted
        downloadUrl = null;

        letButtonBePressed = true;

        if (getIntent().hasExtra(PickActivity.MESSAGE_EXTRA)) { // Check if extra with the message code exists
            // Get string uri from intent, parse into Uri
            imageUri = Uri.parse(getIntent().getStringExtra(PickActivity.MESSAGE_EXTRA));
            // Make ImageView visible if user has picked an image
            mTestImage.setVisibility(View.VISIBLE);
            mTestImage.setImageURI(imageUri);
        }

        floaterButt.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                if (letButtonBePressed) { //  Only execute if upload is not occuring
                    // Get the EditText View from its ID
                    EditText input = (EditText) findViewById(R.id.text_input);
                    String ed_string = input.getText().toString().trim(); // For checking if the input is empty
                    // Check if input is empty
                    if (!(ed_string.isEmpty() || ed_string.length() == 0 || ed_string.equals("") || ed_string == null)) {
                        Log.d(TAG, "Not empty bleddy");
                        if (mTestImage.getVisibility() == View.VISIBLE) {
                            // Attempt to upload file (if image is picked)
                            uploadFile();
                        } else {
                            FirebaseDatabase.getInstance()
                                    .getReference()
                                    .push() // Means a key is auto-generated
                                    .setValue(new Message(input.getText().toString(), // Makes Message object with message and user
                                            FirebaseAuth.getInstance()
                                                    .getCurrentUser()
                                                    .getDisplayName()));
                            Log.i(TAG, "Download Uri (non-download message made) is: " + downloadUrl);
                            input.setText("");
                        }
                    }
                    else {
                        Toast.makeText(MessagingActivity.this,
                                "Input text fam",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Don't show empty ImageView
            mTestImage.setVisibility(View.INVISIBLE);
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
                Toast.makeText(this, "Thy ist insert " + FirebaseAuth
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
                        // Go to MainActivity rather than PickActivity on sign-out
                        Intent intent = new Intent(MessagingActivity.this, MainActivity.class);
                        startActivity(intent);
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
        // Don't show back button, user should sign out to leave
        ActionBar supActionBar = getSupportActionBar();
        supActionBar.setDisplayHomeAsUpEnabled(false);
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
                                    Intent intent = new Intent(MessagingActivity.this, MainActivity.class);
                                    startActivity(intent);
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

    // displayChatMessages run in background
    public class LoadMsgBackground extends AsyncTask<String, Void, String[]> {

        @Override
        protected void onPreExecute() {
            // Display loading indicator. Made invisible after all messages loaded (in displayChatMessages())
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

    public void enlargeView(View view) {
        Log.d(TAG, "Called");
        ImageView testImage;
        Drawable drawable = ((PhotoView) view).getDrawable();
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.test_dialog, null);
        ((PhotoView) dialogView.findViewById(R.id.enlarged_image)).setImageDrawable(drawable);
        dialogBuilder.setView(dialogView);
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
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

                PhotoView imagePVs = v.findViewById(R.id.downloaded_PV_self);

                TextView textTV = v.findViewById(R.id.message);
//                TextView timeTV = v.findViewById(R.id.time);
                TextView nameTV = v.findViewById(R.id.username);

                PhotoView imagePV = v.findViewById(R.id.downloaded_PV);

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
                        if (model.getDownloadUrl() != null) {
                            String httpsString = model.getDownloadUrl();
                            downloadFile(httpsString, imagePVs);
                            imagePVs.setVisibility(View.VISIBLE);
//                            textTVs.setText("HasUrl");

                            imagePVs.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    enlargeView(view);
                                }
                            });

//                            AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) imagePVs.getLayoutParams();
//                            layoutParams.height = 1000;
//                            layoutParams.width = 1000;
                        }
                        else {
                            imagePVs.setVisibility(View.INVISIBLE);
//                            AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) imagePVs.getLayoutParams();
//                            layoutParams.width = 1;
//                            layoutParams.height = 1;
                        }
                        textTV.setText("");
                        nameTV.setText("");
                        imagePV.setImageResource(R.drawable.empty_drawable);
                        textTV.setVisibility(View.INVISIBLE);

                    } else { // Else, empty right
                        textTV.setVisibility(View.VISIBLE);
                        textTV.setText(model.getText());
                        nameTV.setText(model.getUser());
                        if (model.getDownloadUrl() != null) {
                            String httpsString = model.getDownloadUrl();
                            downloadFile(httpsString, imagePV);
                            imagePV.setVisibility(View.VISIBLE);
//                            textTVs.setText("HasUrl");
//
//                            AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) imagePVs.getLayoutParams();
//                            layoutParams.height = 1000;
//                            layoutParams.width = 1000;
                        }
                        else {
                            imagePV.setVisibility(View.INVISIBLE);
//                            AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) imagePV.getLayoutParams();
//                            layoutParams.width = 1;
//                            layoutParams.height = 1;
                        }
                        textTVs.setText("");
                        nameTVs.setText("");
                        imagePVs.setImageResource(R.drawable.empty_drawable);
                        textTVs.setVisibility(View.INVISIBLE);
                    }
                } catch (Exception e) { // Name may be empty
                    textTV = v.findViewById(R.id.message);
                    nameTV = v.findViewById(R.id.username);
                }

                // TODO(1) set random colour for people

//                timeTV.setText(DateFormat.format("dd-MM-yyyy (HH:mm:ss)",
//                        model.getMsgTime());
                if ((position == mAdapter.getCount()-1) || (mAdapter.getCount() == 0)) {
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

    // Button methods

    public void attachFile(View view) {
        Intent intent = new Intent(this, PickActivity.class);
        startActivity(intent);
    }

    public void uploadFile() {

        letButtonBePressed = false; // Don't let the send button be pressed if an upload is occurring

        ImageView imageView = mTestImage;

        UUID uuid = new UUID(362548546, 83746383);
        UUID rUuid = uuid.randomUUID();

        // Create name for image in cloud storage
        StorageReference imageRef = storageRef.child(rUuid + ".jpg");

        // Get the data from an ImageView as bytes

        imageView.setDrawingCacheEnabled(true);
        imageView.buildDrawingCache();
        Bitmap bitmap = imageView.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = imageRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Log.i(TAG, "upload failed");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.i(TAG, "upload successful");
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                @SuppressWarnings("VisibleForTests") Uri downloadUrl = taskSnapshot.getDownloadUrl(); // Unnecessary warning occurs
                Log.i(TAG, "DownloadUrl direct: " + downloadUrl.toString());
                mTestImage.setVisibility(View.INVISIBLE); // Set TestImage to invisible so there is not another upload until another image is picked
                MessagingActivity.setDownloadUrl(downloadUrl.toString());
                EditText input = (EditText) findViewById(R.id.text_input);
                FirebaseDatabase.getInstance()
                        .getReference()
                        .push() // Means a key is auto-generated
                        .setValue(new Message(input.getText().toString(), // Makes Message object with message and user
                                FirebaseAuth.getInstance()
                                        .getCurrentUser()
                                        .getDisplayName(),
                                downloadUrl.toString()));
                Log.i(TAG, "Download Uri (message made) is: " + downloadUrl);
                mTestImage.setVisibility(View.INVISIBLE); // Make mTestImage invisible after upload is complete
                input.setText("");
                letButtonBePressed = true;
            }
        });
    }

    public void downloadFile(String httpsString, PhotoView photoView) {

        StorageReference httpsReference = storage.getReferenceFromUrl(httpsString);
        Log.i(TAG, "Download String from downloadFile()" + httpsString);

        photoView.setVisibility(View.VISIBLE);
        // Load the image using Glide
        Glide.with(this /* context */)
                .using(new FirebaseImageLoader())
                .load(httpsReference)
                .into(photoView);
    }

    public static void setDownloadUrl(String inUrl) {
        downloadUrl = inUrl;
    }

    private String getRealPathFromURI(Uri contentURI) { // Used to get path to file from its Uri
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }
}
