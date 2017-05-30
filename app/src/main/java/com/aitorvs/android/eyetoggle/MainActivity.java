/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Aitor Viana Sanchez
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.aitorvs.android.eyetoggle;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.aitorvs.android.eyetoggle.event.LeftEyeClosedEvent;
import com.aitorvs.android.eyetoggle.event.NeutralFaceEvent;
import com.aitorvs.android.eyetoggle.event.RightEyeClosedEvent;
import com.aitorvs.android.eyetoggle.tracker.FaceTracker;
import com.aitorvs.android.eyetoggle.util.PlayServicesUtil;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {

    private static final int REQUEST_CAMERA_PERM = 69;
    private static final String TAG = "FaceTracker";
    private SwitchCompat mSwitch;
    private View mLight;
    private FaceDetector mFaceDetector;
    private CameraSource mCameraSource;
    private final AtomicBoolean updating = new AtomicBoolean(false);
    private TextView mEmoticon;
    private FaceTracker face_tracker;
    private int check;
    private double left_thres = 0;
    private double right_thres = 0;
    private Button textbutton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSwitch = (SwitchCompat) findViewById(R.id.switchButton);
        mLight = findViewById(R.id.light);
        mEmoticon = (TextView) findViewById(R.id.emoticon);
        check = 0;

        try {
            Intent intent = this.getIntent();
            if(intent != null) {
                left_thres = intent.getDoubleExtra("Left_thred", 0.0);
                right_thres = intent.getDoubleExtra("Right_thred", 0.0);
                Toast.makeText(this, left_thres + " " + right_thres, Toast.LENGTH_SHORT).show();
            }
            }catch(NullPointerException e){
            e.printStackTrace();
        }
        // check that the play services are installed
        PlayServicesUtil.isPlayServicesAvailable(this, 69);

        // permission granted...?
        if (isCameraPermissionGranted()) {
            // ...create the camera resource
            createCameraResources();
        } else {
            // ...else request the camera permission
            requestCameraPermission();
        }


    }


    public void onClick( View v )
    {
        ToggleButton tb = ( ToggleButton ) v;
        Intent service = new Intent( this, ScreenFilterService.class );

        if( tb.isChecked() )
        {
            startService( service );
        }
        else
        {
            stopService( service );
        }

    }

    private String readTxt(){

        InputStream inputStream = getResources().openRawResource(R.raw.mytext);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int i;
        try {
            i = inputStream.read();
            while (i != -1)
            {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return byteArrayOutputStream.toString();
    }

    public void onClickText (View v ){

        setContentView(R.layout.text);
        TextView helloTxt = (TextView)findViewById(R.id.hellotxt);
        helloTxt.setText(readTxt());

        // check that the play services are installed
        PlayServicesUtil.isPlayServicesAvailable(this, 69);

        // permission granted...?
        if (isCameraPermissionGranted()) {
            // ...create the camera resource
            createCameraResources();
        } else {
            // ...else request the camera permission
            requestCameraPermission();
        }
    }

    public void onClickInitial (View v ){

        Intent intent=new Intent(MainActivity.this,FaceTrackerActivity.class);

        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }

        Toast.makeText(this, "초기화를 시작합니다", Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "시작버튼을 누른 후, 약 삼초간 눈을 감아주세요", Toast.LENGTH_SHORT).show();

        startActivity(intent);

    }
    /**
     * Check camera permission
     *
     * @return <code>true</code> if granted
     */
    private boolean isCameraPermissionGranted() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request the camera permission
     */
    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CAMERA_PERM);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CAMERA_PERM) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createCameraResources();
            return;
        }

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("EyeControl")
                .setMessage("No camera permission")
                .setPositiveButton("Ok", listener)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register the event bus
        EventBus.getDefault().register(this);

        // start the camera feed
        if (mCameraSource != null && isCameraPermissionGranted()) {
            try {
                //noinspection MissingPermission
                mCameraSource.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "onResume: Camera.start() error");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // unregister from the event bus
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

        // stop the camera source
        if (mCameraSource != null) {
            mCameraSource.stop();
        } else {
            Log.e(TAG, "onPause: Camera.stop() error");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // release them all...
        if (mFaceDetector != null) {
            mFaceDetector.release();
        } else {
            Log.e(TAG, "onDestroy: FaceDetector.release() error");
        }
        if (mCameraSource != null) {
            mCameraSource.release();
        } else {
            Log.e(TAG, "onDestroy: Camera.release() error");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLeftEyeClosed(LeftEyeClosedEvent e) {
        setEmoji(R.string.emoji_winking);

        if (mSwitch.isChecked() && catchUpdatingLock()) {
            mSwitch.setChecked(false);
            mLight.setBackgroundColor(ContextCompat.getColor(this, R.color.green));
            releaseUpdatingLock();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRightEyeClosed(RightEyeClosedEvent e) {
        setEmoji(R.string.emoji_winking);

        if (!mSwitch.isChecked() && catchUpdatingLock()) {
            mSwitch.setChecked(true);
            mLight.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
            releaseUpdatingLock();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNeutralFace(NeutralFaceEvent e) {
        setEmoji(R.string.emoji_neutral);
      //  Toast.makeText(this,"감음",Toast.LENGTH_SHORT).show();
    }

    private void setEmoji(int resource) {
        if (!mEmoticon.getText().equals(getString(resource)) && catchUpdatingLock()) {
            mEmoticon.setText(resource);
            releaseUpdatingLock();
        }
    }

    private boolean catchUpdatingLock() {
        // set updating and return previous value
        return !updating.getAndSet(true);
    }
    private void releaseUpdatingLock() {
        updating.set(false);
    }

    private void createCameraResources() {
        Context context = getApplicationContext();

        // create and setup the face detector
        mFaceDetector = new FaceDetector.Builder(context)
                .setProminentFaceOnly(true) // optimize for single, relatively large face
                .setTrackingEnabled(true) // enable face tracking
                .setClassificationType(/* eyes open and smile */ FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.FAST_MODE) // for one face this is OK
                .build();

        // now that we've got a detector, create a processor pipeline to receive the detection
        // results
        mFaceDetector = new FaceDetector.Builder(context)
                .setProminentFaceOnly(true) // optimize for single, relatively large face
                .setTrackingEnabled(true) // enable face tracking
                .setClassificationType(/* eyes open and smile */ FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.FAST_MODE) // for one face this is OK
                .build();

        // now that we've got a detector, create a processor pipeline to receive the detection
        // results

        mFaceDetector.setProcessor(new LargestFaceFocusingProcessor(mFaceDetector, face_tracker = new FaceTracker()));

        if(left_thres == 0 && right_thres == 0){

        }
        else{
            Toast.makeText(this, "개인화가 이미 되어 있습니다.", Toast.LENGTH_SHORT).show();
            face_tracker.set_indi(left_thres, right_thres);
        }



            // operational...?
        if (!mFaceDetector.isOperational()) {
            Log.w(TAG, "createCameraResources: detector NOT operational");
        } else {
            Log.d(TAG, "createCameraResources: detector operational");
        }

        // Create camera source that will capture video frames
        // Use the front camera
        mCameraSource = new CameraSource.Builder(this, mFaceDetector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30f)
                .build();
    }
}
