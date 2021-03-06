package com.example.indoormaps;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/* 
 * Location Service that sends out the users identity with the
 * current list of wifi scan data available to be used to 
 * determine the location of the user.
 * Author: Shaurya Saluja
 */

public class LocationService extends Service {

	WifiManager mainWifi;
	Receiver receiver;
	List<ScanResult> wifiList;

	@Override
	public void onCreate() {
		super.onCreate();

	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "Location Service Sending", Toast.LENGTH_LONG).show();
		Log.i("Log", "Pushing Location");

		mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		
		// If wifi disabled then enable it
		if (mainWifi.isWifiEnabled() == false) {  
			mainWifi.setWifiEnabled(true);
			Toast.makeText(getApplicationContext(), "Wifi is disabled..Enabling it now!", Toast.LENGTH_LONG).show();
			if (!isOnline()) {
				mainWifi.setWifiEnabled(false);
				Log.i("Log", "Not online through wifi- switching to data");
				Toast.makeText(getApplicationContext(), "Wifi is disabled..Turning on Mobile Data Now!", Toast.LENGTH_LONG).show();
				if (!connectToData(getApplicationContext())) {
					Log.i("Log", "Could not connect to Data - stopping service");
					Toast.makeText(getApplicationContext(), "Could not connect to Data... Stopping Service!", Toast.LENGTH_LONG).show();
					stopSelf();
				}
			}

			//			boolean isNetworkConnected(Context c) {
			//			      ConnectivityManager conManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
			//			      NetworkInfo netInfo = conManager.getActiveNetworkInfo();
			//			}
			//			
			//			final ConnectivityManager connMgr = (ConnectivityManager)Context.getSystemService(Context.CONNECTIVITY_SERVICE);
			//		    final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			//		    final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			//			Context.getSystemService(Context.CONNECTIVITY_SERVICE);

		}
		Log.i("Log","Finished checking network");
		receiver = new Receiver();
		registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		mainWifi.startScan();
		wifiList = mainWifi.getScanResults();
		return START_NOT_STICKY;
	}

	public static boolean isOnline() {
		try {
			InetAddress.getByName("google.com").isReachable(3);
			return true;
		} catch (UnknownHostException e){
			return false;
		} catch (IOException e){
			return false;
		}
	}

	public static boolean connectToWifi(Context c) {
		ConnectivityManager conManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (conManager != null) {
			conManager.setNetworkPreference(ConnectivityManager.TYPE_MOBILE);
			return true;
		}
		return false;
	}

	public static boolean connectToData(Context c) {
		ConnectivityManager conManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (conManager != null) {
			Log.i("Log", "ConManager initialized");
			conManager.setNetworkPreference(ConnectivityManager.TYPE_MOBILE);
			Log.i("Log", "set network preference ");
			return true;
		}
		Log.i("Log", "ConManager null - no data service");
		return false;
	}

	/*
	 * Receives the wifi scans data and submits it in a JSON Array to the HTTP post method
	 */
	class Receiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			wifiList = mainWifi.getScanResults();
			Collections.sort(wifiList, new Comparator<ScanResult>() {
				@Override
				public int compare(ScanResult lhs, ScanResult rhs) {
					return (lhs.level > rhs.level ? -1 : (lhs.level==rhs.level ? 0 : 1));
				}
			});
			String trail = wifiList.get(0).BSSID + "|" + wifiList.get(0).level;
			for(int i = 1; i < wifiList.size(); i++) {
				trail += ";";
				trail += wifiList.get(i).BSSID + "|" + wifiList.get(i).level;
			}
			Log.i("Log","Current UserTrail " + trail);
			JSONArray jsonArray = new JSONArray();
			JSONObject closest = new JSONObject();
			try {
				closest.put("username", "shaurya@wooo.com");
				closest.put("usertrail", trail);
			} catch (JSONException e) {
				e.printStackTrace();
				Log.i("JSON Object Put Error: ", e.toString());
			}
			jsonArray.put(closest);
			Log.i("Log", "Posting closest Json Array - " + jsonArray.toString());
			try {
				RestMethods.doPost(jsonArray, "http://ec2-54-201-114-123.us-west-2.compute.amazonaws.com:8080/manual", 1, getApplicationContext());
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			unregisterReceiver(receiver);
		}
	}
	
	@Override
	public void onDestroy() {
		Toast.makeText(this, "Location Service Stopped", Toast.LENGTH_LONG).show();
	    super.onDestroy();
	}
}
