package com.example.spendtracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 101;
    private FirebaseAuth mAuth;
    private Button loginWihtEmailbtn;
    private DatabaseReference userRef;
    private ProgressDialog loader;
    private FirebaseAuth.AuthStateListener authStateListener;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull  FirebaseAuth firebaseAuth)
            {
                FirebaseUser user = mAuth.getCurrentUser();
                if(user!=null)
                {
                    Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                }

            }
        };

        userRef = FirebaseDatabase.getInstance().getReference("users");
        loader = new ProgressDialog(this);
        mAuth = FirebaseAuth.getInstance();
        loginWihtEmailbtn = findViewById(R.id.loginWithGooglebtn);

        createRequest();
        loginWihtEmailbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });


    }

    private void startloader()
    {
        loader.setMessage("Please wait...");
        loader.setCanceledOnTouchOutside(false);
        loader.show();
    }

    private void createRequest()
    {
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

    }

    private void signIn()
    {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent,RC_SIGN_IN);
        startloader();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                loader.dismiss();
                // Google Sign In failed, update UI appropriately
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken)
    {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            loader.dismiss();
                            updateUI(user);
                        } else {
                            Toast.makeText(getApplicationContext(),"Failed",Toast.LENGTH_SHORT).show();
                            loader.dismiss();
                        }
                    }
                });
    }

    private void updateUI(FirebaseUser user)
    {
        GoogleSignInAccount googleSignInAccount= GoogleSignIn.getLastSignedInAccount(LoginActivity.this);
        if(googleSignInAccount!=null)
        {
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            Calendar calendar = Calendar.getInstance();
            String date = dateFormat.format(calendar.getTime());

            Uri picuri = googleSignInAccount.getPhotoUrl();
            String profilepictureurl = picuri.toString();
            String name = googleSignInAccount.getDisplayName();
            String email = googleSignInAccount.getEmail();
            String id = mAuth.getCurrentUser().getUid();

            HashMap<String,Object> hashMap =  new HashMap<>();
            hashMap.put("name",name);
            hashMap.put("profilepicurl",profilepictureurl);
            hashMap.put("id",id);
            hashMap.put("email",email);
            hashMap.put("logedinon",date);

            userRef.child(id).updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull  Task<Void> task) {
                    if(task.isSuccessful())
                    {
                        Toast.makeText(LoginActivity.this,"Successfull",Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(LoginActivity.this,"Failed to save data",Toast.LENGTH_SHORT).show();
                    }

                }
            });

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(authStateListener);
    }
}
