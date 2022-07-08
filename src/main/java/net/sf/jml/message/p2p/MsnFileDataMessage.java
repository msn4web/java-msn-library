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

import net.sf.jml.util.NumberUtils;
import net.sf.jml.util.StringUtils;

import java.nio.ByteBuffer;
import net.sf.jml.MsnContact;
import net.sf.jml.impl.MsnFileTransferImpl;
import net.sf.jml.protocol.MsnSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Damian Minkov
 */
public class MsnFileDataMessage 
    extends MsnP2PMessage
{
    private static final Log logger =
        LogFactory.getLog(MsnFileDataMessage.class);

    public final static int MAX_DATA_LENGTH = 1202;

    private byte[] body;

    public MsnFileDataMessage()
    {
    	setFlag(FLAG_OLD_DATA);
    }

    public MsnFileDataMessage(int sessionId, int identifier, int offset,
                             int totalLength, byte[] data, String p2pDest)
    {
        this.body = data;
        setP2PDest(p2pDest);
        setSessionId(sessionId);
        setIdentifier(identifier);
        setOffset(offset);
        setTotalLength(totalLength);
        setCurrentLength(body.length);
        setFlag(FLAG_DATA);
        setField7(NumberUtils.getIntRandom());
        setAppId(1);
    }

    @Override
	protected byte[] bodyToMessage() {
        return body;
    }

    @Override
	protected void parseP2PBody(ByteBuffer buffer) {
        body = new byte[this.getCurrentLength()];
        buffer.get(body);
    }

	protected String toDebugBody() {
		return StringUtils.debug(ByteBuffer.wrap(body));
	}

    @Override
	protected void messageReceived(MsnSession session, MsnContact contact)
    {
        MsnFileTransferImpl ft = (MsnFileTransferImpl)session.getMessenger().
            getFileTransferManager().
                getFileTransfer(String.valueOf(getSessionId()));

        // find file transfer put the data and there fire that
        // file is being processed
        if(ft != null)
            ft.process(
                body, getCurrentLength(), getTotalLength(), getOffset(), this);
        else
            logger.error("Unknown filetransfer!");
    }

}
