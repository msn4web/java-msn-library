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
package net.sf.jml;

import java.util.ArrayList;

import net.sf.jml.event.*;
import net.sf.jml.exception.JmlException;
import net.sf.jml.message.p2p.DisplayPictureDuelManager;
import net.sf.jml.message.p2p.FileTransferManager;
import net.sf.jml.protocol.MsnOutgoingMessage;

/**
 * Msn Messenger interface.
 * 
 * @author Roger Chen
 */
public interface MsnMessenger {

    /**
     * Get the attachment.
     * 
     * @return
     * 		attachment
     */
    public Object getAttachment();

    /**
     * Set the attachment.
     * 
     * @param attachment
     * 		attachment
     */
    public void setAttachment(Object attachment);

    /**
     * Is log incoming message. For debug purpose.
     * 
     * @return
     * 		is log incoming message
     */
    public boolean isLogIncoming();

    /**
     * Set log incoming message. For debug purpose.
     * 
     * @param logIncoming
     * 		set log incoming message
     */
    public void setLogIncoming(boolean logIncoming);

    /**
     * Is log outgoing message. For debug purpose.
     * 
     * @return
     * 		is log outgoing message
     */
    public boolean isLogOutgoing();

    /**
     * Set log outgoing message. For debug purpose.
     * 
     * @param logOutgoing
     * 		set log outgoing message
     */
    public void setLogOutgoing(boolean logOutgoing);

    /**
     * Get supported protocols.
     * 
     * @return
     * 		supported protocols
     */
    public MsnProtocol[] getSupportedProtocol();

    /**
     * Set supported protocols. This will take effect only after re-login.
     * 
     * @param supportedProtocol
     * 		supported protocols
     */
    public void setSupportedProtocol(MsnProtocol[] supportedProtocol);

    /**
     * Get current user.
     * 
     * @return
     * 		current user
     */
    public MsnOwner getOwner();

    /**
     * Get contact list. 
     * 
     * @return
     * 		contact list
     */
    public MsnContactList getContactList();

    /**
     * Get current connection information.
     * 
     * @return
     * 		current connect information
     */
    public MsnConnection getConnection();

    /**
     * Get actual used protocol.
     * 
     * @return
     * 		current used protocol
     */
    public MsnProtocol getActualMsnProtocol();

    /**
     * Get the outgoing message chain.
     * 
     * @return
     * 		outgoing message chain
     */
    public MsnMessageChain getOutgoingMessageChain();

    /**
     * Get the incoming message chain.
     * 
     * @return
     * 		incoming message chain
     */
    public MsnMessageChain getIncomingMessageChain();

    /**
     * Login.
     */
    public void login();

    /**
     * Logout.
     */
    public void logout();

    /**
     * Send a message to DS/NS server. If block, the method will return
     * after the message successfully sent or failed. If not block, the
     * method always return false.
     * 
     * @param message
     * 		MsnOutgoingMessage  
     * @param block
     * 		is block
     * @return
     * 		if block, return message send successful, else return false  
     */
    public boolean send(MsnOutgoingMessage message, boolean block);

    /**
     * This method is a shorthand for:
     * <pre>
     *     send(message, false);
     * </pre>
     * 
     * @param message
     * 		MsnOutgoingMessage
     */
    public void send(MsnOutgoingMessage message);

    /**
     * Create a switchboard and start. Send a message to NS server
     * and wait response to start a new switchboard. 
     * <p>
     * You can use the attachement to identify the MsnSwitchboard by
     * call switchboard.getAttachment().
     * 
     * @param attachment
     * 		MsnSwitchboard's attachment
     */
    public void newSwitchboard(Object attachment);

    /**
     * Get all active MsnSwitchboard.
     * 
     * @return
     * 		all active MsnSwitchboard
     */
    public MsnSwitchboard[] getActiveSwitchboards();

    /**
     * Add a new listener for all interfaces.
     *
     * @param listener Instance of MsnAdapter that listen to all interfaces.
     */
    public void addListener(MsnAdapter listener);

    /**
     * Remove the MsnAdapter listener.
     * 
     * @param listener Instance of the listener to be removed.
     */
    public void removeListener(MsnAdapter listener);

    /**
     * Add a new Messenger events listener.
     * 
     * @param listener New Messenger listener.
     */
    public void addMessengerListener(MsnMessengerListener listener);

    /**
     * Remove the Messenger listener.
     * 
     * @param listener Instance of Messenger listener to be removed.
     */
    public void removeMessengerListener(MsnMessengerListener listener);

    /**
     * Add a new listener for incoming messages.
     * 
     * @param listener Instance of the listener.
     */
    public void addMessageListener(MsnMessageListener listener);

    /**
     * Remosves a listener for incoming messages.
     * 
     * @param listener Instance of the listener.
     */
    public void removeMessageListener(MsnMessageListener listener);

    /**
     * Add a new listener for the contact list icoming events.
     * 
     * @param listener New instance of the listener.
     */
    public void addContactListListener(MsnContactListListener listener);

