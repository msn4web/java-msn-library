package net.sf.jml.message.p2p;

import net.sf.jml.MsnContact;
import net.sf.jml.MsnObject;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.msnslp.MsnslpMessage;
import net.sf.jml.protocol.msnslp.MsnslpResponse;
import net.sf.jml.protocol.outgoing.OutgoingMSG;
import net.sf.jml.util.Charset;
import net.sf.jml.util.JmlConstants;
import net.sf.jml.util.NumberUtils;
import net.sf.jml.util.StringHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.ByteBuffer;

/**
 * Class that executes the work to send a required image.
 *
 * @author Angel Barragán Chacón
 */
public class DisplayPictureDuel {

	protected transient final Log log = LogFactory.getLog(getClass());

	private DisplayPictureDuelManager duelManager;
	private MsnSession session;
	private int timerStatus;

	/**
	 * step: 
	 * 1:200 OK Message finished 
	 * 2:Data Preparation Message finished
	 */
	private int step;

	private int baseId;

	private int sessionId;

	private ByteBuffer displayPicture;

	public int getBaseId() {
		return baseId;
	}

	private int nextBaseId() {
		baseId = MsnP2PBaseIdGenerator.getInstance().getNextId();
		return baseId;
	}

	public DisplayPictureDuel(MsnSession session, 
			                  MsnObject picture, 
			                  DisplayPictureDuelManager duelManager) {
		this.duelManager = duelManager;
		this.session = session;
		this.timerStatus = 0;
		this.step = 0;
		if (picture != null && picture.getMsnObj() != null) {
			displayPicture = ByteBuffer.wrap(picture.getMsnObj());
			displayPicture.clear();
		}
	}

	public MsnSession getSession() {
		return session;
	}

	public void processError() {
		duelManager.remove(getBaseId());
		log.warn("display picture duel process error,in step:" + step);
	}

	public void process(MsnP2PMessage message, MsnContact contact) {
		switch (step) {
		case 1:
			if (message.getFlag() != MsnP2PMessage.FLAG_ACK) {
				processError();
				return;
			}

			if (message.getField7() != getBaseId()) {
				processError();
				return;
			}

			MsnP2PPreperationMessage preperationMessage = new MsnP2PPreperationMessage(
					sessionId, nextBaseId(), contact.getEmail()
							.getEmailAddress());
			OutgoingMSG[] outgoingOkMessages = preperationMessage
					.toOutgoingMsg(session.getMessenger()
							.getActualMsnProtocol());
            for (OutgoingMSG outgoingOkMessage : outgoingOkMessages) {
                session.sendSynchronousMessage(outgoingOkMessage);
            }
            this.step = 2;

			break;
		case 2:
			if (message.getFlag() != MsnP2PMessage.FLAG_ACK) {
				processError();
				return;
			}

			if (message.getField7() != getBaseId()) {
				processError();
				return;
			}

			if (message.getSessionId() != sessionId) {
				processError();
				return;
			}

			// Make it in parallel
			Thread thread = new Thread() {
			
				private MsnContact contact;
				
				public Thread setValue(MsnContact c) {
					this.contact = c;
					return this;
				}
				
				public void run() {
					
					int currentBaseId = nextBaseId();
					while (displayPicture != null && displayPicture.hasRemaining()) {
		                int offset = displayPicture.position();
						int dataLength = displayPicture.remaining() > MsnP2PDataMessage.MAX_DATA_LENGTH ? MsnP2PDataMessage.MAX_DATA_LENGTH
								: displayPicture.remaining();
						byte data[] = new byte[dataLength];
						displayPicture.get(data);
		
						MsnP2PDataMessage dataMessage = new MsnP2PDataMessage(
								sessionId, currentBaseId, offset, displayPicture
										.capacity(), data, contact.getEmail()
										.getEmailAddress());
						OutgoingMSG[] outgoingDataMessages = dataMessage
								.toOutgoingMsg(session.getMessenger()
										.getActualMsnProtocol());
		                for (OutgoingMSG outgoingDataMessage : outgoingDataMessages) {
		                    session.sendSynchronousMessage(outgoingDataMessage);
		                }
		            }
					step = 3;
				}
			}.setValue(contact);
			thread.start();
			
			break;
		case 3:
			if (message.getFlag() != MsnP2PMessage.FLAG_ACK) {
				processError();
				return;
			}

			if (message.getField7() != getBaseId()) {
				processError();
				return;
			}

			if (message.getSessionId() != sessionId) {
				processError();
				return;
			}
			duelManager.remove(getBaseId());
			step = 4;
			return;
		}
		startDuelTimer();
	}

	public void start(MsnP2PSlpMessage message, MsnContact contact) {

		MsnP2PAckMessage ack = new MsnP2PAckMessage(nextBaseId(), contact
				.getEmail().getEmailAddress(), message);
		OutgoingMSG[] outgoing = ack.toOutgoingMsg(session.getMessenger()
				.getActualMsnProtocol());
        for (OutgoingMSG anOutgoing : outgoing) {
            session.sendSynchronousMessage(anOutgoing);
        }

        MsnslpMessage msnslpRequest = message.getSlpMessage();
		MsnslpResponse okSlpMessage = new MsnslpResponse();
		okSlpMessage.setTo(msnslpRequest.getFrom());
		okSlpMessage.setFrom(msnslpRequest.getTo());
		okSlpMessage.setVia(msnslpRequest.getVia());
		okSlpMessage.setCSeq(msnslpRequest.getCSeq() + 1);
		okSlpMessage.setCallId(msnslpRequest.getCallId());
		okSlpMessage.setMaxForwards(msnslpRequest.getMaxForwards());
		okSlpMessage.setContentType(msnslpRequest.getContentType());
		sessionId = msnslpRequest.getBodys().getIntProperty("SessionID", -1);
		StringHolder body = new StringHolder();
		body.setProperty("SessionID", sessionId);
		okSlpMessage.setBody(body.toString() + JmlConstants.LINE_SEPARATOR
				+ "\0");

		int okSlpMessageLength = Charset.encodeAsByteArray(okSlpMessage
				.toString()).length;

		MsnP2PSlpMessage okMessage = new MsnP2PSlpMessage();
		okMessage.setSlpMessage(okSlpMessage);
		okMessage.setIdentifier(nextBaseId());
		okMessage.setTotalLength(okSlpMessageLength);
		okMessage.setCurrentLength(okSlpMessageLength);
		okMessage.setField7(NumberUtils.getIntRandom());
		okMessage.setP2PDest(contact.getEmail().getEmailAddress());

		OutgoingMSG[] outgoingOkMessages = okMessage.toOutgoingMsg(session
				.getMessenger().getActualMsnProtocol());
        for (OutgoingMSG outgoingOkMessage : outgoingOkMessages) {
            session.sendSynchronousMessage(outgoingOkMessage);
        }
        this.step = 1;

		startDuelTimer();
	}

	private void startDuelTimer() {
		this.timerStatus++;
		DisplayPictureDuelTimer.getDuelTimer().schedule(
				new DisplayPictureDuelTimerTask(getBaseId(), this.timerStatus, duelManager),
				1000 * 60);
	}

	public int getDuelTimerStatus() {
		return this.timerStatus;
	}
}