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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import net.sf.jml.MsnFileTransferState;
import net.sf.jml.MsnSwitchboard;
import net.sf.jml.impl.AbstractMessenger;
import net.sf.jml.impl.MsnFileTransferImpl;
import net.sf.jml.protocol.MsnIncomingMessage;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.msnslp.MsnslpMessage;
import net.sf.jml.protocol.msnslp.MsnslpRequest;
import net.sf.jml.protocol.msnslp.MsnslpResponse;
import net.sf.jml.protocol.outgoing.OutgoingMSG;
import net.sf.jml.util.Charset;
import net.sf.jml.util.JmlConstants;
import net.sf.jml.util.NumberUtils;
import net.sf.jml.util.StringHolder;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Manages start and stopping filetransfers. Also sending filetransfers.
 * @author Damian Minkov
 */
public class FileTransferWorker
{
    private static final Log logger =
        LogFactory.getLog(FileTransferWorker.class);

    private MsnFileTransferImpl fileTransfer;

    /**
	 * Base identifier for the messages.
	 */
	private int baseId = NumberUtils.getIntRandom();

    private int baseP2PId;

    private MsnSwitchboard switchboard = null;

    private FileInputStream fileInput = null;
    private long fileOffset = 0;


    public FileTransferWorker(MsnFileTransferImpl fileTransfer)
    {
        this.fileTransfer = fileTransfer;
    }

    /**
     * @return the fileTransfer
     */
    public MsnFileTransferImpl getFileTransfer()
    {
        return fileTransfer;
    }

	/**
	 * Retrieves the next identifier to send a message.
	 *
	 * @return Next identifier to send messages.
	 */
	private int getNextIdentifier()
    {
		return ++baseId;
	}

    /**
	 * Retrieves the last generated identifier.
	 *
	 * @return Last generated identifier.
	 */
	private int getLastIdentifier()
    {
		return baseId;
	}

    private int nextP2PBaseId()
    {
		baseP2PId = MsnP2PBaseIdGenerator.getInstance().getNextId();
		return baseP2PId;
	}

    public void startFileTransfer()
    {
        if(fileTransfer.isReceiver())
        {
            MsnslpMessage msnslpRequest = fileTransfer.getReqMessage().getSlpMessage();
            MsnslpResponse okSlpMessage = new MsnslpResponse();
            okSlpMessage.setTo(msnslpRequest.getFrom());
            okSlpMessage.setFrom(msnslpRequest.getTo());
            okSlpMessage.setVia(msnslpRequest.getVia());
            okSlpMessage.setCSeq(msnslpRequest.getCSeq() + 1);
            okSlpMessage.setCallId(msnslpRequest.getCallId());
            okSlpMessage.setMaxForwards(msnslpRequest.getMaxForwards());
            okSlpMessage.setContentType(msnslpRequest.getContentType());

            StringHolder body = new StringHolder();
            body.setProperty("SessionID", fileTransfer.getID());

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
            okMessage.setFlag(MsnP2PMessage.FLAG_OLD_NONE);
            okMessage.setP2PDest(fileTransfer.getEmail().getEmailAddress());

            OutgoingMSG[] outgoingOkMessages = okMessage.toOutgoingMsg(
                fileTransfer.getSession().getMessenger().getActualMsnProtocol());
            for (OutgoingMSG outgoingOkMessage : outgoingOkMessages)
            {
                fileTransfer.getSession().sendSynchronousMessage(outgoingOkMessage);
            }
        }
        else
        {
            try
            {
                if(fileInput == null)
                {
                    fileInput = new FileInputStream(fileTransfer.getFile());
                    // increase identifier
                    getNextIdentifier();
                }

                int remaining = (int)(fileTransfer.getFile().length() - fileOffset);

                int dataLength = remaining > MsnP2PDataMessage.MAX_DATA_LENGTH ?
                    MsnP2PDataMessage.MAX_DATA_LENGTH : remaining;

                byte data[] = new byte[dataLength];
                fileInput.read(data);

                MsnP2PDataMessage dataMessage = new MsnP2PDataMessage(
                        Integer.parseInt(fileTransfer.getID()),
                        getLastIdentifier(),
                        (int)fileOffset,
                        (int)fileTransfer.getFileTotalSize(),
                        data,
                        fileTransfer.getEmail().getEmailAddress())
                {
                    protected void receivedResponse(MsnSession session,
                        MsnIncomingMessage response)
                    {
                        try
                        {
                            int remaining =
                                (int)(fileTransfer.getFile().length() - fileOffset);

                            if(remaining == 0)
                            {
                                fileTransfer.setState(
                                    MsnFileTransferState.COMPLETED);

                                 ((AbstractMessenger)fileTransfer.getSession().
                                     getMessenger()).
                                        fireFileTransferFinished(fileTransfer);
                                 return;
                            }

                            if(fileTransfer.getState()
                                    == MsnFileTransferState.CANCELED)
                                return;

                            binaryHeader.rewind();

                            int dataLength = remaining > MsnP2PDataMessage.MAX_DATA_LENGTH ?
                                MsnP2PDataMessage.MAX_DATA_LENGTH : remaining;

                            byte data[] = new byte[dataLength];
                            fileInput.read(data);

                            setCurrentLength(data.length);
                            parseP2PBody(ByteBuffer.wrap(data));

                            setOffset(fileOffset);

                            fileOffset += data.length;

                            fileTransfer.setTransferredSize(getOffset());
                            ((AbstractMessenger)fileTransfer.getSession().getMessenger()).
                                fireFileTransferProcess(fileTransfer);

                            OutgoingMSG[] outgoingMessages = toOutgoingMsg(
                                session.getMessenger().getActualMsnProtocol());
                            for (OutgoingMSG outgoingMessage : outgoingMessages)
                            {
                                switchboard.send(outgoingMessage);
                            }
                        }
                        catch (IOException e)
                        {
                            logger.info("Cannot send file", e);
                        }
                    }
                };
                dataMessage.setFlag(MsnP2PMessage.FLAG_OLD_DATA);
                fileOffset += data.length;

                fileTransfer.setTransferredSize(fileOffset);
                ((AbstractMessenger)fileTransfer.getSession().getMessenger()).
                    fireFileTransferProcess(fileTransfer);

                OutgoingMSG[] outgoingDataMessages = dataMessage
                        .toOutgoingMsg(fileTransfer.getSession().getMessenger()
                                .getActualMsnProtocol());
                for (OutgoingMSG outgoingDataMessage : outgoingDataMessages)
                {
                    switchboard.send(outgoingDataMessage);
                }
            }
            catch (FileNotFoundException ex)
            {
                logger.error("Cannot open file", ex);
            }
            catch (IOException ex)
            {
                logger.error("Cannot read from file", ex);
            }
        }
    }

