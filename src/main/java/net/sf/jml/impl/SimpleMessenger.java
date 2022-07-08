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
package net.sf.jml.impl;

import java.util.*;
import net.sf.jml.*;
import net.sf.jml.protocol.*;
import net.sf.jml.protocol.outgoing.*;
import net.sf.jml.util.StringUtils;

/**
 * Simple msn messenger.
 * 
 * @author Roger Chen
 * @author Damian Minkov
 */
public class SimpleMessenger extends BasicMessenger {

    private RateThread rater = null;
    private Thread rateThread = null;
    
    SimpleMessenger(Email email, String password) 
    {
        super(email, password);
        rater = new RateThread();
        
    }

    public void newSwitchboard(Object attachment) {
        OutgoingXFR outgoing = new OutgoingXFR(getActualMsnProtocol());
        outgoing.setAttachment(attachment);
        rater.sendMessage(outgoing);
//        send(outgoing);
    }
    
    public void logout() 
    {
        if(rater != null)
            rater.stopCurrentRun();
        
        super.logout();
    }
    
     public void login(String ip, int port) 
     {
        rateThread = new Thread(rater);
        rateThread.setDaemon(true);
        rateThread.start();
         
        super.login(ip, port);
     }

    public void addGroup(String groupName) {
        if (groupName == null)
            return;
        
        if(getActualMsnProtocol().before(MsnProtocol.MSNP13))
        {
            OutgoingADG message = new OutgoingADG(getActualMsnProtocol());
            message.setGroupName(groupName);
            send(message);
        }
        else
        {
            session.getContactList().createGroup(groupName);
        }
    }

    public void removeGroup(String groupId) 
    {
        MsnGroup group = getContactList().getGroup(groupId);
        if (group != null) 
        {
            if(getActualMsnProtocol().before(MsnProtocol.MSNP13))
            {
                OutgoingRMG message = new OutgoingRMG(getActualMsnProtocol());
                message.setGroupId(groupId);
                send(message);
            }
            else
            {
                session.getContactList().removeGroup(groupId);
            }
        }
    }

    public void renameGroup(String groupId, String newGroupName) {
        if (groupId == null || newGroupName == null)
            return;
        MsnGroup group = getContactList().getGroup(groupId);
        if (group != null && !group.isDefaultGroup()
                && !group.getGroupName().equals(newGroupName)) 
        {
            if(getActualMsnProtocol().before(MsnProtocol.MSNP13))
            {
                OutgoingREG message = new OutgoingREG(getActualMsnProtocol());
                message.setGroupId(groupId);
                message.setGroupName(newGroupName);
                send(message);
            }
            else
            {
                session.getContactList().renameGroup(groupId, newGroupName);
            }
        }
    }

    public void addFriend(MsnList list, Email email, String friendlyName) {
        if (list == null || email == null || list == MsnList.RL
                || list == MsnList.PL)
            return;
        MsnContact contact = getContactList().getContactByEmail(email);
        if (contact != null && contact.isInList(list)){
            return;
        }

        MsnProtocol protocol = getActualMsnProtocol();
        if (protocol.after(MsnProtocol.MSNP9) && protocol.before(MsnProtocol.MSNP13)) 
        {
            OutgoingADC message = new OutgoingADC(protocol);
            message.setAddtoList(list);
            message.setEmail(email);
            if (list == MsnList.FL)
                message.setFriendlyName(friendlyName == null ? email
                        .getEmailAddress() : friendlyName);
            send(message);
        } 
        else if(protocol.after(MsnProtocol.MSNP13)) 
        {
            session.getContactList().addFriend(email, friendlyName);
        }
        else
        {
            OutgoingADD message = new OutgoingADD(protocol);
            message.setAddtoList(list);
            message.setEmail(email);
            message.setFriendlyName(friendlyName == null ? email
                    .getEmailAddress() : friendlyName);
            send(message);
        }
    }

