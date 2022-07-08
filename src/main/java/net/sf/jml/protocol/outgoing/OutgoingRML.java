/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.jml.protocol.outgoing;

import net.sf.jml.MsnContact;
import net.sf.jml.MsnList;
import net.sf.jml.MsnProtocol;
import net.sf.jml.protocol.MsnOutgoingMessage;

/**
 * MSN13
 * Remove contact
 * @author Damian Minkov
 */
public class OutgoingRML
    extends MsnOutgoingMessage
{
    private MsnContact contact;
    private MsnList list;

    public OutgoingRML(MsnProtocol protocol)
    {
        super(protocol);
        setCommand("RML");
    }

    @Override
    protected boolean isSupportChunkData() {
        return true;
    }

    // can only remove from FL, AL and BL
    public void setRemoveFromList(MsnList list, MsnContact contact)
    {
        if (list == null)
        {
            throw new NullPointerException();
        }

        if (list == MsnList.RL ||
            !(list == MsnList.FL || list == MsnList.AL || list == MsnList.BL))
        {
            throw new IllegalArgumentException(list.toString());
        }

        this.contact = contact;
        this.list = list;

        String email = contact.getEmail().getEmailAddress();
        String domain = email.substring(email.indexOf("@") + 1);
        String name = email.substring(0, email.indexOf("@"));

        StringBuilder mess = new StringBuilder();
        mess.append("<ml>");
        mess.append("<d n=\"").append(domain).append("\">");
        mess.append("<c n=\"").append(name).
            append("\" t=\"1\" l=\"").append(list.getListId()).append("\" />");
        mess.append("</d>");
        mess.append("</ml>");

        setChunkData(mess.toString());
    }

    public MsnContact getContact()
    {
        return contact;
    }

    public MsnList getList()
    {
        return list;
    }
}
