/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.jml.protocol.outgoing;

import net.sf.jml.MsnList;
import net.sf.jml.MsnProtocol;
import net.sf.jml.impl.MsnContactImpl;
import net.sf.jml.protocol.MsnOutgoingMessage;

/**
 * Query client's online status
 * @author Damian Minkov
 */
public class OutgoingFQY
    extends MsnOutgoingMessage
{
    public OutgoingFQY(MsnProtocol protocol)
    {
        super(protocol);
        setCommand("FQY");
    }

    @Override
    protected boolean isSupportChunkData()
    {
        return true;
    }

    public void setContact(MsnContactImpl mc)
    {
        StringBuilder mess = new StringBuilder();
        mess.append("<ml l=\"2\">");

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

    private static String getDomain(String email)
    {
        return email.substring(email.indexOf("@") + 1);
    }

    private static String getName(String email)
    {
        return email.substring(0, email.indexOf("@"));
    }
}
