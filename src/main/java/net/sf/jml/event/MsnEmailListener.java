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
import net.sf.jml.MsnSwitchboard;
import net.sf.jml.message.MsnEmailActivityMessage;
import net.sf.jml.message.MsnEmailInitMessage;
import net.sf.jml.message.MsnEmailNotifyMessage;
import net.sf.jml.message.MsnEmailInitEmailData;

/**
 * E-Mail event listener.
 *
 * @author Daniel Henninger
 */
public interface MsnEmailListener {

    /**
     * Received initial email notification message.
     *
     * @param switchboard
     * 		MsnSwitchboard
     * @param message
     * 		initial email notification message
     * @param contact
     * 		the user who sent this message
     */
    public void initialEmailNotificationReceived(MsnSwitchboard switchboard,
            MsnEmailInitMessage message, MsnContact contact);

    /**
     * Received initial email data notification message.
     *
     * @param switchboard
     * 		MsnSwitchboard
     * @param message
     * 		initial email notification message
     * @param contact
     * 		the user who sent this message
     */
    public void initialEmailDataReceived(MsnSwitchboard switchboard,
            MsnEmailInitEmailData message, MsnContact contact);

    /**
     * Received new email notification message.
     *
     * @param switchboard
     * 		MsnSwitchboard
     * @param message
     *      new email notification message
     * @param contact
     *      the user who sent this message
     */
    public void newEmailNotificationReceived(MsnSwitchboard switchboard,
            MsnEmailNotifyMessage message, MsnContact contact);

    /**
     * Received email activity notification message.
     *
     * @param switchboard
     * 		MsnSwitchboard
     * @param message
     *      email activity notification message
     * @param contact
     *      the user who sent this message
     */
    public void activityEmailNotificationReceived(MsnSwitchboard switchboard,
            MsnEmailActivityMessage message, MsnContact contact);

}