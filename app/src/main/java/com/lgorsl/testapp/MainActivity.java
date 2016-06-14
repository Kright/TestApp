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

        if (computationTime > 0) {
            if (progressUpdater!=null){
                progressUpdater.strongCancel = true;
                progressUpdater.cancel(true);
            }

            iComputationContext cntx = pascalComputationService.startNewComputation(computationTime * 1000);
            setProgressUpdate(cntx);
        }
    }

    public void onButtonResetClick(View v) {
        log("resetClick");

        pascalComputationService.resetComputation();
        fieldInput.setText("");

        if (progressUpdater!=null){
            progressUpdater.cancel(true);
            progressUpdater.strongCancel = true;
        }
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

        if (progressUpdater != null) {
            progressUpdater.cancel(true);
            progressUpdater.strongCancel = true;
        }

        if(pascalComputationService!=null) {
            pascalComputationService.setActivityAlive(false);
        }
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
                textView.setText(status.biggestNumber.toString());
                progressBar.setProgress((int) (progressBar.getMax() * status.getProgress()));
            }
            setProgressUpdate(cntxt);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log("service disconnected");
            pascalComputationService = null;
        }
    };

    private ProgressUpdater progressUpdater;

    /**
     * updates progress bar and biggest value while computation running
     *
     * @param cntxt
     */
    private void setProgressUpdate(iComputationContext cntxt) {
        log("set progress update for " + (cntxt.getStatus().computationIsFinished() ? "finished" : "unfinished"));
        progressUpdater = new ProgressUpdater(pascalComputationService);
        progressUpdater.execute(cntxt);
    }

    class ProgressUpdater extends AsyncTask<iComputationContext, ComputationStatus, ComputationStatus> {

        private final PascalComputationService service;
        volatile boolean strongCancel = false;  // bugs without this

        ProgressUpdater(PascalComputationService service) {
            this.service = service;
        }

        @Override
        protected ComputationStatus doInBackground(iComputationContext... params) {
            log("AsyncTaskStarted");
            if (params.length != 1) {
                log("wrong params length : " + params.length);
                return new ComputationStatus(0, 0, 1, new BigInteger("1"), true);
            }
            iComputationContext cntxt = params[0];
            ComputationStatus s = cntxt.getStatus();

            while (service.getComputationContext() == cntxt) {
                ComputationStatus newS = cntxt.getStatus();
                if (s != newS) {
                    s = newS;
                    publishProgress(s);
                    if (s.computationIsFinished() || isCancelled() || strongCancel) break;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(25);
                } catch (InterruptedException e) {
                }
            }
            log("AsyncTaskFinished");
            return s;
        }

        @Override
        protected void onProgressUpdate(ComputationStatus... values) {
            if (strongCancel || isCancelled()) return;
            ComputationStatus last = values[values.length - 1];
            MainActivity.this.progressBar.setProgress((int) (progressBar.getMax() * last.getProgress()));
            MainActivity.this.textView.setText(last.biggestNumber.toString());
        }

        @Override
        protected void onPostExecute(ComputationStatus status) {
            if (strongCancel || isCancelled()) return;
            MainActivity.this.progressBar.setProgress((int) (progressBar.getMax() * status.getProgress()));
            MainActivity.this.textView.setText(status.biggestNumber.toString());
        }
    }

    public static void log(String msg) {
        Log.d("TestApp", "activity: " + msg);
    }
}
