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
package net.sf.jml.protocol;

import net.sf.jml.net.Message;
import net.sf.jml.net.Session;
import net.sf.jml.net.SessionAdapter;
import net.sf.jml.net.SessionListener;
import net.sf.jml.MsnMessageChain;
import net.sf.jml.MsnMessageIterator;
import net.sf.jml.MsnMessenger;
import net.sf.jml.MsnSwitchboard;
import net.sf.jml.impl.AbstractMessenger;
import net.sf.jml.impl.MsnMessageChainImpl;
import net.sf.jml.protocol.incoming.IncomingQNG;
import net.sf.jml.util.JmlConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.SocketAddress;
import net.sf.jml.protocol.soap.*;

/**
 * Msn Session. Support write and read MsnMessage.
 *
 * @author Roger Chen
 */
public final class MsnSession {

    private static final Log log = LogFactory.getLog(MsnSession.class);

    private final Session session = new Session();

    private SSO sso = null;
    private ContactList contactList = null;

    private final MsnMessenger messenger;
    private final MsnSwitchboard switchboard;

    private final TransactionId trId = new TransactionId();
    private final MsnMessageChainImpl outgoingChain = new MsnMessageChainImpl(
            JmlConstants.MESSAGE_CHAIN_LENGTH);
    private final MsnMessageChainImpl incomingChain = new MsnMessageChainImpl(
            JmlConstants.MESSAGE_CHAIN_LENGTH);

    public MsnSession(final MsnMessenger messenger, SocketAddress address) {
        this.messenger = messenger;
        this.switchboard = null;
        session.setSocketAddress(address);
        init();
    }

    public MsnSession(final MsnSwitchboard switchboard, SocketAddress address) {
        this.messenger = switchboard.getMessenger();
        this.switchboard = switchboard;
        session.setSocketAddress(address);
        init();
    }

    private void init() {
        session.setAttachment(this);
        session.setMessageRecognizer(MsnMessageRecognizer.getInstance());
        session.addSessionListener(new SessionAdapter() {

            private String getConnectionType() {
                return switchboard == null ? "NS" : "SB (" + switchboard.getAttachment() + ")" ;
            }

            @Override
			public void messageReceived(Session session, Message message) {
                if (messenger.isLogIncoming()) {
                    log.info(messenger.getOwner().getEmail() + " "
                            + getConnectionType() + " <<< "
                            + message.toString());
                }

                MsnIncomingMessage incoming = (MsnIncomingMessage) ((WrapperMessage) message)
                        .getMessage();
                incomingChain.addMsnMessage(incoming);
                if (incoming.getTransactionId() > 0
                        || (incoming instanceof IncomingQNG)) {
                    int trId = incoming.getTransactionId();
                    for (MsnMessageIterator iter = outgoingChain.iterator(); iter
                            .hasPrevious();) {
                        MsnOutgoingMessage outgoing = (MsnOutgoingMessage) iter
                                .previous();
                        if (outgoing.getTransactionId() == trId) {
                            incoming.setOutgoingMessage(outgoing);
                            break;
                        }
                    }
                }
                try {
                    incoming.messageReceived(MsnSession.this);
                } catch (Exception e) { //Protect catch
                    exceptionCaught(session, e);
                }

                if (incoming.getOutgoingMessage() != null)
                    try {
                        incoming.getOutgoingMessage().receivedResponse(
                                MsnSession.this, incoming);
                    } catch (Exception e) { //Protect catch
                        exceptionCaught(session, e);
                    }
            }

            @Override
			public void messageSent(Session session, Message message) {
                if (messenger.isLogOutgoing()) {
                    log.info(messenger.getOwner().getEmail() + " "
                            + getConnectionType() + " >>> "
                            + message.toString());
                }

                MsnOutgoingMessage outgoing = (MsnOutgoingMessage) ((WrapperMessage) message)
                        .getMessage();
                outgoingChain.addMsnMessage(outgoing);
                try {
                    outgoing.messageSent(MsnSession.this);
                } catch (Exception e) { //Protect catch
                    exceptionCaught(session, e);
                }
            }

            @Override
			public void exceptionCaught(Session session, Throwable cause) {
                ((AbstractMessenger) messenger).fireExceptionCaught(cause);
            }
        });
    }

    public MsnMessenger getMessenger() {
        return messenger;
    }

    public MsnSwitchboard getSwitchboard() {
        return switchboard;
    }

    public String getLocalAddress() {
        if (session.getSocket() != null)
            return session.getSocket().getLocalAddress()
                    .getHostAddress();
        return null;
    }

    public int getLocalPort() {
        if (session.getSocket() != null)
            return session.getSocket().getLocalPort();
        return -1;
    }

    public boolean isAvailable() {
        return session.isAvailable();
    }

    public void start() {
        session.start(false);
    }

    public void close() {
        session.close(false);
    }

    public void sendAsynchronousMessage(MsnOutgoingMessage message) {
        if (message != null) {
            if (message.isSupportTransactionId())
                message.setTransactionId(trId.nextTransactionId());
            session.write(new WrapperMessage(message));
        }
    }

    public boolean sendSynchronousMessage(MsnOutgoingMessage message) {
        if (message != null) {
            if (message.isSupportTransactionId())
                message.setTransactionId(trId.nextTransactionId());
            return session.blockWrite(new WrapperMessage(message));
        }
        return false;
    }

    public void addSessionListener(SessionListener listener) {
        session.addSessionListener(listener);
    }

    public void removeSessionListener(SessionListener listener) {
        session.removeSessionListener(listener);
    }

    public MsnMessageChain getOutgoingMessageChain() {
        return outgoingChain;
    }

    public MsnMessageChain getIncomingMessageChain() {
        return incomingChain;
    }

    public void setSessionTimeout(int timeout) {
        session.setSessionTimeout(timeout);
    }

    public SSO getSSO()
    {
        return sso;
    }

    public SSO createSSO(String username, String pass, String policy, String nonce)
    {
        sso = new SSO(username, pass, policy, nonce);
        contactList = new ContactList(this);
        return sso;
    }

    public ContactList getContactList()
    {
        return contactList;
    }
}
