package net.sf.jml.message.invitation;

/**
 * Accept computer call.
 * 
 * @author Lars Hoogweg
 */
class MsnCallAcceptMessage extends MsnAcceptMessage {

	public MsnCallAcceptMessage(MsnCallInviteMessage invite) {
		super(invite);
	}
}