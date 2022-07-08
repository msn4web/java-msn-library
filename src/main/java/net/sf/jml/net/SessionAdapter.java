/*
 * SessionAdapter.java
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
public class SessionAdapter
    implements SessionListener
{
     public SessionAdapter()
    {
    }

    public void sessionClosed(Session session)
        throws Exception
    {
    }

    public void sessionEstablished(Session session)
        throws Exception
    {
    }

    public void sessionIdle(Session session)
        throws Exception
    {
    }

    public void sessionTimeout(Session session)
        throws Exception
    {
    }

    public void messageReceived(Session session, Message message)
        throws Exception
    {
    }

    public void messageSent(Session session, Message message)
        throws Exception
    {
    }

    public void exceptionCaught(Session session, Throwable throwable)
        throws Exception
    {
    }
   
    
}
