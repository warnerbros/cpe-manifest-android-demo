package com.wb.cpedemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;

import com.wb.cpedemo.dash.CPEDemoMovieFragment_Dash;
import com.wb.nextgenlibrary.NextGenExperience;
import com.wb.nextgenlibrary.util.concurrent.Worker;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.Callable;

import com.wb.cpedemo.mainmoviefragment.SimpleMainMovieFragment;
public class CPEDemoActivity extends AppCompatActivity {

/*
	final NextGenExperience.ManifestItem testItem = new NextGenExperience.ManifestItem("Batman vs Superman w/360", "urn:dece:cid:eidr-s:B257-8696-871C-A12B-B8C1-S",
			"https://d19p213wjrwt85.cloudfront.net/uvvu-images/2C89FE061219D322E05314345B0AFE72",
			"http://localhost:8080/suicidesquad_manifest_preview.xml",
			"http://localhost:8080/suicidesquad_appdata_preview.xml",
			"http://localhost:8080/suicidesquad_cpestyle_preview.xml");*/



	final NextGenExperience.ManifestItem testItem = new NextGenExperience.ManifestItem("Batman vs Superman w/360", "urn:dece:cid:eidr-s:B257-8696-871C-A12B-B8C1-S",
			"https://d19p213wjrwt85.cloudfront.net/uvvu-images/2C89FE061219D322E05314345B0AFE72",
			"https://d3hu292hohbyvv.cloudfront.net/xml/urn:dece:cid:eidr-s:B257-8696-871C-A12B-B8C1-S/bvs_manifest-2.2.xml",
			"https://d3hu292hohbyvv.cloudfront.net/xml/urn:dece:cid:eidr-s:B257-8696-871C-A12B-B8C1-S/bvs_appdata_locations-2.2.xml",
			"https://d3hu292hohbyvv.cloudfront.net/xml/urn:dece:cid:eidr-s:B257-8696-871C-A12B-B8C1-S/bvs_cpestyle-2.2.xml");


	final NextGenExperience.ManifestItem ssItem = new NextGenExperience.ManifestItem("Batman vs Superman w/360", "urn:dece:cid:eidr-s:B257-8696-871C-A12B-B8C1-S",
			"https://cpe-manifest.s3.amazonaws.com/uvvu-images/2C89FE061219D322E05314345B0AFE72",
			"https://cpe-manifest.s3.amazonaws.com/xml/urn:dece:cid:eidr-s:41D1-41F1-B266-14D0-416F-9/suicidesquad_manifest-1.1.xml",
			"https://cpe-manifest.s3.amazonaws.com/xml/urn:dece:cid:eidr-s:41D1-41F1-B266-14D0-416F-9/suicidesquad_appdata-1.1.xml",
			"https://cpe-manifest.s3.amazonaws.com/xml/urn:dece:cid:eidr-s:41D1-41F1-B266-14D0-416F-9/suicidesquad_cpestyle-1.1.xml");


	final NextGenExperience.ManifestItem ssItemGZip = new NextGenExperience.ManifestItem("Batman vs Superman w/360", "urn:dece:cid:eidr-s:B257-8696-871C-A12B-B8C1-S",
			"https://cpe-manifest.s3.amazonaws.com/uvvu-images/2C89FE061219D322E05314345B0AFE72",
			"https://d3hu292hohbyvv.cloudfront.net/xml/urn:dece:cid:eidr-s:41D1-41F1-B266-14D0-416F-9/suicidesquad_manifest-1.1.xml",
			"https://d3hu292hohbyvv.cloudfront.net/xml/urn:dece:cid:eidr-s:41D1-41F1-B266-14D0-416F-9/suicidesquad_appdata-1.1.xml",
			"https://d3hu292hohbyvv.cloudfront.net/xml/urn:dece:cid:eidr-s:41D1-41F1-B266-14D0-416F-9/suicidesquad_cpestyle-1.1.xml");

	final NextGenExperience.ManifestItem singItemGZip = new NextGenExperience.ManifestItem("Batman vs Superman w/360", "urn:dece:cid:eidr-s:B257-8696-871C-A12B-B8C1-S",
			"https://cpe-manifest.s3.amazonaws.com/uvvu-images/2C89FE061219D322E05314345B0AFE72",
			"https://d3hu292hohbyvv.cloudfront.net/xml/urn:dece:cid:eidr-s:4CC8-D548-A0E4-D319-5B2D-H/suicidesquad_extended_manifest_preview-1.0.xml",
			"https://d3hu292hohbyvv.cloudfront.net/xml/urn:dece:cid:eidr-s:4CC8-D548-A0E4-D319-5B2D-H/suicidesquad_extended_appdata_preview-1.0.xml",
			"https://d3hu292hohbyvv.cloudfront.net/xml/urn:dece:cid:eidr-s:4CC8-D548-A0E4-D319-5B2D-H/suicidesquad_extended_cpestyle_preview-1.1.xml");

	static int DESIRE_VISIBILITY =
			View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
					View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
					View.SYSTEM_UI_FLAG_FULLSCREEN |
					View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

	Button startNEGButton, startNGEDashButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_next_gen_demo);

		if (getSupportActionBar() != null)
			getSupportActionBar().hide();

		startNEGButton = (Button) findViewById(R.id.start_normal_btn);
		startNEGButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					NextGenExperience.startNextGenExperience(getApplicationContext(), CPEDemoActivity.this,
							singItemGZip, "http://wb-extras.warnerbros.com/extrasplus/prod/Manifest/SuicideSquad_MiniManifest/assets/Video/SS_MiniManifest_Feature.m3u8", SimpleMainMovieFragment.class, null,
							new CPEDemoHandler(), Locale.US, "WB");
				} catch (NextGenExperience.NextGenEmptyStudioStringException ex){

				}
			}
		});

		startNGEDashButton = (Button) findViewById(R.id.start_dash_btn);
		startNGEDashButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CPEDemoMovieFragment_Dash.DashContent dashContent =
						new CPEDemoMovieFragment_Dash.DashContent("https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears_hd.mpd",
								"https://proxy.uat.widevine.com/proxy",
								"0894c7c8719b28a0", "widevine_test");
				try {
					NextGenExperience.startNextGenExperience(getApplicationContext(), CPEDemoActivity.this,
							ssItemGZip, dashContent, CPEDemoMovieFragment_Dash.class, null,
							new CPEDemoHandler(), Locale.US, "WB");
				} catch (NextGenExperience.NextGenEmptyStudioStringException ex){

				}

			}
		});

	}

	@Override
	protected void onStart() {
		super.onStart();
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | DESIRE_VISIBILITY);


	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus)
			getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | DESIRE_VISIBILITY);
	}

}
