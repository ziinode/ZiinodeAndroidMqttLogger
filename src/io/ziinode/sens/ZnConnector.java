package io.ziinode.sens;

import java.io.InputStream;
import java.net.HttpURLConnection;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ZnConnector implements MqttCallback, Runnable{
    private static final String TAG = "znconn";// "znconn";

    public static byte TRAP = 7; // Client sends metrics data sent to server
    public static byte EVENT = 8; // Client sends EVENT data sent to
    public static byte LOG = 9; // Client sends log data sent to server

    public static final int STATE_DISCONNECED = 0;
    public static final int STATE_GET_DEV_ID = 1;
    public static final int STATE_GET_SERVER = 2;
    public static final int STATE_CONNECTING = 3;
    public static final int STATE_ONLINE = 4;

    private volatile int state = STATE_DISCONNECED;

    MqttAsyncClient client;

    ZnConnectorInf m;
    private String type;
    private short version;

    public ZnConnector(ZnConnectorInf m, String type, short version) {
        this.m = m;
        this.type = type;
        this.version = version;
    }

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture future;
    public ScheduledExecutorService getScheduler(){
        return scheduler;
    }

    public void stop() {
        setState(STATE_DISCONNECED);
        if (future != null) {
            future.cancel(false);
        }
        try {
            if (client != null) {
                client.close();
                client = null;
            }
        } catch (Exception e) {
            android.util.Log.i(TAG,"stop:clse socket",e);
        }
        android.util.Log.i(TAG,"disconn");
    }

    public void start() {
        android.util.Log.i(TAG,"start "+m.getDsid() +" "+type);
        lastTime = 0;
        if (state == STATE_DISCONNECED) {
            if (m.getDsid().equals(type)) {
                setState(STATE_GET_DEV_ID);
            } else {
                setState(STATE_GET_SERVER);
            }
            future = scheduler.scheduleAtFixedRate(this, 0, 500, TimeUnit.MILLISECONDS);
        }
    }

    public void setState(int state) {
        android.util.Log.i(TAG,"setState "+state);
        if (this.state == STATE_ONLINE && state != STATE_ONLINE) {
            try {
                if (client != null) {
                    client.disconnect();
                    // in=null;
                }
            } catch (Exception e) {
                android.util.Log.i(TAG,"stop:clse socket");
            }

        }
        this.state = state;
        m.onStatus(state);
    }


    public int getState() {
        return state;
    }

    public void log(long time, int category, String log){
        if(client()!=null && client().isConnected()){
            //android.util.Log.i(TAG,"writeLog:"+log);

            try {
                byte[] bb = log.getBytes();
                ByteBuffer buf = ByteBuffer.allocate(bb.length+4+8);
                buf.putLong(time);
                buf.putShort((short)category);
                buf.putShort((short)bb.length);
                buf.put(bb);
                client().publish(getTopicBase()+ZnConnector.LOG, buf.array(), 0, true);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                android.util.Log.i(TAG,"writeLog",e);
            }
        }
    }

    public void event(long time, int category, String log){
        if(client()!=null && client().isConnected()){
            //android.util.Log.i(TAG,"writeLog:"+log);

            try {
                byte[] bb = log.getBytes();
                ByteBuffer buf = ByteBuffer.allocate(bb.length+4+8);
                buf.putLong(time);
                buf.putShort((short)category);
                buf.putShort((short)bb.length);
                buf.put(bb);
                client().publish(getTopicBase()+ZnConnector.EVENT, buf.array(), 0, true);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                android.util.Log.i(TAG,"writeLog",e);
            }
        }
    }

    String topic;
    private void conn(String server, String key, String pin) {
        try {
            String fullDeviceId = type + key;
            String sddr = "tcp://"+server+":1883";
            android.util.Log.i(TAG,"target::"+sddr);
            if(client==null){
                client = new MqttAsyncClient(sddr, fullDeviceId, new MemoryPersistence(),new TimerPingSender());
            }
            android.util.Log.i(TAG,"init options...");
            final MqttConnectOptions connOpt = new MqttConnectOptions();
            connOpt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            connOpt.setCleanSession(true);
            connOpt.setKeepAliveInterval(30);
            connOpt.setUserName(fullDeviceId);
            connOpt.setPassword(("1111 "+version).toCharArray());
            topic = "ds/" + fullDeviceId + "/out/";
            // Connect to Broker
            client.setCallback(this);
            client.connect(connOpt);
            android.util.Log.i(TAG,"connecting...");
            final long start = System.currentTimeMillis();
            while (!client.isConnected()
                    && System.currentTimeMillis() - start < 5000) {
                Thread.sleep(100);
            }
            setState(STATE_ONLINE);
            android.util.Log.i(TAG,"connn OK");
        } catch (Exception ee) {
            android.util.Log.e(TAG,"connEx",ee);
            setState(STATE_GET_SERVER);
        }
    }

    public String getTopicBase(){
        return topic;
    }

    @Override
    public void connectionLost(Throwable throwable) {
        android.util.Log.i(TAG,"Connection lost!");
        //System.exit(0);
        setState(STATE_GET_SERVER);
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        android.util.Log.i(TAG,"Arrived | Topic:" + topic);
        m.onMessage(topic, mqttMessage.getPayload());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
//        try {
//            System.out.println("Pub complete" + new String(token.getMessage().getPayload()));
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
    }

    public MqttAsyncClient client(){
        return client;
    }

    public static final String URL = "http://ziinode.io/api/v1/node/host/";
    long lastTime = 0;
    @Override
    public void run() {
        try {
            if (connected()) {
            }
        } catch (Exception e) {
            android.util.Log.i(TAG,"runEx",e);
            setState(STATE_GET_SERVER);
        }
    }

    protected boolean connected() {
        if (state == STATE_ONLINE || state == STATE_CONNECTING) {
            return true;
        } else if (state == STATE_DISCONNECED) {
            return false;
        } else {
            if (state == STATE_GET_SERVER) {
                if ((System.currentTimeMillis() - lastTime) < 5000) {
                    return false;
                } else {
                    lastTime = System.currentTimeMillis();
                }
            }
        }
        String adr = null;
        String ttt = URL + type + m.getDsid() + m.getPin();
        try {
            // Execute the method.
            android.util.Log.i(TAG,"Open:" + ttt);
            URL urla = new URL(ttt);
            HttpURLConnection conn = (HttpURLConnection) urla.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            // conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() == 200) {
                Object response = conn.getContent();
                android.util.Log.i(TAG,"response:200");
                InputStream dataIs = (InputStream) response;
                byte[] data = new byte[60];
                int received = dataIs.read(data);
                if (received > 0) {
                    final String str = new String(Arrays.copyOf(data, received));
                    android.util.Log.i(TAG,"resp:" + received + " str " + str);
                    if (data[0] == data[1] && data[1] == data[2]
                            && data[2] == data[3] && data[3] == 0) {
                        android.util.Log.i(TAG,"sleep 0.0.0.0");
                        setState(STATE_GET_SERVER);
                    } else if (state == STATE_GET_DEV_ID) {
                        android.util.Log.i(TAG,"devid:" + adr);
                        m.onDeviceId(str);
                        setState(STATE_GET_SERVER);
                        return false;
                    }
                    if (str.lastIndexOf(":") > 6) {
                        adr = str.substring(0, str.lastIndexOf(":"));
                    } else {
                        adr = str;
                    }
                    setState(STATE_CONNECTING);
                    conn.disconnect();
                    //android.util.Log.i(TAG,"Server addr:" + adr);
                    conn(adr, m.getDsid(), m.getPin());
                } else {
                    android.util.Log.i(TAG,"faled to open URL:" + ttt);
                }
            }
        } catch (Exception ee) {
            android.util.Log.i(TAG,"faled to open URL" + ttt);
        }
        return false;
    }
}