    private void removeFriend(MsnList list, Email email, String id,
            String groupId) {
        if (list == null || list == MsnList.RL)
            return;
        if (list == MsnList.FL) {
            if (id == null)
                return;
        } else if (email == null)
            return;
        MsnContact contact = getContactList().getContactByEmail(email);
        if (contact == null || !contact.isInList(list)){
            return;
        }

        if(getActualMsnProtocol().before(MsnProtocol.MSNP13))
        {
            OutgoingREM message = new OutgoingREM(getActualMsnProtocol());
            message.setRemoveFromList(list);
            if (list == MsnList.FL) {
                message.setId(id);
                if (groupId != null)
                    message.setGroupId(groupId);
            } else {
                message.setEmail(email);
            }
            send(message);
        }
        else
        {
            session.getContactList().removeFriend(list, email, id, groupId);
        }
    }

    public void addFriend(Email email, String friendlyName) {
        if (email == null)
            return;
        if (friendlyName == null)
            friendlyName = email.getEmailAddress();

        if(!getActualMsnProtocol().before(MsnProtocol.MSNP13))
        {
            session.getContactList().addFriend(email, friendlyName);
            return;
        }

        MsnContact contact = getContactList().getContactByEmail(email);
        if (contact != null) 
        {
            if (!contact.isInList(MsnList.FL)) {
                if(getActualMsnProtocol().before(MsnProtocol.MSNP13))
                    addFriend(MsnList.FL, email, friendlyName);
                else
                    session.getContactList().addFriendToList(new MsnList[]{MsnList.FL}, (MsnContactImpl)contact);
            }
            if (!contact.isInList(MsnList.AL)) {
                if(getActualMsnProtocol().before(MsnProtocol.MSNP13))
                    addFriend(MsnList.AL, email, friendlyName);
                else
                    session.getContactList().addFriendToList(new MsnList[]{MsnList.AL}, (MsnContactImpl)contact);
            }
        }
        else {
            if(getActualMsnProtocol().before(MsnProtocol.MSNP13))
            {
                addFriend(MsnList.FL, email, friendlyName);
                addFriend(MsnList.AL, email, friendlyName);
            }
            else
                session.getContactList().addFriend(email, friendlyName);
        }
    }

    public void blockFriend(Email email) 
    {
        if(getActualMsnProtocol().before(MsnProtocol.MSNP13))
        {
            removeFriend(MsnList.AL, email, null, null);
            addFriend(MsnList.BL, email, null);
        }
        else
            session.getContactList().blockFriend(email);
    }

    public void copyFriend(Email email, String groupId) {
        MsnContact contact = getContactList().getContactByEmail(email);
        MsnGroup group = getContactList().getGroup(groupId);
        if (contact == null || group == null || group.isDefaultGroup())
            return;

        MsnProtocol protocol = getActualMsnProtocol();
        if (protocol.after(MsnProtocol.MSNP9) && protocol.before(MsnProtocol.MSNP13)) 
        {
            OutgoingADC outgoing = new OutgoingADC(protocol);
            outgoing.setAddtoList(MsnList.FL);
            outgoing.setId(contact.getId());
            outgoing.setGroupId(groupId);
            send(outgoing);
        }
        else if (protocol.before(MsnProtocol.MSNP9)) 
        {
            OutgoingADD outgoing = new OutgoingADD(protocol);
            outgoing.setAddtoList(MsnList.FL);
            outgoing.setEmail(email);
            outgoing.setFriendlyName(contact.getFriendlyName());
            outgoing.setGroupId(groupId);
            send(outgoing);
        }
        else
        {
            session.getContactList().copyFriend(email, groupId);
        }
    }

    public void moveFriend(Email email, String srcGroupId, String destGroupId) {
        if (email == null)
            return;
        MsnGroup srcGroup = getContactList().getGroup(srcGroupId);
        MsnGroup destGroup = getContactList().getGroup(destGroupId);
        if (srcGroup == null || destGroup == null || srcGroup.equals(destGroup))
            return;

        if (getActualMsnProtocol().before(MsnProtocol.MSNP13))
        {
            if (destGroup.isDefaultGroup()) { //move to default group
                removeFriend(email, srcGroupId);
            } else {
                copyFriend(email, destGroupId);
                if (!srcGroup.isDefaultGroup()) //not move from default group
                    removeFriend(email, srcGroupId);
            }
        }
        else
        {
            session.getContactList().moveFriend(email, srcGroupId, destGroupId);
        }
    }

