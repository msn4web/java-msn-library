/*
 * MessageRecognizer.java
 *
 * Created on March 13, 2007, 6:19 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.jml.net;

import java.nio.ByteBuffer;

/**
 *
 * @author damencho
 */
public interface MessageRecognizer
{
    public abstract Message recognize(Session session, ByteBuffer bytebuffer);
}