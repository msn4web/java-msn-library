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
package net.sf.jml.protocol.incoming;

import net.sf.jml.Email;
import net.sf.jml.MsnProtocol;
import net.sf.jml.protocol.MsnIncomingMessage;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.outgoing.OutgoingUUN;

/**
 * Example <SNM opcode="SNM" csid="{7F5AEEDC-74A7-4C98-5DEF-C8FF30C7781C}"/>
 * SNM means that the clients are still in negotiation period ("Share New Media")
 * 
 * <p>
 * Supported Protocol: MSNP13
 * <p>
 * @author Damian Minkov
 */
public class IncomingUBN
    extends MsnIncomingMessage
{
    public IncomingUBN(MsnProtocol protocol)
    {
        super(protocol);
    }

    @Override
    protected boolean isSupportTransactionId()
    {
        return false;
    }

    @Override
    protected boolean isSupportChunkData()
    {
        return true;
    }

    public Email getEmail()
    {
        return Email.parseStr(getParam(0));
    }

    @Override
    protected void messageReceived(MsnSession session)
    {
        super.messageReceived(session);
//        OutgoingUUN m = new OutgoingUUN(protocol);
//        // will deny for now
//        m.setData("{F26D1F07-95E2-403C-BC18-D4BFED493428}", false);
//        session.getMessenger().send(m);
    }
}
