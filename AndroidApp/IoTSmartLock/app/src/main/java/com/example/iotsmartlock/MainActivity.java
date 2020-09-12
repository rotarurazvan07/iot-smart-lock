package com.example.iotsmartlock;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    SharedPreferences pref;
    TextView welcomeTextView;
    Button addDeviceButton;
    Spinner selectDeviceSpinner;
    List<String> spinnerArray;
    public static final int OPEN_LOGIN_ACTIVITY = 1;
    public static final int OPEN_ADD_DEVICE_ACTIVITY = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        welcomeTextView = findViewById(R.id.welcomeTextView);
        addDeviceButton = findViewById(R.id.addDeviceButton);
        selectDeviceSpinner = findViewById(R.id.selectDeviceSpinner);

        pref = getApplicationContext().getSharedPreferences("MyPref", 0);

        if (pref.getString("email", null) != null){
            // retrieve saved credentials
            FirebaseManager.loggedUser = new User(pref.getString("email", null),
                    pref.getString("password", null),
                    pref.getString("pushID", null));

            //retrieve saved devices
            if (pref.getStringSet("devices", new HashSet<String>()) != null){
                Set<String> devices = new HashSet<>(pref.getStringSet("devices", new HashSet<String>()));
                if(devices != null) {
                    pref.getStringSet("devices", devices);


                    spinnerArray =  new ArrayList<>();
                    for (String dev : devices) {
                        FirebaseManager.addDevice(new Device(dev));
                        spinnerArray.add(dev);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, spinnerArray);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    selectDeviceSpinner.setAdapter(adapter);
                }
            }

            FirebaseManager.loggedUser.printAll();
            welcomeTextView.setText(getResources().getString(R.string.welcome, FirebaseManager.loggedUser.getEmail()));
        }
        else {
            // otherwise, login user
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, OPEN_LOGIN_ACTIVITY);
        }
    }

    public void addDevice(View view){
        Intent intent = new Intent(this, AddDeviceActivity.class);
        startActivityForResult(intent, OPEN_ADD_DEVICE_ACTIVITY);
    }

    public void logOut(View view){
        FirebaseManager.loggedUser = null;

        SharedPreferences.Editor editor = pref.edit();

        editor.remove("email");
        editor.remove("password");
        editor.remove("devices");

        editor.apply();
        editor.clear();
        editor.commit();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, OPEN_LOGIN_ACTIVITY);
    }
    // TODO add delete device, reset train data, remove face, change password, reset device, add face, watch camera.
    // TODO check all modules and disable touch when spinning bar !!!
    // TODO Add restart device that reboots the pi and starts the 03_face_recognizer.py
    // TODO remeber that a device was reset via firebase and alert the user that restarting a reseted device will not start the camera,
    // TODO as it need some files that were deleted by reseting, so add face first, but it will only work with keypad !!!

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_ADD_DEVICE_ACTIVITY || requestCode == OPEN_LOGIN_ACTIVITY) {
            System.out.println("Came back from login or add, refreshing spinner");
            for (Device dev : FirebaseManager.loggedUser.getDevices()){
                System.out.println(dev.getHostAddress());
            }
            welcomeTextView.setText(getResources().getString(R.string.welcome, FirebaseManager.loggedUser.getEmail()));

            spinnerArray =  new ArrayList<>();
            for (Device dev : FirebaseManager.loggedUser.getDevices()) {
                spinnerArray.add(dev.getHostAddress());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, spinnerArray);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            selectDeviceSpinner.setAdapter(adapter);
        }
    }

    public void changePassword(View view){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Security box");
        alert.setMessage("Input the following: ");

        Context context = this;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText currentPasswordEditText = new EditText(context);
        currentPasswordEditText.setHint("Current password");
        currentPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(currentPasswordEditText);

        final EditText newPasswordEditText = new EditText(context);
        newPasswordEditText.setHint("New password");
        newPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(newPasswordEditText);

        final EditText newPasswordConfirmEditText = new EditText(context);
        newPasswordConfirmEditText.setHint("Confirm new password");
        newPasswordConfirmEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(newPasswordConfirmEditText);

        alert.setView(layout);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final String currPassword = currentPasswordEditText.getText().toString();
                final String newPassword = newPasswordEditText.getText().toString();
                String newPasswordConfirm = newPasswordConfirmEditText.getText().toString();
                if (currPassword.isEmpty() || newPassword.isEmpty() || newPasswordConfirm.isEmpty() ||
                    currPassword.length() < 4 || newPassword.length() < 4 || newPasswordConfirm.length() < 4)
                    return;

                if (!newPassword.equals(newPasswordConfirm))
                    return;

                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setIndeterminate(true);
                progressDialog.setMessage("Authenticating...");
                progressDialog.show();


                final Thread thread = new Thread(new Runnable() {
                    public void run() {
                        SshManager.changePassword(selectDeviceSpinner.getSelectedItem().toString(), currPassword, newPassword);
                    }
                });
                thread.start();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (SshManager.passwordChanged == 0){
                            handler.postDelayed(this, 100);
                        }
                        else if (SshManager.passwordChanged > 0) {
                            if (SshManager.passwordChanged == SshManager.HOSTFOUNDSUCCES) {
                                SshManager.passwordChanged = 0;
                                onPasswordChangeSuccess();
                            } else if (SshManager.passwordChanged == SshManager.HOSTWRONGIP) {
                                SshManager.passwordChanged = 0;
                                onPasswordChangeFail("Host unreachable!");
                            }
                            else if (SshManager.passwordChanged == SshManager.HOSTWRONGPASSWORD){
                                SshManager.passwordChanged = 0;
                                onPasswordChangeFail("Wrong password for that host!");
                            }
                            else if (SshManager.passwordChanged == SshManager.HOSTCONNECTIONREFUSED){
                                SshManager.passwordChanged = 0;
                                onPasswordChangeFail("Connection refused!");
                            }
                            else if (SshManager.passwordChanged == SshManager.HOSTCONNECTIONTIMEDOUT){
                                SshManager.passwordChanged = 0;
                                onPasswordChangeFail("Connection timed out!");
                            }
                            progressDialog.dismiss();
                        }
                    }
                }, 100);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void onPasswordChangeSuccess(){
        Toast.makeText(getBaseContext(), "Password changed successfully", Toast.LENGTH_LONG).show();
    }

    public void onPasswordChangeFail(String failText){
        Toast.makeText(getBaseContext(), failText, Toast.LENGTH_LONG).show();
    }

    public void deleteDevice(View view){
        // ! This wont reset the device (delete faces trained, etc.), it will only delete it from
        // current user's account, so it wont appear in the spinner, please reset first if u want to
        // empty the device completely
        String deviceToDelete = selectDeviceSpinner.getSelectedItem().toString();
        FirebaseManager.removeDevice(deviceToDelete);

        // delete from pref
        pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        SharedPreferences.Editor editor = pref.edit();

        Set<String> devices = new HashSet<>();
        if (pref.getStringSet("devices", new HashSet<String>()) != null){
            devices = new HashSet<>(Objects.requireNonNull(pref.getStringSet("devices", new HashSet<String>())));
            devices.remove(deviceToDelete);
        }

        editor.remove("devices");
        editor.apply();
        editor.commit();

        editor.putStringSet("devices", devices);
        editor.apply();


        // refresh spinner
        spinnerArray.remove(deviceToDelete);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectDeviceSpinner.setAdapter(adapter);

        Toast.makeText(getBaseContext(), "Successfully deleted " + deviceToDelete, Toast.LENGTH_LONG).show();
    }

    public void resetDevice(View view){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Security box");
        alert.setMessage("Input the following: ");

        Context context = this;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText currentPasswordEditText = new EditText(context);
        currentPasswordEditText.setHint("Device password");
        currentPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(currentPasswordEditText);

        alert.setView(layout);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final String currPassword = currentPasswordEditText.getText().toString();

                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setIndeterminate(true);
                progressDialog.setMessage("Resetting...");
                progressDialog.show();


                final Thread thread = new Thread(new Runnable() {
                    public void run() {
                        SshManager.resetDevice(selectDeviceSpinner.getSelectedItem().toString(), currPassword);
                    }
                });
                thread.start();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (SshManager.resetDevice == 0){
                            handler.postDelayed(this, 100);
                        }
                        else if (SshManager.resetDevice > 0) {
                            if (SshManager.resetDevice == SshManager.HOSTFOUNDSUCCES) {
                                SshManager.resetDevice = 0;
                                onResetSuccess();
                            } else if (SshManager.resetDevice == SshManager.HOSTWRONGIP) {
                                SshManager.resetDevice = 0;
                                onResetFail("Host unreachable!");
                            }
                            else if (SshManager.resetDevice == SshManager.HOSTWRONGPASSWORD){
                                SshManager.resetDevice = 0;
                                onResetFail("Wrong password for that host!");
                            }
                            else if (SshManager.resetDevice == SshManager.HOSTCONNECTIONREFUSED){
                                SshManager.resetDevice = 0;
                                onResetFail("Connection refused!");
                            }
                            else if (SshManager.resetDevice == SshManager.HOSTCONNECTIONTIMEDOUT){
                                SshManager.resetDevice = 0;
                                onResetFail("Connection timed out!");
                            }
                            progressDialog.dismiss();
                        }
                    }
                }, 100);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void onResetSuccess(){
        Toast.makeText(getBaseContext(),
                "Device " + selectDeviceSpinner.getSelectedItem().toString() + " reset successfully",
                     Toast.LENGTH_LONG).show();
    }

    public void onResetFail(String failText){
        Toast.makeText(getBaseContext(), failText, Toast.LENGTH_LONG).show();
    }

    public void restartDevice (View view){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Security box");
        alert.setMessage("Input the following: ");

        Context context = this;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText currentPasswordEditText = new EditText(context);
        currentPasswordEditText.setHint("Device password");
        currentPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(currentPasswordEditText);

        alert.setView(layout);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final String currPassword = currentPasswordEditText.getText().toString();

                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setIndeterminate(true);
                progressDialog.setMessage("Restarting...");
                progressDialog.show();


                final Thread thread = new Thread(new Runnable() {
                    public void run() {
                        SshManager.restartDevice(selectDeviceSpinner.getSelectedItem().toString(), currPassword);
                    }
                });
                thread.start();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (SshManager.restartDevice == 0){
                            handler.postDelayed(this, 100);
                        }
                        else if (SshManager.restartDevice > 0) {
                            if (SshManager.restartDevice == SshManager.HOSTFOUNDSUCCES) {
                                SshManager.restartDevice = 0;
                                onRestartSuccess();
                            } else if (SshManager.restartDevice == SshManager.HOSTWRONGIP) {
                                SshManager.restartDevice = 0;
                                onRestartFailed("Host unreachable!");
                            }
                            else if (SshManager.restartDevice == SshManager.HOSTWRONGPASSWORD){
                                SshManager.restartDevice = 0;
                                onRestartFailed("Wrong password for that host!");
                            }
                            else if (SshManager.restartDevice == SshManager.HOSTCONNECTIONREFUSED){
                                SshManager.restartDevice = 0;
                                onRestartFailed("Connection refused!");
                            }
                            else if (SshManager.restartDevice == SshManager.HOSTCONNECTIONTIMEDOUT){
                                SshManager.restartDevice = 0;
                                onRestartFailed("Connection timed out!");
                            }
                            progressDialog.dismiss();
                        }
                    }
                }, 100);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void onRestartSuccess(){
        Toast.makeText(getBaseContext(),
                "Device " + selectDeviceSpinner.getSelectedItem().toString() + " restarted successfully",
                Toast.LENGTH_LONG).show();
    }

    public void onRestartFailed(String failText){
        Toast.makeText(getBaseContext(), failText, Toast.LENGTH_LONG).show();
    }

    public void addFace(View view){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Security box");
        alert.setMessage("Input the following: ");

        Context context = this;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText currentPasswordEditText = new EditText(context);
        currentPasswordEditText.setHint("Device password");
        currentPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(currentPasswordEditText);

        final EditText faceOwnerEditText = new EditText(context);
        faceOwnerEditText.setHint("Whose face will it be?");
        layout.addView(faceOwnerEditText);

        alert.setView(layout);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final String currPassword = currentPasswordEditText.getText().toString();
                final String faceOwner = faceOwnerEditText.getText().toString();

                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setIndeterminate(true);
                progressDialog.setMessage("Adding face...");
                progressDialog.show();


                final Thread thread = new Thread(new Runnable() {
                    public void run() {
                        SshManager.addFace(selectDeviceSpinner.getSelectedItem().toString(), currPassword, faceOwner, progressDialog);
                    }
                });
                thread.start();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (SshManager.addFace == 0){
                            handler.postDelayed(this, 100);
                        }
                        else if (SshManager.addFace > 0) {
                            if (SshManager.addFace == SshManager.HOSTFOUNDSUCCES) {
                                SshManager.addFace = 0;
                                onAddFaceSuccess(faceOwner);
                            } else if (SshManager.addFace == SshManager.HOSTWRONGIP) {
                                SshManager.addFace = 0;
                                onAddFaceFailed("Host unreachable!");
                            }
                            else if (SshManager.addFace == SshManager.HOSTWRONGPASSWORD){
                                SshManager.addFace = 0;
                                onAddFaceFailed("Wrong password for that host!");
                            }
                            else if (SshManager.addFace == SshManager.HOSTCONNECTIONREFUSED){
                                SshManager.addFace = 0;
                                onAddFaceFailed("Connection refused!");
                            }
                            else if (SshManager.addFace == SshManager.HOSTCONNECTIONTIMEDOUT){
                                SshManager.addFace = 0;
                                onAddFaceFailed("Connection timed out!");
                            }
                            progressDialog.dismiss();
                        }
                    }
                }, 100);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void onAddFaceSuccess(String faceOwner){
        Toast.makeText(getBaseContext(),
                faceOwner + "'s face added with success",
                Toast.LENGTH_LONG).show();
    }

    public void onAddFaceFailed(String failText){
        Toast.makeText(getBaseContext(), failText, Toast.LENGTH_LONG).show();
    }
}