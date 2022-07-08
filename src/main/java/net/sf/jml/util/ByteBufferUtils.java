/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * The contents of this file has been copied from the Base64 and Base64Encoder
 * classes of the Bouncy Castle libraries and included the following license.
 *
 * Copyright (c) 2000 - 2006 The Legion Of The Bouncy Castle
 * (http://www.bouncycastle.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.sf.jml.util;

import java.nio.ByteBuffer;

/**
 *
 * @author Damian Minkov
 */
public class ByteBufferUtils
{
    /**
     * Returns the index within this buffer of the first occurrence of the
     * specified pattern buffer.
     * 
     * @param buffer
     * 		the buffer
     * @param pattern
     * 		the pattern buffer
     * @return
     * 		the position within the buffer of the first occurrence of the 
     * pattern buffer
     */
    public static int indexOf(ByteBuffer buffer, ByteBuffer pattern)
    {
        int patternPos = pattern.position();
        int patternLen = pattern.remaining();
        int lastIndex = buffer.limit() - patternLen + 1;

        Label: for (int i = buffer.position(); i < lastIndex; i++) {
            for (int j = 0; j < patternLen; j++) {
                if (buffer.get(i + j) != pattern.get(patternPos + j))
                    continue Label;
            }
            return i;
        }
        return -1;
    }    
    
    public static ByteBuffer allocate(int i, boolean flag)
        throws IllegalArgumentException
    {
        if(i < 0)
            throw new IllegalArgumentException("capacity can't be negative");
        else
            return flag ? ByteBuffer.allocateDirect(i) : ByteBuffer.allocate(i);
    }

    public static ByteBuffer increaseCapacity(ByteBuffer bytebuffer, int i)
        throws IllegalArgumentException
    {
        if(bytebuffer == null)
            throw new IllegalArgumentException("buffer is null");
        if(i < 0)
        {
            throw new IllegalArgumentException("size less than 0");
        } else
        {
            int j = bytebuffer.capacity() + i;
            ByteBuffer bytebuffer1 = allocate(j, bytebuffer.isDirect());
            bytebuffer.flip();
            bytebuffer1.put(bytebuffer);
            return bytebuffer1;
        }
    }
}
