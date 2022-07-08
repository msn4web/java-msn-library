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
package net.sf.jml.message.p2p;

import java.io.*;
import java.util.*;
import net.sf.jml.util.*;
import net.sf.jml.util.Base64; // disambiguation

/**
 * Parses context of incoming filerequests. Extracts filename and filesize.
 * 
 * @author Lubomir Marinov
 */
public class MsnFileContextParser
{
    private static final int MAX_FILE_NAME_LENGTH = 0x226;

    public static String getFileName(byte[] bytes, int offset)
        throws UnsupportedEncodingException
    {
        /*
         * The length used to be bytes.length - offset but that was because I
         * didn't know there were 4 0xFF bytes at the end and that the fileName
         * in bytes was MAX_FILE_NAME_LENGTH, I thought fileName was till the
         * end of bytes.
         */
        int len = bytes.length - offset;
        if (len > MAX_FILE_NAME_LENGTH)
            len = MAX_FILE_NAME_LENGTH;

        String str = new String(bytes, offset, len, "UTF-16LE");
        int endIndex = str.indexOf('\0');

        return (endIndex < 0) ? str : str.substring(0, endIndex);
    }

    public static long getFileSize(byte[] bytes, int offset)
    {
        long value = 0;
        for (int i = 0; i < 4; i++)
            value |= ((bytes[offset + i] & 0xFF) << (i * 8));
        return value;
    }

    private static void writeUInt32(long value, byte[] bytes, int offset)
    {
        for (int i = 0; i < 4; i++)
            bytes[offset + i] = (byte) ((value >>> i * 8) & 0xFF);
    }

    public static String getEncodedContext(File file)
        throws UnsupportedEncodingException
    {
        int headerSize
            = 4 // 32-bit unsigned int for the size of the context
            + 4 // 32-bit unsigned int for the first unknown struct field
            + 4 // 32-bit unsigned int for the file size
            + 4 // 32-bit unsigned int for the second unknown struct field
            + 4;// 32-bit unsigned int for the third unknown struct field
        int contextSize
            = headerSize
            + MAX_FILE_NAME_LENGTH
            + 4;
        byte[] bytes = new byte[contextSize];
        int offset = 0;

        // the size of the context
        writeUInt32(contextSize, bytes, offset);
        offset += 4;
        // the first unknown struct field
        writeUInt32(2, bytes, offset);
        offset += 4;
        // the file size
        writeUInt32(file.length(), bytes, offset);
        offset += 4;
        // the second unknown struct field
        writeUInt32(0, bytes, offset);
        offset += 4;
        // the third unknown struct field
        writeUInt32(0, bytes, offset);
        offset += 4;

        // the file name
        byte[] fileNameBytes = file.getName().getBytes("UTF-16LE");
        System.arraycopy(fileNameBytes, 0, bytes, offset, fileNameBytes.length);
        Arrays.fill(
            bytes,
            offset + fileNameBytes.length,
            offset + MAX_FILE_NAME_LENGTH,
            (byte) 0);
        offset += MAX_FILE_NAME_LENGTH;

        // the last 4 0xFF bytes
        Arrays.fill(bytes, offset, offset + 4, (byte) 0xFF);
        offset += 4;

        if (offset != contextSize)
            throw new IllegalStateException("offset");

        byte[] barrEnc = Base64.encode(bytes);

        return new String(barrEnc);
    }
}
