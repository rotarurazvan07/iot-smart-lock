package com.example.iotsmartlock;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class LoginActivity extends AppCompatActivity {

    EditText emailEditText;
    EditText passwordEditText;
    Button loginButton;

    static int loginStatus = 0;
    static final int WRONGEMAIL = 2;
    static final int WRONGPASSWORD = 1;
    static final int LOGINSUCCES = 3;

    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        pref = getApplicationContext().getSharedPreferences("MyPref", 0);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
    }

    public void createAccount(View view){
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    public void loginAccount(View view){

        if (!validate()) {
            onLoginFailed();
            return;
        }

        loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        FirebaseManager.loginUser(new User(email, password, null));

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (loginStatus == 0){
                    handler.postDelayed(this, 100);
                }
                else if (loginStatus > 0) {
                    if (loginStatus == LOGINSUCCES) {
                        loginStatus = 0;
                        onLoginSuccess();
                    } else if (loginStatus == WRONGEMAIL) {
                        loginStatus = 0;
                        emailEditText.setError("No account found with that email!");
                        onLoginFailed();
                    }
                    else if (loginStatus == WRONGPASSWORD){
                        loginStatus = 0;
                        passwordEditText.setError("Wrong password for that email!");
                        onLoginFailed();
                    }
                    progressDialog.dismiss();
                }
            }
        }, 100);
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed!", Toast.LENGTH_LONG).show();
        loginButton.setEnabled(true);
    }

    public void onLoginSuccess(){
        loginButton.setEnabled(true);

        SharedPreferences.Editor editor = pref.edit();
        editor.putString("email", FirebaseManager.loggedUser.getEmail());
        editor.putString("password", FirebaseManager.loggedUser.getPassword());
        editor.putString("pushID", FirebaseManager.loggedUser.getPushID());
        Set<String> devices = new HashSet<>();
        for (Device dev : FirebaseManager.loggedUser.getDevices()){
            devices.add(dev.getHostAddress());
        }
        editor.putStringSet("devices", devices);
        editor.apply();

        finish();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(getBaseContext(), "Please login!", Toast.LENGTH_LONG).show();
    }

    public boolean validate() {
        boolean valid = true;

        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("enter a valid email address");
            valid = false;
        } else {
            emailEditText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4) {
            passwordEditText.setError("more than 4 alphanumeric characters");
            valid = false;
        } else {
            passwordEditText.setError(null);
        }

        return valid;
    }
}