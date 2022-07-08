package net.sf.jml.message.p2p;

// JRE io

import net.sf.jml.*;
import net.sf.jml.event.MsnAdapter;
import net.sf.jml.message.*;
import net.sf.jml.protocol.msnslp.MsnslpMessage;
import net.sf.jml.protocol.msnslp.MsnslpRequest;
import net.sf.jml.protocol.msnslp.MsnslpResponse;
import net.sf.jml.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This class implements the process to retrieve a display picture or emoticon.
 * Other objects must be done through file transer. An instance of this class
 * can only be used once.
 * 
 * @author Angel Barragán Chacón
 */
public class DisplayPictureRetrieveWorker extends MsnAdapter { 

	/**
	 * Loger for the class.
	 */
    private static final Log log = 
    	LogFactory.getLog(DisplayPictureRetrieveWorker.class);

    ////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Instance of the MsnMessenger instance where we must perform the process.
	 */
	private MsnMessenger messenger = null;
	
	/**
	 * Instance of the MsnObject to be retrieved.
	 */
	private MsnObject msnObject = null; 

	/**
	 * Instance of the listerner to notify about the retrieval progress.
	 */
	private DisplayPictureListener listener = null;
	
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Creates a new instance of the process to retrieve a MsnObject.
	 * 
	 * @param messenger Instance of the messenger client.
	 * @param msnObject Instance of the MsnObject to be retrieved.
	 * @param listener Listener for the process progress.
	 */
	public DisplayPictureRetrieveWorker(
			MsnMessenger messenger, 
			MsnObject msnObject,
			DisplayPictureListener listener) {
		this.messenger = messenger;
		this.msnObject = msnObject;
		this.listener = listener;
		
		// Add listener for messages
		messenger.addMessageListener(this);
	}
	
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Creator of the MsnObject.
	 */
	private Email creator = null;
	
	/**
	 * Switchboard where the retrieval will be performed.
	 */
	private MsnSwitchboard switchboard = null;
	
	/**
	 * Signals if this task is active.
	 */
	private boolean active = true;
	
	/**
	 * Flag to denote if the notification has already been sended.
	 */
	private boolean notified = false;

	/**
	 * Start the process of retrieving the object.
	 */
	public void start() {

		// Clear everithing
		if (!active) {
			return;
		}
		
		// Get the creator of the object
		creator = Email.parseStr(msnObject.getCreator());
		
		// Add this object as a switchboard listener
		messenger.addSwitchboardListener(this);
			
		// Create the switchboard
		messenger.newSwitchboard(this);
	}

	/**
	 * Perform the final steps of the object retrieval.
	 */
	private synchronized void finishProcess() {

		// Check that it isn't already disabled
		if (!active) {
			return;
		}
		
		// Disable this worker
		active = false;

		// Remove the switchboard listener
		messenger.removeSwitchboardListener(this);
		
		// Remove listeners
		messenger.removeMessageListener(this);
		
		// If the switchboard has been created, close it
		switchboard.close();
		
	}

	/**
	 * Finish this process and notify the listener about finalization.
	 * 
	 * @param result Result of the finalization.
	 * @param context Context of the finalization.
	 * @param finishProcess True if we must finish the process and false if not.
	 */
	private synchronized void notifyFinalization(
			DisplayPictureListener.ResultStatus result,
			Object context,
			boolean finishProcess) {
		
		// First finish the switchboard
		if (finishProcess) {
			finishProcess();
		}

		// Check if the notification has already been sent
		if (notified) {
			return;
		}
		
		// Mark the notification
		notified = true;

		// Check if we must remove the file
		if (buffer != null && 
			result != DisplayPictureListener.ResultStatus.GOOD) {
                        // clear the buffer as creating it once again
                        buffer = new ByteArrayOutputStream();
//			storeFile.delete();
		}
		
                byte[] res;
                if(buffer == null)
                    res = new byte[0];
                else
                    res = buffer.toByteArray();
                
		// Notify the result
		listener.notifyMsnObjectRetrieval(
				messenger, 
				this, 
				msnObject, 
				result, 
                                res,
				context);
	}
	
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Base identifier for the messages.
	 */
	private int baseId = NumberUtils.getIntRandom();
	
