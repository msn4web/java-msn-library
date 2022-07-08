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
package net.sf.jml.impl;

import java.io.FileOutputStream;
import java.io.IOException;
import net.sf.jml.Email;
import net.sf.jml.MsnFileTransferState;
import net.sf.jml.message.p2p.FileTransferWorker;
import net.sf.jml.message.p2p.MsnP2PMessage;
import net.sf.jml.message.p2p.MsnP2PSlpMessage;
import net.sf.jml.protocol.MsnSession;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 *
 * @author Damian Minkov
 */
public class MsnFileTransferImpl
    extends AbstractFileTransfer
{
    private static final Log logger =
        LogFactory.getLog(MsnFileTransferImpl.class);

    private boolean started = false;
    private boolean sender = false;
    private MsnP2PSlpMessage reqMessage = null;
    private MsnSession session = null;
    private String sessionId = null;
    private Email email = null;

    public MsnFileTransferImpl(
        String sessionId,
        Email email,
        MsnP2PSlpMessage message,
        MsnSession session,
        boolean sender)
    {
        super(null, null);

        this.email = email;
        this.sessionId = sessionId;
        this.sender = sender;
        this.reqMessage = message;
        this.session = session;
    }

    public boolean isStarted()
    {
        return started;
    }

    public void start()
    {
        setState(MsnFileTransferState.ACCEPTED);

        FileTransferWorker worker = getSession().getMessenger()
            .getFileTransferManager().getFileTransferWorker(sessionId);

        worker.startFileTransfer();

        started = true;

        // fire file transfer started
        ((AbstractMessenger)getSession().getMessenger()).
            fireFileTransferStarted(this);
    }

    public void cancel()
    {
        cancel(true);
    }

    public void refuse()
    {
        setState(MsnFileTransferState.REFUSED);
        ((AbstractMessenger)getSession().getMessenger()).
                    fireFileTransferFinished(this);

        getSession().getMessenger().getFileTransferManager().
            removeFileTransfer(getID());
    }

    public void cancel(boolean sendNotify)
    {
        setState(MsnFileTransferState.CANCELED);

        if(sendNotify)
        {
            FileTransferWorker ftw =
                getSession().getMessenger()
                    .getFileTransferManager().getFileTransferWorker(sessionId);
            if(ftw != null)
                ftw.cancelFileTransfer();
        }

        ((AbstractMessenger)getSession().getMessenger()).
                    fireFileTransferFinished(this);

        getSession().getMessenger().getFileTransferManager().
            removeFileTransfer(getID());
    }

    public boolean isSender()
    {
        return sender;
    }

    public String getID()
    {
        return sessionId;
    }

    /**
     * Method to process new incoming data. Stores the data and fires
     * events for processing and if finished the final event.
     * Here we must remove the transfer from active ones if finished.
     *
     * @param bodyPart the current body bytes.
     * @param currentLength the current part length.
     * @param totalLength the total length to be processed.
     * @param offset the offset of the current data.
     */
    public synchronized void process(byte[] bodyPart, 
        int currentLength, long totalLength, long offset, MsnP2PMessage origMsg)
    {
        if(getState() == MsnFileTransferState.CANCELED)
            return;

        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(getFile(), true);
            out.write(bodyPart);
            out.flush();
            out.close();

            setTransferredSize(getTransferredSize() + currentLength);

            ((AbstractMessenger)getSession().getMessenger()).
                fireFileTransferProcess(this);

            if(getTransferredSize() == getFileTotalSize())
            {
                setState(MsnFileTransferState.COMPLETED);

                getSession().getMessenger().getFileTransferManager()
                    .getFileTransferWorker(sessionId).sendP2PAck(origMsg, true);

                ((AbstractMessenger)getSession().getMessenger()).
                    fireFileTransferFinished(this);

                getSession().getMessenger().getFileTransferManager().
                    removeFileTransfer(sessionId);
            }
        }
        catch (Exception ex)
        {
            logger.error("Cannot save to this file:" + getFile(), ex);
        }
        finally
        {
            try
            {
                out.close();
            }
            catch (IOException ex)
            {
                logger.error(null, ex);
            }
        }
    }

    /**
     * @return the session
     */
    public MsnSession getSession()
    {
        return session;
    }

    /**
     * @return the reqMessage
     */
    public MsnP2PSlpMessage getReqMessage()
    {
        return reqMessage;
    }

    /**
     * @return the email
     */
    public Email getEmail()
    {
        return email;
    }

    
}
