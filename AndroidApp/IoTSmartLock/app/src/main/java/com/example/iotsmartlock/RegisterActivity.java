package com.example.iotsmartlock;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterActivity extends AppCompatActivity {
    EditText emailEditText;
    EditText passwordEditText;
    EditText confirmPasswordEditText;
    Button registerButton;

    public static int registerStatus = 0;

    static final int EMAILUSED = 2;
    static final int REGISTERSUCCES = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        this.emailEditText = findViewById(R.id.emailEditText);
        this.passwordEditText = findViewById(R.id.passwordEditText);
        this.confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        this.registerButton = findViewById(R.id.createAccountButton);
    }

    public void registerAccount(View view){
        if (!validate()) {
            onRegisterFailed("Register failed!");
            return;
        }

        registerButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(RegisterActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        FirebaseManager.registerUser(new User(email, password, null));

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (registerStatus == 0){
                    handler.postDelayed(this, 100);
                }
                else if (registerStatus > 0) {
                    if (registerStatus == REGISTERSUCCES) {
                        registerStatus = 0;
                        onRegisterSuccess();
                    } else if (registerStatus == EMAILUSED) {
                        registerStatus = 0;
                        emailEditText.setError("Email already used!");
                        onRegisterFailed("Register failed!");
                    }
                    progressDialog.dismiss();
                }
            }
        }, 100);
    }

    public void onRegisterSuccess() {
        Toast.makeText(getBaseContext(), "Register success!", Toast.LENGTH_LONG).show();
        registerButton.setEnabled(true);
        setResult(RESULT_OK, null);
        finish();
    }

    public void onRegisterFailed(String failText) {
        Toast.makeText(getBaseContext(), failText, Toast.LENGTH_LONG).show();
        registerButton.setEnabled(true);
    }
    public boolean validate(){
        boolean valid = true;

        String email = this.emailEditText.getText().toString();
        String password = this.passwordEditText.getText().toString();
        String confirmPassword = this.confirmPasswordEditText.getText().toString();


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

        if (confirmPassword.isEmpty() || confirmPassword.length() < 4 || !(confirmPassword.equals(password))) {
            confirmPasswordEditText.setError("Password Do not match");
            valid = false;
        } else {
            confirmPasswordEditText.setError(null);
        }

        return valid;
    }
}