	/**
	 * Retrieves the next identifier to send a message.
	 * 
	 * @return Next identifier to send messages. 
	 */
	private int getNextIdentifier() {
		return ++baseId;
	}
	
	/**
	 * Retrieves the last generated identifier.
	 * 
	 * @return Last generated identifier.
	 */
	private int getLastIdentifier() {
		return baseId;
	}

	////////////////////////////////////////////////////////////////////////////

	/**
	 * Generates a new Call identifier for the invite command.
	 * 
	 * @return The new call identifier. 
	 */
	private String generateNewCallId() {
		
		// Generate the variant number
		int variable = NumberUtils.getIntRandom();
		
		// Convert to hex value
		String hex =  NumberUtils.toHexValue(variable);
		
		// Compose the final call id
		return "{2B073406-65D8-A7B2-5B13-B287" + hex + "}";		
	}

	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Session identification for this MsnObject transmision.
	 */
	private int transferSessionId = -1;

	/**
	 * Call identifier.
	 */
	private String callId;
	
	/**
	 * Random identifier for the messages.
	 */
	private int lastRandomIdentifier = 0;
	
	/**
	 * Sends a P2P invitation request to init the MsnObject retrieval.
	 */
	private void sendP2PInviteRequest() {

		// Create the session identifier
		transferSessionId = NumberUtils.getIntRandom();
		lastRandomIdentifier = NumberUtils.getIntRandom();
		callId = generateNewCallId();
		
		// Create an invitation message
		MsnslpRequest req = new MsnslpRequest();
		MsnP2PInvitationMessage msg = new MsnP2PInvitationMessage();
		msg.setSlpMessage(req);

		// Set the destination for the message (the MsnObject creator)
		msg.setP2PDest(creator.getEmailAddress());

		// Set the binary Header
		msg.setSessionId(0);
		msg.setIdentifier(getNextIdentifier());
		msg.setFlag(0);
		msg.setField7(lastRandomIdentifier);
		msg.setField8(0);
		msg.setField9(0);
		
		// Body
		req.setRequestMethod(MsnP2PInvitationMessage.METHOD_INVITE);
		req.setRequestURI("MSNMSGR:" + creator.getEmailAddress());
		req.setTo("<msnmsgr:" + creator.getEmailAddress() + ">");
		req.setFrom("<msnmsgr:" + 
				    messenger.getOwner().getEmail().getEmailAddress() + ">");
		req.setVia("MSNSLP/1.0/TLP ;branch=" + callId);
		req.setCSeq(0);
		req.setCallId(callId);
		req.setMaxForwards(0);
		req.setContentType("application/x-msnmsgr-sessionreqbody");
		StringHolder body = new StringHolder();
		body.setProperty(MsnP2PInvitationMessage.KEY_GUID_EUF, 
				         MsnP2PInvitationMessage.GUID_EUF);
		body.setProperty("SessionID", transferSessionId); 
		body.setProperty("AppID", 1); 
		body.setProperty(MsnP2PInvitationMessage.KEY_CONTEXT, 
		                 StringUtils.encodeBase64(msnObject.toString()));
		req.setBody(body.toString() +
				    JmlConstants.LINE_SEPARATOR + "\0");

		// Get the size of the message to be setted
		int slpMessageLength = Charset.encodeAsByteArray(req.toString()).length;
		msg.setTotalLength(slpMessageLength);
		msg.setCurrentLength(slpMessageLength);
		
		// Binary Footer
		msg.setAppId(0);
		
		// Send the message
		switchboard.sendMessage(msg);

		// Log
		log.info("Sended Invite request for retrieval of avatar for " + 
				  msnObject.getCreator());
		
		// Log the message
//		log.info("Sending P2P message\n" + msg.toDebugString());
		
	}