    /**
     * Removes a listener for incoming events for the contact list.
     * 
     * @param listener Instance of the listener to be removed.
     */
    public void removeContactListListener(MsnContactListListener listener);

    /**
     * Add a new switchboard incominf events listener.
     * 
     * @param listener New listener instance.
     */
    public void addSwitchboardListener(MsnSwitchboardListener listener);

    /**
     * Remove a switchboard listener.
     * 
     * @param listener Instance of the listener.
     */
    public void removeSwitchboardListener(MsnSwitchboardListener listener);

    /**
     * Add a file transfer events listener.
     * 
     * @param listener Instance of the listener.
     */
    public void addFileTransferListener(MsnFileTransferListener listener);

    /**
     * Removes a file transfer listener.
     * 
     * @param listener Instance of the listener.
     */
    public void removeFileTransferListener(MsnFileTransferListener listener);

    /**
     * Add a email events listener.
     *
     * @param listener Instance of the listener.
     */
    public void addEmailListener(MsnEmailListener listener);

    /**
     * Removes a email listener.
     *
     * @param listener Instance of the listener.
     */
    public void removeEmailListener(MsnEmailListener listener);

    /**
     * Send text message to someone without format. If the email address is not
     * in any switchboard, will create a switchboard and send the text. 
     * 
     * @param email
     * 		email
     * @param text
     * 		text
     */
    public void sendText(Email email, String text);

    /**
     * Add group.
     * 
     * @param groupName
     * 		group name
     */
    public void addGroup(String groupName);

    /**
     * Remove group.
     * 
     * @param groupId
     * 		group id
     */
    public void removeGroup(String groupId);

    /**
     * Rename group.
     * 
     * @param groupId
     * 		group id
     * @param newGroupName
     * 		new group name
     */
    public void renameGroup(String groupId, String newGroupName);

    /**
     * Adds a contact to the specified list.
     * 
     * @param list
     * 		The list to which the contact should be added.
     * @param email
     *  	The contact's e-mail address.
     * @param friendlyName
     * 		The contact's friendly name. (Optional; applies to FL only.)
     */
    public void addFriend(MsnList list, Email email, String friendlyName);
    
    /**
     * Add friend to FL and AL.
     * 
     * @param email
     *  	email
     * @param friendlyName
     * 		friendly name
     */
    public void addFriend(Email email, String friendlyName);

    /**
     * Copy friend to other group, but user can't both in 
     * default group and user defined group. 
     * 
     * @param email
     * 		email
     * @param groupId
     *      group id 
     */
    public void copyFriend(Email email, String groupId);

    /**
     * Removes a contact from the specified list.
     * 
     * @param list
     * 		The list from which the contact should be removed.
     * @param email
     *  	The contact's e-mail address.
     */
    public void removeFriend(MsnList list, Email email);
    
    /**
     * Remove friend.
     * 
     * @param email
     * 		email
     * @param block
     * 		remove and block
     */
    public void removeFriend(Email email, boolean block);

    /**
     * Remove friend from one group.
     * 
     * @param email
     * 		email
     * @param groupId
     * 		group id
     */
    public void removeFriend(Email email, String groupId);

    /**
     * Move friend from one group to other group.
     * 
     * @param email
     * 		email
     * @param srcGroupId
     * 		source group id 
     * @param destGroupId
     * 		dest group id
     */
    public void moveFriend(Email email, String srcGroupId, String destGroupId);

    /**
     * Block friend.
     * 
     * @param email
     * 		email
     */
    public void blockFriend(Email email);

    /**
     * Unblock friend.
     * 
     * @param email
     * 		email
     */
    public void unblockFriend(Email email);

    /**
     * Rename friend.
     * 
     * @param email
     * 		email
     * @param friendlyName
     * 		new friendly name
     */
    public void renameFriend(Email email, String friendlyName);

    /**
     * Retrieves the content of a display picture, given as a MsnObject. This
     * method can only be used with display pictures and emoticons. If the file
     * specified already exists, then it is removed first.
     * 
     * @param displayPicture Instance of the MsnObject for the display picture.
     * @param listener Listener for the display pictura retrieval progress.
     * @throws JmlException If the MsnObject instance is null, or isn't a 
     * display picture or emoticon.
     */
    public void retrieveDisplayPicture(MsnObject displayPicture,
                                           DisplayPictureListener listener)
    throws JmlException;
    
    /**
     * Retrieves the instance of the duel manager to be used by this session.
     * 
     * @return Instance of the duel manager.
     */
    public DisplayPictureDuelManager getDisplayPictureDuelManager();

    /**
     * Retrieves the instance of the FileTransferManager to be used by this session.
     *
     * @return Instance of the FileTransferManager.
     */
    public FileTransferManager getFileTransferManager();
    
    /**
     * Retrieve OfflineMessages (This could be done after sync)
     */
    public void retreiveOfflineMessages();
    
    /**
     * Set to true to delete the OIMs after the retrieval
     * @param postDel 
     */
    public void setDeleteOfflineMessages(boolean postDel);
    
    /**
    * @return pending list
    */
    public ArrayList<MsnContactPending> getPendingList();

}