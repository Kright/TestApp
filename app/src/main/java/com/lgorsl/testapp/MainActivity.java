package com.lgorsl.testapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private EditText fieldInput;
    private TextView textView;
    private ProgressBar progressBar;

    private PascalComputationService pascalComputationService;

    private boolean wasntRestored = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fieldInput = (EditText) findViewById(R.id.fieldTimeInput);
        textView = (TextView) findViewById(R.id.viewBestNumber);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        if (savedInstanceState != null) {   //restoring previous state
            fieldInput.setText(savedInstanceState.getString("fieldInput"));
            textView.setText(savedInstanceState.getString("textView"));
        } else {
            wasntRestored = true;
        }
    }

    public void onButtonStartClick(View v) {
        log("startClick");

        String str = fieldInput.getText().toString();
        int computationTime;
        try {
            computationTime = Integer.decode(str);
        } catch (NumberFormatException ex) {
            fieldInput.setText("");
            return;
        }

        if (computationTime > 0 && pascalComputationService != null) {

            iComputationContext cntx = pascalComputationService.startNewComputation(computationTime * 1000);
            setProgressUpdate(cntx);
        }
    }

    public void onButtonResetClick(View v) {
        log("resetClick");

        if (pascalComputationService != null) {
            pascalComputationService.resetComputation();
        }
        fieldInput.setText("");
        removeProgressUpdating();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = new Intent(this, PascalComputationService.class);
        startService(intent); //for service be alive
        bindService(intent, connection, Context.BIND_AUTO_CREATE);  //for channel to service

        log("onResume");
    }

    @Override
    public void onBackPressed() {
        log("back button, exit");
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        log("onSave");

        String input = fieldInput.getText().toString();
        outState.putString("fieldInput", input);
        outState.putString("textView", textView.getText().toString());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        log("onPause");

        if (pascalComputationService != null) {
            pascalComputationService.setActivityAlive(false);
        }

        removeProgressUpdating();
        unbindService(connection);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        log("onDestroy");
        super.onDestroy();
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            log("service connected");
            PascalComputationService.MyBinder b = (PascalComputationService.MyBinder) service;
            pascalComputationService = b.getService();
            pascalComputationService.setActivityAlive(true);

            iComputationContext cntxt = pascalComputationService.getComputationContext();
            if (wasntRestored) {
                wasntRestored = true;
                ComputationStatus status = cntxt.getStatus();

                fieldInput.setText("" + status.timeForComputationMs / 1000);
                setProgress(status);
            }
            setProgressUpdate(cntxt);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log("service disconnected");
            pascalComputationService = null;
            removeProgressUpdating();
        }
    };

    private ProgressUpdater progressUpdater = null;

    /**
     * updates progress bar and biggest value while computation running
     *
     * @param cntxt - context for observing
     */
    private void setProgressUpdate(iComputationContext cntxt) {
        ComputationStatus st = cntxt.getStatus();
        log("set progress update for " + (st.computationIsFinished() ? "finished" : "unfinished"));
        if (st.computationIsFinished()) {
            setProgress(st);
        } else {
            removeProgressUpdating();
            progressUpdater = new ProgressUpdater();
            progressUpdater.execute(cntxt);
        }
    }

    private void removeProgressUpdating() {
        if (progressUpdater == null) return;
        progressUpdater.cancel(true);
        progressUpdater = null;
    }

    private class ProgressUpdater extends AsyncTask<iComputationContext, ComputationStatus, Void> {

        private void log(String msg) {
            Log.d("TestApp", "AsynTask: " + msg);
        }

        @Override
        protected Void doInBackground(iComputationContext... params) {
            log("start");

            if (isCancelled()) {
                log("cancelled");
                return null;
            }

            if (params.length != 1) {
                log("wrong params length = " + params.length);
                return null;
            }

            iComputationContext cntxt = params[0];
            ComputationStatus status = cntxt.getStatus();
            if (status == null) {
                log("status == null!");
                return null;
            }

            while (!isCancelled()) {
                ComputationStatus newStatus = cntxt.getStatus();
                if (newStatus != status) {
                    status = newStatus;
                    publishProgress(status);
                    if (status.isFinished) break;
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(25);
                } catch (InterruptedException e) {
                    log("can't sleep");
                }
            }

            log("finish");
            return null;
        }

        @Override
        protected void onProgressUpdate(ComputationStatus... values) {
            setProgress(values[values.length - 1]);
        }
    }

    private void setProgress(ComputationStatus status) {
        textView.setText(status.biggestNumber.toString());
        progressBar.setProgress((int) (progressBar.getMax() * status.getProgress()));
    }

    public static void log(String msg) {
        Log.d("TestApp", "activity: " + msg);
    }
}