	/**
	 * Send a P2P ACK message for a received P2P message.
	 * 
	 * @param message Received P2P message.
	 * @param sessionIdFlag True if the session Id must be setted.
	 */
	private void sendP2PAck(MsnP2PMessage message, boolean sessionIdFlag) { 
		
		// Create the ACK message
		MsnP2PAckMessage ack = new MsnP2PAckMessage(
				getNextIdentifier(),
				creator.getEmailAddress(),
				message);
		if (sessionIdFlag) {
			ack.setSessionId(transferSessionId);
		}
		
		// Send the message
		switchboard.sendMessage(ack);
	}
	
	/**
	 * Send the bye Message.
	 */
	private void sendP2PByeMessage() {

		MsnslpRequest req = new MsnslpRequest();
		MsnP2PByeMessage bye = new MsnP2PByeMessage();
		bye.setSlpMessage(req);
		lastRandomIdentifier = NumberUtils.getIntRandom();

		// Set the destination for the message (the MsnObject creator)
		bye.setP2PDest(creator.getEmailAddress());
		
		// Set the binary Header
		bye.setSessionId(0);
		bye.setIdentifier(getNextIdentifier());
		bye.setFlag(MsnP2PMessage.FLAG_BYE);
		bye.setField7(lastRandomIdentifier);
		bye.setField8(0);
		bye.setField9(0);
		
		// Set body
		req.setRequestMethod(MsnP2PByeMessage.METHOD_BYE);
		req.setRequestURI("MSNMSGR:" + creator.getEmailAddress());
		req.setTo("<msnmsgr:" + creator.getEmailAddress() + ">");
		req.setFrom("<msnmsgr:" + 
				    messenger.getOwner().getEmail().getEmailAddress() + ">");
		req.setVia(
			"MSNSLP/1.0/TLP ;branch=" + callId);
		req.setCSeq(0);
		req.setCallId(callId);
		req.setMaxForwards(0);
		req.setContentType("application/x-msnmsgr-sessionclosebody");
		req.setBody(JmlConstants.LINE_SEPARATOR + "\0");

		// Get the size of the message to be setted
		int slpMessageLength = Charset.encodeAsByteArray(req.toString()).length;
		bye.setTotalLength(slpMessageLength);
		bye.setCurrentLength(slpMessageLength);
		
		// Set footer
		bye.setAppId(0);

		// Send the message
		switchboard.sendMessage(bye);
		
		// Log
		log.info("Sended Bye request for retrieval of avatar for " + 
				  msnObject.getCreator());
	}

	/**
	 * Send a response to deny a direct connection invitation.
	 * 
	 * @param invite Original invitation message.
	 */
	private void sendDirectConnectionDeny(MsnP2PInvitationMessage invite) {

        MsnslpMessage msnslpRequest = invite.getSlpMessage();
		MsnslpResponse okSlpMessage = new MsnslpResponse();
		okSlpMessage.setStatusCode(405);
		okSlpMessage.setReasonPhrase("Not supported");
		okSlpMessage.setTo(msnslpRequest.getFrom());
		okSlpMessage.setFrom(msnslpRequest.getTo());
		okSlpMessage.setVia(msnslpRequest.getVia());
		okSlpMessage.setCSeq(msnslpRequest.getCSeq() + 1);
		okSlpMessage.setCallId(msnslpRequest.getCallId());
		okSlpMessage.setMaxForwards(msnslpRequest.getMaxForwards());
		okSlpMessage.setContentType(msnslpRequest.getContentType());
		
		StringHolder body = new StringHolder();
		body.setProperty("SessionID", transferSessionId);
		okSlpMessage.setBody(body.toString() + JmlConstants.LINE_SEPARATOR
				+ "\0");

		int okSlpMessageLength = Charset.encodeAsByteArray(okSlpMessage
				.toString()).length;

		MsnP2PSlpMessage okMessage = new MsnP2PSlpMessage();
		okMessage.setSlpMessage(okSlpMessage);
		okMessage.setIdentifier(getNextIdentifier());
		okMessage.setTotalLength(okSlpMessageLength);
		okMessage.setCurrentLength(okSlpMessageLength);
		okMessage.setField7(NumberUtils.getIntRandom());
		okMessage.setP2PDest(creator.getEmailAddress());

		// Send the message
		switchboard.sendMessage(okMessage);
		
		// Log
		log.info("Denied direct-Connection invitation for retrieval of avatar for " + 
				  msnObject.getCreator());
	}
	
