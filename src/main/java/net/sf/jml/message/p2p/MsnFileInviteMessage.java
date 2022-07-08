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
import net.sf.jml.MsnContact;
import net.sf.jml.impl.AbstractMessenger;
import net.sf.jml.impl.MsnFileTransferImpl;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.msnslp.MsnslpRequest;
import net.sf.jml.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Damian Minkov
 */
public class MsnFileInviteMessage
    extends MsnP2PSlpMessage
{
    private static final Log logger =
        LogFactory.getLog(MsnFileInviteMessage.class);

	public static final String METHOD_INVITE = "INVITE";

    public static final String KEY_GUID_EUF = "EUF-GUID";
    public static final String KEY_CONTEXT = "Context";
    public static final String KEY_FROM = "From";

    public static final String GUID_EUF =
            "{5D3E02AB-6190-11D3-BBBB-00C04F795683}";

    public MsnFileInviteMessage()
    {
	}
    	
	@Override
	protected void messageReceived(MsnSession session, MsnContact contact)
    {
        if(!(getSlpMessage() instanceof MsnslpRequest))
        {
            logger.info("not a MsnslpRequest: " +
                getSlpMessage().getClass().getName());
            return;
        }

		// Get Slp message
        MsnslpRequest msnslpRequest = (MsnslpRequest) getSlpMessage();

        // Get a properties
        String method = msnslpRequest.getRequestMethod();
        String guid_euf = msnslpRequest.getBodys().getProperty(KEY_GUID_EUF);
        String context = msnslpRequest.getBodys().getProperty(KEY_CONTEXT);

        int sessionIdInt =
            getSlpMessage().getBodys().getIntProperty("SessionID", -1);
        String sessionId = String.valueOf(sessionIdInt);

        if (method != null && method.equals(METHOD_INVITE) &&
            msnslpRequest.getCSeq() == 0 &&
            guid_euf != null && guid_euf.equals(GUID_EUF) &&
	        context != null)
        {
            context = context.substring(0,context.length()-1);

            byte[] contextDecoded = Base64.decode(context);

            long fileSize = MsnFileContextParser.getFileSize(contextDecoded, 8);
            String fileName = null;
            try
            {
                fileName = MsnFileContextParser.getFileName(contextDecoded, 20);
            }
            catch (UnsupportedEncodingException ex)
            {
                logger.error("Cannot extract fileName", ex);
                return;
            }

            MsnFileTransferImpl fileTransfer =
                new MsnFileTransferImpl(sessionId, contact.getEmail(), this, session, false);
            fileTransfer.setContact(contact);

            fileTransfer.setFile(new File(fileName));
            fileTransfer.setFileTotalSize(fileSize);
            FileTransferWorker ftw = new FileTransferWorker(fileTransfer);

            session.getMessenger().getFileTransferManager().
                addFileTransfer(sessionId, ftw);

            ftw.sendP2PAck(this, false);

            ((AbstractMessenger)session.getMessenger()).
                fireFileTransferRequestReceived(fileTransfer);
        }
        else 
        {
            // we can receive second invite containing
            // some network properties for peer2peer connection
            // like ipv6 address
            // if not responded after 5 seconds a filetransfer
            // with msn relay will be received
            FileTransferWorker ftw = session.getMessenger().getFileTransferManager()
                .getFileTransferWorker(sessionId);

            if(ftw != null)
                ftw.sendDeny(this);
        	//super.messageReceived(session, contact);
        }
    }
}
