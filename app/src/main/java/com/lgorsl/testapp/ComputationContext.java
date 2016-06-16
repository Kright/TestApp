package com.lgorsl.testapp;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by lgor on 13.06.2016.
 */
public class ComputationContext implements iComputationContext {

    private final PascalComputationService service;
    private ComputationStatus status;
    private ComputationState state;

    private Thread t;
    private volatile boolean stop = false;
    private volatile boolean saveResult = true;

    private ComputationContext(ComputationStatus status, ComputationState state, PascalComputationService service) {
        this.status = status;
        this.state = state;
        this.service = service;
    }

    @Override
    public synchronized ComputationStatus getStatus() {
        return status;
    }

    private synchronized void setStatus(ComputationStatus status) {
        this.status = status;
    }

    private synchronized void setState(ComputationState state) {
        this.state = state;
    }

    private synchronized ComputationState getState() {
        return this.state;
    }

    @Override
    public void start() {
        log("start");
        if (getStatus().computationIsFinished()) {
            service.stopSelfIfNecessary();
            return;
        }

        final ComputationState startState = getState();
        final ComputationStatus startStatus = getStatus();

        t = new Thread() {
            @Override
            public void run() {
                log("new thread started");

                long startTime = System.currentTimeMillis();

                ArrayList<BigInteger> arr1 = new ArrayList<>(Arrays.asList(startState.values));
                ArrayList<BigInteger> arr2 = new ArrayList<>();
                ComputationStatus status = startStatus;

                loop:
                while (!stop) {
                    arr2.clear();

                    arr2.add(arr1.get(0));
                    for (int i = 0; i < arr1.size() - 1; i++) {
                        arr2.add(arr1.get(i).add(arr1.get(i + 1)));
                    }
                    arr2.add(arr1.get(0)); //last equals first

                    ArrayList<BigInteger> tmp = arr2;
                    arr2 = arr1;
                    arr1 = tmp;

                    long now = System.currentTimeMillis();
                    int delta = (int) (now - startTime);
                    status = new ComputationStatus(
                            startStatus.timeForComputationMs,
                            startStatus.elapsedTimeMs + delta,
                            status.rowNumber + 1,
                            arr1.get(arr1.size() / 2),
                            (startStatus.elapsedTimeMs + delta >= startStatus.timeForComputationMs));

                    setStatus(status);

                    if (status.computationIsFinished()) break;

                    //delay, because algorithm works too fast and consumes a lot of memory
                    try {
                        for (int i = 0; i < status.rowNumber; i++) {
                            TimeUnit.MILLISECONDS.sleep(1);
                            if (stop) break loop;
                        }
                    } catch (InterruptedException e) {
                    }
                }

                ComputationState resultState = new ComputationState(arr1);
                setState(resultState);

                saveContext(saveResult);

                setStatus(status.finishedCopy());
                log("computation finished");
                service.stopSelfIfNecessary();
            }
        };

        t.start();
    }

    @Override
    public void remove() {
        saveResult = false;
        stop = true;
    }

    @Override
    public void save() {
        saveResult = true;
        stop = true;
    }

    private boolean saveContext(boolean saveNew) {
        File save = new File(service.getFilesDir(), "save");
        save.delete();

        if (!saveNew) return false;

        boolean success = false;
        try {
            success = save.createNewFile();
        } catch (IOException e) {
        }

        if (!success) {
            log("can't create file");
            return false;
        }

        try {
            FileOutputStream fos = new FileOutputStream(save);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            synchronized (this) {
                oos.writeObject(status);
                if (!status.isFinished) {
                    oos.writeObject(state);
                }
            }

            oos.close();
            return true;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        log("can't save to file");
        save.delete();
        return false;
    }

    public static iComputationContext newTask(int timeToComputation, PascalComputationService service) {
        ComputationState state = new ComputationState();
        ComputationStatus status = new ComputationStatus(timeToComputation, 0, 1, state.values[0], false);
        return new ComputationContext(status, state, service);
    }

    public static ComputationContext genEmptyTask(PascalComputationService service) {
        ComputationState state = new ComputationState();
        ComputationStatus status = new ComputationStatus(0, 0, 1, state.values[0], true);
        return new ComputationContext(status, state, service);
    }

    public static iComputationContext loadPreviousTask(PascalComputationService service) {
        File save = new File(service.getFilesDir(), "save");
        if (!save.exists()) {
            log("save not exists");
            return genEmptyTask(service);
        }

        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(save));

            ComputationStatus status = (ComputationStatus) ois.readObject();
            ComputationState state;
            if (!status.isFinished) {
                state = (ComputationState) ois.readObject();
            } else {
                state = new ComputationState();
            }
            return new ComputationContext(status, state, service);
        } catch (IOException | ClassNotFoundException | ClassCastException e ) {
        }

        log("can't read");
        return genEmptyTask(service);
    }

    public static void log(String msg) {
        Log.d("TestApp", "computationContext : " + msg);
    }
}
