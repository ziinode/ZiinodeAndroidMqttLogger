package io.ziinode.sens;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class main extends Activity{
    public static final String PREFS_NAME = "ZnPrefs";
    public static final String TAG = "io.ziinode.sens";
    public static final String TYPE = "<YOUR TYPE ID>";
    /**
     * Called when the activity is first created.
     */
    TextView status;
    Button conn;
    Button logBtn;
    TextView dsid;
    EditText interval;
    EditText logEt;
    main m;
    SharedPreferences settings;
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    ScheduledFuture future;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        m=this;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        settings = getSharedPreferences(PREFS_NAME, 0);
        status = (TextView) findViewById(R.id.status);


        dsid = (TextView)findViewById(R.id.dsid);
        dsid.setText(settings.getString("dsId",TYPE));
        interval =(EditText)findViewById(R.id.interval);
        logEt =(EditText)findViewById(R.id.logEt);
        String ii = settings.getString("int", null);
        if(ii!=null){
            interval.setText(ii);
        }
        conn = (Button) findViewById(R.id.conn);
        conn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(log.conn.getState()==ZnConnector.STATE_DISCONNECED) {
                    try {
                        Integer.parseInt(interval.getText().toString());
                    } catch (Exception e) {
                        Toast.makeText(m, "Sending interval no correct, please provide number in seconds" + dsid.getText().toString().length(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("dsId", dsid.getText().toString());
                    editor.putString("int", interval.getText().toString());
                    editor.commit();

                    if (log.conn != null) {
                        log.conn.start();
                        conn.setText("Disconnect");
                    }
                }else{
                    log.conn.stop();
                }
            }
        });
        logBtn = (Button) findViewById(R.id.logBtn);
        logBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(log.conn.getState()==ZnConnector.STATE_ONLINE) {
                    log.conn.log(System.currentTimeMillis(),0,logEt.getText().toString());
                }else{
                    log.conn.stop();
                }
            }
        });

    }

    private void makeT(final String toast){
        Log.i(TAG,"toast:"+toast);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(m, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void setStatus(final String st){
        Log.i(TAG,"Sst:"+st);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                status.setText(st);
            }
        });
    }



    public void setDevId(final String devId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("dsId", dsid.getText().toString());
                editor.apply();
                dsid.setText(devId);
            }
        });
    }

    public String getDsid() {
        return dsid.getText().toString();
    }
    public int getInterval() {
        return Integer.parseInt(interval.getText().toString());
    }

    Intent intent;
    @Override
    protected void onResume() {
        super.onResume();
        intent= new Intent(this, RemoteLogger.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    RemoteLogger log;

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            RemoteLogger.LocalBinder b = (RemoteLogger.LocalBinder) binder;
            log = b.getService();
            log.setM(m);
            Log.i(TAG, "onServiceConnected");
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.i(TAG, "onServiceConnected");
            log = null;
        }
    };
 }
