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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

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
    private SuperSpeechTraverser speechTraverser;
    
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
    	Log.d(TAG, intent.toUri(0));
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
        
        (new Thread(new Runnable(){

			@Override
			public void run() {
//				String text = "Hi! My name is Waseem Ahmad! I'm a senior studying computer science at Rice University. Today, I'm going to demonstrate an application that my team has created called Telepromptu. It is a Google Glass application that serves as a live automatic teleprompter. The application uses speech recognition to get snippets of text from Google Speech recognition API. Because the speech to text recognition is not fully accurate, our application uses a local subsequence alignment algorithm to match the recognized text with text on the teleprompter.";
				String presentationId = "1VkYAnSokGCLiSHs33v7VTYttHaWPvmuLFIFNS5FudY4";
				List<Slide> slides = connect("http://telepromptu.appspot.com/glass?id=" + presentationId);				
				String text = "";
				for(Slide slide : slides) {
					text += slide.notes + " ";
				}
				mCallback.mTeleprompterView.setText(text);
				speechTraverser = new SuperSpeechTraverser(text);
			}
        	
        })).start();
        
        
        
        startListening();

        return START_STICKY;
    }
    
    public List<Slide> connect(String url)
    {
        HttpClient httpclient = new DefaultHttpClient();

        // Prepare a request object
        Log.d(TAG, "Executing get request");
        HttpGet httpget = new HttpGet(url); 

        // Execute the request
        HttpResponse response;
        try {
            response = httpclient.execute(httpget);
            // Examine the response status
            Log.i("Praeda",response.getStatusLine().toString());

            // Get hold of the response entity
            HttpEntity entity = response.getEntity();
            // If the response does not enclose an entity, there is no need
            // to worry about connection release

            if (entity != null) {

                // A Simple JSON Response Read
                InputStream instream = entity.getContent();
                String result= convertStreamToString(instream);
                Log.d(TAG, result);
                // now you have the string representation of the HTML request
                instream.close();
                ArrayList<Slide> slides = new ArrayList<Slide>();
                JSONArray jsonObj = new JSONArray(result);
                for (int i = 0; i < jsonObj.length(); i++) {
                	JSONObject s = jsonObj.getJSONObject(i);
					slides.add(new Slide(s.getString("speaker_notes"),s.getString("page_id"),s.getString("img_url")));
				}
                Log.d(TAG, slides.get(0).notes);
                return slides;
            }

        } catch (Exception e) {
        	e.printStackTrace();
        }
        return new ArrayList<Slide>();
    }

        private static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
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
//    		speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
//    		speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 300000);
//    		speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 300000);
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
				String text = data.get(0);
	        	Log.d(TAG, data.get(0));
	        	speechTraverser.inputSpeech(text);
	        	int lastWordNumber = speechTraverser.getCurrentWord();
	        	int numCharacters = 0;
	        	for (String w : speechTraverser.getWords().subList(0, lastWordNumber)) {
	        		numCharacters += w.length();
	        	}
	        	numCharacters = Math.max(numCharacters - 15, 0);
	        	String lastWordSpoken = speechTraverser.getCurrentWordString();
	        	Log.d(TAG, "Last word number: " + lastWordNumber + " spoken: " + lastWordSpoken);
	        	TeleprompterView tView = mCallback.mTeleprompterView;
	        	int lineNumber = tView.lineNumberFor(numCharacters);
	        	Log.d(TAG, "Line number: " + lineNumber);

	        	mCallback.mTeleprompterView.scrollToLineNumber(lineNumber);
				stopListening();
				startListening();
			}
		}

		@Override
		public void onRmsChanged(float arg0) {
		}
    	
    }
    
}
