/*
  Offscreen Android Views library for Qt

  Authors:
  Sergey A. Galin <sergey.galin@gmail.com>
  Vyacheslav O. Koscheev <vok1980@gmail.com>

  Distrbuted under The BSD License

  Copyright (c) 2014, DoubleGIS, LLC.
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:

  * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.
  * Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.
  * Neither the name of the DoubleGIS, LLC nor the names of its contributors
    may be used to endorse or promote products derived from this software
    without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS
  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
  THE POSSIBILITY OF SUCH DAMAGE.
*/

package ru.dublgis.androidlocation;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.app.Dialog;

import java.lang.Exception;
import java.lang.Override;
import java.util.LinkedHashMap;

import android.os.Bundle;
import android.location.Location;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationResult;

import java.util.Map;

import ru.dublgis.androidhelpers.Log;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.google.android.gms.location.LocationServices.FusedLocationApi;


public class GmsLocationProvider implements ConnectionCallbacks, OnConnectionFailedListener
{
	public static final String TAG = "Grym/GmsLocProvider";
	
	public final static int STATUS_DISCONNECTED			= 0;
	public final static int STATUS_CONNECTED			= 1;
	public final static int STATUS_CONNECTION_ERROR		= 2;
	public final static int STATUS_CONNECTION_SUSPENDED	= 3;

	private volatile long native_ptr_ = 0;

	private GoogleApiClient mGoogleApiClientCache = null;
	private boolean mGoogleApiClientCreateTried = false;

	private synchronized GoogleApiClient googleApiClient() {
		try {
			if (!mGoogleApiClientCreateTried) {
				mGoogleApiClientCreateTried = true;
				mGoogleApiClientCache = new GoogleApiClient.Builder(getActivity())
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.addApi(LocationServices.API)
					.build();
			}
			return mGoogleApiClientCache;
		} catch (final Throwable e) {
			Log.e(TAG, "Exception in googleApiClient: ", e);
			return null;
		}
	}

	private Location mCurrentLocation;
	private long mLastRequestId = 0;

	private Looper mlocationUpdatesLooper = null;
	final private Thread mlocationUpdatesThread = new Thread() {
		public void run() {
			Looper.prepare();
			mlocationUpdatesLooper = Looper.myLooper();
			Looper.loop();
		}
	};

	private class RequestHolder {
		public long mRequestId;
		public LocationRequest mRequest = null;
		public LocationCallback mCallback = null;

		RequestHolder(long id, LocationRequest request, LocationCallback callback) {
			mRequestId = id;
			mRequest = request;
			mCallback = callback;
		}
	}

	final private Map<Long, RequestHolder> mRequests = new LinkedHashMap<Long, RequestHolder>();


	public GmsLocationProvider(long native_ptr)
	{
		native_ptr_ = native_ptr;
		googleApiClientStatus(native_ptr_, STATUS_DISCONNECTED);
		mlocationUpdatesThread.start();
	}


