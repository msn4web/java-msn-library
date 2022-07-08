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

import net.sf.jml.*;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class implements the MsnContact interface.
 * 
 * @author Roger Chen
 * @author Angel Barragán Chacón
 */
public class MsnContactImpl extends MsnUserImpl implements MsnContact {

	/**
	 * Creates a new MsnContact instace.
	 * 
	 * @param contactList MsnContactList to which this MsnContact belongs to.
	 */
    public MsnContactImpl(MsnContactList contactList) {
        this.contactList = contactList;
    }

    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * MsnContactList to which this MsnContact belongs to.
     */
    private final MsnContactList contactList;

    /**
     * @see MsnContact#getContactList()
     */
    public MsnContactList getContactList() {
        return contactList;
    }
    
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Runtime identifier for this MsnContact.
     */
    private String id;

    /**
     * @see MsnContact#getId()
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the new Runtime identifier for this MsnContact.
     * 
     * @param id New Runtime identifier for this MsnContact.
     */
    public void setId(String id) {
        this.id = id;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Friendly name it is given to this MsnContact.
     */
    private String friendlyName;

    /**
     * @see MsnContact#getFriendlyName()
     */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * Sets a new friendly name for this contact.
     * 
     * @param friendlyName New friendly name for this contact.
     */
    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Location of this MsnContact in the list.
     */
    private int listNumber;

    /**
     * Retrieves the location in the list for this MsnContact.
     * 
     * @return Integer with the location in the list.
     */
    public int getListNumber() {
        return listNumber;
    }

    /**
     * Sets the new location in the list.
     * 
     * @param listNumber New location in the list.
     */
    public void setListNumber(int listNumber) {
        this.listNumber = listNumber;
    }

    /**
     * Set this MsnContact in the given list.
     * 
     * @param list Instance of the list.
     * @param b True to add to the list and false to remove from it.
     */
    public void setInList(MsnList list, boolean b) {
        if (b) {
            listNumber = listNumber | list.getListId();
        } else {
            listNumber = listNumber & ~list.getListId();
        }
    }

    /**
     * @see MsnContact#isInList(MsnList)
     */
    public boolean isInList(MsnList list) {
        if (list == null)
            return false;
        return (listNumber & list.getListId()) == list.getListId();
    }
    
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Set of groups to which this MsnContact belongs to.
     */
    private final Set<MsnGroup> belongGroups = new LinkedHashSet<MsnGroup>();

    /**
     * @see MsnContact#getBelongGroups()
     */
    public MsnGroup[] getBelongGroups() {
        synchronized (belongGroups) {
            MsnGroup[] groups = new MsnGroup[belongGroups.size()];
            belongGroups.toArray(groups);
            return groups;
        }
    }

    /**
     * @see MsnContact#belongGroup(MsnGroup)
     */
    public boolean belongGroup(MsnGroup group) {
        synchronized (belongGroups) {
            return belongGroups.contains(group);
        }
    }

    /**
     * Add a new group to which this MsnContact belongs to.
     * 
     * @param groupId Identifier of the group.
     */
    public void addBelongGroup(String groupId) {
        MsnGroup group = getContactList().getGroup(groupId);
        if (group != null)
            addBelongGroup(group);
    }

    /**
     * Removes this MsnContact from the given group.
     * 
     * @param groupId Group Identifier from which this MsnContact must be 
     * removed from.
     */
    public void removeBelongGroup(String groupId) {
        MsnGroup group = getContactList().getGroup(groupId);
        if (group != null)
            removeBelongGroup(group);
    }

    /**
     * Add this MsnContact to the given intance of MsnGroup.
     * 
     * @param group instance of MsnGroup to which this contact must be added to.
     */
    void addBelongGroup(MsnGroup group) {
        belongGroups.add(group);
        if (!group.containContact(this))
            ((MsnGroupImpl) group).addContact(this);
    }

    /**
     * Removes this MsnContact from the given MsnGroup instance.
     * 
     * @param group MsnGroup instance from which this MsnContact must be removed
     * from.
     */
    void removeBelongGroup(MsnGroup group) {
        if (belongGroups.remove(group))
            if (group.containContact(this))
                ((MsnGroupImpl) group).removeContact(this);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Personal message of the MsnContact.
     */
    private String personalMessage = "";

    /**
     * @see MsnContact#getPersonalMessage()
     */
    public String getPersonalMessage() {
        return personalMessage;
    }

    /**
     * Sets a new personal message for this MsnContact.
     * 
     * @param msg New personal Message.
     */
    public void setPersonalMessage(String msg) {
        this.personalMessage = msg;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Current media for this MsnContact.
     */
    private String currentMedia = "";

    /**
     * Retrieves the current media for this MsnContact.
     * 
     * @return Current media for this MsnContact.
     */
    public String getCurrentMedia() {
        return currentMedia;
    }

    /**
     * Sets the new current media for this MsnContact.
     * 
     * @param currentMedia New Current media for this MsnContact.
     */
    public void setCurrentMedia(String currentMedia) {
        this.currentMedia = currentMedia;
    }
    
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Instance of the MsnObject representing this MsnContact avatar.
     */
    private MsnObject avatar = null;
    
    /**
     * @see MsnContact#getAvatar()
     */
    public MsnObject getAvatar() {
		return avatar;
	}

    /**
     * Sets the new avatar for this MsnContact.
     * 
     * @param avatar Instance of the new Avatar.
     */
	public void setAvatar(MsnObject avatar) {
		this.avatar = avatar;
	}
    
    ////////////////////////////////////////////////////////////////////////////

	/**
	 * @see MsnUserImpl#setEmail(Email)
	 */
	@Override
	public void setEmail(Email email) {
        super.setEmail(email);
        if (email != null) {
            if (id == null)
                setId(email.getEmailAddress());
            if (friendlyName == null)
                setFriendlyName(email.getEmailAddress());
        }
    }

	/**
	 * @see Object#equals(Object)
	 */
    @Override
	public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MsnContactImpl)) {
            return false;
        }
        MsnContactImpl user = (MsnContactImpl) obj;

        if (!(id == null ? user.id == null : id.equals(user.id)))
            return false;
        return contactList == null ? user.contactList == null : contactList
                .equals(user.contactList);
    }

	/**
	 * @see Object#hashCode()
	 */
    @Override
	public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

	/**
	 * @see Object#toString()
	 */
    @Override
	public String toString() {
        return "MsnContact: [ID]" + id + " [Email] " + getEmail()
                + " [DisplayName] " + getDisplayName() + " [FriendlyName] "
                + friendlyName + " [Status] " + getStatus() + " [ListNum] "
                + listNumber;
    }

}