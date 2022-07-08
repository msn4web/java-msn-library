/*
 * SessionListener.java
 *
 * Created on March 13, 2007, 6:13 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.jml.net;

/**
 *
 * @author damencho
 */
public interface SessionListener
{
     public abstract void sessionEstablished(Session session)
        throws Exception;

    public abstract void sessionClosed(Session session)
        throws Exception;

    public abstract void sessionIdle(Session session)
        throws Exception;

    public abstract void sessionTimeout(Session session)
        throws Exception;

    public abstract void messageReceived(Session session, Message message)
        throws Exception;

    public abstract void messageSent(Session session, Message message)
        throws Exception;

    public abstract void exceptionCaught(Session session, Throwable throwable)
        throws Exception;
}
