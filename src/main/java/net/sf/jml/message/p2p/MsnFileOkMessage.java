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

import net.sf.jml.MsnContact;
import net.sf.jml.MsnFileTransfer;
import net.sf.jml.impl.MsnFileTransferImpl;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.msnslp.MsnslpResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Damian Minkov
 */
public class MsnFileOkMessage
    extends MsnP2PSlpMessage
{
    private static final Log logger = LogFactory.getLog(MsnFileOkMessage.class);

    public MsnFileOkMessage()
    {
	}
    	
	@Override
	protected void messageReceived(MsnSession session, MsnContact contact)
    {
        MsnslpResponse msnslpResponse = (MsnslpResponse) getSlpMessage();
        String sessionID = msnslpResponse.getBodys().getProperty("SessionID");

        if(sessionID == null)
            return;

        MsnFileTransferImpl ft = (MsnFileTransferImpl)session.getMessenger().
            getFileTransferManager().getFileTransfer(sessionID);

        if(ft == null)
            return;

        ft.setContact(contact);
        ft.start();
    }
}
