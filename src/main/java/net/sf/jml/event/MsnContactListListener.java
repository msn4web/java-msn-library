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
package net.sf.jml.event;

import net.sf.jml.MsnContact;
import net.sf.jml.MsnContactPending;
import net.sf.jml.MsnMessenger;
import net.sf.jml.MsnGroup;
import net.sf.jml.MsnList;

/**
 * Contact list listener.
 * 
 * @author Roger Chen
 * @author Damian Minkov
 */
public interface MsnContactListListener {

    /**
     * Contact list synchronize completed. Now all friends in
     * contact list, but their status have not been determined. 
     * 
     * @param messenger
     * 		MsnMessenger
     */
    public void contactListSyncCompleted(MsnMessenger messenger);

    /**
     * Contact list init completed. Now all user status 
     * have been determined.
     * 
     * @param messenger
     *    	MsnMessenger
     */
    public void contactListInitCompleted(MsnMessenger messenger);

    /**
     * Contact status changed such as online and offline or friend
     * changed his display name.
     * 
     * @param messenger
     *      MsnMessenger
     * @param contact
     *   	contact
     */
    public void contactStatusChanged(MsnMessenger messenger, MsnContact contact);
    
    /**
     * Contact has changed his PersonalMessage, or sending the first time
     * Changes in current media are being notified here too.
     * 
     * @param messenger
     * @param contact
     */
    public void contactPersonalMessageChanged(MsnMessenger messenger, MsnContact contact);

    /**
     * Owner status changed or name changed or profile changed.
     * 
     * @param messenger
     *		MsnMessenger
     */
    public void ownerStatusChanged(MsnMessenger messenger);

    /**
     * Owner display name changed.
     * 
     * @param messenger
     *		MsnMessenger
     */
    public void ownerDisplayNameChanged(MsnMessenger messenger);

    /**
     * Some one add current login user to his contact list.
     * 
     * @param messenger
     * 		MsnMessenger
     * @param contact
     *      the one who add you
     */
    public void contactAddedMe(MsnMessenger messenger, MsnContact contact);

    
    /**
     * Someone add current login user to his contact list MSNP>13 version
     * 
     * @param messenger
     * @param pending
     *  (A pending array)
     */
    public void contactAddedMe(MsnMessenger messenger, MsnContactPending[] pending);
    
    /**
     * Some one remove current login user from his contact list.
     * 
     * @param messenger
     * 		MsnMessenger
     * @param contact
     *      the one who remove you
     */
    public void contactRemovedMe(MsnMessenger messenger, MsnContact contact);

    /**
     * A contact you requested to be added has been added to the server.
     *
     * @param messenger
     *      MsnMessenger
     * @param contact
     *      the contact that you added
     * @param list
     *      the list to which the contact has been added
     */
    public void contactAddCompleted(MsnMessenger messenger, MsnContact contact, MsnList list);
    
    /**
     * A contact you requested to be added has been added to the server.
     *
     * @param messenger
     *      MsnMessenger
     * @param contact
     *      the contact that you added
     * @param group the group
     */
    public void contactAddInGroupCompleted(MsnMessenger messenger, MsnContact contact, MsnGroup group);

    /**
     * A contact you requested to be removed has been removed from the server.
     *
     * @param messenger
     *      MsnMessenger
     * @param contact
     *      the contact that you removed
     * @param list
     *      the list from which the contact has been removed
     */
    public void contactRemoveCompleted(MsnMessenger messenger, MsnContact contact, MsnList list);
    
    /**
     * A contact you requested to be removed has been removed from the server.
     *
     * @param messenger
     *      MsnMessenger
     * @param contact
     *      the contact that you removed
     * @param group the group
     */
    public void contactRemoveFromGroupCompleted(MsnMessenger messenger, MsnContact contact, MsnGroup group);

    /**
     * A group you requested to be added has been added to the server.
     *
     * @param messenger
     *      MsnMessenger
     * @param group
     *      the group that you added
     */
    public void groupAddCompleted(MsnMessenger messenger, MsnGroup group);

    /**
     * A group you requested to be removed has been removed from the server.
     *
     * @param messenger
     *      MsnMessenger
     * @param group
     *      the group that you removed
     */
    public void groupRemoveCompleted(MsnMessenger messenger, MsnGroup group);

}