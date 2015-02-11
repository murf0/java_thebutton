package se.thebutton;


import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class SqlConnector {
	private Connection con = null;
    private ResultSet rs = null;
    private PreparedStatement pst_check = null;
    
    private ComboPooledDataSource cpds;
    
    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private String url;
    private String user;
    private String password;

	public  SqlConnector(Configuration config) throws IOException, SQLException, PropertyVetoException {
		cpds = new ComboPooledDataSource();
        
        // the settings below are optional -- c3p0 can work with defaults
        cpds.setMinPoolSize(5);
        cpds.setAcquireIncrement(5);
        cpds.setMaxPoolSize(20);
        cpds.setMaxStatements(180);
        
		url = config.getProperty("sqlUrl");
		user = config.getProperty("sqlUser");
		password = config.getProperty("sqlPassword");
		try {
			cpds.setDriverClass("com.mysql.jdbc.Driver"); //loads the jdbc driver
	        cpds.setJdbcUrl(url);
	        cpds.setUser(user);
	        cpds.setPassword(password);
	        con = cpds.getConnection();
	        //pst = con.prepareStatement("INSERT INTO raw(timestamp,device,user,topic,latitude,longitude,speed,altitude) VALUES(?,?,?,?,?,?,?,?)");
	        pst_check = con.prepareStatement("SELECT * FROM btnDevice WHERE deviceid=? LIMIT 1");
	} catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
	}

	/*
		create table btnDevice (
			id INT AUTO_INCREMENT,
			deviceid VARCHAR(20),
			user VARCHAR(20),
			CB VARCHAR(20),
			PRIMARY KEY(id)
		);
		insert into btnDevice (deviceid,user,CB) values("test","murf","testcompany");
	 */

	public btnDevice checkOwner(String DeviceID) {
			try {
				LOGGER.finer("Get Device Owner from ID: " + DeviceID);
				pst_check.setString(1, DeviceID); //Device
				ResultSet rs = pst_check.executeQuery();
				if (rs.next()) {
					btnDevice Result=new btnDevice();
					Result.setUser(rs.getString("user"));
					Result.setDeviceID(rs.getString("deviceid"));
					Result.setCB(rs.getString("CB"));
					return Result;
				} else {
					return null;
				}
			} catch (SQLException ex) {
	            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
			} catch (Exception e) {
			e.printStackTrace();
		}
			return null;
	}

	public void disconnect() {
		try {
            if (rs != null) {
                rs.close();
            }
            if (pst_check != null) {
            	pst_check.close();
            }
            if (con != null) {
                con.close();
            }

        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
	}
	
    public Connection getConnection() throws SQLException {
        return this.cpds.getConnection();
    }

}
