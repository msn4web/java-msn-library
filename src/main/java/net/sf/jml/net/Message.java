/*
 * Message.java
 *
 * Created on March 13, 2007, 6:29 PM
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
public interface Message
{

    public abstract boolean readFromBuffer(ByteBuffer bytebuffer);
//    public abstract boolean readFromBuffer(byte[] bytebuffer);

    public abstract ByteBuffer[] toByteBuffer();
}