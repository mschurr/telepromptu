package com.telepromptu;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class TeleprompterServiceDeprecated extends Service {

    private static final String TAG = "TeleprompterService";
    private static final String LIVE_CARD_TAG = "teleprompter";
	private String[] strings;
    private LiveCard mLiveCard;
    private TimelineManager mTimelineManager;
    private RemoteViews remoteViews;
	
    public TeleprompterServiceDeprecated() {
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
            
            // Create the remote view
            remoteViews = new RemoteViews(this.getPackageName(), R.layout.card_teleprompter);

//            TextView tv = (TextView)findViewbyId(R.id.teleprompter_linear);
            remoteViews.setTextViewText(R.id.teleprompter_linear, "Hello World! Testing Testing One Two Three YOLO HASHTAG YODO. In a real life situation this text is going to be really long and we wan't to make sure that this textview can handle this endless flow of text by allowing scrolling. Why does the python version have a Metaphone version that returns a list. Now the text is so long that it doesn't fit on the single textview. We must do something about this.");


            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
	        
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
