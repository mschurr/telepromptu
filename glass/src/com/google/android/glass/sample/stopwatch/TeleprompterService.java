package com.google.android.glass.sample.stopwatch;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;

public class TeleprompterService extends Service {

    private static final String TAG = "TeleprompterService";
    private static final String LIVE_CARD_TAG = "teleprompter";
	private String[] strings;
    private LiveCard mLiveCard;
    private TimelineManager mTimelineManager;
    private RemoteViews remoteViews;
	
    public TeleprompterService() {
    	this.strings = (new String[]{"Hey man","this is pretty cool", "I'm a thug", "Yolo swag"});
    }
    
	@Override
	public void onCreate() {
		super.onCreate();
        mTimelineManager = TimelineManager.from(this);
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	if (mLiveCard == null) {
            Log.d(TAG, "Publishing LiveCard");
            mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_TAG);
            remoteViews = new RemoteViews(this.getPackageName(), R.layout.card_teleprompter);
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
	        
    		for(String line : strings) {
    			Log.d(TAG,"String : "+line);
    			RemoteViews row = new RemoteViews(this.getPackageName(), R.layout.teleprompter_row);
    			row.setCharSequence(R.id.teleprompt_row, "setText", line);
        		remoteViews.addView(R.id.teleprompter_linear, row);
    		}
            mLiveCard.setViews(remoteViews);
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

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
