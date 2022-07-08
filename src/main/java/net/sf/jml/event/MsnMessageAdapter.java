/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.jml.event;

import java.util.Date;

import net.sf.jml.MsnContact;
import net.sf.jml.MsnMessenger;
import net.sf.jml.MsnSwitchboard;
import net.sf.jml.message.*;
import net.sf.jml.message.p2p.MsnP2PMessage;

/**
 * MsnMessageListener adapter.
 * 
 * @author Roger Chen
 * @author Angel Barragán Chacón
 */
public class MsnMessageAdapter implements MsnMessageListener {

	/**
	 * @see MsnMessageListener#datacastMessageReceived(MsnSwitchboard, MsnDatacastMessage, MsnContact)
	 */
    public void datacastMessageReceived(MsnSwitchboard switchboard,
                                        MsnDatacastMessage message, 
                                        MsnContact contact) {
    	// Empty implementation, intended to be overridden.
    }

	/**
	 * @see MsnMessageListener#instantMessageReceived(MsnSwitchboard, MsnInstantMessage, MsnContact)
	 */
    public void instantMessageReceived(MsnSwitchboard switchboard,
                                       MsnInstantMessage message, 
                                       MsnContact contact) {
    	// Empty implementation, intended to be overridden.
    }
    
    /**
	 * @see MsnMessageListener#offlineMessageReceived(String,String,String,Date,MsnContact)
	 */
    public void offlineMessageReceived(String body,
                                       String contentType, 
                                       String encoding,
                                       Date date,
                                       MsnContact contact)
    {
        // Empty implementation, intended to be overridden.
    }

	/**
	 * @see MsnMessageListener#systemMessageReceived(MsnMessenger, MsnSystemMessage)
	 */
    public void systemMessageReceived(MsnMessenger messenger,
                                      MsnSystemMessage message) {
    	// Empty implementation, intended to be overridden.
    }

	/**
	 * @see MsnMessageListener#controlMessageReceived(MsnSwitchboard, MsnControlMessage, MsnContact)
	 */
    public void controlMessageReceived(MsnSwitchboard switchboard,
                                       MsnControlMessage message, 
                                       MsnContact contact) {
    	// Empty implementation, intended to be overridden.
    }

	/**
	 * @see MsnMessageListener#unknownMessageReceived(MsnSwitchboard, MsnUnknownMessage, MsnContact)
	 */
    public void unknownMessageReceived(MsnSwitchboard switchboard,
                                       MsnUnknownMessage message, 
                                       MsnContact contact) {
    	// Empty implementation, intended to be overridden.
    }

	/**
	 * @see MsnMessageListener#p2pMessageReceived(MsnSwitchboard, MsnP2PMessage, MsnContact)
	 */
	public void p2pMessageReceived(MsnSwitchboard switchboard, 
			                       MsnP2PMessage message, 
			                       MsnContact contact) {
    	// Empty implementation, intended to be overridden.
	}
}