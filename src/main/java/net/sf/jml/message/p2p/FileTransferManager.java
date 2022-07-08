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
package net.sf.jml.message.p2p;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Iterator;
import net.sf.jml.Email;
import net.sf.jml.MsnContact;
import net.sf.jml.MsnFileTransfer;
import net.sf.jml.MsnSwitchboard;
import net.sf.jml.MsnUserStatus;
import net.sf.jml.event.MsnSwitchboardAdapter;
import net.sf.jml.impl.MsnFileTransferImpl;
import net.sf.jml.protocol.MsnIncomingMessage;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.msnslp.MsnslpRequest;
import net.sf.jml.protocol.outgoing.OutgoingMSG;
import net.sf.jml.util.Charset;
import net.sf.jml.util.JmlConstants;
import net.sf.jml.util.NumberUtils;
import net.sf.jml.util.StringHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manages filetransfers.
 * 
 * @author Damian Minkov
 */
public class FileTransferManager
{
    private static final Log logger =
        LogFactory.getLog(FileTransferManager.class);

    private Hashtable<String, FileTransferWorker>
        activeFileTransfers = new Hashtable<String, FileTransferWorker>();
    private MsnSession session = null;

    public void addFileTransfer(String id, FileTransferWorker ftw)
    {
        activeFileTransfers.put(id, ftw);
    }

    public void removeFileTransfer(String id)
    {
        activeFileTransfers.remove(id);
    }

    public MsnFileTransfer getFileTransfer(String id)
    {
        FileTransferWorker fw = activeFileTransfers.get(id);

        if(fw == null)
            return null;

        return fw.getFileTransfer();
    }

    public FileTransferWorker getFileTransferWorker(String id)
    {
        return activeFileTransfers.get(id);
    }

    /**
	 * Generates a new Call identifier for the invite command.
	 *
	 * @return The new call identifier.
	 */
	private String generateNewCallId()
    {

		// Generate the variant number
		int variable = NumberUtils.getIntRandom();

		// Convert to hex value
		String hex =  NumberUtils.toHexValue(variable);

		// Compose the final call id
		return "{2B073406-65D8-A7B2-5B13-B287" + hex + "}";
	}

    public MsnFileTransfer sendFile(final Email email, final File file)
    {
        if (email == null || file == null)
            return null;

        // if contact is offline send as OIM
        MsnContact c = session.getMessenger().
            getContactList().getContactByEmail(email);

        if(c != null && c.getStatus().equals(MsnUserStatus.OFFLINE))
        {
            logger.warn("Contact is either null or offline cannot send file");
            return null;
        }

        MsnSwitchboard switchboard = null;
        MsnSwitchboard[] switchboards = session.getMessenger().getActiveSwitchboards();
        for (MsnSwitchboard switchboard1 : switchboards)
        {
            if (switchboard1.containContact(email)
                    && switchboard1.getAllContacts().length == 1)
            {
                switchboard = switchboard1;
                break;
            }
        }

        if(switchboard == null)
        {
            final Object attachment = new Object();
            SwitchboardListener listener =
                new SwitchboardListener(attachment, email, file);
            session.getMessenger().addSwitchboardListener(listener);
            session.getMessenger().newSwitchboard(attachment);
            // wait for new filetransfer at maximum 15 seconds
            return listener.waitForFileTransfer(15000);
        }
        else
            return sendFile(switchboard, email, file);
    }

