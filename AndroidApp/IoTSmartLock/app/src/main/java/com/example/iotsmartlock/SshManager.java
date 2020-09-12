package com.example.iotsmartlock;

import android.app.ProgressDialog;
import android.widget.ProgressBar;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SshManager {

    static int hostFound = 0;
    static int passwordChanged = 0;
    static int resetDevice = 0;
    static int restartDevice = 0;
    static int addFace = 0;
    final static int HOSTFOUNDSUCCES = 1;
    final static int HOSTWRONGIP = 2;
    final static int HOSTWRONGPASSWORD = 3;
    final static int HOSTCONNECTIONREFUSED = 4;
    final static int HOSTCONNECTIONTIMEDOUT = 5;


    public static void checkHost(String IP, String password){
        try{
            JSch jsch = new JSch();
            Session session = jsch.getSession("pi", IP, 22);
            session.setPassword(password);

            // Avoid asking for key confirmation
            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            session.setConfig(prop);

            session.connect();
            session.disconnect();
        }
        catch (JSchException e){
            if (e.toString().contains("Host unreachable")){
                hostFound = HOSTWRONGIP;
            }
            else if (e.toString().contains("Auth fail")){
                hostFound = HOSTWRONGPASSWORD;
            }
            else if (e.toString().contains("Connection refused")){
                hostFound = HOSTCONNECTIONREFUSED;
            }
            else if (e.toString().contains("Connection timed out"))
                hostFound = HOSTCONNECTIONTIMEDOUT;
            System.out.println(e.toString());
            return;
        }

        hostFound = HOSTFOUNDSUCCES;
    }

    public static void changePassword(String IP, String currPassword, String newPassword){
        checkHost(IP, currPassword);

        if (hostFound == HOSTFOUNDSUCCES) {
            System.out.println("Host is up, Changing password to " + newPassword);

            try{
                JSch jsch = new JSch();
                Session session = jsch.getSession("pi", IP, 22);
                session.setPassword(currPassword);

                Properties prop = new Properties();
                prop.put("StrictHostKeyChecking", "no");
                session.setConfig(prop);

                session.connect();

                ChannelExec channelSsh = (ChannelExec)
                        session.openChannel("exec");
                channelSsh.setCommand("echo 'pi:" + newPassword + "' | sudo chpasswd");
                channelSsh.connect();
                Thread.sleep(5000);
                channelSsh.disconnect();

                session.disconnect();
            }
            catch (JSchException | InterruptedException e){
                e.printStackTrace();
            }
        }
        passwordChanged = hostFound;
        hostFound = 0;
    }

    public static void resetDevice(String IP, String password){
        checkHost(IP, password);
        if (hostFound == HOSTFOUNDSUCCES) {
            System.out.println("resetting device " + IP);

            try{
                JSch jsch = new JSch();
                Session session = jsch.getSession("pi", IP, 22);
                session.setPassword(password);

                Properties prop = new Properties();
                prop.put("StrictHostKeyChecking", "no");
                session.setConfig(prop);

                session.connect();

                ChannelExec channelSsh = (ChannelExec)
                        session.openChannel("exec");
                channelSsh.setCommand("cd MyGit/iot-smart-lock/ && nohup ./reset.sh &");
                channelSsh.connect();
                Thread.sleep(5000);
                channelSsh.disconnect();

                session.disconnect();
            }
            catch (JSchException | InterruptedException e){
                e.printStackTrace();
            }

        }
        resetDevice = hostFound;
        hostFound = 0;
    }

    public static void restartDevice(String IP, String password){
        checkHost(IP, password);

        if (hostFound == HOSTFOUNDSUCCES){
            System.out.println("restarting device " + IP);

            try{
                JSch jsch = new JSch();
                Session session = jsch.getSession("pi", IP, 22);
                session.setPassword(password);

                Properties prop = new Properties();
                prop.put("StrictHostKeyChecking", "no");
                session.setConfig(prop);

                session.connect();

                ChannelExec channelSsh = (ChannelExec)
                        session.openChannel("exec");
                channelSsh.setCommand("cd MyGit/iot-smart-lock/ && nohup ./restart.sh &");
                channelSsh.connect();
                Thread.sleep(5000);
                channelSsh.disconnect();

                session.disconnect();
            }
            catch (JSchException | InterruptedException e){
                e.printStackTrace();
            }
        }
        restartDevice = hostFound;
        hostFound = 0;
    }

    public static void addFace(String IP, String password, String faceOwner, ProgressDialog progressBar){
        checkHost(IP, password);

        if (hostFound == HOSTFOUNDSUCCES){
            System.out.println("add face " + IP + " " + faceOwner);

            try{
                JSch jsch = new JSch();
                Session session = jsch.getSession("pi", IP, 22);
                session.setPassword(password);

                Properties prop = new Properties();
                prop.put("StrictHostKeyChecking", "no");
                session.setConfig(prop);

                session.connect();

                ChannelExec channelSsh = (ChannelExec)
                        session.openChannel("exec");
                channelSsh.setCommand("cd MyGit/iot-smart-lock/ && ./retrain.sh " + faceOwner);
                System.out.println("cd MyGit/iot-smart-lock/ && ./retrain.sh " + faceOwner);
                channelSsh.setInputStream(null);
                ((ChannelExec)channelSsh).setErrStream(System.err);
                try {
                    InputStream in=channelSsh.getInputStream();
                    channelSsh.connect();
                    progressBar.setMessage("Look at the camera");
                    byte[] tmp=new byte[1024];
                    while(true){
                        String message = "";
                        while(in.available()>0){
                            int i=in.read(tmp, 0, 1024);
                            if(i<0)break;
                            message = new String(tmp, 0, i);
                            System.out.print(message);
                        }
                        if(channelSsh.isClosed()){
                            if(in.available()>0) continue;
                            System.out.println("exit-status: "+channelSsh.getExitStatus());
                            break;
                        }
                        if (message.contains("Gathered"))
                            progressBar.setMessage("Face is training");
                        if (message.contains("Trained"))
                            break;
                        try{Thread.sleep(1000);}catch(Exception ee){}
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                channelSsh.disconnect();
                session.disconnect();
            }
            catch (JSchException e){
                e.printStackTrace();
            }
        }
        addFace = hostFound;
        hostFound = 0;
    }
}
