package se.thebutton;


import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import se.thebutton.SqlConnector;

public class InitiateMQTT implements MqttCallback {
	private MqttAsyncClient client;
	private String topic;
	private String publishtopic;
	private MqttConnectOptions options;
	private String Server;
	private String Port;
	private String ClientID;
	private int QOS=2;
	private boolean RETAIN=false;
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private SqlConnector sql=null;
	private IMqttToken conToken=null;
	//private initiateCoffeBreak[] coffeBreaks;
	private Map<String, initiateCoffeBreak> coffeBreaks = new HashMap<String, initiateCoffeBreak>();
	
	public InitiateMQTT(Configuration config) throws Exception  {
		this.topic=config.getProperty("mqttTopic");
		this.publishtopic=config.getProperty("mqttTopic");
		this.Server=config.getProperty("mqttServer");
		this.Port=config.getProperty("mqttPort");
		this.ClientID=config.getProperty("mqttClientid");
		options = new MqttConnectOptions();
		try {
			Properties props = new Properties();
			if( ! config.getProperty("mqttKeystore").isEmpty()) {
		        System.setProperty("javax.net.ssl.trustStore", config.getProperty("mqttKeystore"));
		        System.setProperty("javax.net.ssl.trustStorePassword", config.getProperty("mqttKeystorePW"));
		        //System.setProperty("javax.net.ssl.keyStore", config.getKEYSTORE());
		        //System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
		        client = new MqttAsyncClient("ssl://" + Server + ":" + Port , ClientID);
		        
		        props.setProperty("com.ibm.ssl.protocol", "TLSv1.2");
		        options.setSSLProperties(props);
			} else {
				client = new MqttAsyncClient("tcp://" + Server + ":" + Port , ClientID);
			}
			if(config.getProperty("mqttClean").equalsIgnoreCase("true")) {
				options.setCleanSession(true);
			} else {
				options.setCleanSession(false);
			}
			options.setPassword(config.getProperty("mqttPassword").toCharArray());
			options.setUserName(config.getProperty("mqttUsername"));
			client.setCallback(this);
			connect();
			
		} catch (MqttException e) { 
			e.printStackTrace();
		}
	}
	public void connect(){
		LOGGER.info(" Connect MQTT");
		try {
			conToken = client.connect(options,null,null);
			conToken.waitForCompletion();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect( ) {
		try {
			LOGGER.info(" Disconnect MQTT");
			client.disconnect();
		} catch (MqttException e) { 
			e.printStackTrace();
		}
		
	}
	
	public void SendMsg(String msg) {
	    try {
			client.publish(publishtopic, msg.getBytes(),QOS,RETAIN);
		} catch (MqttException e) {
			e.printStackTrace();
			System.exit(99);
		}
	}
	public void SendMsg(String msg, String intopic) {
		this.publishtopic=intopic;
	    try {
			client.publish(intopic, msg.getBytes(),QOS,RETAIN);
		} catch (MqttException e) {
			e.printStackTrace();
			System.exit(99);
		}
	}
	
	public void setTopic(String intopic) {
		this.topic = intopic;
	}
	public void setPublishTopic(String intopic) {
		this.publishtopic = intopic;
	}
	public void setSubscribe() throws MqttException {
		LOGGER.info("Start subscription " + topic);
		client.subscribe(topic, QOS);
	}
	public void setSubscribe(String topic) throws MqttException {
		this.topic=topic;
		LOGGER.info("Start subscription " + topic);
		client.subscribe(topic, QOS);
	}
	public void messageArrived(String ontopic, MqttMessage msg) throws Exception {
		
		
		//Spin off a thread for each device? how to communicate with that thread when unregistering?
		
		//Spin deviceid as threadid. 
		
		
		// thebutton/cb/<device>/register
		// {"register": "<device>"}
		// thebutton/cb/<device>/set

		LOGGER.info(ontopic + " " + new String (msg.getPayload()));
		String data= new String (msg.getPayload());
		System.out.println(ontopic + " " + new String (msg.getPayload()));
		JSONObject obj;
		obj=new JSONObject(data);
		
		if(ontopic.contains("/cb/")) {
			// IN THE FUTURE THIS NEEDS TO BE MOVED TO A SEPERATE CLASS TO MODULIRIZE THEBUTTON
			LOGGER.finest("CB Device register Parsing");
			String device=null;
			if(obj.has("REGISTER")) {
				device=obj.get("REGISTER").toString();
			} else if(obj.has("UNREGISTER")) {
				device=obj.get("UNREGISTER").toString();
			}
			//Check if there is a instantiated class of Device already.
			if(coffeBreaks.get(device) != null) {
				LOGGER.info("Device Already Active " + device);
				if(obj.has("UNREGISTER")) {
					if(coffeBreaks.get(device).active()) {
						LOGGER.info("WS still active for device: " + "");
						coffeBreaks.get(device).unregister();
						coffeBreaks.remove(device);
					} else {
						LOGGER.info("WS not active for device: " + "");
					}
				} else {
					if(coffeBreaks.get(device).active()) {
						LOGGER.info("WS still active for device: " + "");
						//Send a + to the WS
						coffeBreaks.get(device).sendRegisterWS();
					} else {
						LOGGER.info("WS not active for device: " + "");
						//Initate new class
						String destUri = "ws://coffeebreak.ws:1880";
						WebSocketClient client = new WebSocketClient();
				        initiateCoffeBreak CB = new initiateCoffeBreak(coffeBreaks.get(device).owner);
				        try {
				            client.start();
				            URI echoUri = new URI(destUri);
				            ClientUpgradeRequest request = new ClientUpgradeRequest();
				            client.connect(CB, echoUri, request);
				            LOGGER.info("Connecting to: " + echoUri.toString());
				            CB.awaitClose(5, TimeUnit.SECONDS);
				            coffeBreaks.put(device,CB);
				        } catch (Throwable t) {
				            t.printStackTrace();
				        }
					}
				}
			} else {
				//Check device owner
				if( sql != null && obj != null && device !=null ) {
					LOGGER.info("Checking Device owner in SQL to " + device);
					btnDevice owner=sql.checkOwner(device);
					if(owner != null) {
						LOGGER.info("Device owner: " + owner.User);
						owner.setMQTT(this);
						//Open WS connection
						String destUri = "ws://coffeebreak.ws:1880";
						WebSocketClient client = new WebSocketClient();
				        initiateCoffeBreak CB = new initiateCoffeBreak(owner);
				        try {
				            client.start();
				            URI echoUri = new URI(destUri);
				            ClientUpgradeRequest request = new ClientUpgradeRequest();
				            client.connect(CB, echoUri, request);
				            LOGGER.info("Connecting to: " + echoUri.toString());
				            CB.awaitClose(5, TimeUnit.SECONDS);
				            coffeBreaks.put(device,CB);
				        } catch (Throwable t) {
				            t.printStackTrace();
				        }
					} else {
						LOGGER.info("No owner found");
					}
				
				}
			}
		} else {
			LOGGER.info("Unknown Topic " + ontopic);
			obj = null;
		}
	}

	public void setSql(SqlConnector insql) {
		sql = insql;
	}

	public void connectionLost(Throwable arg0) {
		try {
			Thread.sleep(10000);
			connect();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}

}