	////////////////////////////////////////////////////////////////////////////
	//                                                                        //
	//                  MsnSwitchboardListener Implementation                 //
	//                                                                        //
	////////////////////////////////////////////////////////////////////////////

	/**
	 * @see net.sf.jml.event.MsnSwitchboardListener#switchboardStarted(MsnSwitchboard)
	 */
	public void switchboardStarted(MsnSwitchboard switchboard) {
		
		// Check if it is the switchboard we are looking for
		if (switchboard.getAttachment() != this) {
			return;
		}
		
		// Log
		log.info("Switchboard started for avatar download for " + 
				  msnObject.getCreator());
		
		// Store the switchboard
		this.switchboard = switchboard;
		
		// Add the contact to the switchboard
		switchboard.inviteContact(creator);

	}
	
	/**
	 * @see net.sf.jml.event.MsnSwitchboardListener#switchboardClosed(MsnSwitchboard)
	 */
	public void switchboardClosed(MsnSwitchboard switchboard) {

		// Check if it is the switchboard we are looking for
		if (switchboard.getAttachment() != this) {
			return;
		}

		// Notify the finalization
		notifyFinalization(
				DisplayPictureListener.ResultStatus.PROTOCOL_ERROR, 
				0,
				true);
	}
	
	/**
	 * @see net.sf.jml.event.MsnSwitchboardListener#contactJoinSwitchboard(MsnSwitchboard,
	 *                                                    MsnContact)
	 */
	public void contactJoinSwitchboard(MsnSwitchboard switchboard, 
			                           MsnContact contact) {
		
		// Check if it is the switchboard we are looking for
		if (switchboard.getAttachment() != this) {
			return;
		}

		// Send the invite request
		sendP2PInviteRequest();
	}

	/**
	 * @see net.sf.jml.event.MsnSwitchboardListener#contactLeaveSwitchboard(MsnSwitchboard,
	 *                                                     MsnContact)
	 */
	public void contactLeaveSwitchboard(MsnSwitchboard switchboard, 
			                            MsnContact contact) {
		// Check if it is the switchboard we are looking for
		if (switchboard.getAttachment() != this) {
			return;
		}

		// Notify the finalization
		notifyFinalization(
				DisplayPictureListener.ResultStatus.PROTOCOL_ERROR,
                0,
				true);
	}
	

	////////////////////////////////////////////////////////////////////////////
	//                                                                        //
	//                    MsnMessageListener Implementation                   //
	//                                                                        //
	////////////////////////////////////////////////////////////////////////////

//	/**
//	 * File to store the MsnObject.
//	 */
//	private RandomAccessFile buffer = null;
        
        private ByteArrayOutputStream buffer = null;

	/**
	 * State of P2P messages reception.
	 */
	private TransferState state = TransferState.WAITING_INVITE_ACK;

	/**
	 * Ammount of received data for the avatar.
	 */
	private int receivedData = 0;
	
	/**
	 * States of messages reception
	 */
	private enum TransferState { 
		/**
		 * We are waiting for the ACK for the INVITE request.
		 */
		WAITING_INVITE_ACK,
		/**
		 * We are waiting fro the 200 ok message.
		 */
		WAITING_200_OK,
		/**
		 * We are waiting for the data preparation message.
		 */
		WAITING_DATA_PREPARATION,
		/**
		 * We are waiting for the data messages.
		 */
		WAITING_DATA,
		/**
		 * We are waiting the Bye ack message.
		 */
		WAITING_BYE_ACK
	}
	
