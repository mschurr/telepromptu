/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.telepromptu;

import java.util.ArrayList;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.timeline.TimelineManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;



/**
 * Service owning the LiveCard living in the timeline.
 */
public class TeleprompterService extends Service {

    private static final String TAG = "TeleprompterService";
    private static final String LIVE_CARD_TAG = "teleprompter";

    private TeleprompterDrawer mCallback;

    private TimelineManager mTimelineManager;
    private LiveCard mLiveCard;
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    
    @Override
    public void onCreate() {
        super.onCreate();
        mTimelineManager = TimelineManager.from(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	super.onStartCommand(intent, flags, startId);
        if (mLiveCard == null) {
            Log.d(TAG, "Publishing LiveCard");
            mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_TAG);

            // Keep track of the callback to remove it before unpublishing.
            mCallback = new TeleprompterDrawer(this);
            mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mCallback);

            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            mLiveCard.publish(PublishMode.REVEAL);
            Log.d(TAG, "Done publishing LiveCard");
        } else {
            // TODO(alainv): Jump to the LiveCard when API is available.
        }
        startListening();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(speechIntent != null) {
        	stopService(speechIntent);        	
        }
        stopListening();
        if (mLiveCard != null && mLiveCard.isPublished()) {
            Log.d(TAG, "Unpublishing LiveCard");
            if (mCallback != null) {
                mLiveCard.getSurfaceHolder().removeCallback(mCallback);
            }
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }
    

    private void startListening() {
    	if (speechRecognizer == null) {
    		speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);       
    		speechRecognizer.setRecognitionListener(new DictationListener());
    		Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);        
    		speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    		speechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());
    		speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
    		speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
    		speechRecognizer.startListening(speechIntent);    		
    	}
    }
    
    private void stopListening() {
    	if (speechRecognizer != null) {
    		speechRecognizer.stopListening(); 
    		speechRecognizer = null;
    	}
    }
    

    private class DictationListener implements RecognitionListener {

		@Override
		public void onBeginningOfSpeech() {
        	Log.d(TAG, "Starting speech");
		}

		@Override
		public void onBufferReceived(byte[] arg0) {}

		@Override
		public void onEndOfSpeech() {
        	Log.d(TAG, "onEndOfSpeech ");
		}

		@Override
		public void onError(int errorCode) {
			switch (errorCode) {
			case 1:
				Log.e(TAG, "Network timeout");
				stopListening();
				startListening();
				break;
			case 2:
				Log.e(TAG, "No internet connection found.");
				break;
			case 5:
				Log.d(TAG, "Generic error.");
				break;
			case 7:
				Log.d(TAG, "No match found.");
				stopListening();
				startListening();
				break;
			default:
				Log.d(TAG, "onError on Listening : "+errorCode);
				break;
			}
		}

		@Override
		public void onEvent(int arg0, Bundle results) {}

		@Override
		public void onPartialResults(Bundle results) {
			Log.d(TAG, "onPartialResults of speech");
		}

		@Override
		public void onReadyForSpeech(Bundle arg0) {
		}

		@Override
		public void onResults(Bundle results) {
        	Log.d(TAG, "onResult of speech");
			ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			if(data.size() > 0) {
	        	Log.d(TAG, data.get(0));
			}
			stopListening();
			startListening();
		}

		@Override
		public void onRmsChanged(float arg0) {
		}
    	
    }
}
