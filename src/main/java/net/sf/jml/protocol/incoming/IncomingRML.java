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

import net.sf.jml.MsnGroup;
import net.sf.jml.MsnList;
import net.sf.jml.MsnMessageChain;
import net.sf.jml.MsnMessageIterator;
import net.sf.jml.MsnProtocol;
import net.sf.jml.impl.AbstractMessenger;
import net.sf.jml.impl.MsnContactImpl;
import net.sf.jml.impl.MsnContactListImpl;
import net.sf.jml.protocol.MsnIncomingMessage;
import net.sf.jml.protocol.MsnOutgoingMessage;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.outgoing.OutgoingRML;

/**
 * MSN13
 * Remove contact 
 * 
 * @author Damian Minkov
 */
public class IncomingRML
    extends MsnIncomingMessage
{

    public IncomingRML(MsnProtocol protocol)
    {
        super(protocol);
    }

    @Override
    protected void messageReceived(MsnSession session)
    {
        super.messageReceived(session);

        MsnContactListImpl contactList =
            (MsnContactListImpl) session.getMessenger().getContactList();

        MsnMessageChain chain = session.getOutgoingMessageChain();
        int trId = getTransactionId();

        for (MsnMessageIterator iterator = chain.iterator(); iterator.hasPrevious();)
        {
            MsnOutgoingMessage message = (MsnOutgoingMessage) iterator.previous();
            if (message.getTransactionId() == trId)
            {
                MsnContactImpl contact = (MsnContactImpl)((OutgoingRML) message).getContact();

                MsnList list = ((OutgoingRML) message).getList();

                contact.setInList(list, false);
                if (contact.getListNumber() == 0)
                { //Not in any group, delete from contact list
                    contactList.removeContactByEmail(contact.getEmail());
                }
                if (list == MsnList.FL)
                { //In FL, remove user from the group.
                    try
                    {
                        MsnGroup[] gs = contact.getBelongGroups();
                        for (int i = 0; i < gs.length; i++)
                        {
                            MsnGroup msnGroup = gs[i];
                            contact.removeBelongGroup(msnGroup.getGroupId());
                            
                            ((AbstractMessenger) session.getMessenger()).
                                fireContactRemoveFromGroupCompleted(contact, msnGroup);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    ((AbstractMessenger) session.getMessenger()).
                        fireContactRemoveCompleted(contact, MsnList.FL);
                }
            }
        }
    }
}
