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

import net.sf.jml.util.ByteBufferUtils;
import net.sf.jml.MsnContact;
import net.sf.jml.MsnProtocol;
import net.sf.jml.impl.AbstractMessenger;
import net.sf.jml.message.MessageConstants;
import net.sf.jml.message.MsnMimeMessage;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.outgoing.OutgoingMSG;
import net.sf.jml.util.Charset;
import net.sf.jml.util.JmlConstants;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import net.sf.jml.protocol.MsnIncomingMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Msn P2P message. have a binary header. See:
 * <a href="http://zoronax.bot2k3.net/msn6/msnp9/msnslp_p2p.html">http://zoronax.bot2k3.net/msn6/msnp9/msnslp_p2p.html</a> and
 * <a href="http://siebe.bot2k3.net/docs/?url=binheaders.html">http://siebe.bot2k3.net/docs/?url=binheaders.html</a>.
 * 
 * @author Roger Chen
 * @author Angel Barragán Chacón
 */
public abstract class MsnP2PMessage extends MsnMimeMessage {

	/**
	 * Loger for the class.
	 */
    private static final Log log = LogFactory.getLog(MsnSession.class);

    ////////////////////////////////////////////////////////////////////////////
    
	/**
	 * Creates a new P2P message.
	 */
	public MsnP2PMessage() {
		setContentType(MessageConstants.CT_P2P);
	}

	////////////////////////////////////////////////////////////////////////////
	
	@Override
	protected void parseMessage(byte[] message) {
		ByteBuffer split = Charset.encode(JmlConstants.LINE_SEPARATOR
				+ JmlConstants.LINE_SEPARATOR);
		int pos = ByteBufferUtils.indexOf(ByteBuffer.wrap(message), split);
		// header
		String header = pos == -1 ? Charset.decode(message) : Charset.decode(
				message, 0, pos);
		headers.parseString(header);
		// binaryHeader
		pos += split.remaining();
		binaryHeader.put(message, pos, BINARY_HEADER_LEN);
		binaryHeader.flip();
		// body
		pos += BINARY_HEADER_LEN;
		parseP2PBody(ByteBuffer.wrap(message, pos, message.length - pos
				- BINARY_FOOTER_LEN));

		// binaryFoot
		binaryFooter.put(message, message.length - BINARY_FOOTER_LEN,
				BINARY_FOOTER_LEN);
		binaryFooter.flip();
	}

	/**
	 * @see MsnMimeMessage#toOutgoingMsg(MsnProtocol)
	 */
	@Override
	public OutgoingMSG[] toOutgoingMsg(MsnProtocol protocol) {
		OutgoingMSG message = new OutgoingMSG(protocol)//;
        {
            protected void receivedResponse(MsnSession session, MsnIncomingMessage response)
            {
                MsnP2PMessage.this.receivedResponse(session, response);
            }
        };
		message.setMsgType(OutgoingMSG.TYPE_MSNC1);

		byte[] mimeMessageHeader = Charset.encodeAsByteArray(toString());

		byte[] body = bodyToMessage();
		if (body == null) {
			body = new byte[0];
		}

        // move to next data, as the body is the whole actual body
        // and we obey the length,currentLength and offset
        // if body is not the whole (length not equal to total length )
        // it means that external will take care fill the data
        if(getTotalLength() > getCurrentLength() && body.length == getTotalLength())
        {
            byte[] newBody = new byte[getCurrentLength()];
            System.arraycopy(body, (int)getOffset(), newBody, 0, newBody.length);
            body = newBody;
        }

		ByteBuffer msg = ByteBuffer
				.allocate(mimeMessageHeader.length + BINARY_HEADER_LEN
						+ body.length + BINARY_FOOTER_LEN);

		msg.put(mimeMessageHeader);
		msg.put(binaryHeader);
		msg.put(body);
		msg.put(binaryFooter);
		message.setMsg(msg.array());
		return new OutgoingMSG[] { message };
	}

	@Override
	protected void messageReceived(MsnSession session, MsnContact contact) {
		
		// Log the message
		//log.info("Received P2P message\n" + toDebugString());
		
		// Check for current transmision from this client.
		DisplayPictureDuel duel = session.getMessenger().
			getDisplayPictureDuelManager().get(this.getField7());
		if (duel != null) {
			duel.process(this, contact);
		}
		
		// It is not a transmision from this client so may be a retrieval
		else {
	        ((AbstractMessenger) session.getMessenger())
            .fireP2PMessageReceived(session.getSwitchboard(), 
            		                this,
                                    contact);
		}
	}

    protected void receivedResponse(MsnSession session, MsnIncomingMessage response)
    {
        
    }

