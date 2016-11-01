package com.wb.cpedemo.dash;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.metadata.id3.GeobFrame;
import com.google.android.exoplayer.metadata.id3.Id3Frame;
import com.google.android.exoplayer.metadata.id3.PrivFrame;
import com.google.android.exoplayer.metadata.id3.TxxxFrame;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.Util;
import com.google.android.exoplayer.util.VerboseLogUtil;
import com.wb.nextgenlibrary.fragment.AbstractNextGenMainMovieFragment;
import com.wb.nextgenlibrary.util.concurrent.ResultListener;
import com.wb.nextgenlibrary.widget.CustomMediaController;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;
import java.util.Locale;

import com.wb.cpedemo.R;

/**
 * Created by gzcheng on 10/27/16.
 */

public class CPEDemoMovieFragment_Dash extends AbstractNextGenMainMovieFragment implements
		/*ObservableVideoView.PlayPauseListener,*/ SurfaceHolder.Callback, View.OnClickListener,
		ExoPlayerWrapper.Listener, ExoPlayerWrapper.CaptionListener, ExoPlayerWrapper.Id3MetadataListener,
		AudioCapabilitiesReceiver.Listener {
		// For use within demo app code.
	public static final String CONTENT_ID_EXTRA = "content_id";
	public static final String CONTENT_TYPE_EXTRA = "content_type";
	public static final String PROVIDER_EXTRA = "provider";

	// For use when launching the demo app using adb.
	private static final String CONTENT_EXT_EXTRA = "type";

	private static final String TAG = "PlayerActivity";
	private static final int MENU_GROUP_TRACKS = 1;
	private static final int ID_OFFSET = 2;

	private static final CookieManager defaultCookieManager;
	static {
		defaultCookieManager = new CookieManager();
		defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
	}

	private DashPlayerEventLogger eventLogger;
	//private View debugRootView;
	private View shutterView;
	private AspectRatioFrameLayout videoFrame;
	private SurfaceView surfaceView;
	//private TextView debugTextView;
	//private TextView playerStateTextView;
	private SubtitleLayout subtitleLayout;
	private ExoPlayerWrapper player;
	//private DebugTextViewHelper debugViewHelper;
	private boolean playerNeedsPrepare;

	private long playerPosition;
	private boolean enableBackgroundAudio;

	private int contentType;


	//StreamAsset streamAsset;
	CustomMediaController mediaController;

	View rootView;

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

	private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.dash_video_player, container, false);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		rootView = view.findViewById(R.id.root);
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

		shutterView = view.findViewById(R.id.shutter);
		//debugRootView = findViewById(R.id.controls_root);

		videoFrame = (AspectRatioFrameLayout) view.findViewById(R.id.video_frame);
		surfaceView = (SurfaceView) view.findViewById(R.id.surface_view);
		surfaceView.getHolder().addCallback(this);
		//debugTextView = (TextView) findViewById(R.id.debug_text_view);

		//playerStateTextView = (TextView) findViewById(R.id.player_state_view);
		subtitleLayout = (SubtitleLayout) view.findViewById(R.id.subtitles);

		/*retryButton = (Button) findViewById(R.id.retry_button);
		retryButton.setOnClickListener(this);
		videoButton = (Button) findViewById(R.id.video_controls);
		textButton = (Button) findViewById(R.id.text_controls);*/
		//audioButton = (Button) findViewById(R.id.audio_controls);
		//audioButton.setOnClickListener(this);

		CookieHandler currentHandler = CookieHandler.getDefault();
		if (currentHandler != defaultCookieManager) {
			CookieHandler.setDefault(defaultCookieManager);
		}

		audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(getActivity(), this);
		audioCapabilitiesReceiver.register();
	}

    /*@Override
    public void onNewIntent(Intent intent) {
        releasePlayer();
        playerPosition = 0;
        getActivity().setIntent(intent);
    }*/

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (Util.SDK_INT <= 23 || player == null) {
			onShown();
		}
		if (rootView != null && mediaController != null)
		mediaController.setAnchorView(rootView);

	}

	private void onShown() {

		playContent();
		hideLoadingView();
		//Intent intent = getActivity().getIntent();  // should not get info from intent, should be controlled by playback object
		//contentUri = intent.getData();
	}

	private void playContent(){
		if (player == null) {
			if (!maybeRequestPermission()) {
				preparePlayer(true);
			}
		} else {
			player.setBackgrounded(false);
			player.setSelectedTrack(ExoPlayerWrapper.TYPE_TEXT, 0);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
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
		if (!enableBackgroundAudio) {
			releasePlayer();
		} else {
			player.setBackgrounded(true);
		}
		shutterView.setVisibility(View.VISIBLE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		audioCapabilitiesReceiver.unregister();
		releasePlayer();
		}


// User controls

	private void updateButtonVisibilities() {
	}


	private boolean haveTracks(int type) {
		return player != null && player.getTrackCount(type) > 0;
	}

	public void showVideoPopup(View v) {
		PopupMenu popup = new PopupMenu(getActivity(), v);
		configurePopupWithTracks(popup, null, ExoPlayerWrapper.TYPE_VIDEO);
		popup.show();
	}

	public void showAudioPopup(View v) {
		PopupMenu popup = new PopupMenu(getActivity(), v);
		Menu menu = popup.getMenu();
		menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.enable_background_audio);
		final MenuItem backgroundAudioItem = menu.findItem(0);
		backgroundAudioItem.setCheckable(true);
		backgroundAudioItem.setChecked(enableBackgroundAudio);
		PopupMenu.OnMenuItemClickListener clickListener = new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item == backgroundAudioItem) {
					enableBackgroundAudio = !item.isChecked();
					return true;
				}
				return false;
			}
		};
		configurePopupWithTracks(popup, clickListener, ExoPlayerWrapper.TYPE_AUDIO);
		popup.show();
	}

	public void showTextPopup(View v) {
		PopupMenu popup = new PopupMenu(getActivity(), v);
		configurePopupWithTracks(popup, null, ExoPlayerWrapper.TYPE_TEXT);
		popup.show();
	}

	public void showVerboseLogPopup(View v) {
		PopupMenu popup = new PopupMenu(getActivity(), v);
		Menu menu = popup.getMenu();
		menu.add(Menu.NONE, 0, Menu.NONE, R.string.logging_normal);
		menu.add(Menu.NONE, 1, Menu.NONE, R.string.logging_verbose);
		menu.setGroupCheckable(Menu.NONE, true, true);
		menu.findItem((VerboseLogUtil.areAllTagsEnabled()) ? 1 : 0).setChecked(true);
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == 0) {
					VerboseLogUtil.setEnableAllTags(false);
				} else {
					VerboseLogUtil.setEnableAllTags(true);
				}
				return true;
			}
		});
		popup.show();
	}

	private void configurePopupWithTracks(PopupMenu popup, final PopupMenu.OnMenuItemClickListener customActionClickListener, final int trackType) {
		if (player == null) {
			return;
		}
		int trackCount = player.getTrackCount(trackType);
		if (trackCount == 0) {
			return;
		}
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				return (customActionClickListener != null && customActionClickListener.onMenuItemClick(item)) || onTrackItemClick(item, trackType);
			}
		});
		Menu menu = popup.getMenu();
		// ID_OFFSET ensures we avoid clashing with Menu.NONE (which equals 0).
		menu.add(MENU_GROUP_TRACKS, ExoPlayerWrapper.TRACK_DISABLED + ID_OFFSET, Menu.NONE, R.string.off);
		for (int i = 0; i < trackCount; i++) {
			menu.add(MENU_GROUP_TRACKS, i + ID_OFFSET, Menu.NONE,
			buildTrackName(player.getTrackFormat(trackType, i)));
		}
		menu.setGroupCheckable(MENU_GROUP_TRACKS, true, true);
		menu.findItem(player.getSelectedTrack(trackType) + ID_OFFSET).setChecked(true);
	}

	private static String buildTrackName(MediaFormat format) {
		if (format.adaptive) {
			return "auto";
		}
		String trackName;
		if (MimeTypes.isVideo(format.mimeType)) {
			trackName = joinWithSeparator(joinWithSeparator(buildResolutionString(format),
			buildBitrateString(format)), buildTrackIdString(format));
		} else if (MimeTypes.isAudio(format.mimeType)) {
			trackName = joinWithSeparator(joinWithSeparator(joinWithSeparator(buildLanguageString(format),
			buildAudioPropertyString(format)), buildBitrateString(format)),
			buildTrackIdString(format));
		} else {
			trackName = joinWithSeparator(joinWithSeparator(buildLanguageString(format),
			buildBitrateString(format)), buildTrackIdString(format));
		}
		return trackName.length() == 0 ? "unknown" : trackName;
	}

	private static String buildResolutionString(MediaFormat format) {
		return format.width == MediaFormat.NO_VALUE || format.height == MediaFormat.NO_VALUE
			? "" : format.width + "x" + format.height;
	}

	private static String buildAudioPropertyString(MediaFormat format) {
		return format.channelCount == MediaFormat.NO_VALUE || format.sampleRate == MediaFormat.NO_VALUE
			? "" : format.channelCount + "ch, " + format.sampleRate + "Hz";
	}

	private static String buildLanguageString(MediaFormat format) {
		return TextUtils.isEmpty(format.language) || "und".equals(format.language) ? ""
			: format.language;
	}

	private static String buildBitrateString(MediaFormat format) {
		return format.bitrate == MediaFormat.NO_VALUE ? ""
			: String.format(Locale.US, "%.2fMbit", format.bitrate / 1000000f);
	}

	private static String joinWithSeparator(String first, String second) {
		return first.length() == 0 ? second : (second.length() == 0 ? first : first + ", " + second);
	}

	private static String buildTrackIdString(MediaFormat format) {
		return format.trackId == null ? "" : " (" + format.trackId + ")";
	}

	private boolean onTrackItemClick(MenuItem item, int type) {
		if (player == null || item.getGroupId() != MENU_GROUP_TRACKS) {
			return false;
		}
		player.setSelectedTrack(type, item.getItemId() - ID_OFFSET);
		return true;
	}

	private void toggleControlsVisibility()  {
		if (mediaController.isShowing()) {
			mediaController.hide();
			//debugRootView.setVisibility(View.GONE);
		} else {
			showControls();
		}
	}

	private void showControls() {
		mediaController.show(0);
		//debugRootView.setVisibility(View.VISIBLE);
	}

