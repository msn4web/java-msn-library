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
public final class MsnEmailActivityMessage extends MsnMimeMessage {

    private static final String KEY_SRC_FOLDER = "Src-Folder";
    private static final String KEY_DEST_FOLDER = "Dest-Folder";
    private static final String KEY_MESSAGE_DELTA = "Message-Delta";

    protected final StringHolder bodykeys = new StringHolder();

    private String srcFolder;
    private String destFolder;
    private Integer messageDelta;

    public MsnEmailActivityMessage() {
        setContentType(MessageConstants.CT_REALTIME_EMAIL_NOTIFY + MessageConstants.CHARSET);
    }

    public String getSrcFolder() {
        return srcFolder;
    }

    public void setSrcFolder(String srcFolder) {
        this.srcFolder = srcFolder;
    }

    public String getDestFolder() {
        return destFolder;
    }

    public void setDestFolder(String destFolder) {
        this.destFolder = destFolder;
    }

    public Integer getMessageDelta() {
        return messageDelta;
    }

    public void setMessageDelta(Integer messageDelta) {
        this.messageDelta = messageDelta;
    }

    @Override
	protected void messageReceived(MsnSession session, MsnContact contact) {
        super.messageReceived(session, contact);

        ((AbstractMessenger) session.getMessenger())
                .fireEmailActivityNotificationReceived(session.getSwitchboard(), this,
                        contact);
    }

    @Override
	protected void parseMessage(byte[] message) {
        super.parseMessage(message);
        setSrcFolder(bodykeys.getProperty(KEY_SRC_FOLDER));
        setDestFolder(bodykeys.getProperty(KEY_DEST_FOLDER));
        setMessageDelta(bodykeys.getIntProperty(KEY_MESSAGE_DELTA));
    }

    @Override
	protected void parseBuffer(ByteBuffer buffer) {
        super.parseBuffer(buffer);
        bodykeys.parseString(Charset.decode(ByteBuffer.wrap(buffer.array())));
    }

}