	/**
	 * Parse the body part of this P2P message.
	 * 
	 * @param buffer Buffer with the body to be parsed.
	 */
	protected abstract void parseP2PBody(ByteBuffer buffer);
	
	/**
	 * Retrieve the body part for this P2P message.
	 * 
	 * @return Binary content for the body part of this P2P message.
	 */
	protected abstract byte[] bodyToMessage();

	/**
	 * Creates a debug representation for this P2P message.
	 * @return String representation for the message.
	 */
	public String toDebugString() {

		// Create a buffer
		StringBuffer result = new StringBuffer();
		
		// Add the message and headers
		result.append(toString());
		
		// Add the binary headers
		result.append("===================\n");
		result.append("=  Binary Headers =\n");
		result.append("===================\n");
		result.append("SessionID: ").append(getSessionId()).append('\n');
		result.append("Identifier: ").append(getIdentifier()).append(" (").append(toHex(getIdentifier())).append(")\n");
		result.append("Data Offset: ").append(getOffset()).append('\n');
		result.append("Data Total Size: ").append(getTotalLength()).append('\n');
		result.append("Message Length: ").append(getCurrentLength()).append('\n');
		result.append("Flag: ").append(toHex(getFlag())).append('\n');
		result.append("Ack Identifier: ").append(getField7()).append(" (").append(toHex(getField7())).append(")\n");
		result.append("Ack Unique  ID: ").append(getField8()).append(" (").append(toHex(getField8())).append(")\n");
		result.append("Ack Data  Size: ").append(getField9()).append('\n');
		
		// Add the body
		result.append("===================\n");
		result.append("=       Body      =\n");
		result.append("===================\n");
		result.append(toDebugBody());
		
		// Add the binary footer
		result.append("===================\n");
		result.append("=  Binary Footer  =\n");
		result.append("===================\n");
		result.append("AppID: ").append(getAppId()).append('\n');
		
		// Return the result
		return result.toString();
	}
	
	private String toHex(long value) {
		return "0x" + BigInteger.valueOf(value).toString(16);
	}
	
	/**
	 * Retrieves a String representation of the body for this message.
	 * 
	 * @return String value.
	 */
	protected String toDebugBody() {
		
		// Create the buffer
		StringBuffer buffer = new StringBuffer();
		
		// Add the body
		byte[] body = bodyToMessage();
		if (body == null) {
			return "";
		}
		buffer.append(Charset.decode(body));
		
		// Return the buffer
		return buffer.toString();
		
	}
	
	////////////////////////////////////////////////////////////////////////////
	//                                                                        //
	//                     Headers for the MSG message                        //
	//                                                                        //
	////////////////////////////////////////////////////////////////////////////

	/**
	 * P2P message destination header name.
	 */
	protected static final String KEY_P2P_DEST = "P2P-Dest";

	/**
     * P2P message source header name.
     * 
     * It seems never used for MSNP<16 ???
     * but WLM 2009 added this field, this causes
     * invitation/perhaps other P2P messages to fail (Sending DP fail) 
     * ~BLuEGoD 
     */
	
    protected static final String KEY_P2P_SRC = "P2P-Src";

    /**
     * Retrieve the P2P source header value.
     * 
     * @return Value for the source of this P2P message.
     */
    public String getP2PSrc() {
        return headers.getProperty(KEY_P2P_SRC);
    }
	
	/**
	 * Retrieve the P2P destination header value.
	 * 
	 * @return Value for the destination of this P2P message.
	 */
	public String getP2PDest() {
		return headers.getProperty(KEY_P2P_DEST);
	}

    /**
     * Sets the source for this P2P message.
     * 
     * @param src New source for this P2P message.
     */
    public void setP2PSrc(String src) {
        headers.setProperty(KEY_P2P_SRC, src);
    }
	
	/**
	 * Sets the destination for this P2P message.
	 * 
	 * @param dest New destination for this P2P message.
	 */
	public void setP2PDest(String dest) {
		headers.setProperty(KEY_P2P_DEST, dest);
	}

	
	////////////////////////////////////////////////////////////////////////////
	//                                                                        //
	//                           Binary Header Fields                         //
	//                                                                        //
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Length of the binary header for the P2P messages.
	 */
	protected static final int BINARY_HEADER_LEN = 48;

	/**
	 * Buffer for the binary header of this P2P message.
	 */
	protected final ByteBuffer binaryHeader =
		ByteBuffer.allocate(BINARY_HEADER_LEN).order(ByteOrder.LITTLE_ENDIAN);
	
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Retrieves the session identifier for this P2P message.
	 * 
	 * @return Session identifier. 
	 */
	public int getSessionId() {
		return binaryHeader.getInt(0);
	}
	
