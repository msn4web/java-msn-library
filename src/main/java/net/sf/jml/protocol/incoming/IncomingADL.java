/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.jml.protocol.incoming;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sf.jml.Email;
import net.sf.jml.MsnList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import net.sf.jml.MsnMessageChain;
import net.sf.jml.MsnMessageIterator;
import net.sf.jml.MsnProtocol;
import net.sf.jml.impl.AbstractMessenger;
import net.sf.jml.impl.MsnContactImpl;
import net.sf.jml.impl.MsnContactListImpl;
import net.sf.jml.protocol.MsnIncomingMessage;
import net.sf.jml.protocol.MsnOutgoingMessage;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.outgoing.OutgoingADL;
import net.sf.jml.protocol.outgoing.OutgoingCHG;
import net.sf.jml.protocol.outgoing.OutgoingFQY;
import net.sf.jml.protocol.outgoing.OutgoingUUX;
import net.sf.jml.util.XmlUtils;
import org.apache.commons.logging.*;

/**
 *
 * @author Damian Minkov
 */
public class IncomingADL
    extends MsnIncomingMessage
{
    private boolean isChunkSupported = true;

    private static final Log logger = LogFactory.getLog(IncomingADL.class);
    

    public IncomingADL(MsnProtocol protocol)
    {
        super(protocol);
        
    }

//    @Override
    protected boolean isSupportChunkData()
    {
        return isChunkSupported;
    }

     protected boolean load(ByteBuffer buffer)
     {
         boolean result = super.load(buffer);

         if(!result)
         {
             isChunkSupported = false;
             result = super.load(buffer);
         }

         return result;
     }

    @Override
    protected void messageReceived(MsnSession session)
    {
        super.messageReceived(session);
        
        if (protocol.after(MsnProtocol.MSNP13) && session.getContactList().isADLSent())
            session.getContactList().processInit();

        MsnMessageChain chain = session.getOutgoingMessageChain();
        int trId = getTransactionId();

        for (MsnMessageIterator iterator = chain.iterator(); iterator.hasPrevious();)
        {
            MsnOutgoingMessage message = (MsnOutgoingMessage) iterator.previous();
            if (message.getTransactionId() == trId)
            {
                MsnContactImpl contact = ((OutgoingADL) message).getContact();
                if(contact != null)
                {
                    MsnList list = null;

                    if(contact.isInList(MsnList.FL))
                        list = MsnList.FL;
                    else if(contact.isInList(MsnList.AL))
                        list = MsnList.AL;

                    ((AbstractMessenger) session.getMessenger())
                        .fireContactAddCompleted(contact, list);

                    OutgoingFQY m = new OutgoingFQY(protocol);
                    m.setContact(contact);
                    session.getMessenger().send(m);

                    return;
                }
            }
        }


        byte[] chunk = getChunkData();
        if(chunk == null || chunk.length == 0)
            return;

        try
        {
            MsnContactListImpl contactList = (MsnContactListImpl) session
                .getMessenger().getContactList();

            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
            dbfactory.setIgnoringComments(true);
            DocumentBuilder docBuilder = dbfactory.newDocumentBuilder();

            ByteArrayInputStream in = new ByteArrayInputStream(chunk);
            Document doc = docBuilder.parse(in);
            Element rootEl = doc.getDocumentElement();

            String domain =
                XmlUtils.findChild(rootEl, "d").getAttribute("n");

            String contactName =
                XmlUtils.findChildByChain(rootEl, new String[]{"d", "c"}).getAttribute("n");
            String type =
                XmlUtils.findChildByChain(rootEl, new String[]{"d", "c"}).getAttribute("t");

            String listStr =
                XmlUtils.findChildByChain(rootEl, new String[]{"d", "c"}).getAttribute("l");

            String name =
                XmlUtils.findChildByChain(rootEl, new String[]{"d", "c"}).getAttribute("f");

            MsnContactImpl contact = (MsnContactImpl) contactList
                .getContactByEmail(Email.parseStr(contactName + "@" + domain));

            if (contact == null)
            {
                contact = new MsnContactImpl(contactList);
                contact.setEmail(Email.parseStr(contactName + "@" + domain));
                contact.setFriendlyName(name);
                contact.setDisplayName(name);

                contactList.addContact(contact);
            }

            boolean isInRList = contact.isInList(MsnList.RL);
            int listNumber = Integer.parseInt(listStr);

            if(!(!isInRList && ((listNumber & MsnList.RL.getListId()) == MsnList.RL.getListId())))
                return;

            // when we are added from other user we receive ADL for
            // the event and must add him to the Reverse List
            // its server initiated transaction so its 0
            if(trId == 0 && listNumber == MsnList.RL.getListId())
            {
                contact.setInList(MsnList.RL, true);

                ((AbstractMessenger) session.getMessenger())
                    .fireContactAddedMe(contact);
            }
            else
            {
                MsnList lists[] = new MsnList[]{
                    MsnList.AL, MsnList.BL, MsnList.FL, MsnList.PL, MsnList.RL};

                for (int i = 0; i < lists.length; i++)
                {
                    MsnList list = lists[i];
                    if((listNumber & list.getListId()) == list.getListId())
                        contact.setInList(list, true);
                    else
                        contact.setInList(list, false);
                }

                ((AbstractMessenger) session.getMessenger())
                        .fireContactAddedMe(contact);
            }
        }
        catch (Exception e)
        {
            logger.error("Erro parsing incoming ADL!", e);
        }
        
    }
}
