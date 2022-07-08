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

import net.sf.jml.*;
import net.sf.jml.impl.AbstractMessenger;
import net.sf.jml.impl.MsnContactImpl;
import net.sf.jml.protocol.MsnIncomingMessage;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.util.NumberUtils;
import net.sf.jml.util.StringUtils;

/**
 * The notice message that friends change their status.
 * <p>
 * Supported Protocol: All
 * <p>
 * MSNP8 Syntax: NLN userStatus email displayName clientId
 * <p>
 * MSNP9/MSNP10 Syntax: NLN userStatus email displayName clientId msnObject
 * <p>
 * MSNP13 Syntax: NLN userStatus email 1 displayName clientId msnObject
 * 
 * @author Roger Chen
 * @author Angel Barragán Chacón
 */
public class IncomingNLN extends MsnIncomingMessage {

	/**
	 * Create a new instance for this message.
	 * 
	 * @param protocol Instance of the protocol used for the message.
	 */
    public IncomingNLN(MsnProtocol protocol) {
        super(protocol);
    }

    @Override
	protected boolean isSupportTransactionId() {
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Retrieve the contact status.
     * 
     * @return Contact current status.
     */
    public MsnUserStatus getUserStatus() {
        return MsnUserStatus.parseStr(getParam(0));
    }

    /**
     * Retrieves the contact e-mail.
     * 
     * @return Email for the contact.
     */
    public Email getEmail() {
        return Email.parseStr(getParam(1));
    }

    /**
     * Retrieves the contact display name.
     * 
     * @return Conatact display name.
     */
    public String getDisplayName() {
        if (protocol.after(MsnProtocol.MSNP13)) {
            return StringUtils.urlDecode(getParam(3));
        }
        else {
            return StringUtils.urlDecode(getParam(2));
        }
    }

    /**
     * Retrieves the contact id.
     * 
     * @return Contact unique id.
     */
    public int getClientId() {
        if (protocol.after(MsnProtocol.MSNP14)) {
            return NumberUtils.stringToInt(getParam(4));
        }
        else {
            return NumberUtils.stringToInt(getParam(3));
        }
    }

    /**
     * Retrieves the MsnObject (avatar) for the contact.
     * 
     * @return MsnObject instance for the avatar of the contact.
     */
    public MsnObject getMsnObject() {
        String object = null;
        
        if(protocol.before(MsnProtocol.MSNP13))
            object = getParam(4);
        else
            object = getParam(5);
        
        if (object != null) {
            return MsnObject.parseMsnObject(StringUtils.urlDecode(object));
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    
    @Override
	protected void messageReceived(MsnSession session) {
        super.messageReceived(session);

        MsnContactImpl contact = (MsnContactImpl) session.getMessenger()
                .getContactList().getContactByEmail(getEmail());

        if (contact != null) {
            contact.setDisplayName(getDisplayName());
            contact.setClientId(MsnClientId.parseInt(getClientId()));
            contact.setStatus(getUserStatus());
            MsnObject obj = getMsnObject();
            if (obj != null) {
            	contact.setAvatar(obj);
            }
            ((AbstractMessenger) session.getMessenger())
                    .fireContactStatusChanged(contact);
        }
    }

}