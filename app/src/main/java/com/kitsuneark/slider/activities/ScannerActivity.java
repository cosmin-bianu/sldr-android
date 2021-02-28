package com.kitsuneark.slider.activities;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.kitsuneark.slider.R;
import com.kitsuneark.slider.handlers.MessageHandler;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler{

    //private static final String TAG = "ScannerActivity";
    private static final int CAMERA_REQUEST_CODE = 4378;

    private ZXingScannerView mScannerView;
    private AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        final Activity thisInstance = this;
        final int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        setContentView(R.layout.activity_scanner);


        //MobileAds.initialize(this, "ca-app-pub-5590113961797976~9201388515");

        builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.camera_rationale_message)
                .setTitle(R.string.camera_rationale_title)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(thisInstance,new String[]{Manifest.permission.CAMERA},CAMERA_REQUEST_CODE);
                    }
                });


        if(permissionCheck==PackageManager.PERMISSION_DENIED)
            builder.create().show();


        mScannerView = findViewById(R.id.sv_scanner_view);

        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        adView.loadAd(adRequest);

        Button demoButton = findViewById(R.id.btn_demo);
        demoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNextActivity();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(MessageHandler.getInstance().isAlive())
            MessageHandler.getInstance().shutdown(this, "Resumed scanner activity");
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();

    }

    @Override
    public void handleResult(final Result result) {
        final BarcodeFormat format  = result.getBarcodeFormat();
        if(format==BarcodeFormat.QR_CODE){
            final String resultString = result.getText();
            final String[] split = resultString.split(";");
            if(split.length!=3){
                Toast.makeText(this, R.string.invalid_qr_code, Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                final int port = Integer.parseInt(split[0]);
            final String key = split[1];
            final String[] ipList = split[2].split(",");
            if(key == null || key.equals("") || ipList.length==0){
                Toast.makeText(this, R.string.invalid_qr_code, Toast.LENGTH_SHORT).show();
                return;
            }
            MessageHandler.getInstance().startThreads(mScannerView.getContext(), ipList, port, key, this);
            }catch (NumberFormatException e){
                Toast.makeText(this, R.string.invalid_qr_code, Toast.LENGTH_SHORT).show();
            }
        }

        mScannerView.resumeCameraPreview(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showCameraRationale();
            }
        }
    }

    private void showCameraRationale(){
        builder.create().show();
    }

    public void startNextActivity(){
        startActivity(new Intent(this, RemoteControlActivity.class));
    }
}
