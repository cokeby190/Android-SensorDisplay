package com.android.fyp.sensors;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

/**
 * Class to write the Sensor Data to the External Memory
 * (Since we currently have no SD Card, it is writing to External Memory that the user can access)
 * @author Cheryl
 *
 */
public class SaveExt {

	boolean mExtAvail = false;
	boolean mExtWrite = false;
	public String pathname = "sensor_data";
	
	Context getContext;
	
	/**
	 * Class Constructor initialising Context from Activity
	 * @param context	Context passed to Class from Activity (since class has no Context attribute)
	 */
	public SaveExt(Context context) { 
		this.getContext = context;
	}
	
	/**
	 * Set if the File is read for read and write depending on conditions
	 */
	public void setState() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExtAvail = mExtWrite = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExtAvail = true;
		    mExtWrite = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    // to know is we can neither read nor write
		    mExtAvail = mExtWrite = false;
		}
		
		Toast.makeText(getContext, "ExtAvail : " + mExtAvail, Toast.LENGTH_SHORT).show();
		Toast.makeText(getContext, "ExtWrite : " + mExtWrite, Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Write Sensor Data to file
	 * @param date			current date to store as filename
	 * @param file_content	content to write to file (includes timestamp)
	 * @param filename		filename to write contents into
	 */
	public void writeExt(String date, String file_content, String filename) {

		String path = Environment.getExternalStorageDirectory().toString();
		
	    if (mExtWrite == true) {
	        try {
	            File file = new File(path, pathname);
	            if(!file.exists())
	            	file.mkdirs();
	            File writeFile = new File(file, date + "_" + filename + ".txt");
	            BufferedOutputStream fw = new BufferedOutputStream(new FileOutputStream(writeFile, true));
	            //BufferedWriter fw = new BufferedWriter(new FileWriter(writeFile));
	            fw.write(file_content.getBytes());
	            fw.close();
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    } else
	    	Toast.makeText(getContext, "File Cannot be assessed.", Toast.LENGTH_LONG).show();
		
	}

	/**
	 * Return the value of ExtAvail : availability to read
	 * @return	ExtAvail
	 */
	public boolean getExtAvail() {
		return mExtAvail;
	}

	/**
	 * Return the value of ExtWrite : availability to write
	 * @return	ExtWrite
	 */
	public boolean getExtWrite() {
		return mExtWrite;
	}

}
