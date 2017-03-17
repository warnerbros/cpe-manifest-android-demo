package com.wb.cpedemo.mainmoviefragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;

import com.wb.nextgenlibrary.fragment.AbstractNGEMainMovieFragment;
import com.wb.nextgenlibrary.widget.CustomMediaController;

import com.wb.cpedemo.R;
import com.wb.cpedemo.mainmoviefragment.SimpleObserveVideoView.IVideoViewActionListener;

/**
 * Created by gzcheng on 10/28/16.
 */

public class SimpleMainMovieFragment extends AbstractNGEMainMovieFragment implements SimpleObserveVideoView.PlayPauseListener{


	protected SimpleObserveVideoView videoView;
	protected MediaController mc;
	//protected ProgressDialog mDialog;

	private int mStartTime;
	private boolean mPlaybackPrepared;
	private boolean mPlaybackCompleted;
	private MediaPlayer mPlayer;
	private boolean isPlaying;

	private boolean bPlayAtResumeActivityState = false;
	//private boolean mIsSubtitleON = false;


	private boolean bErrorOut = false;

	public static final int LOCAL_PLAYBACK_STARTTIME = -1;

	CustomMediaController customMediaController;

	String movieUrl = "";

	final static String NGE_DEMO_RESUME_TIME_KEY = "NGE_DEMO_RESUME_TIME";
	final static String NGE_DEMO_PLAYBACK_STATE_KEY = "NGE_DEMO_PLAYBACK_STATE_KEY";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.video_player_view, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);

		videoView = (SimpleObserveVideoView) view.findViewById(R.id.surface_view);
		if(mc != null)
			videoView.setMediaController(mc);
		videoView.setOnErrorListener(getOnErrorListener());
		videoView.setOnPreparedListener(getOnPreparedListener());
		videoView.setOnCompletionListener(getOnCompletionListener());
		videoView.requestFocus();
	}

    /*@Override
    public void setProgressDialog(ProgressDialog dialog){
        mDialog = dialog;
    }*/

	@Override
	public void setCustomMediaController(CustomMediaController customMC){
		mc = customMC;
		if (videoView != null)
			videoView.setMediaController(mc);
	}

	public void setResumeTime(int resumeTime){
		mStartTime = resumeTime;
	}

	@Override
	public void setPlaybackObject(Object playbackObject){
		if (playbackObject instanceof String)
			movieUrl = (String) playbackObject;
	}

	@Override
	public int getCurrentPosition(){
		if (videoView != null) {
			try {
				return videoView.getCurrentPosition();
			}catch (Exception ex){
				return 0;
			}
		}else
			return 0;
	}

	@Override
	public int getDuration(){
		if (videoView != null) {
			try {
				return videoView.getDuration();
			}catch (Exception ex){
				return 0;
			}
		}else
			return 0;
	}

	@Override
	public boolean isPlaying(){
		if (videoView != null)
			return videoView.isPlaying();
		else
			return false;
	}

	@Override
	public void pause(){
		videoView.pause();
	}

	@Override
	public void resumePlayback(){
		videoView.start();
	}

	@Override
	public void streamStartPreparations(final com.wb.nextgenlibrary.util.concurrent.ResultListener<Boolean> resultListener){
		resultListener.onResult(true);

	}

	@Override
	public void onDestroy(){
		//mc.onDestroy();
		//mc = null;
		super.onDestroy();
		playbackActive = false;
	}

	public void onResume() {
		super.onResume();
		isPlaying = wasPlaying();

		trackPlaybackEvent("playback_init_", 0);

		Uri uri = Uri.parse(movieUrl);

		videoView.setVisibility(View.VISIBLE);
		videoView.setVideoURI(uri);

		mPlaybackPrepared = false;

		videoView.setOnPreparedListener(getOnPreparedListener());

		videoView.setMediaController(mc);
		showLoadingView();
	}

	private class ErrorListener implements MediaPlayer.OnErrorListener {
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {


			showErrorDialogHandler.sendMessage(Message.obtain(null, what, extra));


			return true;
		}
	}

	private final Handler showErrorDialogHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (getActivity().isFinishing()) {
				//FlixsterLogger.d(F.TAG_DRM, "WidevinePlayer.showErrorDialogHandler activity finished");
				return;
			}

			// Release videoView
			if (videoView.isPlaying())
				videoView.stopPlayback();
			videoView.setVideoURI(null);
			videoView.setVisibility(View.GONE);

			hideLoadingView();
			//mDialog.hide();

			int what = msg.what;
			int extra = (Integer)msg.obj;
			String message = "Error streaming";

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(message).setCancelable(false)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							getActivity().finish();
						}
					});
			AlertDialog alert = builder.create();
			alert.show();

		}
	};



	private class PreparedListener implements MediaPlayer.OnPreparedListener {
		@Override
		public void onPrepared(MediaPlayer mp) {
			if (getActivity().isFinishing()) {

				return;
			}

				hideLoadingView();
				//mDialog.hide();
				mp.setOnInfoListener(new InfoListener());

				if (mStartTime >= videoView.getDuration()) {
					mStartTime = 0;
				}

				videoView.requestFocus();
					try {
						Thread.sleep(1000);
					} catch (Exception ex) {
					}


				videoView.start();


					try {
						Thread.sleep(10);
					} catch (Exception ex) {
					}


				videoView.seekTo(mStartTime);



				videoView.setVideoViewListener(new IVideoViewActionListener() {

					@Override
					public void onTimeBarSeekChanged(int currentTime) {
						savePlayPosition(currentTime);
						if (nextGenVideoViewListener != null)
							nextGenVideoViewListener.onTimeBarSeekChanged(currentTime);
					}

					@Override
					public void onResume() {
						isPlaying = true;
						savePlayState(isPlaying);
						if (nextGenVideoViewListener != null)
							nextGenVideoViewListener.onVideoResume();
					}

					@Override
					public void onPause() {
						isPlaying = false;
						savePlayState(isPlaying);
						if (nextGenVideoViewListener != null)
							nextGenVideoViewListener.onVideoPause();
					}
				});

				videoView.setPlayPauseListener(SimpleMainMovieFragment.this);

				mPlaybackPrepared = true;
				mPlayer = mp;

			}


	}


	private class CompletionListener implements MediaPlayer.OnCompletionListener {
		@Override
		public void onCompletion(MediaPlayer mp) {
			trackPlaybackEvent("playback_complete", 0);

			mPlaybackCompleted = true;
			savePlayPosition(0);

			if (completionListener != null)
				completionListener.onCompletion(mp);

		}
	}


	protected MediaPlayer.OnErrorListener getOnErrorListener(){
		return new ErrorListener();
	}

	protected MediaPlayer.OnPreparedListener getOnPreparedListener(){
		return new PreparedListener();
	}

	protected MediaPlayer.OnCompletionListener getOnCompletionListener(){
		return new CompletionListener();
	}

	@Override
	public void onStart() {
		super.onStart();
		Intent intent = getActivity().getIntent();

		//mLocalBroadcaster = CastLocalBroadcaster.get(getActivity());

		if (!bPlayAtResumeActivityState) {
			mStartTime = 0;
			if (mStartTime == LOCAL_PLAYBACK_STARTTIME)
				mStartTime = loadPlayPosition();
		}else{
			mStartTime = loadPlayPosition();
		}
	}

	boolean playbackActive = false;

	@Override
	public void onStop() {
		super.onStop();
	}

	/**
	 * Get play position from prefs
	 * This function returns Second, please * 1000 for player to use as player uses MiniSecond
	 */
	private int loadPlayPosition() {
		SharedPreferences prefs = getActivity().getSharedPreferences("PlayPosition", 0);
		int second = prefs.getInt(NGE_DEMO_RESUME_TIME_KEY, 0);
		return prefs.getInt(NGE_DEMO_RESUME_TIME_KEY, 0);

	}

	/**
	 * Save play position to shared prefs
	 */
	private void savePlayPosition(int position) {
		if (!bErrorOut){
			SharedPreferences prefs = getActivity().getSharedPreferences("PlayPosition", 0);
			SharedPreferences.Editor editor = prefs.edit();
			mStartTime = mPlaybackCompleted ? 0 : position;
			editor.putInt(NGE_DEMO_RESUME_TIME_KEY, mStartTime);
			editor.commit();
		}
	}

	private boolean wasPlaying() {
		SharedPreferences prefs = getActivity().getSharedPreferences("PlayState", 0);
		boolean result = prefs.getBoolean(NGE_DEMO_PLAYBACK_STATE_KEY, true);
		return result;

	}

	private void savePlayState(boolean state) {

		SharedPreferences prefs = getActivity().getSharedPreferences("PlayState", 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(NGE_DEMO_PLAYBACK_STATE_KEY, state);
		editor.commit();

	}

	/**
	 * On pause.
	 */
	@Override
	public void onPause() {
		super.onPause();
		bPlayAtResumeActivityState = true;
		isPlaying = false;


		int currentPosition = 0;
		if (mPlaybackPrepared) {
			try {
				currentPosition = mPlayer.getCurrentPosition();
				savePlayPosition(currentPosition);
			} catch (IllegalStateException ex) {
			}
		}
		if (videoView.isPlaying())
			videoView.stopPlayback();
		videoView.setVideoURI(null);

	}

	private void trackPlaybackEvent(String eventLabel, int eventValue) {

	}

	@Override
	public void playerPaused(boolean paused) {
		try{

			savePlayPosition(videoView.getCurrentPosition());
		}catch (IllegalStateException is){}
		//}

	}


	protected class InfoListener implements MediaPlayer.OnInfoListener {
		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {

			switch (what) {
				case MediaPlayer.MEDIA_INFO_BUFFERING_START: // @since API Level 9

					showLoadingView();
                    /*mDialog.setCanceledOnTouchOutside(false);
                    mDialog.setMessage(Localizer.get(KEYS.ANDROID_BUFFERING));
                    mDialog.show();*/


					break;
				case MediaPlayer.MEDIA_INFO_BUFFERING_END: // @since API Level 9
					hideLoadingView();
					//mDialog.hide();

					break;

				case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
					hideLoadingView();
					//mDialog.hide();

					break;

				default:
					hideLoadingView();
					//mDialog.hide();
					break;

			}
			return true;
		}
	}

	public void pauseForIME(){
	}

	public void resumePlaybackFromIME(){

	}
}