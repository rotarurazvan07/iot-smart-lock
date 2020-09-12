package com.example.iotsmartlock;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class FirebaseManager {

    static FirebaseDatabase database = FirebaseDatabase.getInstance();
    static DatabaseReference usersRef = database.getReference("Users/");

    static User loggedUser = null;

    private interface OnGetDataListener {
        void onSuccess(DataSnapshot dataSnapshot);
        void onStart();
        void onFailure();
    }

    private static void readData(Query ref, final OnGetDataListener listener) {
        listener.onStart();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listener.onSuccess(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onFailure();
            }
        });

    }

    public static void registerUser(final User user) {
        readData(usersRef.orderByChild("email").equalTo(user.getEmail()), new OnGetDataListener() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    usersRef.push().setValue(user);
                    RegisterActivity.registerStatus = RegisterActivity.REGISTERSUCCES;
                }
                else
                    RegisterActivity.registerStatus = RegisterActivity.EMAILUSED;
            }
            @Override
            public void onStart() {

            }
            @Override
            public void onFailure() {}
        });
    }

    public static void addDevice(Device device){
        loggedUser.addDevice(device);
        usersRef.child(loggedUser.getPushID()).child("devices").child(device.getHostAddress().replace(".", "@")).setValue(true);
    }

    public static void removeDevice(String device){
        FirebaseManager.loggedUser.deleteDevice(device);
        usersRef.child(loggedUser.getPushID()).child("devices").child(device.replace(".","@")).setValue(null);
    }

    public static void loginUser(final User user){
        readData(usersRef.orderByChild("email").equalTo(user.getEmail()), new OnGetDataListener() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot ds : dataSnapshot.getChildren()) {
                        String email = ds.child("email").getValue(String.class);
                        String password = ds.child("password").getValue(String.class);
                        String pushID = ds.getKey();

                        if (!password.equals(user.getPassword())) {
                            LoginActivity.loginStatus = LoginActivity.WRONGPASSWORD;
                        }
                        else {
                            loggedUser = new User(email, password, pushID);
                            for(DataSnapshot device : ds.child("devices").getChildren()){
                                loggedUser.addDevice(new Device(device.getKey().replace("@", ".")));
                            }
                            LoginActivity.loginStatus = LoginActivity.LOGINSUCCES;

                            FirebaseManager.loggedUser.printAll();
                        }
                    }
                }
                else
                {
                    LoginActivity.loginStatus = LoginActivity.WRONGEMAIL;
                }
            }
            @Override
            public void onStart() {

            }
            @Override
            public void onFailure() {}
        });
    }
}
