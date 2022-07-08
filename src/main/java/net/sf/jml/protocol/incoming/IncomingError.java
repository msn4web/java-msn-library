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
package net.sf.jml.protocol.incoming;

import net.sf.jml.MsnMessageChain;
import net.sf.jml.MsnMessageIterator;
import net.sf.jml.MsnProtocol;
import net.sf.jml.exception.MsnProtocolException;
import net.sf.jml.protocol.MsnIncomingMessage;
import net.sf.jml.protocol.MsnOutgoingMessage;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.util.NumberUtils;

import org.apache.commons.logging.*;

/**
 * Error message.
 * <p>
 * Supported Protocol: All
 * <p>
 * Syntax: errorcode trId
 * 
 * @author Roger Chen
 */
public class IncomingError extends MsnIncomingMessage {

    private static final Log logger = LogFactory.getLog(IncomingError.class);

    private boolean isChunkSupported = false;

    public IncomingError(MsnProtocol protocol) {
        super(protocol);

        switch(getErrorCode())
        {
            // error with code 241 supports chunk data
            case 241: isChunkSupported = true; break;
            case 508: isChunkSupported = true; break;
        }
    }

    protected boolean isSupportChunkData()
    {
        return isChunkSupported;
    }

    public int getErrorCode() {
        return NumberUtils.stringToInt(getCommand());
    }

    @Override
	protected void messageReceived(MsnSession session) {
        super.messageReceived(session);

        MsnMessageChain chain = session.getOutgoingMessageChain();
        int errorCode = getErrorCode();
        int trId = getTransactionId();

        if(isSupportChunkData() && getChunkData() != null)
            logger.trace("Error " + errorCode + ": " +
                    new String(getChunkData()));

        for (MsnMessageIterator iterator = chain.iterator(); iterator
                .hasPrevious();) {
            MsnOutgoingMessage message = (MsnOutgoingMessage) iterator
                    .previous();
            if (message.getTransactionId() == trId) {
                throw new MsnProtocolException(errorCode, this, message);
            }
        }
    }

}