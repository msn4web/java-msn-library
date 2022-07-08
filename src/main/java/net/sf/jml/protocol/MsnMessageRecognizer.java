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
package net.sf.jml.protocol;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import net.sf.jml.net.Message;
import net.sf.jml.net.MessageRecognizer;
import net.sf.jml.net.Session;
import net.sf.jml.util.ByteBufferUtils;
import net.sf.jml.MsnMessenger;
import net.sf.jml.MsnProtocol;
import net.sf.jml.impl.AbstractMessenger;
import net.sf.jml.message.IncomingMimeMessage;
import net.sf.jml.protocol.incoming.*;
import net.sf.jml.util.Charset;
import net.sf.jml.util.JmlConstants;
import net.sf.jml.util.NumberUtils;

/**
 * Msn Message Recognizer.
 * 
 * @author Roger Chen
 */
final class MsnMessageRecognizer implements MessageRecognizer {

    private static final MsnMessageRecognizer instance = new MsnMessageRecognizer();

    private static final Map<String, Class<? extends MsnIncomingMessage>> normalMappingMap = new HashMap<String, Class<? extends MsnIncomingMessage>>();

    static {
        normalMappingMap.put("MSG", IncomingMimeMessage.class);
        normalMappingMap.put("VER", IncomingVER.class);
        normalMappingMap.put("CVR", IncomingCVR.class);
        normalMappingMap.put("XFR", IncomingXFR.class);
        normalMappingMap.put("USR", IncomingUSR.class);
        normalMappingMap.put("SYN", IncomingSYN.class);
        normalMappingMap.put("GTC", IncomingGTC.class);
        normalMappingMap.put("BLP", IncomingBLP.class);
        normalMappingMap.put("PRP", IncomingPRP.class);
        normalMappingMap.put("SBP", IncomingSBP.class);
        normalMappingMap.put("LSG", IncomingLSG.class);
        normalMappingMap.put("LST", IncomingLST.class);
        normalMappingMap.put("OUT", IncomingOUT.class);
        normalMappingMap.put("CHG", IncomingCHG.class);
        normalMappingMap.put("ILN", IncomingILN.class);
        normalMappingMap.put("FLN", IncomingFLN.class);
        normalMappingMap.put("NLN", IncomingNLN.class);
        normalMappingMap.put("QNG", IncomingQNG.class);
        normalMappingMap.put("CHL", IncomingCHL.class);
        normalMappingMap.put("QRY", IncomingQRY.class);
        normalMappingMap.put("ADD", IncomingADD.class);
        normalMappingMap.put("REM", IncomingREM.class);
        normalMappingMap.put("REA", IncomingREA.class);
        normalMappingMap.put("ADG", IncomingADG.class);
        normalMappingMap.put("RMG", IncomingRMG.class);
        normalMappingMap.put("REG", IncomingREG.class);
        normalMappingMap.put("CAL", IncomingCAL.class);
        normalMappingMap.put("JOI", IncomingJOI.class);
        normalMappingMap.put("BYE", IncomingBYE.class);
        normalMappingMap.put("RNG", IncomingRNG.class);
        normalMappingMap.put("ANS", IncomingANS.class);
        normalMappingMap.put("IRO", IncomingIRO.class);
        normalMappingMap.put("ACK", IncomingACK.class);
        normalMappingMap.put("NAK", IncomingNAK.class);
        normalMappingMap.put("BPR", IncomingBPR.class);
        normalMappingMap.put("ADC", IncomingADC.class);
        normalMappingMap.put("SBS", IncomingSBS.class);
        normalMappingMap.put("URL", IncomingURL.class);
        normalMappingMap.put("UBX", IncomingUBX.class);
        normalMappingMap.put("UUX", IncomingUUX.class);
        normalMappingMap.put("UBN", IncomingUBN.class);
        normalMappingMap.put("NOT", IncomingNOT.class);
        normalMappingMap.put("GCF", IncomingGCF.class);
        normalMappingMap.put("ADL", IncomingADL.class);  
        normalMappingMap.put("RFS", IncomingRFS.class);
        normalMappingMap.put("RML", IncomingRML.class);
        normalMappingMap.put("FQY", IncomingFQY.class);
    }

    private static final ByteBuffer SPLIT = Charset
            .encode(JmlConstants.LINE_SEPARATOR);

    public static MsnMessageRecognizer getInstance() {
        return instance;
    }

    private MsnMessageRecognizer() {
    }

    public Message recognize(Session session, ByteBuffer buffer) {
        if (ByteBufferUtils.indexOf(buffer, SPLIT) < 0)
            return null;
        if (buffer.remaining() < 3)
            return null;
        String charSequence = Charset.decode((ByteBuffer) buffer.limit(buffer
                .position() + 3));

        MsnMessenger messenger = ((MsnSession) session.getAttachment())
                .getMessenger();
        String key = charSequence.substring(0, 3);
        Class<? extends MsnIncomingMessage> c = normalMappingMap.get(key);

        MsnMessage message;
        if (c != null)
            message = getMessageInstance(c, messenger);
        else if (NumberUtils.isDigits(key))
            message = getMessageInstance(IncomingError.class, messenger);
        else
            //don't know how to parse this msg, just skip one line
            message = new IncomingUnknown(messenger.getActualMsnProtocol());

        return new WrapperMessage(message);
    }

    private MsnMessage getMessageInstance(
			Class<? extends MsnIncomingMessage> c, MsnMessenger messenger)
	{
		try
		{
			Constructor<? extends MsnIncomingMessage> constructor = c.getConstructor(MsnProtocol.class);
			return constructor.newInstance(messenger.getActualMsnProtocol());
		}
		catch (Exception e)
		{
			((AbstractMessenger) messenger).fireExceptionCaught(e);
		}
		return null;
	}

}