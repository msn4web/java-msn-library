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

import net.sf.jml.*;
import net.sf.jml.message.*;
import net.sf.jml.message.p2p.MsnP2PMessage;

/**
 * Implements all listeners which jml provided.
 * 
 * @author Roger Chen
 * @author Angel Barragán Chacón
 * @author Damian Minkov
 */
public class MsnAdapter 
implements MsnContactListListener, 
           MsnMessageListener,
           MsnMessengerListener, 
           MsnSwitchboardListener, 
           MsnFileTransferListener,
           MsnEmailListener {

	////////////////////////////////////////////////////////////////////////////
	//                                                                        //
	//                 MsnContactListListener Implementation                  //
	//                                                                        //
	////////////////////////////////////////////////////////////////////////////

    /**
     * @see MsnContactListListener#contactListInitCompleted(MsnMessenger)
     */
    public void contactListInitCompleted(MsnMessenger messenger) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnContactListListener#contactListSyncCompleted(MsnMessenger)
     */
    public void contactListSyncCompleted(MsnMessenger messenger) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnContactListListener#contactStatusChanged(MsnMessenger, MsnContact)
     */
    public void contactStatusChanged(MsnMessenger messenger, 
    		                         MsnContact contact) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnContactListListener#ownerStatusChanged(MsnMessenger)
     */
    public void ownerStatusChanged(MsnMessenger messenger) {
    	// Empty implementation, intended to be overridden. 
    }

    public void ownerDisplayNameChanged(MsnMessenger messenger) {
    	// Empty implementation, intended to be overridden.
    }
    
    /**
     * @see MsnContactListListener#contactPersonalMessageChanged(MsnMessenger, MsnContact)
     * James Lopez - BLuEGoD
     */
    public void contactPersonalMessageChanged(MsnMessenger messenger,
            MsnContact contact) {
     // Empty implementation, intended to be overridden. 
    }
    /**
     * @see MsnContactListListener#contactAddedMe(MsnMessenger, MsnContact)
     */
    public void contactAddedMe(MsnMessenger messenger, MsnContact contact) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnContactListListener#contactAddedMe(MsnMessenger, MsnContactPending[])
     */
    public void contactAddedMe(MsnMessenger messenger, MsnContactPending[] pending) {
        // Empty implementation, intended to be overridden. 
    }
    
    /**
     * @see MsnContactListListener#contactRemovedMe(MsnMessenger, MsnContact)
     */
    public void contactRemovedMe(MsnMessenger messenger, MsnContact contact) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnContactListListener#contactAddCompleted(net.sf.jml.MsnMessenger, net.sf.jml.MsnContact, net.sf.jml.MsnList) 
     */
    public void contactAddCompleted(MsnMessenger messenger, 
    		                        MsnContact contact, MsnList list) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnContactListListener#contactRemoveCompleted(MsnMessenger, MsnContact, MsnList)
     */
    public void contactRemoveCompleted(MsnMessenger messenger, 
    		                           MsnContact contact, MsnList list) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnContactListListener#contactAddInGroupCompleted(MsnMessenger, MsnContact, MsnGroup)
     */
    public void contactAddInGroupCompleted(MsnMessenger messenger, MsnContact contact, MsnGroup group)
    {
        // Empty implementation, intended to be overridden. 
    }
    
    /**
     * @see MsnContactListListener#contactRemoveFromGroupCompleted(MsnMessenger, MsnContact, MsnGroup)
     */
    public void contactRemoveFromGroupCompleted(MsnMessenger messenger, MsnContact contact, MsnGroup group)
    {
        // Empty implementation, intended to be overridden. 
    }
    
    /**
     * @see MsnContactListListener#groupAddCompleted(MsnMessenger, MsnGroup)
     */
    public void groupAddCompleted(MsnMessenger messenger, MsnGroup group) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnContactListListener#groupRemoveCompleted(MsnMessenger, MsnGroup)
     */
    public void groupRemoveCompleted(MsnMessenger messenger, MsnGroup group) {
    	// Empty implementation, intended to be overridden. 
    }

    
	////////////////////////////////////////////////////////////////////////////
	//                                                                        //
	//                   MsnMessageListener Implementation                    //
	//                                                                        //
	////////////////////////////////////////////////////////////////////////////

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
	
    
	////////////////////////////////////////////////////////////////////////////
	//                                                                        //
	//                  MsnMessengerListener Implementation                   //
	//                                                                        //
	////////////////////////////////////////////////////////////////////////////

	/**
	 * @see MsnMessengerListener#exceptionCaught(MsnMessenger, Throwable)
	 */
    public void exceptionCaught(MsnMessenger messenger, Throwable throwable) {
    	// Empty implementation, intended to be overridden. 
    }

	/**
	 * @see MsnMessengerListener#loginCompleted(MsnMessenger)
	 */
    public void loginCompleted(MsnMessenger messenger) {
    	// Empty implementation, intended to be overridden. 
    }

	/**
	 * @see MsnMessengerListener#logout(MsnMessenger)
	 */
    public void logout(MsnMessenger messenger) {
    	// Empty implementation, intended to be overridden. 
    }
    
    
	////////////////////////////////////////////////////////////////////////////
	//                                                                        //
	//                 MsnSwitchboardListener Implementation                  //
	//                                                                        //
	////////////////////////////////////////////////////////////////////////////

    /**
     * @see MsnSwitchboardListener#switchboardClosed(MsnSwitchboard)
     */
    public void switchboardClosed(MsnSwitchboard switchboard) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnSwitchboardListener#switchboardStarted(MsnSwitchboard)
     */
    public void switchboardStarted(MsnSwitchboard switchboard) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnSwitchboardListener#contactJoinSwitchboard(MsnSwitchboard, MsnContact)
     */
    public void contactJoinSwitchboard(MsnSwitchboard switchboard,
                                       MsnContact contact) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnSwitchboardListener#contactLeaveSwitchboard(MsnSwitchboard, MsnContact)
     */
    public void contactLeaveSwitchboard(MsnSwitchboard switchboard,
                                        MsnContact contact) {
    	// Empty implementation, intended to be overridden. 
    }
    
    
	////////////////////////////////////////////////////////////////////////////
	//                                                                        //
	//                MsnFileTransferListener Implementation                  //
	//                                                                        //
	////////////////////////////////////////////////////////////////////////////

    /**
     * @see MsnFileTransferListener#fileTransferRequestReceived(MsnFileTransfer)
     */
    public void fileTransferRequestReceived(MsnFileTransfer transfer) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnFileTransferListener#fileTransferStarted(MsnFileTransfer)
     */
    public void fileTransferStarted(MsnFileTransfer transfer) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnFileTransferListener#fileTransferProcess(MsnFileTransfer)
     */
    public void fileTransferProcess(MsnFileTransfer transfer) {
    	// Empty implementation, intended to be overridden. 
    }

    /**
     * @see MsnFileTransferListener#fileTransferFinished(MsnFileTransfer)
     */
    public void fileTransferFinished(MsnFileTransfer transfer) {
    	// Empty implementation, intended to be overridden. 
    }


    ////////////////////////////////////////////////////////////////////////////
    //                                                                        //
    //                MsnEmailListener Implementation                         //
    //                                                                        //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * @see MsnEmailListener#initialEmailNotificationReceived(net.sf.jml.MsnSwitchboard, net.sf.jml.message.MsnEmailInitMessage, net.sf.jml.MsnContact)
     */
    public void initialEmailNotificationReceived(MsnSwitchboard switchboard, MsnEmailInitMessage message, MsnContact contact) {
        // Empty implementation, intended to be overridden.
    }

    /**
     * @see MsnEmailListener#initialEmailDataReceived(net.sf.jml.MsnSwitchboard, net.sf.jml.message.MsnEmailInitEmailData, net.sf.jml.MsnContact)
     */
    public void initialEmailDataReceived(MsnSwitchboard switchboard, MsnEmailInitEmailData message, MsnContact contact) {
        // Empty implementation, intended to be overridden.
    }

    /**
     * @see MsnEmailListener#newEmailNotificationReceived(net.sf.jml.MsnSwitchboard, net.sf.jml.message.MsnEmailNotifyMessage, net.sf.jml.MsnContact)
     */
    public void newEmailNotificationReceived(MsnSwitchboard switchboard, MsnEmailNotifyMessage message, MsnContact contact) {
        // Empty implementation, intended to be overridden.
    }

    /**
     * @see MsnEmailListener#activityEmailNotificationReceived(net.sf.jml.MsnSwitchboard, net.sf.jml.message.MsnEmailActivityMessage, net.sf.jml.MsnContact)
     */
    public void activityEmailNotificationReceived(MsnSwitchboard switchboard, MsnEmailActivityMessage message, MsnContact contact) {
        // Empty implementation, intended to be overridden.
    }

}