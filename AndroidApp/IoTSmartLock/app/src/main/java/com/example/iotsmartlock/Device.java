package com.example.iotsmartlock;

public class Device {
    private String hostAddress;

    Device(String hostAddress){
        this.hostAddress = hostAddress;
    }

    public String getHostAddress() {
        return hostAddress;
    }

}
