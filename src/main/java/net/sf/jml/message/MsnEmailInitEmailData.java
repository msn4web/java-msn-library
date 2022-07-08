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
public final class MsnEmailInitEmailData extends MsnMimeMessage {

    private static final String KEY_MAIL_DATA = "Mail-Data";
    private static final String KEY_INBOX_URL = "Inbox-URL";
    private static final String KEY_FOLDERS_URL = "Folders-URL";
    private static final String KEY_POST_URL = "Post-URL";

    protected final StringHolder bodykeys = new StringHolder();

    private Integer inboxAll;
    private Integer inboxUnread;
    private Integer foldersAll;
    private Integer foldersUnread;
    private String inboxURL;
    private String foldersURL;
    private String postURL;

    public MsnEmailInitEmailData() {
        setContentType(MessageConstants.CT_INIT_MAIL_DATA_NOTIFY + MessageConstants.CHARSET);
    }


    public Integer getInboxUnread() {
        return inboxUnread;
    }

    public void setInboxUnread(Integer inboxUnread) {
        this.inboxUnread = inboxUnread;
    }

    public Integer getInboxAll() {
        return inboxAll;
    }

    public void setInboxAll(Integer inboxAll) {
        this.inboxAll = inboxAll;
    }

    public Integer getFoldersUnread() {
        return foldersUnread;
    }

    public void setFoldersUnread(Integer foldersUnread) {
        this.foldersUnread = foldersUnread;
    }

    public Integer getFoldersAll() {
        return foldersAll;
    }

    public void setFoldersAll(Integer foldersAll) {
        this.foldersAll = foldersAll;
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
                .fireInitialEmailDataReceived(session.getSwitchboard(), this,
                        contact);

        // can start retreiving offline messages from here.
        // but left it to the user to decide when and whether he wants them
        // new OIM(session).getMessages(oimIds);
    }

    @Override
	protected void parseMessage(byte[] message) {
        super.parseMessage(message);
        String mail = bodykeys.getProperty(KEY_MAIL_DATA);
        int inbox_unread = 0;
        int inbox_all = 0;
        int folders_unread = 0;
        int folders_all = 0;
        if (mail.indexOf("<I>") > 0 && mail.indexOf("</I>") > 0) {
            inbox_all = Integer.parseInt(mail.substring(mail.indexOf("<I>") + 3, mail.indexOf("</I>")));
        }

        if (mail.indexOf("<IU>") > 0 && mail.indexOf("</IU>") > 0) {
            inbox_unread = Integer.parseInt(mail.substring(mail.indexOf("<IU>") + 4, mail.indexOf("</IU>")));
        }

        if (mail.indexOf("<O>") > 0 && mail.indexOf("</O>") > 0) {
            folders_all = Integer.parseInt(mail.substring(mail.indexOf("<O>") + 3, mail.indexOf("</O>")));
        }

        if (mail.indexOf("<OU>") > 0 && mail.indexOf("</OU>") > 0) {
            folders_unread = Integer.parseInt(mail.substring(mail.indexOf("<OU>") + 4, mail.indexOf("</OU>")));
        }
        setInboxUnread(inbox_unread);
        setInboxAll(inbox_all);
        setFoldersUnread(folders_unread);
        setFoldersAll(folders_all);
        setInboxURL(bodykeys.getProperty(KEY_INBOX_URL));
        setFoldersURL(bodykeys.getProperty(KEY_FOLDERS_URL));
        setPostURL(bodykeys.getProperty(KEY_POST_URL));
        
        
        // here can be offline messages
//        String[] r = OIM.parseMdata(mail);
    }
    
    @Override
	protected void parseBuffer(ByteBuffer buffer) {
        super.parseBuffer(buffer);
        bodykeys.parseString(Charset.decode(ByteBuffer.wrap(buffer.array())));
    }
    
}
