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

public class initiateCoffeBreak {
	
	private final CountDownLatch closeLatch;
	String destUri = "ws://echo.websocket.org";
	//@SuppressWarnings("unused")
	private Session session;
	btnDevice owner;
	
	int users=0;
	
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	public initiateCoffeBreak(btnDevice owner) {
		this.closeLatch = new CountDownLatch(1);
		this.owner=owner;
	}
    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }
 
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
    	LOGGER.finer("Connection closed:" + statusCode + " - "+ reason.toString());
        this.session = null;
        this.closeLatch.countDown();
        // Session closes and CB removes our interest in a CB
        owner.getMQTT().SendMsg("NOLED", owner.getPubTopic());
    }
 
    @OnWebSocketConnect
    public void onConnect(Session session) {
    	LOGGER.finer("Got connect: " + session.toString());
        this.session = session;
        //{"tag":"Company","action":"owner.getCB()"}
        JSONObject sendRegistertag = new JSONObject();
        sendRegistertag.put("tag", owner.getCB());
        sendRegistertag.put("action", "#");
        LOGGER.finer("Sending First WS msg");
        sendMsg(sendRegistertag.toString());
        sendRegistertag = new JSONObject();
        sendRegistertag.put("user", owner.getUser());
        sendRegistertag.put("action", "+");
        LOGGER.finer("Sending Second WS msg");
        sendMsg(sendRegistertag.toString());
        
    }
 
    @OnWebSocketMessage
    public void onMessage(String msg) {
    	LOGGER.finer("Got msg: "+ msg);
    	JSONObject obj=new JSONObject(msg);
    	if(obj.has("action")) {
			String action=obj.get("action").toString();
			switch(action) {
				case "+":
					//Users added to Coffebreak (Or yourself, check if it's you. Otherwise turn on +1 led)
					owner.getMQTT().SendMsg("ADDLED", owner.getPubTopic());
					LOGGER.finer("Action + Sendmsg ADDLED");
					break;
				case "-":
					//Users removed from Coffebreak (Or yourself, check if it's you then reset button. Otherwise turn off -1 led)
					owner.getMQTT().SendMsg("REMOVELED", owner.getPubTopic());
					LOGGER.finer("Action - Sendmsg REMOVELED");
					break;
				case "/":
					//Status has been checked. Send response to Button (Lightning leds)
					owner.getMQTT().SendMsg("NOLED", owner.getPubTopic());
					int i=0;
					for(i=0; i<=users;i++) {
						owner.getMQTT().SendMsg("ADDLED", owner.getPubTopic());
					}
					LOGGER.finer("Action / Sent NOLED and " + i + " ADDLEDS");
					break;
				case "!":
					//CoffeBreak Active!
					owner.getMQTT().SendMsg("FLASHLED", owner.getPubTopic());
					LOGGER.finer("Action ! Sendmsg FLASHLED");
					break;
				case "*":
					//Break is Over reset button
					owner.getMQTT().SendMsg("NOLED", owner.getPubTopic());
					LOGGER.finer("Action * Sendmsg NOLED");
					break;
			}
    	}
    }
    
    public void sendMsg(String msg) {
    	try {
            Future<Void> fut;
            LOGGER.finer("Sent msg: "+ msg);
            fut = session.getRemote().sendStringByFuture(msg);
            fut.get(2, TimeUnit.SECONDS);        
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    public void closeSession() {
    	session.close(StatusCode.NORMAL, "");
    }
}