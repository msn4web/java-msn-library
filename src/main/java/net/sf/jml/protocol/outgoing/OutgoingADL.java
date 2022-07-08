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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;
import net.sf.jml.MsnProtocol;
import net.sf.jml.MsnContact;
import net.sf.jml.MsnList;
import net.sf.jml.impl.MsnContactImpl;
import net.sf.jml.impl.MsnContactListImpl;
import net.sf.jml.protocol.MsnOutgoingMessage;
import net.sf.jml.protocol.MsnSession;

/**
 * MSN13
 * Add users to your contact lists
 * @author Damian Minkov
 */
public class OutgoingADL
     extends MsnOutgoingMessage
{
    private MsnContactImpl contact = null;

    public OutgoingADL(MsnProtocol protocol)
    {
        super(protocol);
        setCommand("ADL");
    }

    @Override
    protected boolean isSupportChunkData()
    {
        return true;
    }

    public void setContact(MsnContactImpl mc)
    {
        this.contact = mc;

        StringBuilder mess = new StringBuilder();
        mess.append("<ml l=\"1\">");

        String emailAddr = mc.getEmail().getEmailAddress();

        mess.append("<d n=\"" + getDomain(emailAddr) + "\">");

        int listNumber = mc.getListNumber();

        if(mc.isInList(MsnList.PL))
            listNumber -= MsnList.PL.getListId();

        if(mc.isInList(MsnList.RL))
            listNumber -= MsnList.RL.getListId();

        mess.append("<c n=\"" + getName(mc.getEmail().getEmailAddress()) +
            "\" l=\"" + listNumber + "\" t=\"1\" />");

        mess.append("</d>");

        mess.append("</ml>");

        setChunkData(mess.toString());

    }

    public void addContacts(MsnContact[] cs)
    {
        Hashtable domains = new Hashtable();
        for (int i = 0; i < cs.length; i++)
        {
            MsnContact msnContact = cs[i];
            String d = getDomain(msnContact.getEmail().getEmailAddress());

            Vector v = (Vector)domains.get(d);

            if(v == null)
            {
                v = new Vector();
                domains.put(d, v);
            }

            v.add(msnContact);
        }

        StringBuilder mess = new StringBuilder();
        mess.append("<ml l=\"1\">");

        Iterator iter = domains.entrySet().iterator();
        while (iter.hasNext())
        {
            Entry e = (Entry)iter.next();
            String d = (String)e.getKey();

            mess.append("<d n=\"" + d + "\">");

            Vector v = (Vector)e.getValue();
            Iterator i = v.iterator();
            while (i.hasNext())
            {
                MsnContactImpl mc = (MsnContactImpl)i.next();

                int listNumber = mc.getListNumber();

                if(mc.isInList(MsnList.PL))
                    listNumber -= MsnList.PL.getListId();

                if(mc.isInList(MsnList.RL))
                    listNumber -= MsnList.RL.getListId();

                mess.append("<c n=\"" + getName(mc.getEmail().getEmailAddress()) +
                    "\" l=\"" + listNumber + "\" t=\"1\" />");
            }

            mess.append("</d>");
        }

        mess.append("</ml>");

        setChunkData(mess.toString());
    }

    private static String getDomain(String email)
    {
        return email.substring(email.indexOf("@") + 1);
    }

    private static String getName(String email)
    {
        return email.substring(0, email.indexOf("@"));
    }

    public MsnContactImpl getContact()
    {
        return contact;
    }
}