	/**
	 * Sets the session identifier for this P2P message.
	 * 
	 * @param sessionId New session identifier.
	 */
	public void setSessionId(int sessionId) {
		binaryHeader.putInt(0, sessionId);
	}

	////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the message identifier for this P2P message.
	 * 
	 * @return Message identifier.
	 */
	public int getIdentifier() {
		return binaryHeader.getInt(4);
	}
	
	/**
	 * Sets the new message identifier for this P2P message.
	 * 
	 * @param identifier New message identifier.
	 */
	public void setIdentifier(int identifier) {
		binaryHeader.putInt(4, identifier);
	}

	////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the data offset in this P2P message.
	 * 
	 * @return data offset. If this is a data message, this field has the offset 
	 * of the sending data with respect to the total data.
	 */
	public long getOffset() {
		return binaryHeader.getLong(8);
	}

	/**
	 * Sets the offset of the transmitted data for this P2P message.
	 * 
	 * @param offset New offset for the message.
	 */
	public void setOffset(long offset) {
		binaryHeader.putLong(8, offset);
	}

	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Retrieves the total length of the data (MsnObject) to be transmitted.
	 * 
	 * @return Total amount of bytes to be transmitted for the MsnObject.
	 */
	public long getTotalLength() {
		return binaryHeader.getLong(16);
	}

	/**
	 * Sets the total amount of data to be transmitted.
	 * 
	 * @param totalLength New total amount of data to be transmitted for the 
	 * MsnObject.
	 */
	public void setTotalLength(long totalLength) {
		binaryHeader.putLong(16, totalLength);
	}

	////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the current length of this message.
	 * 
	 * @return Current length for this message.
	 */
	public int getCurrentLength() {
		return binaryHeader.getInt(24);
	}

	/**
	 * Sets the current length for this message.
	 * 
	 * @param currentLength New current length for this message.
	 */
	public void setCurrentLength(int currentLength) {
		binaryHeader.putInt(24, currentLength);
	}

	////////////////////////////////////////////////////////////////////////////

	public static final int FLAG_NONE = 0x00;
    public static final int FLAG_OLD_NONE = 0x1000000; //Used in WLM2K9
	public static final int FLAG_ACK = 0x02;
	public static final int FLAG_BYE_ACK = 0x40;
	public static final int FLAG_DATA = 0x20;
    public static final int FLAG_OLD_DATA = 0x1000030;
	public static final int FLAG_BYE = 0x80;
	
	/**
	 * Retrieves the flag value for this P2P message.
	 * 
	 * @return Type of message.
	 */
	public int getFlag() {
		return binaryHeader.getInt(28);
	}

	/**
	 * Sets the new flag value for this P2P message.
	 * 
	 * @param flag Type of message.
	 */
	public void setFlag(int flag) {
		binaryHeader.putInt(28, flag);
	}

	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Retrieves the Acknowledged identifier for this P2P message.
	 * 
	 * @return the identifier.
	 */
	public int getField7() {
		return binaryHeader.getInt(32);
	}

	/**
	 * Sets the Acknowledged identifier for this message.
	 * 
	 * @param field7 The identifier.
	 */
	public void setField7(int field7) {
		binaryHeader.putInt(32, field7);
	}

	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Retrieves the Acknowledged unique ID for this message.
	 * 
	 * @return the identifier.
	 */
	public int getField8() {
		return binaryHeader.getInt(36);
	}

	/**
	 * Sets the Acknowledged unique ID for this message.
	 * 
	 * @param field8 The identifier.
	 */
	public void setField8(int field8) {
		binaryHeader.putInt(36, field8);
	}

	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Retrieves the Acknowledged data size for this P2P message.
	 * 
	 * @return The data size.
	 */
	public long getField9() {
		return binaryHeader.getLong(40);
	}

	/**
	 * Sets the Acknowledged data size for this P2P message.
	 * 
	 * @param field9 The size.
	 */
	public void setField9(long field9) {
		binaryHeader.putLong(40, field9);
	}

	
	////////////////////////////////////////////////////////////////////////////
	//                                                                        //
	//                           Binary Footer Fields                         //
	//                                                                        //
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Binary footer length
	 */
	protected static final int BINARY_FOOTER_LEN = 4;

	/**
	 * Buffer for the binary footer of this P2P message.
	 */
	private final ByteBuffer binaryFooter = 
		ByteBuffer.allocate(BINARY_FOOTER_LEN);
	
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Retrieves the application identifier for this P2P message.
	 * 
	 * @return Identifier.
	 */
	public int getAppId() {
		return binaryFooter.getInt(0);
	}
	
	/**
	 * Sets the application identifier for this P2P message.
	 * 
	 * @param appId New application identifier.
	 */
	public void setAppId(int appId) {
		binaryFooter.putInt(0, appId);
	}


}