	/**
	 * @see net.sf.jml.event.MsnMessageListener#p2pMessageReceived(MsnSwitchboard,
	 *                                            MsnP2PMessage, 
	 *                                            MsnContact)
	 */
	public void p2pMessageReceived(MsnSwitchboard switchboard, 
			                       MsnP2PMessage message, 
			                       MsnContact contact) {
		
		// Check that is for our switchboard
		if (switchboard.getAttachment() != this) {
			return;
		}

		// Check for the incoming message so we can process it //
		
		// Check if it is a Bye ACK (so it is saying good bye)
		if (message.getFlag() == 0x40) {

			// Check if the process is active or not
			if (state == TransferState.WAITING_BYE_ACK) {
				// Finish the process
				finishProcess();
			}
			else {
				// Notify about the error and finish the process
//				notifyFinalization(
//						DisplayPictureListener.ResultStatus.PROTOCOL_ERROR, 
//						new Integer(message.getFlag()),
//						true);
			}
		}
		
		// Check if we receive a wait
		else if (message.getFlag() == 0x4) {
			
//			MIME-Version: 1.0
//			Content-Type: text/x-keepalive

			// Send the keep-alive message
//			sendKeepAliveMessage();
		}
	
		// Check if we receive and ACK
		else if (message.getFlag() == MsnP2PMessage.FLAG_ACK) {
			
			// We can only receive an ACK for the invitation or for the 
			// Bye message
			if (state == TransferState.WAITING_INVITE_ACK && 
				message.getField7() == baseId) {
				// Set next step
				state = TransferState.WAITING_200_OK;
			}
			else if (state == TransferState.WAITING_BYE_ACK &&
				message.getField7() == getLastIdentifier()) {
				// Notify the final of the object retrieval
        		finishProcess();
			}
		}

		// Check if we receive a SLP response
		else if (message instanceof MsnP2PSlpMessage &&
			((MsnP2PSlpMessage) message).getSlpMessage() instanceof MsnslpResponse) {

			// Check that the response is for us
			MsnP2PSlpMessage p2pSlpMessage = (MsnP2PSlpMessage) message;
			MsnslpResponse slpResponse = 
				(MsnslpResponse) p2pSlpMessage.getSlpMessage();
			int sessionId = slpResponse.getBodys().getIntProperty("SessionID");
			if (sessionId == transferSessionId) {
				
				// Check that it is a 200 response
				if (slpResponse.getStatusCode() == 200) {
					
					// Set next step
					state = TransferState.WAITING_DATA_PREPARATION;
					
					// Send ACK
					sendP2PAck(message, false);
				}
				
				// It is an error
				else {
					
					// Send a Bye message
					sendP2PByeMessage();
					
					// Set the next step
					state = TransferState.WAITING_BYE_ACK;
					
					// Notify about the error
					notifyFinalization(
							DisplayPictureListener.ResultStatus.PROTOCOL_ERROR,
                            message.getFlag(),
							false);
				}
			}
		}

		// Check if we receive a data preparation message
		else if (message instanceof MsnP2PPreperationMessage &&
			     message.getSessionId() == transferSessionId) {
				
				// Set next step
				state = TransferState.WAITING_DATA;
				
				// Send the ACK
				sendP2PAck(message, true);
		}
		
		// Check if we receive data messages
		else if (message instanceof MsnP2PDataMessage &&
				 message.getSessionId() == transferSessionId) {

			// Set next step
			state = TransferState.WAITING_DATA;
			
        	// Get the message
        	MsnP2PDataMessage dataMsg = (MsnP2PDataMessage) message;

        	try {
        		
	        	// Check if we have created a buffer for the Object
	        	if (buffer == null) {
//	        		buffer = new RandomAccessFile(storeFile, "rw");
                                buffer = new ByteArrayOutputStream();
	        	}
	        	
	        	// Get the data
	        	byte[] data = dataMsg.bodyToMessage();
	        	
	        	// Add to the already data
//	       		buffer.seek(dataMsg.getOffset());
//	       		buffer.write(data);
                        buffer.write(data, 0, data.length);
	        	
	       		// Set the ammount of received data
	       		receivedData += data.length;

	    		// Log
	    		log.info("Received data message ('" + receivedData + "','" + 
	    				  dataMsg.getTotalLength() + 
	    				  "') for retrieval of avatar for " + 
	    				  msnObject.getCreator());
	       		
	        	// Check if we have finished
//	        	if (buffer.length() == dataMsg.getTotalLength()) {
                        if (buffer.size() == dataMsg.getTotalLength()) {
	
		    		// Log
		    		log.info("Finished receiving data for retrieval of avatar for " + 
		    				  msnObject.getCreator());
	        		
	        		// Close the file
	        		buffer.close();
	        		
	        		// Send the data acknowledgement
					sendP2PAck(message, true);
	
	        		// Send Bye message
					sendP2PByeMessage();
	        		
	        		// Change the state
	        		state = TransferState.WAITING_BYE_ACK;

		    		// Log
		    		log.info("Notifing about retrieved avatar for " + 
		    				  msnObject.getCreator());
	        		
	        		// Notify that we have the file
	        		notifyFinalization(
	        				DisplayPictureListener.ResultStatus.GOOD, 
	        				null,
	        				false);
	        	}
        	} catch (IOException e) {

        		// Send Bye message
				sendP2PByeMessage();
        		
        		// Change the state
        		state = TransferState.WAITING_BYE_ACK;
        		
        		// Notify about the error
        		notifyFinalization(
        			DisplayPictureListener.ResultStatus.FILE_ACCESS_ERROR, 
        			e,
        			false);
        	}
		}
		
		// Check if it is an invitation for a direct connection
		else if (message instanceof MsnP2PInvitationMessage) {
			
			// Check that it is an invitation for direct connection
			MsnP2PInvitationMessage invite = (MsnP2PInvitationMessage) message;
			MsnslpRequest inviteRequest = (MsnslpRequest) invite.getSlpMessage();
			if (inviteRequest.getContentType().equals(
					"application/x-msnmsgr-transreqbody")) {

				// Check that the SessionID and the CallID are the same
				int sessionId = inviteRequest.getBodys().getIntProperty("SessionID");
				String callId = inviteRequest.getCallId();
				if (sessionId == transferSessionId && callId.equals(callId)) {

					// Send an ACK for the message
					sendP2PAck(message, false);
					
					// Send a deny for direct connection
					sendDirectConnectionDeny(invite);
					
				}
			}
		}
		
	}

