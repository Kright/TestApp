package com.lgorsl.testapp;

/**
 * Created by lgor on 13.06.2016.
 */
public interface iComputationContext {

    ComputationStatus getStatus();

    /**
     * start computations if necessary
     */
    void start();

    /**
     * this object will not be used, so it have to stop computation thread (if it works)
     */
    void remove();

    /**
     * save state (if it was changed) and stop computation thread (if it works)
     */
    void save();
}
