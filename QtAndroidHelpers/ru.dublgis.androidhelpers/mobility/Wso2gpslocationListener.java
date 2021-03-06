/*
    Offscreen Android Views library for Qt

    Authors:
    Uladzislau Vasilyeu <vasvlad@gmail.com>

    Distrbuted under The BSD License

    Copyright (c) 2017.

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

package ru.dublgis.androidhelpers.mobility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import ru.dublgis.androidhelpers.Log;
import android.os.RemoteException;
import android.app.ActivityManager;


public class Wso2gpslocationListener extends BroadcastReceiver
{
    final private static String LOG_TAG = "Grym/Wso2gpslocationListener";
    final private static boolean verbose_ = false;
    private volatile long native_ptr_ = 0;
    private boolean started_ = false;
    private static Location location;

    public Wso2gpslocationListener(){
    }

    public Wso2gpslocationListener(long native_ptr)
    {
        native_ptr_ = native_ptr;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w(LOG_TAG, "Broadcast From GPS WSO2 MIRIADA");
        if (intent.hasExtra("location")) {
            Log.w(LOG_TAG, "Location here");
            location = (Location) intent.getParcelableExtra("location");
        }
        if (location == null) {
            Log.w(LOG_TAG, "Location not received");
        } else {
            /*
            Log.d(LOG_TAG, "Location> Lat:" + location.getLatitude()
                        + " Lon:" + location.getLongitude()
                        + " Provider:" + location.getProvider()
                        + " Time:" + location.getTime()
                        );
            */
            double altitude = -1;
            float bearing = -1;
            float speed = -1;
            if (location.hasAltitude())
                altitude = location.getAltitude();
            if (location.hasBearing())
                bearing = location.getBearing();
            if (location.hasSpeed())
                speed = location.getSpeed();
 
            locationInfoUpdate(native_ptr_, (int)(location.getTime()/1000), location.getLatitude(), location.getLongitude(), altitude, bearing, speed);
        }
    }

    //! Called from C++ to notify us that the associated C++ object is being destroyed.
    public void cppDestroyed()
    {
        native_ptr_ = 0;
    }
    // start listening for battery info and report them
    public synchronized boolean start()
    {
        try
        {
            Log.d(LOG_TAG, "Wso2gpslocationListener start");
            if (!started_) {
                ActivityManager manager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (service.service.getClassName().equals("org.wso2.iot.agent.services.location.LocationService")) {
                        getContext().registerReceiver(this, new IntentFilter("org.ws2.iot.agent.LOCATION_UPDATE"));
                        started_ = true;
                        return true;
                    }
                }
                return false;

            } else {
                Log.d(LOG_TAG, "Wso2gpslocationListener start: already started!");
            }
            return true;
        }
        catch (final Throwable e)
        {
            Log.e(LOG_TAG, "Exception while starting Wso2gpslocationListener: ", e);
            return false;
        }
    }

    public synchronized void stop()
    {
        Log.d(LOG_TAG, "Wso2gpslocationListener stop");
        try
        {
            if (started_) {
                getContext().unregisterReceiver(this);
                started_ = false;
            } else {
               Log.d(LOG_TAG, "Wso2gpslocationListener stop: was not started!");
            }
        }
        catch (final Throwable e)
        {
            Log.e(LOG_TAG, "Exception while stopping: ", e);
        }
    }

    private native void locationInfoUpdate(long native_ptr, int time, double lat, double lon, double altitude, float bearing, float speed);
    private native Context getContext();

} // class Wso2gpslocationListener
