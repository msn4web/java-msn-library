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
package net.sf.jml.message;

import net.sf.jml.MsnContact;
import net.sf.jml.impl.AbstractMessenger;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.util.Charset;
import net.sf.jml.util.StringHolder;

import java.nio.ByteBuffer;

/**
 * @author Daniel Henninger
 */
public final class MsnEmailInitMessage extends MsnMimeMessage {

    private static final String KEY_INBOX_UNREAD = "Inbox-Unread";
    private static final String KEY_FOLDERS_UNREAD = "Folders-Unread";
    private static final String KEY_INBOX_URL = "Inbox-URL";
    private static final String KEY_FOLDERS_URL = "Folders-URL";
    private static final String KEY_POST_URL = "Post-URL";

    protected final StringHolder bodykeys = new StringHolder();

    private Integer inboxUnread;
    private Integer foldersUnread;
    private String inboxURL;
    private String foldersURL;
    private String postURL;

    public MsnEmailInitMessage() {
        setContentType(MessageConstants.CT_INIT_EMAIL_NOTIFY + MessageConstants.CHARSET);
    }


    public Integer getInboxUnread() {
        return inboxUnread;
    }

    public void setInboxUnread(Integer inboxUnread) {
        this.inboxUnread = inboxUnread;
    }

    public Integer getFoldersUnread() {
        return foldersUnread;
    }

    public void setFoldersUnread(Integer foldersUnread) {
        this.foldersUnread = foldersUnread;
    }

    public String getInboxURL() {
        return inboxURL;
    }

    public void setInboxURL(String inboxURL) {
        this.inboxURL = inboxURL;
    }

    public String getFoldersURL() {
        return foldersURL;
    }

    public void setFoldersURL(String foldersURL) {
        this.foldersURL = foldersURL;
    }

    public String getPostURL() {
        return postURL;
    }

    public void setPostURL(String postURL) {
        this.postURL = postURL;
    }

    @Override
	protected void messageReceived(MsnSession session, MsnContact contact) {
        super.messageReceived(session, contact);

        ((AbstractMessenger) session.getMessenger())
                .fireInitialEmailNotificationReceived(session.getSwitchboard(), this,
                        contact);
    }

    @Override
	protected void parseMessage(byte[] message) {
        super.parseMessage(message);
        setInboxUnread(bodykeys.getIntProperty(KEY_INBOX_UNREAD));
        setFoldersUnread(bodykeys.getIntProperty(KEY_FOLDERS_UNREAD));
        setInboxURL(bodykeys.getProperty(KEY_INBOX_URL));
        setFoldersURL(bodykeys.getProperty(KEY_FOLDERS_URL));
        setPostURL(bodykeys.getProperty(KEY_POST_URL));
    }

    @Override
	protected void parseBuffer(ByteBuffer buffer) {
        super.parseBuffer(buffer);
        bodykeys.parseString(Charset.decode(ByteBuffer.wrap(buffer.array())));
    }
    
}