    private MsnFileTransfer sendFile(MsnSwitchboard switchboard, Email email, File file)
    {
        MsnslpRequest req = new MsnslpRequest();
        MsnFileInviteMessage invite = new MsnFileInviteMessage()
        {
            protected void receivedResponse(MsnSession session, MsnIncomingMessage response)
            {
                if((getOffset() + getCurrentLength()) < getTotalLength())
                {
                    binaryHeader.rewind();

                    setOffset(getCurrentLength());
                    long currLen = getTotalLength() - getCurrentLength();

                    if(currLen > MsnP2PDataMessage.MAX_DATA_LENGTH)
                        setCurrentLength(MsnP2PDataMessage.MAX_DATA_LENGTH);
                    else
                        setCurrentLength((int)currLen);

                    OutgoingMSG[] outgoingOkMessages = toOutgoingMsg(
                        session.getMessenger().getActualMsnProtocol());
                    for (OutgoingMSG outgoingOkMessage : outgoingOkMessages)
                    {
                        session.sendSynchronousMessage(outgoingOkMessage);
                    }
                }
            }
        };
        invite.setSlpMessage(req);
        int lastRandomIdentifier = NumberUtils.getIntRandom();

        // Set the destination for the message (the MsnObject creator)
        invite.setP2PDest(email.getEmailAddress());

        // Set the binary Header
        invite.setIdentifier(MsnP2PBaseIdGenerator.getInstance().getNextId());
        invite.setFlag(MsnP2PMessage.FLAG_OLD_NONE);
        invite.setField7(lastRandomIdentifier);
        invite.setField8(0);
        invite.setField9(0);

        // Set body
        req.setRequestMethod(MsnFileInviteMessage.METHOD_INVITE);
        req.setRequestURI("MSNMSGR:" + email.getEmailAddress());
        req.setTo("<msnmsgr:" + email.getEmailAddress() + ">");
        req.setFrom("<msnmsgr:" +
                    session.getMessenger().getOwner().getEmail().getEmailAddress() + ">");
        req.setVia(
            "MSNSLP/1.0/TLP ;branch=" + generateNewCallId());
        req.setCSeq(0);
        req.setCallId(generateNewCallId());
        req.setMaxForwards(0);
        req.setContentType("application/x-msnmsgr-sessionreqbody");

        StringHolder body = new StringHolder();
		body.setProperty(MsnFileInviteMessage.KEY_GUID_EUF,
				         MsnFileInviteMessage.GUID_EUF);
		body.setProperty("SessionID", lastRandomIdentifier);
		body.setProperty("AppID", 2);
        try
        {
            body.setProperty(MsnP2PInvitationMessage.KEY_CONTEXT,
                MsnFileContextParser.getEncodedContext(file));
        }
        catch (UnsupportedEncodingException ex)
        {
            logger.error("", ex);
        }

		req.setBody(body.toString() +
				    JmlConstants.LINE_SEPARATOR + "\0");

        // Get the size of the message to be setted
        int slpMessageLength = Charset.encodeAsByteArray(req.toString()).length;
        invite.setTotalLength(slpMessageLength);

        if(slpMessageLength > MsnP2PDataMessage.MAX_DATA_LENGTH)
        {
            invite.setCurrentLength(MsnP2PDataMessage.MAX_DATA_LENGTH);
            invite.setOffset(0l);
        }
        else
        {
            invite.setCurrentLength(slpMessageLength);
        }

        String sessionId = String.valueOf(lastRandomIdentifier);
        MsnFileTransferImpl fileTransfer =
            new MsnFileTransferImpl(sessionId, email, invite, session, true);

        fileTransfer.setFile(file);
        fileTransfer.setFileTotalSize(file.length());
        FileTransferWorker ftw = new FileTransferWorker(fileTransfer);
        ftw.setSwitchboard(switchboard);

        session.getMessenger().getFileTransferManager().
            addFileTransfer(sessionId, ftw);

        OutgoingMSG[] outgoingOkMessages = invite.toOutgoingMsg(
            session.getMessenger().getActualMsnProtocol());
        for (OutgoingMSG outgoingOkMessage : outgoingOkMessages)
        {
            switchboard.send(outgoingOkMessage);
        }

        return fileTransfer;
    }

    /**
     * @param session the session to set
     */
    public void setSession(MsnSession session)
    {
        this.session = session;
    }

    private class SwitchboardListener
        extends MsnSwitchboardAdapter
    {
        Object attachment = null;
        Email email = null;
        File file = null;
        MsnFileTransfer fileTransfer = null;

        SwitchboardListener(Object attachment, Email email, File file)
        {
            this.attachment = attachment;
            this.email = email;
            this.file = file;
        }

        @Override
        public void switchboardStarted(MsnSwitchboard switchboard)
        {
            if (switchboard.getAttachment() == attachment)
            {
                switchboard.inviteContact(email);
            }
        }

        @Override
        public void contactJoinSwitchboard(MsnSwitchboard switchboard,
                MsnContact contact)
        {
            if (switchboard.getAttachment() == attachment
                    && email.equals(contact.getEmail()))
            {
                switchboard.setAttachment(null);
                session.getMessenger().removeSwitchboardListener(this);

                // send file
                fileTransfer = sendFile(switchboard, email, file);

                synchronized(this)
                {
                    notifyAll();
                }
            }
        }

        public MsnFileTransfer waitForFileTransfer(long waitFor)
        {
            logger.trace("Waiting for a FileTransfer Status Event");

            synchronized(this)
            {
                if(fileTransfer != null)
                {
                    return fileTransfer;
                }

                try
                {
                    wait(waitFor);
                    
                    if(fileTransfer != null)
                        logger.trace("Received a FileTransfer.");
                    else
                        logger.trace("No FileTransfer received for "
                            + waitFor + "ms.");

                    return fileTransfer;
                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                        "Interrupted while waiting for a FileTransfer", ex);
                    return null;
                }
            }
        }
    }
}
