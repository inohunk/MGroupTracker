package ru.ogpscenter.ogpstracker.service;

import android.net.Uri;
import android.location.Location;

interface IGPSTrackerServiceRemote {
	int loggingState();
    long startTracking();
	void stopTracking();

//    long startUploading();
//	  void stopStopUploading();

    Location getLastTrackPoint();
    long getLastTrackId();
	int getNumberOfUploadedPoints();
	String getPunchesUploadUrl();
}