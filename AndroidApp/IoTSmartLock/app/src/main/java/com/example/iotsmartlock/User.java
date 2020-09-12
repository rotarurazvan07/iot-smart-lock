package com.example.iotsmartlock;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class User {
    private String email;
    private String password;
    private byte[] salt = hexStringToByteArray("e23be856157964e38a85c6648c6658ab");
    private List<Device> devices = new ArrayList<>();
    private String pushID;

    public String getPushID() {
        return pushID;
    }

    public void printAll(){
        System.out.println(email + password + pushID);
        for(Device dev : devices){
            System.out.println(dev.getHostAddress());
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    public void addDevice(Device device){
        for (Device temp : devices){
            if (temp.getHostAddress().equals(device.getHostAddress()))
                return;
        }
        devices.add(device);
    }
    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    User(String email, String password, String pushID){
        this.email = email;
        this.password = this.hashPassword(password);
        this.pushID = pushID;
    }

    public List<Device> getDevices() {
        return devices;
    }
    public void deleteDevice(String deviceName){
        Device temp = null;
        for (Device device : devices){
            if (device.getHostAddress() == deviceName)
                temp = device;
        }
        devices.remove(temp);
    }
    private String hashPassword(String password) {
        String generatedPassword = null;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt);
            byte[] bytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();

        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }

        return generatedPassword;
    }

}
