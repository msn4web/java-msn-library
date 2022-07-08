package net.sf.jml.message.invitation;

import net.sf.jml.MsnContact;
import net.sf.jml.protocol.MsnSession;

/**
 * Computer call invite.
 * 
 * @author Lars Hoogweg
 */
public class MsnCallInviteMessage extends MsnInviteMessage {

	@Override
	protected void messageReceived(MsnSession session, MsnContact contact) {
		super.messageReceived(session, contact);
	}
}
