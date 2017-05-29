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

package com.aitorvs.android.eyetoggle.tracker;

import com.aitorvs.android.eyetoggle.event.LeftEyeClosedEvent;
import com.aitorvs.android.eyetoggle.event.NeutralFaceEvent;
import com.aitorvs.android.eyetoggle.event.RightEyeClosedEvent;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;

import org.greenrobot.eventbus.EventBus;

public class FaceTracker extends Tracker<Face> {

    private static final float PROB_THRESHOLD = 0.7f;
    private static final String TAG = FaceTracker.class.getSimpleName();
    private boolean leftClosed;
    private boolean rightClosed;
    private double left_thres;
    private double right_thres;
    private boolean initial_check;
    private Face mface;

    public void set_indi(double left, double right){
        left_thres = left;
        right_thres = right;
        initial_check = true;
    }

    @Override
    public void onUpdate(Detector.Detections<Face> detections, Face face) {
        if(initial_check == false) {
            if (leftClosed && face.getIsLeftEyeOpenProbability() > PROB_THRESHOLD) {
                leftClosed = false;
            } else if (!leftClosed && face.getIsLeftEyeOpenProbability() < PROB_THRESHOLD) {
                leftClosed = true;
            }
            if (rightClosed && face.getIsRightEyeOpenProbability() > PROB_THRESHOLD) {
                rightClosed = false;
            } else if (!rightClosed && face.getIsRightEyeOpenProbability() < PROB_THRESHOLD) {
                rightClosed = true;
            }
        }
        else {
            if (leftClosed && face.getIsLeftEyeOpenProbability() > left_thres) {
                leftClosed = false;
            } else if (!leftClosed && face.getIsLeftEyeOpenProbability() < left_thres) {
                leftClosed = true;
            }
            if (rightClosed && face.getIsRightEyeOpenProbability() > right_thres) {
                rightClosed = false;
            } else if (!rightClosed && face.getIsRightEyeOpenProbability() < right_thres) {
                rightClosed = true;
            }
        }

        if (leftClosed && !rightClosed) {
            EventBus.getDefault().post(new LeftEyeClosedEvent());
        } else if (rightClosed && !leftClosed) {
            EventBus.getDefault().post(new RightEyeClosedEvent());
        } else if (!leftClosed && !rightClosed) {
            EventBus.getDefault().post(new NeutralFaceEvent());
        }

    }
}
