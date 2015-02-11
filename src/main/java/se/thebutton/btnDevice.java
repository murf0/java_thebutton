package se.thebutton;

import java.util.logging.Logger;

public class btnDevice {
	String User=null,DeviceID=null,CB=null,PubTopic=null;
	InitiateMQTT MQTT=null;
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	public btnDevice() {
		
	}
	public void setUser(String string) {
		User=string;
	}
	public void setDeviceID(String string) {
		DeviceID=string;
		PubTopic="thebutton/cb/"+this.getDeviceID()+"/set";
		LOGGER.finer("Setting publish topic" + PubTopic);
	}
	public void setCB(String string) {
		CB=string;
	}
	public String getUser() {
		return User;
	}
	public String getDeviceID() {
		return DeviceID;
	}
	public String getCB() {
		return CB;
	}
	public void setPubTopic(String string) {
		PubTopic=string;
	}
	public String getPubTopic() {
		return PubTopic;
	}
	public void setMQTT(InitiateMQTT MQTT) {
		this.MQTT=MQTT;
	}
	public InitiateMQTT getMQTT() {
		return this.MQTT;
	}
}