	/**
	 * @see net.sf.jml.event.MsnMessageListener#controlMessageReceived(MsnSwitchboard,
	 *                                                MsnControlMessage, 
	 *                                                MsnContact)
	 */
	public void controlMessageReceived(MsnSwitchboard switchboard, 
			                           MsnControlMessage message, 
			                           MsnContact contact) {
	}

	/**
	 * @see net.sf.jml.event.MsnMessageListener#datacastMessageReceived(MsnSwitchboard,
	 *                                                 MsnDatacastMessage, 
	 *                                                 MsnContact)
	 */
	public void datacastMessageReceived(MsnSwitchboard switchboard, 
			                            MsnDatacastMessage message, 
			                            MsnContact contact) {
	}

	/**
	 * @see net.sf.jml.event.MsnMessageListener#instantMessageReceived(MsnSwitchboard,
	 *                                                MsnInstantMessage, 
	 *                                                MsnContact)
	 */
	public void instantMessageReceived(MsnSwitchboard switchboard, 
			                           MsnInstantMessage message, 
			                           MsnContact contact) {
	}

	/**
	 * @see net.sf.jml.event.MsnMessageListener#systemMessageReceived(MsnMessenger,
	 *                                               MsnSystemMessage)
	 */
	public void systemMessageReceived(MsnMessenger messenger, 
			                          MsnSystemMessage message) {
	}

	/**
	 * @see net.sf.jml.event.MsnMessageListener#unknownMessageReceived(MsnSwitchboard, 
	 *                                                MsnUnknownMessage, 
	 *                                                MsnContact)
	 */
	public void unknownMessageReceived(MsnSwitchboard switchboard, 
			                           MsnUnknownMessage message, 
			                           MsnContact contact) {
	}
	
}