    private void sendBye()
    {
        MsnslpRequest req = new MsnslpRequest();
        MsnFileByeMessage bye = new MsnFileByeMessage();
        bye.setSlpMessage(req);
        int lastRandomIdentifier = NumberUtils.getIntRandom();

        // Set the destination for the message (the MsnObject creator)
        bye.setP2PDest(fileTransfer.getEmail().getEmailAddress());

        // Set the binary Header
        bye.setSessionId(Integer.parseInt(fileTransfer.getID()));
        bye.setIdentifier(getNextIdentifier());
        bye.setFlag(MsnP2PMessage.FLAG_OLD_NONE);
        bye.setField7(lastRandomIdentifier);
        bye.setField8(0);
        bye.setField9(0);

        // Set body
        req.setRequestMethod(MsnP2PByeMessage.METHOD_BYE);
        req.setRequestURI("MSNMSGR:" + fileTransfer.getSession().getMessenger().getOwner().getEmail().getEmailAddress());
        req.setTo("<msnmsgr:" + fileTransfer.getEmail().getEmailAddress() + ">");
        req.setFrom("<msnmsgr:" +
            fileTransfer.getSession().getMessenger().getOwner().getEmail().getEmailAddress() + ">");
        req.setVia(
            "MSNSLP/1.0/TLP ;branch={A0D624A6-6C0C-4283-A9E0-BC97B4B46D32}");
        req.setCSeq(0);
        req.setCallId(fileTransfer.getReqMessage().getSlpMessage().getCallId());
        req.setMaxForwards(0);
        req.setContentType("application/x-msnmsgr-sessionclosebody");
        req.setBody(JmlConstants.LINE_SEPARATOR + "\0");

        // Get the size of the message to be setted
        int slpMessageLength = Charset.encodeAsByteArray(req.toString()).length;
        bye.setTotalLength(slpMessageLength);
        bye.setCurrentLength(slpMessageLength);

        OutgoingMSG[] outgoingOkMessages = bye.toOutgoingMsg(
            fileTransfer.getSession().getMessenger().getActualMsnProtocol());
        for (OutgoingMSG outgoingOkMessage : outgoingOkMessages)
        {
            if(switchboard != null)
                switchboard.send(outgoingOkMessage);
            else
                fileTransfer.getSession().sendAsynchronousMessage(outgoingOkMessage);
        }
    }

