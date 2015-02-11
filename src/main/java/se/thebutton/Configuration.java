package se.thebutton;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Configuration file reader. 
 * @author Mikael Murf Mellgren
 * @version 1.0.0
 * @email mikael@murf.se
 * @license MIT
 * @web murf.se
 */

public class Configuration {
	Properties prop = new Properties();
	InputStream input = null;
	
	public Configuration(String File) {
		try {
			 
			input = new FileInputStream(File);
	 
			// load a properties file
			prop.load(input);
	 
			Enumeration<?> e = prop.propertyNames();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String value = prop.getProperty(key);
				System.err.println("Key : " + key + ", Value : " + value);
			}
				
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	public String getProperty(String string) {
		if (prop.containsKey(string)) {
			return prop.getProperty(string).trim();
		} else {
			throw new RuntimeException("No " + string + " value in ConfigurationFile");
		}
			
	}
}
