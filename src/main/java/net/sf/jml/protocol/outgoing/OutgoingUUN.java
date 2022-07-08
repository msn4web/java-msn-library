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
package net.sf.jml.protocol.outgoing;

import net.sf.jml.MsnProtocol;
import net.sf.jml.protocol.MsnOutgoingMessage;
import net.sf.jml.util.StringUtils;

/**
 * - ACK means that the invitation has been accepted
 * - NAK means that the invitation has been declined
 * <p>
 * Supported Protocol: MSNP13
 * <p>
 * Syntax: UUN trId msgLen\r\n content
 *
 * @author Damian Minkov
 */
public class OutgoingUUN extends MsnOutgoingMessage
{
    private boolean accept = false;
    private String machineGuid = null;

    public OutgoingUUN(MsnProtocol protocol)
    {
        super(protocol);

        setCommand("UUN");
    }

    @Override
    protected boolean isSupportChunkData()
    {
        return true;
    }

    public void setData(String machineGuid, boolean accept)
    {
        this.machineGuid = machineGuid;
        this.accept = accept;
        buildData();
    }

    private void buildData()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<SNM opcode=\"");

        if (accept)
        {
            buffer.append("ACK");
        }
        else
        {
            buffer.append("NAK");
        }
        buffer.append("\" csid=\"");
        if (this.machineGuid != null)
        {
            buffer.append(StringUtils.xmlEscaping(machineGuid));
        }

        if(!accept)
        {
            buffer.append("\" reason=\"0x80070490");
        }
        buffer.append("\" />");
        setChunkData(buffer.toString());
    }
}