package com.android.fyp.sensors;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Spanned;

public class DialogAct {

	public AlertDialog dialog(Context context, String sensor, Spanned message) {
		
		AlertDialog.Builder sensor_dialog = new AlertDialog.Builder(context);
		
		// set title
		sensor_dialog.setTitle(sensor);

		// set dialog message
		sensor_dialog
			.setMessage(message)
			.setCancelable(false)
			.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
					dialog.cancel();
				}
			  });
			// create alert dialog
			AlertDialog alertDialog = sensor_dialog.create();

			//return
			return alertDialog;
	}
}
