package com.wb.cpedemo.mainmoviefragment;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * Created by gzcheng on 10/28/16.
 */

public class SimpleObserveVideoView extends VideoView {

	public static interface IVideoViewActionListener {
		void onPause();

		void onResume();

		void onTimeBarSeekChanged(int currentTime);
	}


	private IVideoViewActionListener mVideoViewListener;
	private boolean mIsOnPauseMode = false;

	public void setVideoViewListener(IVideoViewActionListener listener) {
		mVideoViewListener = listener;
	}

	@Override
	public void pause() {
		super.pause();

		if (mVideoViewListener != null) {
			mVideoViewListener.onPause();
		}

		if (mListener != null) {
			mListener.playerPaused(true);
		}

		mIsOnPauseMode = true;
	}

	@Override
	public void start() {
		super.start();

		if (mIsOnPauseMode) {
			if (mVideoViewListener != null) {
				mVideoViewListener.onResume();
			}

			mIsOnPauseMode = false;
		}

		if (mListener != null) {
			mListener.playerPaused(false);
		}
	}

	@Override
	public void seekTo(int msec) {
		super.seekTo(msec);

		if (mVideoViewListener != null) {
			mVideoViewListener.onTimeBarSeekChanged(msec);
		}
	}

	public SimpleObserveVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SimpleObserveVideoView(Context context) {
		super(context);
	}

	public SimpleObserveVideoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	private PlayPauseListener mListener;


	/// Register listener
	public void setPlayPauseListener(PlayPauseListener listener) {
		mListener = listener;
	}

	/// Interface for listener
	public interface PlayPauseListener {
		void playerPaused(boolean paused);
	}
}



