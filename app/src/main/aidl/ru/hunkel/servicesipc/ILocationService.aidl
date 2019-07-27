// ILocationService.aidl
package ru.hunkel.servicesipc;

interface ILocationService {
       void startTracking();
       void stopTracking();
       int getTrackingState();
       void setTrackingSettings(long interval);
       Location getTrack();
       long punch(int controlPoint);
}
