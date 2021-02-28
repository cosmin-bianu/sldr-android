package com.kitsuneark.slider.handlers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.kitsuneark.slider.activities.RemoteControlActivity;
import com.kitsuneark.slider.activities.ScannerActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class MessageHandler {

    private static final String TAG = "MessageHandler";
    private static final int CONNECTION_TIMEOUT = 1100;

    //Singleton///////////////////////////////////////////////////////////
    private static MessageHandler instance = new MessageHandler();
    private MessageHandler(){}
    public static MessageHandler getInstance(){
        return instance;
    }
    //Singleton///////////////////////////////////////////////////////////

    private Socket socket;
    private OutputStream os;
    private Thread connectionEstablishmentThread;
    private Thread inboundThread;
    private boolean isAlive;

    public void setRemoteControlActivity(RemoteControlActivity remoteControlActivity) {
        this.remoteControlActivity = remoteControlActivity;
    }

    private RemoteControlActivity remoteControlActivity;

   public void onButtonUp(){
       sendByte((byte)255);
    }

    public void onButtonDown(){
        sendByte((byte)127);
    }

    public void onLocked(){
       sendByte((byte)32);
    }

    public void onUnlocked(){
       sendByte((byte)64);
    }

    public void startThreads(final Context context, final String[] ipList, final int port, final String key, final ScannerActivity scannerActivity){
        if(connectionEstablishmentThread ==null || !connectionEstablishmentThread.isAlive()){
            connectionEstablishmentThread = new Thread(new Runnable(){
                @Override
                public void run(){
                    establishConnection(context, ipList, port, key, scannerActivity);
                    Log.d(TAG, "run: Finishing Thread");
                }
            });
            connectionEstablishmentThread.start();
        }
    }

    private void ReceiveSignals() throws InterruptedException {
        try {
            InputStream is = socket.getInputStream();
            while(true){
                byte[] buf = new byte[1];
                Log.d(TAG, "ReceiveSignals: buffering.");
                if(is.read(buf, 0, 1)!=-1){
                    final byte b  = buf[0];
                    Log.d(TAG, "ReceiveSignals: buffer:" + b);
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "run: Running handler");
                            switch(b){
                                case 64:
                                    Log.d(TAG, "run: Locking");
                                    remoteControlActivity.setLock(true, true);
                                    break;
                                case 127:
                                    Log.d(TAG, "run: Unlocking");
                                    remoteControlActivity.setLock(false, true);
                                    break;
                            }
                        }
                    });
                    Thread.sleep(100);
                } else throw new IOException();
            }
        }catch(IOException e){
            e.printStackTrace();
            forceInterrupt("Inbound Connection IO Exception");
        }
    }

    private void establishConnection(Context context, final String[] ipList, final int port, final String key, final ScannerActivity scannerActivity) {
        try {
            Log.d(TAG, "establishConnection: Total IP's: " + ipList.length);
            for (String ip : ipList) {
                ip = ip.trim();
                socket = new Socket();
                try {
                    socket.connect(new InetSocketAddress(ip, port), CONNECTION_TIMEOUT);
                } catch (SocketTimeoutException | ConnectException ste){
                    ste.printStackTrace();
                    Log.d(TAG, "establishConnection: Failed to connect, continuing");
                    continue;
                }
                Log.d(TAG, "establishConnection: isConnected=" + socket.isConnected());
                if (socket.isConnected()){
                    finalizeConnection(context, key);
                    scannerActivity.startNextActivity();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Looper.prepare();

            forceInterrupt("Connection error");
        }
    }

    private void finalizeConnection(Context context, String key) throws IOException{
        os = socket.getOutputStream();
        sendString(key);
        String deviceName = Settings.Secure.getString(context.getContentResolver(), "bluetooth_name");
        sendString(deviceName);
        inboundThread = new Thread(new Runnable(){
            @Override
            public void run(){
                Looper.prepare();
                try {
                    ReceiveSignals();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        inboundThread.start();
        isAlive = true;
    }

    private void sendString(String s) throws IOException {
        byte[] bytesToSend = s.getBytes();
        int len = bytesToSend.length;
        byte[] toSendLen = new byte[4];
        toSendLen[0] = (byte) (len & 0xff);
        toSendLen[1] = (byte) ((len >> 8) & 0xff);
        toSendLen[2] = (byte) ((len >> 16) & 0xff);
        toSendLen[3] = (byte) ((len >> 24) & 0xff);
        os.write(toSendLen);
        os.write(bytesToSend);
    }

    private void sendByte(final byte value){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] toSend = new byte[1];
                    toSend[0] = value;
                    os.write(toSend);
                } catch (IOException | NullPointerException e) {
                    Looper.prepare();
                    forceInterrupt("Remote computer has become unreachable.");
                }
            }
        });
        thread.start();
    }

    public void shutdown(Context context, String src){
       if(connectionEstablishmentThread != null && connectionEstablishmentThread.isAlive())
            connectionEstablishmentThread.interrupt();
       try {
           if(socket != null)
            socket.close();
           if(remoteControlActivity != null)
               remoteControlActivity.finish();
           isAlive = false;
           Log.d(TAG, "shutdown: Reason/Source: " + src);
       }catch (IOException e) {
           Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
       }
    }

    private void forceInterrupt(final String reason){
       new Handler(Looper.myLooper()).post(new Runnable() {
           @Override
           public void run() {
               Toast.makeText(remoteControlActivity, reason, Toast.LENGTH_SHORT).show();
           }
       });
       shutdown(remoteControlActivity, reason);
    }

    public boolean isAlive(){
       return isAlive;
    }
}
