package com.example.iotsmartlock;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

public class AddDeviceActivity extends AppCompatActivity {

    EditText IPEditText;
    EditText passwordEditText;
    Button addDeviceButton;

    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        IPEditText = findViewById(R.id.IPEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        addDeviceButton = findViewById(R.id.addDeviceButton);
    }

    public void addDevice(View view){
        if (!validate()) {
            onAddDeviceFailed();
            return;
        }

        addDeviceButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(AddDeviceActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        final String IP = IPEditText.getText().toString();
        final String password = passwordEditText.getText().toString();

        final Thread thread = new Thread(new Runnable() {
            public void run() {
                SshManager.checkHost(IP, password);
            }
        });
        thread.start();


        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (SshManager.hostFound == 0){
                    handler.postDelayed(this, 100);
                }
                else if (SshManager.hostFound > 0) {
                    if (SshManager.hostFound == SshManager.HOSTFOUNDSUCCES) {
                        SshManager.hostFound = 0;
                        onAddDeviceSuccess();
                    } else if (SshManager.hostFound == SshManager.HOSTWRONGIP) {
                        SshManager.hostFound = 0;
                        IPEditText.setError("Host unreachable!");
                        onAddDeviceFailed();
                    }
                    else if (SshManager.hostFound == SshManager.HOSTWRONGPASSWORD){
                        SshManager.hostFound = 0;
                        passwordEditText.setError("Wrong password for that host!");
                        onAddDeviceFailed();
                    }
                    else if (SshManager.hostFound == SshManager.HOSTCONNECTIONREFUSED){
                        SshManager.hostFound = 0;
                        IPEditText.setError("Connection refused!");
                        onAddDeviceFailed();
                    }
                    else if (SshManager.hostFound == SshManager.HOSTCONNECTIONTIMEDOUT){
                        SshManager.hostFound = 0;
                        IPEditText.setError("Connection timed out!");
                        onAddDeviceFailed();
                    }
                    progressDialog.dismiss();
                }
            }
        }, 100);
    }

    private void onAddDeviceSuccess(){
        String deviceToAdd = IPEditText.getText().toString();
        FirebaseManager.addDevice(new Device(deviceToAdd));

        pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        SharedPreferences.Editor editor = pref.edit();

        Set<String> devices = new HashSet<>();
        if (pref.getStringSet("devices", new HashSet<String>()) != null){
            devices = new HashSet<>(Objects.requireNonNull(pref.getStringSet("devices", new HashSet<String>())));
        }
        devices.add(deviceToAdd);

        editor.remove("devices");
        editor.apply();
        editor.commit();

        editor.putStringSet("devices", devices);
        editor.apply();

        Toast.makeText(getBaseContext(), "Success adding " + deviceToAdd, Toast.LENGTH_LONG).show();
        addDeviceButton.setEnabled(true);
    }

    private void onAddDeviceFailed(){
        Toast.makeText(getBaseContext(), "Add device failed!", Toast.LENGTH_LONG).show();
        addDeviceButton.setEnabled(true);
    }

    private boolean validate(){
        boolean valid = true;

        String IP = IPEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (IP.isEmpty() || !Patterns.IP_ADDRESS.matcher(IP).matches()) {
            IPEditText.setError("enter a valid IP address");
            valid = false;
        } else {
            IPEditText.setError(null);
        }

/*        if (password.isEmpty() || password.length() < 4) {
            passwordEditText.setError("more than 4 alphanumeric characters");
            valid = false;
        } else {
            passwordEditText.setError(null);
        }*/

        return valid;
    }
}