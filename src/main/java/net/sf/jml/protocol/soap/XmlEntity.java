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
package net.sf.jml.protocol.soap;

import java.io.*;
import org.apache.http.entity.*;

/**
 * Entry for soap requests.
 * 
 * @author Damian Minkov
 */
public class XmlEntity
    extends AbstractHttpEntity
{

    String data;

    XmlEntity(String data)
    {
        this.data = data;
        setContentType("text/xml; charset=utf-8");
    }

    public boolean isRepeatable()
    {
        return false;
    }

    public long getContentLength()
    {
        return data.length();
    }

    public InputStream getContent() throws IOException, IllegalStateException
    {
        return new ByteArrayInputStream(data.getBytes());
    }

    public void writeTo(OutputStream o) throws IOException
    {
        o.write(data.getBytes());
    }

    public boolean isStreaming()
    {
        return false;
    }
}