    public void removeFriend(Email email, boolean block) {
        if (email == null)
            return;
        MsnContact contact = getContactList().getContactByEmail(email);
        if (contact != null) {
            removeFriend(MsnList.FL, email, contact.getId(), null);
            if (block) {
                blockFriend(email);
            }   
        }   
    }

    public void removeFriend(Email email, String groupId) {
        if (email == null)
            return;
        MsnContact contact = getContactList().getContactByEmail(email);
        if (contact != null) {
            removeFriend(MsnList.FL, email, contact.getId(), groupId);
        }
    }

    public void removeFriend(MsnList list, Email email) {
        if (list == null || email == null)
            return;
        MsnContact contact = getContactList().getContactByEmail(email);
        if (contact != null) {
            removeFriend(list, email, contact.getId(), null);
        }
    }

    public void renameFriend(Email email, String friendlyName) {
        if (email == null || friendlyName == null)
            return;
        MsnContact contact = getContactList().getContactByEmail(email);
        if (contact != null) {
            MsnProtocol protocol = getActualMsnProtocol();
            if (protocol.after(MsnProtocol.MSNP10) && protocol.before(MsnProtocol.MSNP13)) 
            {
                OutgoingSBP message = new OutgoingSBP(getActualMsnProtocol());
                message.setId(contact.getId());
                message.setPropertyType(MsnUserPropertyType.MFN);
                message.setProperty(StringUtils.urlEncode(friendlyName));
                send(message);
            }
            else if(protocol.after(MsnProtocol.MSNP13))
            {
                session.getContactList().updateFriend(email, contact.getId(), friendlyName);
            }
            else
            {
                OutgoingREA message = new OutgoingREA(getActualMsnProtocol());
                message.setId(contact.getEmail().getEmailAddress());
                message.setFriendlyName(StringUtils.urlEncode(friendlyName));
                send(message);
            }
        }
    }

    public void unblockFriend(Email email) 
    {
        if(getActualMsnProtocol().before(MsnProtocol.MSNP13))
        {
            removeFriend(MsnList.BL, email, null, null);
            addFriend(MsnList.AL, email, null);
        }
        else
            session.getContactList().unblockFriend(email);
        
    }
    
    public class RateThread
        implements Runnable
    {
        private Vector waitingMessages = new Vector();
        
        private int NUMBER_MESSAGES = 8;
        private long MAX_SECONDS = 55;
        
        Vector times = new Vector(NUMBER_MESSAGES - 1);
        
        private boolean shouldStop = false;
        
        public void run()
        {
            shouldStop = false;
            while(true)
            {
                synchronized(waitingMessages)
                {
                
                    try
                    {
                        if(waitingMessages.isEmpty())
                            waitingMessages.wait();
                        
                        if(shouldStop)
                            return;
                        
                        if(waitingMessages.size() > 0)
                        {
                            sendIt(waitingMessages.remove(0));
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    
                    
                }
            }
        }
        
        public void stopCurrentRun()
        {
            synchronized(waitingMessages)
            {
                shouldStop = true;
                waitingMessages.notifyAll();
            }
        }
        
        private void sendIt(Object o)
        {
            Date curr = new Date();
            if(times.size() == NUMBER_MESSAGES - 1)
            {
                Date d1 = (Date)times.get(0);
                long diff = curr.getTime() - d1.getTime();
                
                if(diff < MAX_SECONDS*1000)
                {
                    waitFor(MAX_SECONDS*1000 - diff);
                    curr = new Date();
                }
                
                times.remove(0);
            }

            send((MsnOutgoingMessage)o);
            times.add(curr);
        }
        
        public void sendMessage(MsnOutgoingMessage message)
        {
            synchronized(waitingMessages)
            {
                waitingMessages.add(message);
                waitingMessages.notifyAll();
            }
        }

        public void waitFor(long l)
        {
            Object lock = new Object();
            synchronized(lock)
            {
                try
                {
                    lock.wait(l);
                }
                catch(InterruptedException ex)
                {
                }
            }

        }
    }
}