// subtitles


	private void configureSubtitleView() {
		CaptionStyleCompat style;
		float fontScale;
		if (Util.SDK_INT >= 19) {
			style = getUserCaptionStyleV19();
			fontScale = getUserCaptionFontScaleV19();
		} else {
			style = CaptionStyleCompat.DEFAULT;
			fontScale = 1.0f;
		}
		subtitleLayout.setStyle(style);
		subtitleLayout.setFractionalTextSize(SubtitleLayout.DEFAULT_TEXT_SIZE_FRACTION * fontScale);
	}

	@TargetApi(19)
	private float getUserCaptionFontScaleV19() {
		CaptioningManager captioningManager = (CaptioningManager) getActivity().getSystemService(Context.CAPTIONING_SERVICE);
		return captioningManager.getFontScale();
	}

	@TargetApi(19)
	private CaptionStyleCompat getUserCaptionStyleV19() {
		CaptioningManager captioningManager = (CaptioningManager) getActivity().getSystemService(Context.CAPTIONING_SERVICE);
		return CaptionStyleCompat.createFromCaptionStyle(captioningManager.getUserStyle());
	}

	/**
	 * Makes a best guess to infer the type from a media {@link android.net.Uri} and an optional overriding file
	 * extension.
	 *
	 * @param uri The {@link android.net.Uri} of the media.
	 * @param fileExtension An overriding file extension.
	 * @return The inferred type.
	 */
	private static int inferContentType(Uri uri, String fileExtension) {
		String lastPathSegment = !TextUtils.isEmpty(fileExtension) ? "." + fileExtension : uri.getLastPathSegment();
		return Util.inferContentType(lastPathSegment);
	}

	public String[] getAudioList(){
		return getTypeList(ExoPlayerWrapper.TYPE_AUDIO);
	}

	public String[] getSubtitleList(){
		return getTypeList(ExoPlayerWrapper.TYPE_TEXT);
	}

	public void setAudioTrack(int selection){
		player.setSelectedTrack(ExoPlayerWrapper.TYPE_AUDIO, selection);
	}

	public void setSubtitleTrack(int selection){
		player.setSelectedTrack(ExoPlayerWrapper.TYPE_TEXT, selection);
	}

	public String getSelectedAudio(){
		return  getSelectedType(ExoPlayerWrapper.TYPE_AUDIO);
	}

	public String getSelectedSubtitle(){
		return  getSelectedType(ExoPlayerWrapper.TYPE_TEXT);
	}

	public int getSelectedAudioIndex(){
			return player.getSelectedTrack(ExoPlayerWrapper.TYPE_AUDIO);
			}

	public int getSelectedSubtitleIndex(){
		return player.getSelectedTrack(ExoPlayerWrapper.TYPE_TEXT);
	}

	private String[] getTypeList(int type){
		int size = player.getTrackCount(type);
		String[] retList = new String[0];
		if (size > 0) {
			retList = new String[size];
			for (int i=0 ; i< size; i++){
				MediaFormat format = player.getTrackFormat(type, i);
				retList[i] = format.language;
			}
		}
		return retList;
	}

	private String getSelectedType(int type){
		int selected = player.getSelectedTrack(type);
		if (selected >= 0 && selected < player.getTrackCount(type)){
			MediaFormat format = player.getTrackFormat(type, selected);
			if (format != null)
				return format.language;
		}
		return null;
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
		if (requiresPermission(Uri.parse(currentPlaybackContent.mediaURL))) {
		//requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
			return true;
		} else {
			return false;
		}
	}

	@TargetApi(23)
	private boolean requiresPermission(Uri uri) {
		return Util.SDK_INT >= 23 && Util.isLocalFileUri(uri)
                /*&& checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED*/;
	}

	// Internal methods

	private ExoPlayerWrapper.RendererBuilder getRendererBuilder() {
		String userAgent = Util.getUserAgent(getActivity(), "ExoPlayerDemo");
		switch (contentType) {
            /*case Util.TYPE_SS:
                return new SmoothStreamingRendererBuilder(this, userAgent, contentUri.toString(),
                        new SmoothStreamingTestMediaDrmCallback());*/
			case Util.TYPE_DASH:
				return new DashRendererBuilder(getActivity(), userAgent, currentPlaybackContent.mediaURL,
					new WidevineMediaDrmCallback(currentPlaybackContent));
            /*case Util.TYPE_HLS:
                return new HlsRendererBuilder(this, userAgent, contentUri.toString());
            case Util.TYPE_OTHER:
                return new ExtractorRendererBuilder(this, userAgent, contentUri);*/
			default:
				throw new IllegalStateException("Unsupported type: " + contentType);
		}
	}

	private void preparePlayer(boolean playWhenReady) {
		if (player == null) {
			player = new ExoPlayerWrapper(getRendererBuilder());
			player.addListener(this);
			player.setCaptionListener(this);
			player.setMetadataListener(this);
			player.seekTo(playerPosition);
			playerNeedsPrepare = true;
			mediaController.setMediaPlayer(player.getPlayerControl());
			mediaController.setEnabled(true);
			eventLogger = new DashPlayerEventLogger();


			eventLogger.startSession();
			player.addListener(eventLogger);
			player.setInfoListener(eventLogger);
			player.setInternalErrorListener(eventLogger);
			//debugViewHelper = new DebugTextViewHelper(player, debugTextView);
			//debugViewHelper.start();
		}
		if (playerNeedsPrepare) {
			player.prepare();
			playerNeedsPrepare = false;
			updateButtonVisibilities();
		}
		player.setSurface(surfaceView.getHolder().getSurface());
		player.setPlayWhenReady(playWhenReady);
	}

	private class DashPlayerEventLogger extends EventLogger{
		@Override
		public void onError(Exception e) {
			super.onError(e);


			hideLoadingView();
			String message = "Streaming Error";//Localizer.get(KEYS.ANDROID_PLAYBACK_PROBLEM);
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
	}

	private void trackPlaybackEvent(String eventLabel, int eventValue) {
		//Trackers.instance().trackEvent(Drm.manager().getPlaybackTag(), Drm.manager().getPlaybackTitle(), "Streaming",
		//		UrlHelper.urlEncode(Build.MODEL), eventLabel, eventValue);
	}

	private void releasePlayer() {
		if (player != null) {
			//debugViewHelper.stop();
			//debugViewHelper = null;
			playerPosition = player.getCurrentPosition();
			player.release();
			player = null;
			eventLogger.endSession();
			eventLogger = null;
		}
	}

	/************************AbstractNextGenMainMovieFragment*********************/

	@Override
	public void setPlaybackObject(Object playbackObject){
		currentPlaybackContent = (DashContent)playbackObject;
		contentType = Util.TYPE_DASH;
	}
	@Override
	public void setCustomMediaController(CustomMediaController customMC){
		mediaController = customMC;
		if (rootView != null)
			mediaController.setAnchorView(rootView);
	}

	@Override
	public void setResumeTime(int resumeTime){
		//mStartTime = resumeTime
		player.seekTo(resumeTime);
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
	public boolean isPlaying(){
		if (player != null)
			return player.getPlayerControl().isPlaying();
		return false;
	}

	@Override
	public void pause(){

	}

	@Override
	public void resumePlayback(){

	}

	@Override
	public void streamStartPreparations(final ResultListener<Boolean> resultLister){
		//TODO: stream API request?
		/*
		if (stream == null){
			showLoadingView();
			final PhysicalAsset.Definition def = PhysicalAsset.Definition.HD;//.valueOf(intent.getStringExtra(F.STANDARD));
			final String audioLang = currentPlaybackContent.getSelectedAudio(false);//intent.getStringExtra(F.AUDIO_LANGUAGE);
			final String subtitleLang = currentPlaybackContent.getSelectedSubtitle() != null ?
					currentPlaybackContent.getSelectedSubtitle().getLocale() : "";//intent.getStringExtra(F.SUBTITLE_LANGUAGE);

			StreamDAO.streamStart(currentPlaybackContent, null, def, F.ANDROID_DRM_ODF, audioLang, F.DASH_ASSET, new net.flixster.android.util.concurrent.ResultListener<Stream>() {
				@Override
				public void onResult(final Stream result) {

					CPEDemoMovieFragment_Dash.this.stream = result;
					resultLister.onResult(true);


				}

				@Override
				public <E extends Exception> void onException(E e) {        // do nothing for now.
					resultLister.onException(e);
				}
			});
		}*/

		resultLister.onResult(true);
	}



	/********************PlayPauseListener*******************/
	//TODO: enable this
	/*@Override
	public void playerPaused(boolean paused) {
		//if (FlixsterApplication.isConnected()){
		try{

			//savePlayPosition(videoView.getCurrentPosition()/1000);
		}catch (IllegalStateException is){}
		//}
		if (!FlixsterApplication.isSimulator()) {
			if (Drm.manager().isStreamingMode()) {
				ConvivaHelper.reportPlayerPaused(paused);
			}
		}

	}*/

	// SurfaceHolder.Callback implementation

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (player != null) {
			player.setSurface(holder.getSurface());
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// Do nothing.
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (player != null) {
			player.blockingClearSurface();
		}
	}

	// OnClickListener
	public void onClick(View v){

	}

	// ExoPlayerWrapper.CaptionListener implementation

	@Override
	public void onCues(List<Cue> cues) {
		subtitleLayout.setCues(cues);
	}

	// ExoPlayerWrapper.MetadataListener implementation

	@Override
	public void onId3Metadata(List<Id3Frame> id3Frames) {
		for (Id3Frame id3Frame : id3Frames) {
			if (id3Frame instanceof TxxxFrame) {
				TxxxFrame txxxFrame = (TxxxFrame) id3Frame;
				Log.i(TAG, String.format("ID3 TimedMetadata %s: description=%s, value=%s", txxxFrame.id,
						txxxFrame.description, txxxFrame.value));
			} else if (id3Frame instanceof PrivFrame) {
				PrivFrame privFrame = (PrivFrame) id3Frame;
				Log.i(TAG, String.format("ID3 TimedMetadata %s: owner=%s", privFrame.id, privFrame.owner));
			} else if (id3Frame instanceof GeobFrame) {
				GeobFrame geobFrame = (GeobFrame) id3Frame;
				Log.i(TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
						geobFrame.id, geobFrame.mimeType, geobFrame.filename, geobFrame.description));
			} else {
				Log.i(TAG, String.format("ID3 TimedMetadata %s", id3Frame.id));
			}
		}
	}

	// ExoPlayerWrapper.Listener implementation

	@Override
	public void onStateChanged(boolean playWhenReady, int playbackState) {
		if (playbackState == ExoPlayer.STATE_ENDED) {
			showControls();
		}
		String text = "playWhenReady=" + playWhenReady + ", playbackState=";
		switch(playbackState) {
			case ExoPlayer.STATE_BUFFERING:
				text += "buffering";
				break;
			case ExoPlayer.STATE_ENDED:
				text += "ended";
				if (completionListener != null)
					completionListener.onCompletion(null);
				break;
			case ExoPlayer.STATE_IDLE:
				text += "idle";
				break;
			case ExoPlayer.STATE_PREPARING:
				text += "preparing";
				break;
			case ExoPlayer.STATE_READY:
				text += "ready";
				break;
			default:
				text += "unknown";
				break;
		}
		//playerStateTextView.setText(text);
		updateButtonVisibilities();
	}

	@Override
	public void onError(Exception e) {
		String errorString = null;
		if (e instanceof UnsupportedDrmException) {
			// Special case DRM failures.
			UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
			errorString = getString(Util.SDK_INT < 18 ? R.string.error_drm_not_supported
					: unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
					? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
		} else if (e instanceof ExoPlaybackException
				&& e.getCause() instanceof MediaCodecTrackRenderer.DecoderInitializationException) {
			// Special case for decoder initialization failures.
			MediaCodecTrackRenderer.DecoderInitializationException decoderInitializationException =
					(MediaCodecTrackRenderer.DecoderInitializationException) e.getCause();
			if (decoderInitializationException.decoderName == null) {
				if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
					errorString = getString(R.string.error_querying_decoders);
				} else if (decoderInitializationException.secureDecoderRequired) {
					errorString = getString(R.string.error_no_secure_decoder/*, decoderInitializationException.mimeType*/);
				} else {
					errorString = getString(R.string.error_no_decoder/*, decoderInitializationException.mimeType*/);
				}
			} else {
				errorString = getString(R.string.error_instantiating_decoder/*, decoderInitializationException.decoderName*/);
			}
		}
		if (errorString != null) {
			Toast.makeText(getActivity().getApplicationContext(), errorString, Toast.LENGTH_LONG).show();
		}
		playerNeedsPrepare = true;
		updateButtonVisibilities();
		showControls();
	}

	@Override
	public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
								   float pixelWidthAspectRatio) {
		shutterView.setVisibility(View.GONE);
		videoFrame.setAspectRatio(
				height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
	}

	// AudioCapabilitiesReceiver.Listener methods

	@Override
	public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
		if (player == null) {
			return;
		}
		boolean backgrounded = player.getBackgrounded();
		boolean playWhenReady = player.getPlayWhenReady();
		releasePlayer();
		preparePlayer(playWhenReady);
		player.setBackgrounded(backgrounded);
	}
}
