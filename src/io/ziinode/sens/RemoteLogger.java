package io.ziinode.sens;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

public class RemoteLogger extends Service implements ZnConnectorInf, Runnable{


    private static final ExecutorService pool = Executors.newSingleThreadExecutor();
    public static RemoteLogger instance;

    public final long MEGS=1048576L;
    String pin="1111";
    private ScheduledFuture future;
    public static final String TYPE = "10000r0";


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    boolean stated = false;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("RemoteLogger", "ZnServ-on start");
        if(!stated) {
            stated = true;
        }
        return START_STICKY;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public RemoteLogger getService() {
            // Return this instance of LocalService so clients can call public methods
            return instance;
        }
    }

    public void setM(main m){
        this.m=m;
    }
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    main m;
    public RemoteLogger() {
        this.conn=new ZnConnector(this,TYPE,(short)0);
    }

    public void stop(){
        if (future != null) {
            future.cancel(false);
        }
        conn.stop();
    }
    public void start(){
        conn.start();
        future = conn.getScheduler().scheduleAtFixedRate(this, 0, 10, TimeUnit.SECONDS);
    }

    final ZnConnector conn;
    private AtomicInteger inCnt = new AtomicInteger(0);
    private AtomicInteger outCnt = new AtomicInteger(0);
    public ZnConnector getConn(){
        return this.conn;
    }
    public static RemoteLogger getInstance() {
        return instance;
    }
    public void inMsg(){
        inCnt.incrementAndGet();
    }
    public void outMsg(){
        outCnt.incrementAndGet();
    }

    @Override
    public void run() {
        //android.util.Log.i("LOG","run:"+conn.client().isConnected());

        try {
            if(conn.client().isConnected()){
                Runtime info = Runtime.getRuntime();
                double freeSize = info.freeMemory()/MEGS;
                double totalSize = info.totalMemory()/MEGS;
                double usedSize = totalSize - freeSize;
                int msgIn = inCnt.getAndSet(0);
                int msgOut = outCnt.getAndSet(0);
                //zise 3x8 + 2x4 + 5x2 = 42
                ByteBuffer buf = ByteBuffer.allocate(42);
                buf.putShort((short)0);
                buf.putDouble(freeSize);
                buf.putShort((short)1);
                buf.putDouble(totalSize);
                buf.putShort((short)2);
                buf.putDouble(usedSize);
                buf.putShort((short)3);
                buf.putInt(msgIn);
                buf.putShort((short)4);
                buf.putInt(msgOut);
                conn.client().publish(conn.getTopicBase()+ZnConnector.TRAP, buf.array(), 0, true);
            }
        } catch (MqttPersistenceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MqttException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private void setStatus(final String st){
        android.util.Log.i("Loger","Sst:"+st);
        m.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m.setStatus(st);
            }
        });
    }

    @Override
    public void onStatus(int status) {
        if(status==ZnConnector.STATE_DISCONNECED){
            setStatus("Disconnected");
            if (future != null) {
                future.cancel(false);
            }
        }else if(status==ZnConnector.STATE_GET_DEV_ID){
            setStatus("Retrieving DeviceId...");
        }else if(status==ZnConnector.STATE_GET_SERVER){
            setStatus("Got deviceId, retrieving server:" + TYPE + " DsId:" + m.getDsid());
            if (future != null) {
                future.cancel(false);
            }
        }else if(status==ZnConnector.STATE_CONNECTING){
            setStatus("Got server address, connecting...");
        }else if(status==ZnConnector.STATE_ONLINE){
            setStatus("Connected, sending data.");
            future = conn.getScheduler().scheduleAtFixedRate(this, 0, 10, TimeUnit.SECONDS);
        }

    }

    @Override
    public void onDeviceId(String devId) {
        m.setDevId(devId);

    }

    @Override
    public String getDsid() {
        return m.getDsid();
    }

    @Override
    public String getPin() {
        // TODO Auto-generated method stub
        return pin;
    }

    @Override
    public void onMessage(String topic, byte[] msg) {
        // TODO Auto-generated method stub

    }

    /**
     * Used to print messages to Logcat
     *
     * @param msg A string with the message you want to print
     * @param level A string with the level of the message you want to print. This
     * string must match one of the following: <br>
     * v = verbose <br>
     * d = debug <br>
     * i = info <br>
     * w = warning <br>
     * e = error
     */
    public static void log(final String level,final String TAG, final String msg){
        if (msg==null){
            return;
        }
        if(instance!=null && instance.getConn()!=null){
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    instance.getConn().log(System.currentTimeMillis(),0, TAG+" "+ msg);
                }
            });
        }
        if (level.equals("v")) {
            android.util.Log.v(TAG, msg);
        }
        else if (level.equals("d")){
            android.util.Log.d(TAG, msg);
        }
        else if (level.equals("i")) {
            android.util.Log.i(TAG, msg);
        }
        else if (level.equals("w")) {
            android.util.Log.w(TAG, msg);
        }
        else if (level.equals("e")) {
            android.util.Log.e(TAG, msg);
        }

    }
}
