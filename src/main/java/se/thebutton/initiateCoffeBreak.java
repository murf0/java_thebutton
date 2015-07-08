package se.thebutton;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONObject;

@WebSocket(maxTextMessageSize = 64 * 1024)

public class initiateCoffeBreak implements Runnable {
	
	private final CountDownLatch closeLatch;
	//@SuppressWarnings("unused")
	private Session session;
	btnDevice owner;
	
	int users=0;
	
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	public boolean active() {
		return (this.session != null);
	}
	public initiateCoffeBreak(btnDevice owner) {
		this.closeLatch = new CountDownLatch(1);
		this.owner=owner;
	}
    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }
 
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
    	LOGGER.info("Connection closed:" + statusCode + " - "+ reason.toString());
        this.session = null;
        this.closeLatch.countDown();
        // Session closes and CB removes our interest in a CB
        owner.getMQTT().SendMsg("{\"DO\":\"NOLED\"}", owner.getPubTopic());
        
    }
 
    @OnWebSocketConnect
    public void onConnect(Session session) {
    	LOGGER.info("Got connect: " + session.toString());
        this.session = session;
        //{"tag":"Company","action":"owner.getCB()"}
        sendRegisterWS();
    }
    public void sendRegisterWS() {
        JSONObject sendRegistertag = new JSONObject();
        sendRegistertag.put("tag", owner.getCB());
        sendRegistertag.put("action", "#");
        LOGGER.info("Sending First WS msg");
        sendMsg(sendRegistertag.toString());
        sendRegistertag = new JSONObject();
        sendRegistertag.put("user", owner.getUser());
        sendRegistertag.put("action", "+");
        LOGGER.info("Sending Second WS msg");
        sendMsg(sendRegistertag.toString());
    }
 
    @OnWebSocketMessage
    public void onMessage(String msg) {
    	LOGGER.info("Got msg: "+ msg);
    	JSONObject obj=new JSONObject(msg);
    	if(obj.has("action")) {
			String action=obj.get("action").toString();
			switch(action) {
				case "+":
					//Users added to Coffebreak (Or yourself, check if it's you. Otherwise turn on +1 led)
					owner.getMQTT().SendMsg("{\"DO\":\"ADDLED\"}", owner.getPubTopic());
					LOGGER.info("Action + Sendmsg {\"DO\":\"ADDLED\"}");
					break;
				case "-":
					//Users removed from Coffebreak (Or yourself, check if it's you then reset button. Otherwise turn off -1 led)
					owner.getMQTT().SendMsg("{\"DO\":\"REMOVELED\"}", owner.getPubTopic());
					LOGGER.info("Action - Sendmsg REMOVELED");
					break;
				case "/":
					//Status has been checked. Send response to Button (Lightning leds)
					owner.getMQTT().SendMsg("{\"DO\":\"NOLED\"}", owner.getPubTopic());
					int i=0;
					for(i=0; i<=users;i++) {
						owner.getMQTT().SendMsg("{\"DO\":\"ADDLED\"}", owner.getPubTopic());
					}
					LOGGER.info("Action / Sent {\"DO\":\"NOLED\"} and " + i + " {\"DO\":\"ADDLED\"}");
					break;
				case "!":
					//CoffeBreak Active!
					owner.getMQTT().SendMsg("{\"DO\":\"FLASHLED\"}", owner.getPubTopic());
					LOGGER.info("Action ! Sendmsg FLASHLED");
					break;
				case "*":
					//Break is Over reset button
					owner.getMQTT().SendMsg("{\"DO\":\"NOLED\"}", owner.getPubTopic());
					LOGGER.info("Action * Sendmsg {\"DO\":\"NOLED\"}");
					break;
			}
    	}
    }
    public void unregister() {
    	//Send unregister
    	//{"user":"Name","action":"-"}
    	JSONObject sendUnregistertag = new JSONObject();
    	sendUnregistertag.put("user", owner.getUser());
    	sendUnregistertag.put("action", "-");
        LOGGER.info("Sending Unregistertag WS msg");
        sendMsg(sendUnregistertag.toString());
    }
    public void sendMsg(String msg) {
    	try {
            Future<Void> fut;
            LOGGER.info("Sent msg: "+ msg);
            fut = session.getRemote().sendStringByFuture(msg);
            fut.get(2, TimeUnit.SECONDS);        
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    public void closeSession() {
    	session.close(StatusCode.NORMAL, "");
    }
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
