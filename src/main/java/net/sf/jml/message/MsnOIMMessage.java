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

import java.nio.ByteBuffer;
import net.sf.jml.MsnContact;
import net.sf.jml.impl.AbstractMessenger;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.soap.OIM;
import net.sf.jml.util.Charset;
import net.sf.jml.util.StringHolder;

/**
 *
 * @author Damian Minkov
 */
public class MsnOIMMessage 
    extends MsnMimeMessage
{
    private static final String KEY_MAIL_DATA = "Mail-Data";
    
    protected final StringHolder bodykeys = new StringHolder();
    
    @Override
	protected void messageReceived(MsnSession session, MsnContact contact) 
    {
        super.messageReceived(session, contact);
                
        String mail = bodykeys.getProperty(KEY_MAIL_DATA);
        new OIM(session).retrieveCurrentOfflineMessages(mail);
        
        
        //((AbstractMessenger) session.getMessenger())
        //        .fireInitialEmailDataReceived(session.getSwitchboard(), this,
        //                contact);
    }
    
    @Override
	protected void parseBuffer(ByteBuffer buffer) {
        super.parseBuffer(buffer);
        bodykeys.parseString(Charset.decode(ByteBuffer.wrap(buffer.array())));
    }
}