	//! Called from C++ to notify us that the associated C++ object is being destroyed.
	public void cppDestroyed()
	{
		Log.i(TAG, "cppDestroyed");

		googleApiClientStatus(native_ptr_, STATUS_DISCONNECTED);
		native_ptr_ = 0;
		
		activate(false);

		if (null != mlocationUpdatesLooper)
		{
			mlocationUpdatesLooper.quit();
		}

		try {
			if (mlocationUpdatesThread.isAlive()) {
				mlocationUpdatesThread.join(300);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	@Override
	public void onConnected(Bundle connectionHint)
	{
		Log.i(TAG, "Connected to GoogleApiClient");
		Location locationToSend = null;

		try {
			Location lastLocation = FusedLocationApi.getLastLocation(googleApiClient());

			synchronized (mRequests) {
				if (null == mCurrentLocation) {
					mCurrentLocation = lastLocation;
				} else if (null != lastLocation) {
					if (lastLocation.getTime() > mCurrentLocation.getTime()) {
						mCurrentLocation = lastLocation;
					}
				}
				locationToSend = mCurrentLocation;
			}
		}
		catch(SecurityException e) {
			Log.e(TAG, "onConnected: Exception while getLastLocation, no permissions: ", e);
		}
		catch(Exception e) {
			Log.e(TAG, "onConnected: Exception while getLastLocation: ", e);
		}

		if (null != locationToSend) {
			googleApiClientLocation(native_ptr_, locationToSend, true, 0);
		}

		googleApiClientStatus(native_ptr_, STATUS_CONNECTED);
		processAllRequests();
	}


	@Override
	public void onConnectionSuspended(int cause) 
	{
		Log.i(TAG, "Connection suspended, cause = " + cause);
		googleApiClientStatus(native_ptr_, STATUS_CONNECTION_SUSPENDED);
	}


	@Override
	public void onConnectionFailed(ConnectionResult result) 
	{
		Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
		googleApiClientStatus(native_ptr_, STATUS_CONNECTION_ERROR);
	}


	private void deinitRequest(final Long key) {
		synchronized (mRequests) {
			if (mRequests.containsKey(key)) {
				final RequestHolder holder = mRequests.get(key);

				final GoogleApiClient client = googleApiClient();
				if (null != client && client.isConnected() && null != holder.mCallback) {
					try {
						PendingResult<Status> result =
							FusedLocationApi.removeLocationUpdates(client, holder.mCallback);

						result.setResultCallback(new ResultCallback<Status>() {
							@Override
							public void onResult(@NonNull Status status) {
								Log.d(TAG, "Result for removeLocationUpdates " + holder.mRequestId + " is " + status);
							}
						});
					} catch (Exception e) {
						Log.e(TAG, "Failed to removeLocationUpdates: ", e);
					}
				}
			}

			mRequests.remove(key);
		}
	}


	private RequestHolder reinitRequest(final Long key, LocationRequest request, LocationCallback callback)
	{
		Log.i(TAG, "Init request with key " + key + ": " + request.toString());

		synchronized (mRequests) {
			deinitRequest(key);
			RequestHolder holder = new RequestHolder(key, request, callback);
			mRequests.put(key, holder);
			return holder;
		}
	}


	private void processAllRequests()
	{
		synchronized (mRequests) {
			for (RequestHolder val : mRequests.values()) {
				processRequest(val);
			}
		}
	}


	private void processRequest(final RequestHolder holder) {
		try {
			final GoogleApiClient client = googleApiClient();
			if (client != null && client.isConnected() && null != holder) {
				Log.i(TAG, "requestLocationUpdates " + holder.mRequestId);

				Context ctx = getActivity();
				if (Build.VERSION.SDK_INT >= 23 &&
						PERMISSION_DENIED == ContextCompat.checkSelfPermission(ctx, ACCESS_FINE_LOCATION) &&
						PERMISSION_DENIED == ContextCompat.checkSelfPermission(ctx, ACCESS_COARSE_LOCATION)) {
					return;
				}

				PendingResult<Status> result =
					FusedLocationApi.requestLocationUpdates(client, holder.mRequest, holder.mCallback, mlocationUpdatesLooper);

				result.setResultCallback(new ResultCallback<Status>() {
					@Override
					public void onResult(@NonNull Status status) {
						Log.d(TAG, "Result for requestLocationUpdates " + holder.mRequestId + " is " + status);
					}
				});
			}
		} catch (IllegalStateException e) {
			Log.e(TAG, "Failed to connect GoogleApiClient, incorrect looper: ", e);
		} catch (SecurityException e) {
			Log.e(TAG, "Failed to connect GoogleApiClient, no permissions: ", e);
		} catch (Exception e) {
			Log.e(TAG, "Failed to connect GoogleApiClient: ", e);
		}
	}


	public long startLocationUpdates(final int priority,
									 final long interval,
									 final long fastestInterval,
									 final long maxWaitTime,
									 final int numUpdates,
									 final long expirationDuration,
									 final long expirationTime)
	{
		Log.i(TAG, "startLocationUpdates");

		LocationRequest request = new LocationRequest();

		try {
			request
				.setPriority(priority)
				.setInterval(interval)
				.setFastestInterval(fastestInterval);

			if (maxWaitTime > 0) {
				request.setMaxWaitTime(maxWaitTime);
			}

			if (numUpdates > 0) {
				request.setNumUpdates(numUpdates);
			}

			if (expirationDuration > 0) {
				request.setExpirationDuration(expirationDuration);
			}

			if (expirationTime > 0) {
				request.setExpirationTime(expirationTime);
			}
		}
		catch (Throwable e) {
			Log.e(TAG, "Failed to init LocationRequest", e);
		}

		final Long requestId = ++mLastRequestId;

		LocationCallback callback = new LocationCallback() {
			@Override
			public void onLocationAvailability(LocationAvailability locationAvailability) {
				boolean available = false;
				if (null != locationAvailability) {
					available = locationAvailability.isLocationAvailable();
				}
				googleApiClientLocationAvailable(native_ptr_, available);
			}

			@Override
			public void onLocationResult(LocationResult result)
			{
				Location location = result.getLastLocation();

				synchronized (mRequests) {
					mCurrentLocation = location;
				}

				if (null != location) {
					googleApiClientLocation(native_ptr_, location, false, requestId);
				}
			}
		};

		final RequestHolder holder = reinitRequest(requestId, request, callback);
		processRequest(holder);

		Log.i(TAG, "Request Id = " + holder.mRequestId);
		return holder.mRequestId;
	}


	public void stopLocationUpdates(long id)
	{
		Log.d(TAG, "stopLocationUpdates(" + id + ")" );

		synchronized (mRequests) {
			deinitRequest(id);
		}
	}


	public void activate(boolean enable)
	{
		final GoogleApiClient client = googleApiClient();
		if (null != client) {
			if (enable) {
				client.connect();
			} else {
				client.disconnect();
				googleApiClientStatus(native_ptr_, STATUS_DISCONNECTED);
			}
		}
	}


	static public boolean isAvailable(final Activity activity, final boolean allowDialog)
	{
		try {
			final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
			final int errorCode = apiAvailability.isGooglePlayServicesAvailable(activity);

			switch (errorCode) {
				case ConnectionResult.SERVICE_MISSING:
				case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
				case ConnectionResult.SERVICE_DISABLED:

					if (allowDialog) {
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Dialog dialog = apiAvailability.getErrorDialog(activity, errorCode, 1);
								if (null != dialog) {
									dialog.show();
								}
							}
						});
					}
			}

			return ConnectionResult.SUCCESS == errorCode;
		}
		catch(Exception e) {
			Log.e(TAG, "isAvailable(): ", e);
		}

		return false;
	}


	static public int getGmsVersion(Activity activity)
	{
		int versionCode = 0;

		try {
			versionCode = activity.getPackageManager().getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0).versionCode;
		}
		catch(Exception e) {
			Log.e(TAG, "getGmsVersion(): " + e);
		}

		return versionCode;
	}


	public boolean runOnUiThread(final Runnable runnable) {
		try {
			if (runnable == null) {
				Log.e(TAG, "runOnUiThread: null runnable!");
				return false;
			}

			final Activity activity = getActivity();

			if (activity == null) {
				Log.e(TAG, "runOnUiThread: cannot schedule task because of the null context!");
				return false;
			}

			activity.runOnUiThread(runnable);
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Exception when posting a runnable:", e);
			return false;
		}
	}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public native Activity getActivity();
	public native void googleApiClientStatus(long nativeptr, int status);
	public native void googleApiClientLocationAvailable(long nativeptr, boolean available);
	public native void googleApiClientLocation(long nativeptr, Location location, boolean initial, long requestId);
}
