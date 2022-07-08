package net.sf.jml.message.invitation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache all invite message until the invite finished or canceled.
 * 
 * @author Roger Chen
 */
final class InviteCache {

    private InviteCache() {
    }

    private static Map<Integer, MsnInviteMessage> cache = Collections.synchronizedMap(new HashMap<Integer, MsnInviteMessage>());

    public static void cache(MsnInviteMessage invite) {
        cache.put(invite.getInvitationCookie(), invite);
    }

    public static MsnInviteMessage getInvite(int invitationCookie) {
        return cache.get(invitationCookie);
    }

    public static void uncache(MsnInviteMessage invite) {
        cache.remove(invite.getInvitationCookie());
    }

}