package com.lgorsl.testapp;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * immutable class
 * <p/>
 * Created by lgor on 13.06.2016.
 */
public class ComputationStatus implements Serializable {

    private static final long serialVersionUID = 1423L;

    public final int timeForComputationMs;
    public final int elapsedTimeMs;

    public final int rowNumber;
    public final BigInteger biggestNumber;

    public final boolean isFinished;

    public ComputationStatus(int timeForComputationMs,
                             int elapsedTimeMs,
                             int rowNumber,
                             BigInteger biggestNumber,
                             boolean isFinished) {
        this.timeForComputationMs = timeForComputationMs;
        this.elapsedTimeMs = Math.min(elapsedTimeMs, timeForComputationMs);
        this.rowNumber = rowNumber;
        this.biggestNumber = biggestNumber;
        this.isFinished = isFinished;
    }

    public int remainingTimeMs() {
        return timeForComputationMs - elapsedTimeMs;
    }

    public boolean computationIsFinished() {
        //return elapsedTimeMs >= timeForComputationMs;
        return isFinished;
    }

    public float getProgress() {
        if (timeForComputationMs == 0) return 0;
        return ((float) elapsedTimeMs) / timeForComputationMs;

    }

    public ComputationStatus finishedCopy() {
        if (isFinished) return this;

        return new ComputationStatus(
                timeForComputationMs,
                elapsedTimeMs,
                rowNumber,
                biggestNumber,
                true);
    }
}
