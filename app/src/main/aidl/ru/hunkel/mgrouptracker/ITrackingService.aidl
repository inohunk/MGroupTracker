// ITrackingService.aidl
package ru.hunkel.mgrouptracker;

// Declare any non-default types here with import statements

interface ITrackingService {
    void startEvent();
    long stopEvent();
    int getState();
}
