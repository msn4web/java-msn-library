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
package net.sf.jml.protocol.outgoing;

import net.sf.jml.Email;
import net.sf.jml.MsnProtocol;
import net.sf.jml.protocol.MsnOutgoingMessage;

/**
 * Join a switchboard.
 * <p>
 * Supported Protocol: All
 * <p>
 * Syntax: ANS trId email authStr sessionId
 * 
 * @author Roger Chen
 */
public class OutgoingANS extends MsnOutgoingMessage {

    public OutgoingANS(MsnProtocol protocol) {
        super(protocol);
        setCommand("ANS");
    }

    public void setEmail(Email email) {
        setParam(0, email.toString());
    }

    public void setAuthStr(String authStr) {
        setParam(1, authStr);
    }

    public void setSessionId(String sessionId) {
        setParam(2, sessionId);
    }
}