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
public final class MsnEmailNotifyMessage extends MsnMimeMessage {

    private static final String KEY_FROM = "From";
    private static final String KEY_MESSAGE_URL = "Message-URL";
    private static final String KEY_POST_URL = "Post-URL";
    private static final String KEY_SUBJECT = "Subject";
    private static final String KEY_DEST_FOLDER = "Dest-Folder";
    private static final String KEY_FROM_ADDR = "From-Addr";
    private static final String KEY_ID = "Id";

    protected final StringHolder bodykeys = new StringHolder();

    private String from;
    private String messageURL;
    private String postURL;
    private String subject;
    private String destFolder;
    private String fromAddr;
    private Integer id;

    public MsnEmailNotifyMessage() {
        setContentType(MessageConstants.CT_ACTIVE_EMAIL_NOTIFY + MessageConstants.CHARSET);
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMessageURL() {
        return messageURL;
    }

    public void setMessageURL(String messageURL) {
        this.messageURL = messageURL;
    }

    public String getPostURL() {
        return postURL;
    }

    public void setPostURL(String postURL) {
        this.postURL = postURL;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDestFolder() {
        return destFolder;
    }

    public void setDestFolder(String destFolder) {
        this.destFolder = destFolder;
    }

    public String getFromAddr() {
        return fromAddr;
    }

    public void setFromAddr(String fromAddr) {
        this.fromAddr = fromAddr;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
	protected void messageReceived(MsnSession session, MsnContact contact) {
        super.messageReceived(session, contact);

        ((AbstractMessenger) session.getMessenger())
                .fireNewEmailNotificationReceived(session.getSwitchboard(), this,
                        contact);
    }

    @Override
	protected void parseMessage(byte[] message) {
        super.parseMessage(message);
        setFrom(bodykeys.getProperty(KEY_FROM));
        setMessageURL(bodykeys.getProperty(KEY_MESSAGE_URL));
        setPostURL(bodykeys.getProperty(KEY_POST_URL));
        setSubject(bodykeys.getProperty(KEY_SUBJECT));
        setDestFolder(bodykeys.getProperty(KEY_DEST_FOLDER));
        setFromAddr(bodykeys.getProperty(KEY_FROM_ADDR));
        setId(bodykeys.getIntProperty(KEY_ID));
    }

    @Override
	protected void parseBuffer(ByteBuffer buffer) {
        super.parseBuffer(buffer);
        bodykeys.parseString(Charset.decode(ByteBuffer.wrap(buffer.array())));
    }

}
