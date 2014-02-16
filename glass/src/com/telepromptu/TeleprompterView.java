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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


/**
 * View used to display draw a running Chronometer.
 *
 * This code is greatly inspired by the Android's Chronometer widget.
 */
public class TeleprompterView extends FrameLayout {

	private static String TAG = "TeleprompterView";

    /**
     * Interface to listen for changes on the view layout.
     */
    public interface ChangeListener {
        /** Notified of a change in the view. */
        public void onChange();
    }

    // About 24 FPS.
    private static final long DELAY_MILLIS = 41;
    
    private final TextView mTextView;

    private boolean mStarted;
    private boolean mForceStart;
    private boolean mVisible;
    private boolean mRunning;
    private Context context;
    private SpeechRecognizer speechRecognizer;
    

    private long mBaseMillis;

    private ChangeListener mChangeListener;

    public TeleprompterView(Context context) {
        this(context, null, 0);
    }

    public TeleprompterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TeleprompterView(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
        LayoutInflater.from(context).inflate(R.layout.card_teleprompter, this);
        
        this.context = context;
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);       
        speechRecognizer.setRecognitionListener(new DictationListener()); 
        
        final String text = "Hello World! Testing Testing One Two Three YOLO HASHTAG YODO. In a real life situation this text is going to be really long and we wan't to make sure that this textview can handle this endless flow of text by allowing scrolling. Why does the python version have a Metaphone version that returns a list. Now the text is so long that it doesn't fit on the single textview. We must do something about this.";
        
   		final int offSet = text.indexOf("situation", 0);
   		Log.d(TAG, "Tag " + offSet);

   		mTextView = (TextView) findViewById(R.id.teleprompter_linear);
   		mTextView.setText(text);
   		mTextView.setMovementMethod(new ScrollingMovementMethod());

   		startListening();

        (new Timer()).schedule(new TimerTask() {
        	public void run() {
        		scrollDownBy(5);

                Layout l = mTextView.getLayout();
                if (l != null) {
                	int lineNumber = l.getLineForOffset(offSet);
                	Log.d(TAG, "Line number " + lineNumber);
                } else {
                	Log.d(TAG, "The layout wasn't found");
                }
        	}
        }, 5000);
        

        setBaseMillis(SystemClock.elapsedRealtime());
    }
    
    /**
     * Returns the line number for the subString specified. 
     * 
     * @param subString the substring to search for
     * @return the line number of the substring specified. -1 if the layout is not found.
     */
    public int lineNumberFor(String subString) {
    	String text = mTextView.getText().toString();
    	int offSet = text.indexOf(subString);
    	if (offSet != -1) {
    		Layout l = mTextView.getLayout();
    		if (l != null) {
    			int lineNumber = l.getLineForOffset(offSet);
    			return lineNumber;
    		}
    	}
    	
    	return -1;
    }
    
    public void scrollDownBy(int numLines) {
        mTextView.scrollBy(0, mTextView.getLineHeight() * numLines);
    }

    /**
     * Set the base value of the chronometer in milliseconds.
     */
    public void setBaseMillis(long baseMillis) {
        mBaseMillis = baseMillis;
        updateText();
    }

    /**
     * Get the base value of the chronometer in milliseconds.
     */
    public long getBaseMillis() {
        return mBaseMillis;
    }

    /**
     * Set a {@link ChangeListener}.
     */
    public void setListener(ChangeListener listener) {
        mChangeListener = listener;
    }

    /**
     * Set whether or not to force the start of the chronometer when a window has not been attached
     * to the view.
     */
    public void setForceStart(boolean forceStart) {
        mForceStart = forceStart;
        updateRunning();
    }

    /**
     * Start the chronometer.
     */
    public void start() {
        mStarted = true;
        updateRunning();
    }

    /**
     * Stop the chronometer.
     */
    public void stop() {
        mStarted = false;
        updateRunning();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;
        updateRunning();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mVisible = (visibility == VISIBLE);
        updateRunning();
    }

    private final Handler mHandler = new Handler();

    private final Runnable mUpdateTextRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRunning) {
                updateText();
                mHandler.postDelayed(mUpdateTextRunnable, DELAY_MILLIS);
            }
        }
    };

    /**
     * Update the running state of the chronometer.
     */
    private void updateRunning() {
        boolean running = (mVisible || mForceStart) && mStarted;
        if (running != mRunning) {
            if (running) {
                mHandler.post(mUpdateTextRunnable);
            } else {
                mHandler.removeCallbacks(mUpdateTextRunnable);
            }
            mRunning = running;
        }
    }
    
    /**
     * Update the value of the chronometer.
     */
    private void updateText() {
        long millis = SystemClock.elapsedRealtime() - mBaseMillis;
        // Cap chronometer to one hour.
        millis %= TimeUnit.HOURS.toMillis(1);

        millis %= TimeUnit.MINUTES.toMillis(1);
        millis = (millis % TimeUnit.SECONDS.toMillis(1)) / 10;
        if (mChangeListener != null) {
            mChangeListener.onChange();
        }
    }
    

    private void startListening() {
    	speechRecognizer.stopListening();
        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);        
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
        speechRecognizer.startListening(speechIntent);
    }
    
    private class DictationListener implements RecognitionListener {

		@Override
		public void onBeginningOfSpeech() {
        	Log.d(TAG, "Starting speech");
		}

		@Override
		public void onBufferReceived(byte[] arg0) {
		}

		@Override
		public void onEndOfSpeech() {
        	Log.d(TAG, "onEndOfSpeech");
	        speechRecognizer.destroy();
		}

		@Override
		public void onError(int arg0) {
		}

		@Override
		public void onEvent(int arg0, Bundle results) {
			Log.d(TAG, "onEvent of speech");
			ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			if(data.size() > 0) {
	        	Log.d(TAG, data.get(0));
			}
		}

		@Override
		public void onPartialResults(Bundle results) {
			Log.d(TAG, "onPartialResults of speech");
			ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			if(data.size() > 0) {
	        	Log.d(TAG, data.get(0));
			}
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
		}

		@Override
		public void onRmsChanged(float arg0) {
		}
    	
    }
}
