package com.lgorsl.testapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class PascalComputationService extends Service {

    private final IBinder myBinder = new MyBinder();

    private iComputationContext computationContext;

    private boolean isActivityAlive = false;

    public void setActivityAlive(boolean value){
        synchronized (this) {
            isActivityAlive = value;
        }
        stopSelfIfNecessary();
    }

    public synchronized boolean isActivityAlive(){
        return isActivityAlive;
    }

    @Override
    public void onCreate() {
        log("onCreate");

        computationContext = ComputationContext.loadPreviousTask(this);
        computationContext.start();
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        computationContext.save();
    }

    /**
     * @param computationTime - time in milliseconds
     * @return true if really started new
     */
    public iComputationContext startNewComputation(int computationTime) {
        log("start new computation ");

        if (computationTime > 0) {
            computationContext.remove();

            computationContext = ComputationContext.newTask(computationTime, this);
            computationContext.start();
        }

        return computationContext;
    }

    public void resetComputation() {
        log("reset");
        if (computationContext.getStatus().computationIsFinished()) return;

        computationContext.remove();
    }

    public void stopSelfIfNecessary(){
        if (!isActivityAlive() && getComputationStatus().computationIsFinished()){
            log("stop self");
            stopSelf();
        }
        //if (!isActivityAlive) stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    ComputationStatus getComputationStatus() {
        return computationContext.getStatus();
    }

    iComputationContext getComputationContext() {
        return computationContext;
    }

    public class MyBinder extends Binder {
        PascalComputationService getService() {
            return PascalComputationService.this;
        }
    }

    public static void log(String msg) {
        Log.d("TestApp", "service : " + msg);
    }
}
