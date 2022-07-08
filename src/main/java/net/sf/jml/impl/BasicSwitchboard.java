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

import java.net.InetSocketAddress;

import net.sf.jml.net.Session;
import net.sf.jml.net.SessionAdapter;
import net.sf.jml.net.SessionListener;
import net.sf.jml.MsnConnection;
import net.sf.jml.MsnFileTransfer;
import net.sf.jml.MsnMessageChain;
import net.sf.jml.message.MsnMimeMessage;
import net.sf.jml.net.Message;
import net.sf.jml.protocol.MsnOutgoingMessage;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.outgoing.OutgoingANS;
import net.sf.jml.protocol.outgoing.OutgoingMSG;
import net.sf.jml.protocol.outgoing.OutgoingUSRAuthSB;

/**
 * basic MsnSwitchboard implement, communication with MSN SB.
 * 
 * @author Roger Chen
 */
public abstract class BasicSwitchboard extends AbstractSwitchboard {

    private final MsnSession session;
    private final MsnConnectionImpl connection = new MsnConnectionImpl();
    private final boolean createdByOwner;

    private String authStr;
    private int sessionId;

    // 5 minutes
    private final int SESSION_ACTIVITY_TIMEOUT = 5 * 60 * 1000;
    private long lastActivityUpdate = 0;

    private boolean closeOnIdle = true;

    public final static String CLOSE_ON_IDLE_PROP =
        "net.sf.jml.impl.CLOSE_ON_IDLE_PROP";

    public BasicSwitchboard(BasicMessenger messenger, boolean createdByOwner,
            String ip, int port) {
        super(messenger);

        this.createdByOwner = createdByOwner;

        connection.setRemoteIP(ip);
        connection.setRemotePort(port);
        connection.setConnectionType(messenger.getConnection()
                .getConnectionType());
        session = new MsnSession(this, new InetSocketAddress(ip, port));
        session.addSessionListener(new SBSessionListener());

        String closeOnIdleProp = System.getProperty(CLOSE_ON_IDLE_PROP);
        if(closeOnIdleProp != null && !Boolean.parseBoolean(closeOnIdleProp))
            closeOnIdle = false;
    }

    public MsnConnection getConnection() {
        return connection;
    }

    public synchronized void close() {
        MsnFileTransfer[] trans = getActiveFileTransfers();
        for (MsnFileTransfer tran : trans) {
            tran.cancel();
        }
        session.close();
    }

    public MsnMessageChain getIncomingMessageChain() {
        if (session == null)
            return null;
        return session.getIncomingMessageChain();
    }

    public MsnMessageChain getOutgoingMessageChain() {
        if (session == null)
            return null;
        return session.getOutgoingMessageChain();
    }

    public boolean sendMessage(MsnMimeMessage message, boolean block) {
        if (message != null) {
            OutgoingMSG[] outgoing = message.toOutgoingMsg(getMessenger()
                    .getActualMsnProtocol());
            if (outgoing != null) {
                for (OutgoingMSG anOutgoing : outgoing) {
                    if (!send(anOutgoing, block) && block)
                        return false;
                }
                if (block)
                    return true;
            }
        }
        return false;
    }

    public boolean send(MsnOutgoingMessage message, boolean block)
    {
        // update the timeout on activity, cause we are tracking the
        // lack of activity
        if(closeOnIdle)
        {
            synchronized(this)
            {
                lastActivityUpdate = System.currentTimeMillis();
                session.setSessionTimeout(SESSION_ACTIVITY_TIMEOUT);
            }
        }

        if (block) {
            return session.sendSynchronousMessage(message);
        }

        session.sendAsynchronousMessage(message);
        return false;
    }

    @Override
	public String toString() {
        return "MsnSwitchboard: " + getMessenger().getOwner().getEmail();
    }

    public void setAuthStr(String authStr) {
        this.authStr = authStr;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public synchronized void start() {
        session.start();
    }

    public void addSessionListener(SessionListener listener) {
        session.addSessionListener(listener);
    }
    
    public void removeSessionListener(SessionListener listener) {
    	session.removeSessionListener(listener);
    }

    private class SBSessionListener extends SessionAdapter {

        @Override
		public void sessionEstablished(Session session) {
            if (createdByOwner) {
                OutgoingUSRAuthSB message = new OutgoingUSRAuthSB(
                        getMessenger().getActualMsnProtocol());
                message.setEmail(getMessenger().getOwner().getEmail());
                message.setAuthStr(authStr);
                send(message, false);
            } else {
                OutgoingANS message = new OutgoingANS(getMessenger()
                        .getActualMsnProtocol());
                message.setEmail(getMessenger().getOwner().getEmail());
                message.setAuthStr(authStr);
                message.setSessionId(sessionId);
                send(message, false);
            }
            connection.setInternalIP(BasicSwitchboard.this.session
                    .getLocalAddress());
            connection.setInternalPort(BasicSwitchboard.this.session
                    .getLocalPort());
        }

        @Override
		public void sessionTimeout(Session session)
        {
            if(closeOnIdle)
            {
                synchronized(this)
                {
                    // if we have not an activity for the period
                    // close the switchboard cause there maybe socket exceptions
                    // otherwise this were just updates in the session timer
                    // and we ignore them
                    if(System.currentTimeMillis() - lastActivityUpdate
                        >= SESSION_ACTIVITY_TIMEOUT)
                    {
                        session.close();
                    }
                }
            }
        }

        @Override
		public void sessionClosed(Session session) {
            ((AbstractMessenger) getMessenger())
                    .fireSwitchboardClosed(BasicSwitchboard.this);
        }

        @Override
        public void messageReceived(Session session, Message message)
            throws Exception
        {
            // if close on idle is activated
            // check whether this session is fo multi chat and if so
            // disable close on idle, making problems in multi chat
            if(closeOnIdle)
                closeOnIdle = !(getAllContacts().length > 1);
        }

        @Override
        public void messageSent(Session session, Message message)
            throws Exception
        {
            // if close on idle is activated
            // check whether this session is fo multi chat and if so
            // disable close on idle, making problems in multi chat
            if(closeOnIdle)
                closeOnIdle = !(getAllContacts().length > 1);
        }
    }
}