    private void sendDecline()
    {
        MsnslpMessage msnslpRequest =
            fileTransfer.getReqMessage().getSlpMessage();

        MsnslpResponse okSlpMessage = new MsnslpResponse();
        okSlpMessage.setStatusCode(603);
        okSlpMessage.setReasonPhrase("Decline");
        okSlpMessage.setTo(msnslpRequest.getFrom());
        okSlpMessage.setFrom(msnslpRequest.getTo());
        okSlpMessage.setVia(msnslpRequest.getVia());
        okSlpMessage.setCSeq(msnslpRequest.getCSeq() + 1);
        okSlpMessage.setCallId(msnslpRequest.getCallId());
        okSlpMessage.setMaxForwards(msnslpRequest.getMaxForwards());
        okSlpMessage.setContentType(msnslpRequest.getContentType());

        StringHolder body = new StringHolder();
        body.setProperty("SessionID", fileTransfer.getID());
        okSlpMessage.setBody(body.toString() + JmlConstants.LINE_SEPARATOR
                + "\0");

        int okSlpMessageLength = Charset.encodeAsByteArray(okSlpMessage
                .toString()).length;

        MsnP2PSlpMessage okMessage = new MsnP2PSlpMessage();
        okMessage.setSlpMessage(okSlpMessage);
        okMessage.setIdentifier(getNextIdentifier());
        okMessage.setTotalLength(okSlpMessageLength);
        okMessage.setCurrentLength(okSlpMessageLength);
        okMessage.setFlag(MsnP2PMessage.FLAG_OLD_NONE);
        okMessage.setField7(NumberUtils.getIntRandom());
        okMessage.setP2PDest(fileTransfer.getEmail().getEmailAddress());

        OutgoingMSG[] outgoingOkMessages = okMessage.toOutgoingMsg(
            fileTransfer.getSession().getMessenger().getActualMsnProtocol());
        for (OutgoingMSG outgoingOkMessage : outgoingOkMessages)
        {
            fileTransfer.getSession().sendSynchronousMessage(outgoingOkMessage);
        }
    }

    public void cancelFileTransfer()
    {
        if(fileTransfer.isSender())
        {
            sendBye();
        }
        else if(!fileTransfer.isSender() && !fileTransfer.isStarted())
        {
            sendDecline();
        }
        else
        {
            sendBye();
        }
    }

    /**
	 * Send a P2P ACK message for a received P2P message.
	 *
	 * @param msg Received P2P message.
	 * @param sessionIdFlag True if the session Id must be setted.
	 */
	public void sendP2PAck(MsnP2PMessage msg, boolean sessionIdFlag)
    {
		// Create the ACK message
		MsnP2PAckMessage ack = new MsnP2PAckMessage(
				getNextIdentifier(),
                fileTransfer.getEmail().getEmailAddress(),
				msg);
		if (sessionIdFlag)
        {
			ack.setSessionId(Integer.valueOf(fileTransfer.getID()));
		}

		// Send the message
		OutgoingMSG[] outgoingOkMessages = ack.toOutgoingMsg(
            fileTransfer.getSession().getMessenger().getActualMsnProtocol());

        for (OutgoingMSG outgoingOkMessage : outgoingOkMessages)
        {
            fileTransfer.getSession().sendSynchronousMessage(outgoingOkMessage);
        }
	}

    public void sendDeny(MsnP2PSlpMessage msg)
    {
        MsnslpRequest msnslpRequest = (MsnslpRequest)msg.getSlpMessage();
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
        body.setProperty("SessionID", fileTransfer.getID());
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
        okMessage.setP2PDest(fileTransfer.getEmail().getEmailAddress());

        OutgoingMSG[] outgoingOkMessages = okMessage.toOutgoingMsg(
            fileTransfer.getSession()
                .getMessenger().getActualMsnProtocol());
        for (OutgoingMSG outgoingOkMessage : outgoingOkMessages)
        {
            fileTransfer.getSession().sendSynchronousMessage(outgoingOkMessage);
        }
    }

    /**
     * @return the switchboard
     */
    public MsnSwitchboard getSwitchboard()
    {
        return switchboard;
    }

    /**
     * @param switchboard the switchboard to set
     */
    public void setSwitchboard(MsnSwitchboard switchboard)
    {
        this.switchboard = switchboard;
    }
}
