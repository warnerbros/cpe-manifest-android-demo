package com.wb.cpedemo;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;

import com.wb.nextgenlibrary.interfaces.NextGenEventHandler;

/**
 * Created by gzcheng on 10/27/16.
 */

public class CPEDemoHandler implements NextGenEventHandler {

	public void handleMovieTitleSelection(final Activity activity, String movieId){
		if (movieId == null && "".equals(movieId)){
			return;
		}

		String encodedString = movieId.replace(":", "").replace("-","").replace(" ", "%20");


		final String url = "http://www.vudu.com/movies/#search/" + encodedString;
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
		alertDialogBuilder.setTitle(activity.getResources().getString(com.wb.nextgenlibrary.R.string.dialog_leave_app));
		alertDialogBuilder.setMessage(activity.getResources().getString(com.wb.nextgenlibrary.R.string.dialog_follow_link));
		alertDialogBuilder.setPositiveButton(activity.getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				activity.startActivity(browserIntent);
			}
		});
		alertDialogBuilder.setNegativeButton(activity.getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		alertDialogBuilder.show();
	}

	public boolean isDebugBuild(){
		return true;
	}

	public void userEventLog(String screen, String subScreen, String button, String action, String value){
		//FlixsterLogger.d("NextGenLog", "Screen = " + screen + "\nsubScreen = " + subScreen + "\nbutton = " + button + "\naction = " + action + "\nvalue = " + value);
		// log conviva, fabric, google analytics
	}

	public void handleShareLink(Activity activity, Fragment fragment, String shareUrl){
		Intent share = new Intent(Intent.ACTION_SEND);
		share.setType("text/plain");
		share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		share.putExtra(Intent.EXTRA_SUBJECT, "Next Gen Share");
		share.putExtra(Intent.EXTRA_TEXT, shareUrl);

		if (fragment != null)
			fragment.startActivity(Intent.createChooser(share, ""));
		else if (activity != null)
			activity.startActivity(Intent.createChooser(share, ""));
	}
}
