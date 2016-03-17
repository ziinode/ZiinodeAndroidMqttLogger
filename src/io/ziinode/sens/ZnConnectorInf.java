package io.ziinode.sens;

public interface ZnConnectorInf {
    public void onStatus(int status);
    public void onDeviceId(String devId);
    public String getDsid();
    public String getPin();
    public void onMessage(String topic, byte[] msg);
}
