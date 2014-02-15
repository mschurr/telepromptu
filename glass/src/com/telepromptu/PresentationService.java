package com.telepromptu;

import java.util.ArrayList;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;

public class PresentationService extends Service {

    private static final String TAG = "PresentationService";
    private static final String LIVE_CARD_TAG = "presentation";
    private LiveCard mLiveCard;
    private TimelineManager mTimelineManager;
    private TextView cardText;
    private RemoteViews remoteViews;
    private SpeechRecognizer speechRecognizer;
    private Handler mainHandler;
    
    @Override
    public void onCreate() {
        super.onCreate();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);       
        speechRecognizer.setRecognitionListener(new DictationListener()); 
        mTimelineManager = TimelineManager.from(this);
        mainHandler = new Handler(this.getMainLooper());
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {
            Log.d(TAG, "Publishing LiveCard");
            mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_TAG);
            remoteViews = new RemoteViews(this.getPackageName(), R.layout.card_speech_recognition);
            mLiveCard.setViews(remoteViews);
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
	        
            Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);        
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());
            speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
            speechRecognizer.startListening(speechIntent);
            
            mLiveCard.publish(PublishMode.REVEAL);
            Log.d(TAG, "Done publishing LiveCard");
        } else {
            // TODO(alainv): Jump to the LiveCard when API is available.
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            Log.d(TAG, "Unpublishing LiveCard");
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }
    
    private void updateText(String text) {
        if (mLiveCard != null && mLiveCard.isPublished()) {
        	remoteViews.setCharSequence(R.id.current_text, "setText", text);
        	Log.d(TAG, "Updated the slide text");
        }
    }
    
    private class DictationListener implements RecognitionListener {

		@Override
		public void onBeginningOfSpeech() {
			updateText("Starting speech");
        	Log.d(TAG, "Starting speech");
		}

		@Override
		public void onBufferReceived(byte[] arg0) {
		}

		@Override
		public void onEndOfSpeech() {
		}

		@Override
		public void onError(int arg0) {
		}

		@Override
		public void onEvent(int arg0, Bundle arg1) {
		}

		@Override
		public void onPartialResults(Bundle arg0) {
		}

		@Override
		public void onReadyForSpeech(Bundle arg0) {
		}

		@Override
		public void onResults(Bundle results) {
        	Log.d(TAG, "onResult of speech");
			ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			if(data.size() > 0) {
				updateText(data.get(0));
	        	Log.d(TAG, data.get(0));
			}
		}

		@Override
		public void onRmsChanged(float arg0) {
		}
    	
    }
    
}
