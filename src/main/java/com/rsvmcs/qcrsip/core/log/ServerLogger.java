
package com.rsvmcs.qcrsip.core.log;


import com.rsvmcs.qcrsip.core.SipStack;
import com.rsvmcs.qcrsip.core.model.SIPMessage;

import java.util.Properties;

/**
 * @author jean.deruelle@gmail.com
 *
 */
public interface ServerLogger extends LogLevels {
	
   
	 void closeLogFile();
	 
	 void logMessage(SIPMessage message, String from, String to, boolean sender, long time);
	 
	 void logMessage(SIPMessage message, String from, String to, String status,
                     boolean sender, long time);
	 
	 void logMessage(SIPMessage message, String from, String to, String status,
                     boolean sender);
	            	
	 void logException(Exception ex);
	 
	 public void setStackProperties(Properties stackProperties);
	 
	 public void setSipStack(SipStack sipStack);
	 
	
}
