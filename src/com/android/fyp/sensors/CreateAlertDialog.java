package com.android.fyp.sensors;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;

public class CreateAlertDialog {

	public AlertDialog newdialog(final Context context, String message) {
		AlertDialog.Builder alert_dialog = new AlertDialog.Builder(context); 
		
		alert_dialog.setMessage(message)
	    .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	        	Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				context.startActivity(intent);
	        }
	    })
	    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	        	dialog.cancel();
	        }
	    });
		AlertDialog alert = alert_dialog.create();
		
		return alert;
	}
}
