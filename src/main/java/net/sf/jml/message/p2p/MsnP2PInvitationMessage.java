package net.sf.jml.message.p2p;

import net.sf.jml.MsnContact;
import net.sf.jml.MsnObject;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.msnslp.MsnslpRequest;
import net.sf.jml.util.StringUtils;

public class MsnP2PInvitationMessage extends MsnP2PSlpMessage{
	
	public static final String METHOD_INVITE = "INVITE";

    public static final String KEY_GUID_EUF = "EUF-GUID";
    public static final String KEY_CONTEXT = "Context";

    public static final String GUID_EUF =
            "{A4268EEC-FEC5-49E5-95C3-F126696BDBF6}";
    
    public MsnP2PInvitationMessage() {
		
	}

	@Override
	protected void messageReceived(MsnSession session, MsnContact contact) {
		// Get Slp message
        MsnslpRequest msnslpRequest = (MsnslpRequest) getSlpMessage();
        
        // Get a properties
        String method = msnslpRequest.getRequestMethod();
        String guid_euf = msnslpRequest.getBodys().getProperty(KEY_GUID_EUF);
        String context = msnslpRequest.getBodys().getProperty(KEY_CONTEXT);
        if (method != null && method.equals(METHOD_INVITE) &&
            msnslpRequest.getCSeq() == 0 &&
            guid_euf != null && guid_euf.equals(GUID_EUF) &&
	        context != null) {
    		// Create a new work to send the MsnObject  
            context = StringUtils.decodeBase64(context);
            context = context.substring(0,context.length()-1);
            MsnObject picture = session.getMessenger().
            	getDisplayPictureDuelManager().getPicture(context);
            if (picture != null) {
                DisplayPictureDuel duel = new DisplayPictureDuel(
                        session,picture, session.getMessenger().
                    	getDisplayPictureDuelManager());
                session.getMessenger().getDisplayPictureDuelManager().add(duel);
                duel.start(this, contact);
            }
        }
        // Notify supper
        else {
        	super.messageReceived(session, contact);
        }
    }

}
