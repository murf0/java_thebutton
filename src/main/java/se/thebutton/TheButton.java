package se.thebutton;

import java.util.logging.Level;
import java.util.logging.Logger;


/*
 * Thebutton.se
 * Register your button with your id
 * Button sends mqtt to connect.
 * Java keeps track of who is signed on and publishes status messages to button
 * 
 * Subscribe to topic thebutton/+/register where devices connects and sends that they want cb 
 * this dispatches a thread checking who owns the button, and opens the WS and subscribes 
 * to the topic with thebutton/<devicename>/cancelled if a CB is canceled and publishes status-updates to iot/<button>/update
 * So that number of leds and such is updated accordingly
 * 
 * 
 */
public class TheButton {
		private static Configuration config;
		private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		static volatile boolean keepRunning = true;
		
		public static void main( String[] args ) throws Exception {
			MyLogger.setup();
			final Thread mainThread = Thread.currentThread();
			Runtime.getRuntime().addShutdownHook(new Thread() {
			    @Override
				public void run() {
			        keepRunning = false;
			        try {
						mainThread.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    }
			});
			/**
			 * Parse Configuration
			 */
			LOGGER.setLevel(Level.FINEST);
			LOGGER.info("Loading Configruation Input");
			config = new Configuration("server.config");
			LOGGER.info("Setting Loglevel to: " + config.getProperty("logLevel"));
			if(config.getProperty("logLevel") != "FINEST") {
				LOGGER.setLevel(Level.INFO);
			}
			LOGGER.info("Connect to MySQL");
			SqlConnector sql = new SqlConnector(config);
			LOGGER.info("Connect to MQTT");
			InitiateMQTT receiver = new InitiateMQTT(config);
			receiver.setSql(sql);
			receiver.setSubscribe();
			while(keepRunning) {
				Thread.sleep(1000);
			}
			/**
			 * Disconnect MQTT
			 */
			LOGGER.info("Disconnect MQTT");
			receiver.disconnect();
			sql.disconnect();
	    }
}
