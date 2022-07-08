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
 * MsnContactListListener adapter.
 * 
 * @author Roger Chen
 * @author Damian Minkov
 */
public class MsnContactListAdapter implements MsnContactListListener {

    public void contactListInitCompleted(MsnMessenger messenger) {
    	// Empty implementation, intended to be overridden.
    }

    public void contactListSyncCompleted(MsnMessenger messenger) {
    	// Empty implementation, intended to be overridden.
    }

    public void contactStatusChanged(MsnMessenger messenger, MsnContact contact) {
    	// Empty implementation, intended to be overridden.
    }
    
    public void contactPersonalMessageChanged(MsnMessenger messenger, MsnContact contact) {
        // Empty implementation, intended to be overridden.
    }

    public void ownerStatusChanged(MsnMessenger messenger) {
    	// Empty implementation, intended to be overridden.
    }

    public void ownerDisplayNameChanged(MsnMessenger messenger) {
    	// Empty implementation, intended to be overridden.
    }

    public void contactAddedMe(MsnMessenger messenger, MsnContact contact) {
        // Empty implementation, intended to be overridden.
    }
    
    public void contactAddedMe(MsnMessenger messenger, MsnContactPending[] pending){
        // Empty implementation, intended to be overridden.        
    }

    public void contactRemovedMe(MsnMessenger messenger, MsnContact contact) {
    	// Empty implementation, intended to be overridden.
    }

    public void contactAddCompleted(MsnMessenger messenger, MsnContact contact, MsnList list) {
    	// Empty implementation, intended to be overridden.
    }
    
    public void contactAddInGroupCompleted(MsnMessenger messenger, MsnContact contact, MsnGroup group) {
        // Empty implementation, intended to be overridden.
    }

    public void contactRemoveCompleted(MsnMessenger messenger, MsnContact contact, MsnList list) {
    	// Empty implementation, intended to be overridden.
    }

    public void contactRemoveFromGroupCompleted(MsnMessenger messenger, MsnContact contact, MsnGroup group) {
        // Empty implementation, intended to be overridden.
    }

    public void groupAddCompleted(MsnMessenger messenger, MsnGroup group) {
    	// Empty implementation, intended to be overridden.
    }

    public void groupRemoveCompleted(MsnMessenger messenger, MsnGroup group) {
    	// Empty implementation, intended to be overridden.
    }
}