package com.wb.cpedemo.dash;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.PopupMenu;
import android.widget.Toast;


import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.wb.nextgenlibrary.NextGenExperience;
import com.wb.nextgenlibrary.activity.InMovieExperience;
import com.wb.nextgenlibrary.fragment.AbstractNGEMainMovieFragment;
import com.wb.nextgenlibrary.interfaces.NGEPlaybackStatusListener;
import com.wb.nextgenlibrary.util.concurrent.ResultListener;
import com.wb.nextgenlibrary.videoview.IVideoViewActionListener;
import com.wb.nextgenlibrary.videoview.ObservableVideoView;
import com.wb.nextgenlibrary.widget.CustomMediaController;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.wb.cpedemo.R;

/**
 * Created by gzcheng on 10/27/16.
 */

public class CPEDemoMovieFragment_Dash extends AbstractNGEMainMovieFragment implements ExoPlayer.EventListener, ObservableVideoView.PlayPauseListener,
		PlaybackControlView.VisibilityListener {

	public static final String KEY_DASH_PLAY_POSITION = "KEY_DASH_PLAY_POSITION";
	//private CastLocalBroadcaster mLocalBroadcaster;

	private static final CookieManager defaultCookieManager;
	static {
		defaultCookieManager = new CookieManager();
		defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
	}

	public static final String ACTION_VIEW = "com.google.android.exoplayer.demo.action.VIEW";
	public static final String EXTENSION_EXTRA = "extension";
	public static final String AUTHORIZATION = "Authorization";

	private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
	private static final CookieManager DEFAULT_COOKIE_MANAGER;
	static {
		DEFAULT_COOKIE_MANAGER = new CookieManager();
		DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
	}

	private Handler mainHandler;
	private Timeline.Window window;
	private SimpleExoPlayerView simpleExoPlayerView;

	private DataSource.Factory mediaDataSourceFactory;
	private SimpleExoPlayer player;
	private DefaultTrackSelector trackSelector;
	private boolean playerNeedsSource;

	private boolean shouldAutoPlay;
	private boolean isTimelineStatic;

	private boolean mPlaybackCompleted = false;
	private long mStartTime = 0;

	private Uri contentUri;

	DashContent currentPlaybackContent;

	static public class DashContent{
		final public String mediaURL;
		final public String drmProxyURL;
		final public String dashContentID;
		final public String dashProvider;
		public DashContent(String mediaURL, String drmProxyURL, String dashContentID, String dashProvider){
			this.mediaURL = mediaURL;
			this.drmProxyURL = drmProxyURL;
			this.dashContentID = dashContentID;
			this.dashProvider = dashProvider;
		}
	}

	private boolean isDRMProtectedPlayback = true;
	private boolean bShowHandleTouchEvent = true;

	CustomMediaController mediaController;

	View rootView;

	NonDRMPlaybackContent nonDRMPlaybackContent = null;

	public static class NonDRMPlaybackContent{
		final public String contentName;
		final public Uri contentUri;
		final public int contentType;
		public NonDRMPlaybackContent(String contentName, Uri contentUri, int contentType){
			this.contentName = contentName;
			this.contentUri = contentUri;
			this.contentType = contentType;
		}
	}

	public void setNonDRMPlaybackContent(NonDRMPlaybackContent content){
		isDRMProtectedPlayback = false;
		nonDRMPlaybackContent = content;
	}

	//private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.dash_video_player, container, false);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		rootView = view.findViewById(R.id.root);


		shouldAutoPlay = true;
		mediaDataSourceFactory = buildDataSourceFactory(true);
		mainHandler = new Handler();
		window = new Timeline.Window();
		if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
			CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
		}

		View rootView = view.findViewById(R.id.root);

		simpleExoPlayerView = (SimpleExoPlayerView) view.findViewById(R.id.player_view);
		simpleExoPlayerView.setControllerVisibilityListener(this);
		simpleExoPlayerView.requestFocus();


		// mediaController Creation
		if (mediaController == null) {
			setCustomMediaController(new CustomMediaController(getContext(), false, false), true);
		}
	}

	public void toggleControlsVisibility()  {
		if (mediaController.isShowing()) {
			mediaController.hide();
			//debugRootView.setVisibility(View.GONE);
		} else {
			showControls();
		}
	}

	@Override
	public void onVisibilityChange(int visibility) {
		//debugRootView.setVisibility(visibility);
	}

	@Override
	public void onStart() {
		super.onStart();
		//mLocalBroadcaster = CastLocalBroadcaster.get(getActivity());
		if (bShowHandleTouchEvent && rootView != null) {
			rootView.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
						toggleControlsVisibility();
					} else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
						view.performClick();
					}
					return true;
				}
			});
		}
		rootView.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE
						|| keyCode == KeyEvent.KEYCODE_MENU) {
					return false;
				}
				return mediaController.dispatchKeyEvent(event);
			}
		});

	}

	@Override
	public void onResume() {
		super.onResume();

		if (Util.SDK_INT <= 23 || player == null) {
			onShown();
			initializePlayer(true);
		}

		if (!isPausedForIMEVideoPlayback) {
			initializePlayer(true);
		}

		if (rootView != null && mediaController != null)
			mediaController.setAnchorView(rootView);
        /*
        if (CastControl.isAvailable() && FlixsterApplication.getCurrentPlayableContent()!= null && FlixsterApplication.getCurrentPlayableContent().isCastable()) {
            mLocalBroadcaster.registerEventListener(mCastEventListener);
            FlixsterApplication.getCastControl().onStart();
        }*/
	}

	private void onShown() {

		playContent();
		hideLoadingView();
		//Intent intent = getActivity().getIntent();  // should not get info from intent, should be controlled by playback object
		//contentUri = intent.getData();




	}

	private void playContent(){
		if (currentPlaybackContent != null && getActivity() != null){
			contentUri = Uri.parse(currentPlaybackContent.mediaURL);
			//configureSubtitleView();
			if (player == null) {
				if (!maybeRequestPermission()) {
					initializePlayer(true);
				}
			} else {
				//player.setBackgrounded(false);
				//player.setSelectedTrack(ExoPlayerWrapper.TYPE_TEXT, 0);
				//player.setSelectedTrack(ExoPlayerWrapper.TYPE_TEXT, 1);
			}

		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mPlaybackCompleted)
			savePlayPosition(0);
		else
			savePlayPosition(getCurrentPosition());

		if (Util.SDK_INT <= 23) {
			onHidden();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (Util.SDK_INT > 23) {
			onHidden();
		}
	}

	private void onHidden() {
		releasePlayer();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//audioCapabilitiesReceiver.unregister();
		releasePlayer();
	}




	// Permission management methods

	/**
	 * Checks whether it is necessary to ask for permission to read storage. If necessary, it also
	 * requests permission.
	 *
	 * @return true if a permission request is made. False if it is not necessary.
	 */
	@TargetApi(23)
	private boolean maybeRequestPermission() {
		if (requiresPermission(contentUri)) {
			//requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
			return true;
		} else {
			return false;
		}
	}

	@TargetApi(23)
	private boolean requiresPermission(Uri uri) {
		return Util.SDK_INT >= 23
				&& Util.isLocalFileUri(uri)
                /*&& checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED*/;
	}

	private void initializePlayer(boolean playWhenReady) {
		if (isDRMProtectedPlayback)
			initializePlayerForDRM(playWhenReady);
		else{
			initializePlayerForDRM(playWhenReady);

		}
	}

	private void initializePlayerForDRM(boolean playWhenReady) {
		showLoadingView();
		//String subtitleUrl = "https://cpe-manifest.s3.amazonaws.com/webvtt/CHOP_WB_Lasp_STORKS_HD_16x9_2_40_5_1_2_0_LTRT_ENGLISH_E2119876_8452518_tt-en.vtt";
		String userAgent = Util.getUserAgent(getActivity(), "ExoPlayerDemo");
		if (!isDRMProtectedPlayback || (bStreamPrepared && !isPausedForIMEVideoPlayback)) {
			if (player == null && currentPlaybackContent != null) {
				DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
				if (isDRMProtectedPlayback){
					//if (drmSchemeUuid != null) {

					String drmLicenseUrl = currentPlaybackContent.drmProxyURL ;
					Map<String, String> keyRequestProperties = new HashMap<>();
					//keyRequestProperties.put(AUTHORIZATION, currentPlaybackContent.);
					//String[] keyRequestPropertiesArray = intent.getStringArrayExtra(DRM_KEY_REQUEST_PROPERTIES);
                    /*Map<String, String> keyRequestProperties;
                    if (keyRequestPropertiesArray == null || keyRequestPropertiesArray.length < 2) {
                        keyRequestProperties = null;
                    } else {
                        keyRequestProperties = new HashMap<>();
                        for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
                            keyRequestProperties.put(keyRequestPropertiesArray[i],
                                    keyRequestPropertiesArray[i + 1]);
                        }
                    }*/
					try {
						drmSessionManager = buildDrmSessionManager(C.WIDEVINE_UUID, drmLicenseUrl,
								keyRequestProperties);
					} catch (UnsupportedDrmException e) {
						int errorStringId = Util.SDK_INT < 18 ? R.string.error_drm_not_supported
								: (e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
								? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
						showToast(errorStringId);
						return;
					}
					int extensionRendererMode = SimpleExoPlayer.EXTENSION_RENDERER_MODE_ON;//SimpleExoPlayer.EXTENSION_RENDERER_MODE_OFF;
					TrackSelection.Factory videoTrackSelectionFactory =
							new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
					trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
					player = ExoPlayerFactory.newSimpleInstance(getActivity(), trackSelector, new DefaultLoadControl(),
							drmSessionManager, extensionRendererMode);
					player.addListener(this);
				}

				@SimpleExoPlayer.ExtensionRendererMode int extensionRendererMode = SimpleExoPlayer.EXTENSION_RENDERER_MODE_ON;//SimpleExoPlayer.EXTENSION_RENDERER_MODE_OFF;


				simpleExoPlayerView.setPlayer(player);

				player.setPlayWhenReady(shouldAutoPlay);
                /*debugViewHelper = new DebugTextViewHelper(player, debugTextView);
                debugViewHelper.start();*/
				mediaController.setMediaPlayer(playerControl);
				playerNeedsSource = true;
			}
			if (playerNeedsSource) {
				MediaSource mediaSource = null;
				if (isDRMProtectedPlayback) {
					mediaSource = buildMediaSource(Uri.parse(currentPlaybackContent.mediaURL), C.TYPE_DASH, EXTENSION_EXTRA);

				} else {
					mediaSource = buildMediaSource(nonDRMPlaybackContent.contentUri, nonDRMPlaybackContent.contentType, EXTENSION_EXTRA);
				}

				player.prepare(mediaSource, !isTimelineStatic, !isTimelineStatic);
				player.seekTo(mStartTime);
				playerNeedsSource = false;
				updateButtonVisibilities();
			}
		}else {
			bPlayWhenStreamStartSucceed = true;
		}
	}

	MediaController.MediaPlayerControl playerControl = new MediaController.MediaPlayerControl() {
		@Override
		public void start() {
			if (player != null)
				player.setPlayWhenReady(true);
			if (nextGenVideoViewListener != null){
				nextGenVideoViewListener.onVideoResume();
			}
		}

		@Override
		public void pause() {
			if (player != null)
				player.setPlayWhenReady(false);
			if (nextGenVideoViewListener != null){
				nextGenVideoViewListener.onVideoPause();
			}
		}

		@Override
		public int getDuration() {
			if (player != null)
				return (int)player.getDuration();
			return 0;
		}

		@Override
		public int getCurrentPosition() {
			if (player != null)
				return (int)player.getCurrentPosition();
			return 0;
		}

		@Override
		public void seekTo(int pos) {
			if (player != null)
				player.seekTo(pos);
		}

		@Override
		public boolean isPlaying() {
			if (player != null)
				return player.getPlaybackState() == ExoPlayer.STATE_READY && player.getPlayWhenReady();
			return false;
		}

		@Override
		public int getBufferPercentage() {
			if (player != null)
				return player.getBufferedPercentage();
			return 0;
		}

		@Override
		public boolean canPause() {
			return true;
		}

		@Override
		public boolean canSeekBackward() {
			return false;
		}

		@Override
		public boolean canSeekForward() {
			return false;
		}

		@Override
		public int getAudioSessionId() {
			return 0;
		}
	};


	private MediaSource buildMediaSource(Uri uri, int type, String overrideExtension) {
        /*int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
                : uri.getLastPathSegment());*/
		switch (type) {
			case C.TYPE_SS:
				return new SsMediaSource(uri, buildDataSourceFactory(false),
						new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, null);
			case C.TYPE_DASH:
				return new DashMediaSource(uri, buildDataSourceFactory(false),
						new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, null);
			case C.TYPE_HLS:
				return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, null);
			case C.TYPE_OTHER:
				return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
						mainHandler, null);
			default: {
				throw new IllegalStateException("Unsupported type: " + type);
			}
		}
	}


	private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager(UUID uuid, String licenseUrl, Map<String, String> keyRequestProperties) throws UnsupportedDrmException {
		if (Util.SDK_INT < 18) {
			return null;
		}
		HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl,
				buildHttpDataSourceFactory(false), keyRequestProperties);
		return new StreamingDrmSessionManager<>(uuid,
				FrameworkMediaDrm.newInstance(uuid), drmCallback, null, mainHandler, null);
	}

	private void trackPlaybackEvent(String eventLabel, int eventValue) {
		//Trackers.instance().trackEvent(Drm.manager().getPlaybackTag(), Drm.manager().getPlaybackTitle(), "Streaming",
		//		UrlHelper.urlEncode(Build.MODEL), eventLabel, eventValue);
	}

	private void releasePlayer() {
		if (player != null) {
			//debugViewHelper.stop();
			//debugViewHelper = null;
			mStartTime = player.getCurrentPosition();
			player.release();
			player = null;
			//eventLogger.endSession();
		}
	}

	/************************AbstractNGEMainMovieFragment*********************/

	@Override
	public void setPlaybackObject(Object playbackObject){
		if (playbackObject instanceof DashContent)
			currentPlaybackContent = (DashContent)playbackObject;

	}
	@Override
	public void setCustomMediaController(CustomMediaController customMC){
		//setCustomMediaController(customMC, true);
	}
	public void setCustomMediaController(CustomMediaController customMC, boolean bShowHandleTouchEvent){
		this.bShowHandleTouchEvent = bShowHandleTouchEvent;
		if (mediaController != null)
			mediaController.setAnchorView(null);

		mediaController = customMC;
		if (bShowHandleTouchEvent) {
			mediaController.setVisibilityChangeListener(new CustomMediaController.MediaControllerVisibilityChangeListener() {
				public void onVisibilityChange(boolean bShow) {
					if (getActivity() != null && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
						if (getActivity() != null && getActivity() instanceof InMovieExperience)
							if (bShow)
								((InMovieExperience) getActivity()).getSupportActionBar().show();
							else
								((InMovieExperience) getActivity()).getSupportActionBar().hide();
					}
				}
			});
		}
		if (rootView != null)
			mediaController.setAnchorView(rootView);
	}

	@Override
	public void setResumeTime(int resumeTime){
		mStartTime = resumeTime;
		if (player != null) {
			player.seekTo(resumeTime);
			if (isPausedByUser)
				pause();
		}else
			mStartTime = resumeTime;

	}

	@Override
	public int getCurrentPosition(){
		if (player != null) {
			try {
				return (int)player.getCurrentPosition();
			}catch (Exception ex){
				return 0;
			}
		}else
			return 0;
	}

	@Override
	public int getDuration(){
		if (player != null) {
			try {
				return (int)player.getDuration();
			}catch (Exception ex){
				return 0;
			}
		}else
			return 0;

	}

	@Override
	public boolean isPlaying(){
		if (player != null) {
			return player.getPlaybackState() == ExoPlayer.STATE_READY && player.getPlayWhenReady();
		}
		return false;
	}

	boolean isPausedByUser = false;
	boolean isPausedForIMEVideoPlayback = false;

	@Override
	public void pause(){
		isPausedByUser = true;
		if (player != null)
			player.setPlayWhenReady(false);
	}

	@Override
	public void resumePlayback(){
		isPausedByUser = false;
		if (player != null)
			player.setPlayWhenReady(true);

		if (nextGenVideoViewListener != null)
			nextGenVideoViewListener.onVideoResume();
	}

	public void pauseForIME(){
		isPausedByUser = false;
		isPausedForIMEVideoPlayback = true;
		//videoViewBlocker.setVisibility(View.VISIBLE);
		if (player != null) {
			player.setPlayWhenReady(false);
			//player.setSurface(null);
		}
		//releasePlayer();
		//player.getPlayerControl().pause();
	}
	public void resumePlaybackFromIME(){
		isPausedByUser = false;
		isPausedForIMEVideoPlayback = false;
		//videoViewBlocker.setVisibility(View.GONE);
		//preparePlayer(true);
		if (player != null)
			player.setPlayWhenReady(true);
	}

	Boolean bStreamPrepared = false, bPlayWhenStreamStartSucceed = false;
	@Override
	public void streamStartPreparations(final ResultListener<Boolean> resultLister){
		if (currentPlaybackContent == null){
			bStreamPrepared = true;
			resultLister.onResult(true);
			return;
		}
		bStreamPrepared = true;
		playContent();

		resultLister.onResult(true);
	}

	public boolean canHandleCommentaryAudioTrackSwitching(){
		return true;
	}

	/*
		if tracknumber < 0 => turn off commentary.
		else select the commentary track from setCommentaryTrackUrls accordingly
	 */
	public void setActiveCommentaryTrack(int tracknumber){
		super.setActiveCommentaryTrack(tracknumber);
		if (player != null) {
			try{

				if (tracknumber == -1) {
					selectAudioTrack(0);
					//player.setSelectedTrack(ExoPlayerWrapper.TYPE_AUDIO, 0);
				}else {
					selectAudioTrack(activeCommentaryTrack + 1);
					//player.setSelectedTrack(ExoPlayerWrapper.TYPE_AUDIO, activeCommentaryTrack + 1);
				}
			} catch (Exception ex){

			}

		}
	}

   /* @Override
    public void setProgressDialog(ProgressDialog dialog){

    }*/

	private void savePlayPosition(long position) {
		/*if (stream != null){
			SharedPreferences prefs = getActivity().getSharedPreferences("PlayPosition", 0);
			SharedPreferences.Editor editor = prefs.edit();
			mStartTime = mPlaybackCompleted ? 0 : position;
			editor.putLong(KEY_DASH_PLAY_POSITION + stream.rightsId, position);
			editor.commit();

			FlixsterLogger.d(F.TAG_DRM, "WidevinePlayer.savePlayPosition: rightId=" + stream.rightsId + ", saving seek time " + mStartTime);
		}*/
	}

	private long loadPlayPosition(String rightId) {
		/*if (!StringHelper.isEmpty(rightId)){
			SharedPreferences prefs = NextGenExperience.getApplicationContext().getSharedPreferences("PlayPosition", 0);
			long second = prefs.getLong(KEY_DASH_PLAY_POSITION + rightId, 0);
			FlixsterLogger.d(F.TAG_DRM, "WidevinePlayer.loadPlayPosition: rightId=" + rightId + " at " + second);
			return second;
		}else
			return 0;	//to support trailer playback
			*/
		return 0;
	}

	public void switchMainFeatureAudio(boolean bOnOff){
		if (player == null)
			return;
		if(bOnOff){
			// player.setSelectedTrack(ExoPlayerWrapper.TYPE_AUDIO, ExoPlayer.TRACK_DEFAULT);
		} else {
			// player.setSelectedTrack(ExoPlayerWrapper.TYPE_AUDIO, ExoPlayer.TRACK_DISABLED);
		}
	}
	/********************PlayPauseListener*******************/
	@Override
	public void playerPaused(boolean paused) {


	}

	/**
	 * Returns a new DataSource factory.
	 *
	 * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
	 *     DataSource factory.
	 * @return A new DataSource factory.
	 */
	private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
		DefaultBandwidthMeter bandwidthMeter = useBandwidthMeter ? BANDWIDTH_METER : null;
		return new DefaultDataSourceFactory(getActivity(), bandwidthMeter,
				buildHttpDataSourceFactory(useBandwidthMeter));

	}

	/**
	 * Returns a new HttpDataSource factory.
	 *
	 * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
	 *     DataSource factory.
	 * @return A new HttpDataSource factory.
	 */
	private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
		String userAgent = Util.getUserAgent(getActivity(), "ExoPlayerDemo");
		DefaultBandwidthMeter bandwidthMeter = useBandwidthMeter ? BANDWIDTH_METER : null;
		return new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);
	}

	// ExoPlayer.EventListener implementation

	@Override
	public void onLoadingChanged(boolean isLoading) {
		// Do nothing.
	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		switch(playbackState){
			case ExoPlayer.STATE_ENDED:
				playbackStatus = NGEPlaybackStatusListener.NextGenPlaybackStatus.COMPLETED;
				if (nextGenVideoViewListener != null)
					nextGenVideoViewListener.onVideoPause();
				mPlaybackCompleted = true;
				if (completionListener != null)
					completionListener.onCompletion(null);
				showControls();
				break;
			case ExoPlayer.STATE_READY:


				//player.setPlayWhenReady(true);
				break;
			case ExoPlayer.STATE_IDLE:
				//text += "idle";
				playbackStatus = NGEPlaybackStatusListener.NextGenPlaybackStatus.READY;
				if (nextGenVideoViewListener != null)
					nextGenVideoViewListener.onVideoPause();
				break;

		}
		updateButtonVisibilities();
	}

	@Override
	public void onPositionDiscontinuity() {
		// Do nothing.
	}

	@Override
	public void onTimelineChanged(Timeline timeline, Object manifest) {
		isTimelineStatic = !timeline.isEmpty()
				&& !timeline.getWindow(timeline.getWindowCount() - 1, window).isDynamic;
	}

	@Override
	public void onPlayerError(ExoPlaybackException e) {
		String errorString = null;
		if (e.type == ExoPlaybackException.TYPE_RENDERER) {
			Exception cause = e.getRendererException();
			if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
				// Special case for decoder initialization failures.
				MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
						(MediaCodecRenderer.DecoderInitializationException) cause;
				if (decoderInitializationException.decoderName == null) {
					if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
						errorString = getString(R.string.error_querying_decoders);
					} else if (decoderInitializationException.secureDecoderRequired) {
						errorString = getString(R.string.error_no_secure_decoder,
								decoderInitializationException.mimeType);
					} else {
						errorString = getString(R.string.error_no_decoder,
								decoderInitializationException.mimeType);
					}
				} else {
					errorString = getString(R.string.error_instantiating_decoder, decoderInitializationException.decoderName);
				}
			}else if (e instanceof ExoPlaybackException && e.getCause().toString().contains("CryptoException")){
				// Report This Error

				//trackSelectionHelper.showSelectionDialog(getActivity(), "Videos", trackSelector.getCurrentMappedTrackInfo(), getRendererIndexForType(C.TRACK_TYPE_VIDEO));
				errorString = e.toString();//"Unknown CryptoException, possibly HDCP error.";
			}
		}
		if (errorString != null) {

			String message = errorString;
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(message).setCancelable(true)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							getActivity().finish();
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
		}
		playerNeedsSource = true;
		updateButtonVisibilities();
		showControls();
	}

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
		updateButtonVisibilities();
		MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
		if (mappedTrackInfo != null) {
			if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
					== MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
				showToast("Unsupport video");
			}
			if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
					== MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
				showToast("Unsupport Audio");
			}
		}
	}

	// User controls

	private void updateButtonVisibilities() {
        /*debugRootView.removeAllViews();

        retryButton.setVisibility(playerNeedsSource ? View.VISIBLE : View.GONE);
        debugRootView.addView(retryButton);*/

		if (player == null) {
			return;
		}

		MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
		if (mappedTrackInfo == null) {
			return;
		}

		for (int i = 0; i < mappedTrackInfo.length; i++) {
			TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
			if (trackGroups.length != 0) {
				Button button = new Button(getActivity());
				int label;
				switch (player.getRendererType(i)) {
					case C.TRACK_TYPE_AUDIO:
						label = R.string.audio;
						break;
					case C.TRACK_TYPE_VIDEO:
						label = R.string.video;
						break;
					case C.TRACK_TYPE_TEXT:
						label = R.string.text;
						break;
					default:
						continue;
				}
				button.setText(label);
				button.setTag(i);
				//button.setOnClickListener(this);
				//debugRootView.addView(button, debugRootView.getChildCount() - 1);
			}
		}
	}

	private void showControls() {
		mediaController.show();
		//debugRootView.setVisibility(View.VISIBLE);
	}

	private void showToast(int messageId) {
		showToast(getString(messageId));
	}

	private void showToast(String message) {
		Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
	}

	private static final TrackSelection.Factory FIXED_FACTORY = new FixedTrackSelection.Factory();

	private void selectAudioTrack(int trackIndex){
		selectTrack(C.TRACK_TYPE_AUDIO, trackIndex);
	}

	private void setSubtitle(int trackIndex){
		selectTrack(C.TRACK_TYPE_TEXT, trackIndex);
	}

	/*
	 * @param rendererType The track type. One of the {@link C} {@code TRACK_TYPE_*} constants.
	 * */
	private void selectTrack(int rendererType, int groupIndex){
		//trackSelectionHelper.showSelectionDialog(getActivity(), "text",          trackSelector.getCurrentMappedTrackInfo(), rendererIndex);

		int rendererIndex = getRendererIndexForType(rendererType);

		if (rendererIndex == -1)
			return;

		MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
		TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
		int trackIndex = 0;
		boolean isDisabled = false;
		if (rendererType == C.TRACK_TYPE_TEXT && groupIndex == -1){
			isDisabled = true;
			groupIndex = 0;
			trackIndex = 0;
		} else {
			trackIndex = trackGroups.get(groupIndex).length - 1;
		}

		if (groupIndex < trackGroups.length && trackGroups.get(groupIndex).length > 0){

			MappingTrackSelector.SelectionOverride override = new MappingTrackSelector.SelectionOverride(FIXED_FACTORY, groupIndex, trackIndex);
			trackSelector.setRendererDisabled(rendererIndex, isDisabled);
			//TrackGroupArray trackGroups = trackSelector.getCurrentMappedTrackInfo().getTrackGroups(rendererIndex);
			if (override != null && !isDisabled) {
				trackSelector.setSelectionOverride(rendererIndex, trackGroups, override);
			} else {
				trackSelector.clearSelectionOverrides(rendererIndex);
			}


		}

	}

	private int getRendererIndexForType(int rendererType){
		for (int i = 0; i < player.getRendererCount(); i++){
			if (player.getRendererType(i) == rendererType){
				return i;
			}
		}
		return -1;
	}

	private int getTrackCount(int rendererIndex){
		return trackSelector.getCurrentMappedTrackInfo().getTrackGroups(rendererIndex).length;
	}


}
