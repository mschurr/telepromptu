package com.google.android.glass.sample.stopwatch;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class TeleprompterService extends Service {

    private static final String TAG = "TeleprompterService";
    private static final String LIVE_CARD_TAG = "teleprompter";
	private String[] strings;
    private LiveCard mLiveCard;
    private TimelineManager mTimelineManager;
    private RemoteViews remoteViews;
	
    public TeleprompterService() {
    	this(new String[]{"Hey man","this is pretty cool", "I'm a thug", "Yolo swag"});
    }
    
	public TeleprompterService(String[] strings) {
		super();
		this.strings = strings;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
        mTimelineManager = TimelineManager.from(this);
        mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_TAG);
        remoteViews = new RemoteViews(this.getPackageName(), R.layout.card_teleprompter);
		for(String line : strings) {
			RemoteViews rowView = new RemoteViews(this.getPackageName(), R.layout.teleprompter_row);
			rowView.setCharSequence(R.id.teleprompt_row, "setText", line);
        	remoteViews.addView(R.layout.teleprompter_row, rowView);
		}
        mLiveCard.setViews(remoteViews);
        Intent menuIntent = new Intent(this, MenuActivity.class);
        menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
        mLiveCard.publish(PublishMode.REVEAL);
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
