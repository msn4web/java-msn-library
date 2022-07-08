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

import java.io.BufferedReader;
import net.sf.jml.message.MsnInstantMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.sf.jml.Email;
import net.sf.jml.MsnContact;
import net.sf.jml.impl.AbstractMessenger;
import net.sf.jml.impl.MsnContactImpl;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.outgoing.OutgoingQRY;
import net.sf.jml.util.Base64;
import net.sf.jml.util.XmlUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * Manages Offline Messages
 *
 * @author Damian Minkov
 */
public class OIM
{
    private static final String oim_url =
        "https://rsi.hotmail.com/rsi/rsi.asmx";
    private static final String oim_getmetadat_soap =
        "http://www.hotmail.msn.com/ws/2004/09/oim/rsi/GetMetadata";
    private static final String oim_getmsgs_soap =
        "http://www.hotmail.msn.com/ws/2004/09/oim/rsi/GetMessage";
    private static final String oim_deletemsgs_soap =
        "http://www.hotmail.msn.com/ws/2004/09/oim/rsi/DeleteMessages";

    private static final String oim_send_url =
        "https://ows.messenger.msn.com/OimWS/oim.asmx";
    private static final String oim_send_soap =
        //"http://messenger.msn.com/ws/2004/09/oim/Store";
        "http://messenger.live.com/ws/2006/09/oim/Store2";

    private static final Log logger = LogFactory.getLog(OIM.class);

    private SSO sso;
    private MsnSession session;
    
    /**
     * Defines whether to delete the OIMs after retrieval or not
     * Default to true ( This should be set by user when and whether he decides )
     * @see net.sf.jml.MsnMessenger#setDeleteOfflineMessages(boolean)
     */
    private static boolean postDel=true;

    public OIM(MsnSession session)
    {
        this.sso = session.getSSO();
        this.session = session;
    }

    public Iterator<MsnInstantMessage> getOfflineMessages()
    {
        return null;
    }

    public static String[] parseMdata(String mdata)
    {
        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        dbfactory.setIgnoringComments(true);
        DocumentBuilder docBuilder;
        Document doc;
        try
        {

            docBuilder = dbfactory.newDocumentBuilder();

            ByteArrayInputStream in = new ByteArrayInputStream(mdata.getBytes());
            doc = docBuilder.parse(in);
        }
        catch (ParserConfigurationException ex)
        {
            Logger.getLogger(OIM.class.getName()).log(Level.SEVERE, null, ex);
            return new String[0];
        }
        catch (SAXException ex)
        {
            Logger.getLogger(OIM.class.getName()).log(Level.SEVERE, null, ex);
            return new String[0];
        }
        catch (IOException ex)
        {
            Logger.getLogger(OIM.class.getName()).log(Level.SEVERE, null, ex);
            return new String[0];
        }

        return getMsgIds(doc.getDocumentElement());
    }

    private static String[] getMsgIds(Element parent)
    {
        List els =
            XmlUtils.findChildrenByChain(parent,
                new String[]{ "M", "I"});

        String[] res = new String[els.size()];
        int c= 0;
        Iterator iter = els.iterator();
        while (iter.hasNext())
        {
            Element e = (Element)iter.next();

            res[c++] = XmlUtils.getText(e);
        }

        return res;
    }

    private String getMetaDataStr()
    {
        String t = sso.getWebTicket();
        String tt = t.substring(t.indexOf("t=") + 2, t.indexOf("&p="));
        String pp = t.substring(t.indexOf("&p=") + 3);

        StringBuilder mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?> ");
        mess.append("<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
        mess.append("  <soap:Header>");
        mess.append("    <PassportCookie xmlns=\"http://www.hotmail.msn.com/ws/2004/09/oim/rsi\">");
        mess.append("      <t>" + tt.replaceAll("&", "&amp;") + "</t> ");
        mess.append("      <p>" + pp.replaceAll("&", "&amp;") + "</p>");
        mess.append("    </PassportCookie>");
        mess.append("  </soap:Header>");
        mess.append("  <soap:Body>");
        mess.append("    <GetMetadata xmlns=\"http://www.hotmail.msn.com/ws/2004/09/oim/rsi\" />");
        mess.append("  </soap:Body>");
        mess.append("</soap:Envelope>");

        try
        {
            HttpResponse resp = sendRequest(mess.toString(), oim_url, oim_getmetadat_soap);
            return EntityUtils.toString(resp.getEntity());
        }
        catch (Exception e)
        {
            logger.error("send or receive mailinitdata!", e);
            return null;
        }
    }

    public List getMsgs(String mdata)
    {
        return null;
    }
    public void retrieveCurrentOfflineMessages(String mail){
        if(mail==null)
            return;
        
        String ids[]=parseMdata(mail);
        
        List oims = new ArrayList();
        for (int i = 0; i < ids.length; i++)
        {
            String id = ids[i];
            OfflineMsg oim = getMessage(id);

            if(oim != null)
                oims.add(oim);
        }

        Collections.sort(oims);

        Iterator<OfflineMsg> iter = oims.iterator();
        while (iter.hasNext())
        {
            OfflineMsg msg = iter.next();

            ((AbstractMessenger) session.getMessenger())
                .fireOfflineMessageReceived(
                    msg.body,
                    msg.contentType,
                    msg.encoding,
                    msg.date,
                    msg.contact);
        }

        if(ids.length > 0 && postDel)
            deleteMessage(ids);
        
    }
    public void retreiveOfflineMessages()
    {
        String resp = getMetaDataStr();

        // todo log
        if(resp == null)
            return;

        Element el = getDocumentElem(resp);

        Element msgs = XmlUtils.locateElement(el,
            "GetMetadataResponse",
            "xmlns",
            "http://www.hotmail.msn.com/ws/2004/09/oim/rsi");

        if(msgs == null)
        {
            if(logger.isTraceEnabled())
                logger.trace(resp);
            return;
        }

        String[] ids = getMsgIds((Element)msgs.getFirstChild());
        List oims = new ArrayList();
        for (int i = 0; i < ids.length; i++)
        {
            String id = ids[i];
            OfflineMsg oim = getMessage(id);

            if(oim != null)
                oims.add(oim);
        }

        Collections.sort(oims);

        Iterator<OfflineMsg> iter = oims.iterator();
        while (iter.hasNext())
        {
            OfflineMsg msg = iter.next();

            ((AbstractMessenger) session.getMessenger())
                .fireOfflineMessageReceived(
                    msg.body,
                    msg.contentType,
                    msg.encoding,
                    msg.date,
                    msg.contact);
        }

        if(ids.length > 0 && postDel)
            deleteMessage(ids);
    }

    public OfflineMsg getMessage(String id)
    {
        String t = sso.getWebTicket();
        String tt = t.substring(t.indexOf("t=") + 2, t.indexOf("&p="));
        String pp = t.substring(t.indexOf("&p=") + 3);

        StringBuilder mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?> ");
        mess.append("<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
        mess.append("  <soap:Header>");
        mess.append("    <PassportCookie xmlns=\"http://www.hotmail.msn.com/ws/2004/09/oim/rsi\">");
        mess.append("      <t>" + tt.replaceAll("&", "&amp;") + "</t> ");
        mess.append("      <p>" + pp.replaceAll("&", "&amp;") + "</p>");
        mess.append("    </PassportCookie>");
        mess.append("  </soap:Header>");
        mess.append("  <soap:Body>");
        mess.append("    <GetMessage xmlns=\"http://www.hotmail.msn.com/ws/2004/09/oim/rsi\">");
        mess.append("    <messageId>" + id + "</messageId>");
        mess.append("    <alsoMarkAsRead>true</alsoMarkAsRead>");
        mess.append("    </GetMessage>");
        mess.append("  </soap:Body>");
        mess.append("</soap:Envelope>");

        HttpResponse resp = sendRequest(mess.toString(), oim_url, oim_getmsgs_soap);

        String respStr = null;

        try
        {
            respStr = EntityUtils.toString(resp.getEntity());
        }
        catch (Exception e){}

        if(resp.getStatusLine().getStatusCode() == 200)
        {
            try
            {
                return parseMessage(respStr);

            }
            catch (Exception e)
            {
                logger.error("Error parsing msg!", e);
            }
        }
        else
            logger.error("Error sending request to retreive offline msg! " + respStr);

        return null;
    }

    public void deleteMessage(String[] ids)
    {
        String t = sso.getWebTicket();
        String tt = t.substring(t.indexOf("t=") + 2, t.indexOf("&p="));
        String pp = t.substring(t.indexOf("&p=") + 3);

        StringBuilder mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?> ");
        mess.append("<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
        mess.append("  <soap:Header>");
        mess.append("    <PassportCookie xmlns=\"http://www.hotmail.msn.com/ws/2004/09/oim/rsi\">");
        mess.append("      <t>" + tt.replaceAll("&", "&amp;") + "</t> ");
        mess.append("      <p>" + pp.replaceAll("&", "&amp;") + "</p>");
        mess.append("    </PassportCookie>");
        mess.append("  </soap:Header>");
        mess.append("  <soap:Body>");
        mess.append("    <DeleteMessages xmlns=\"http://www.hotmail.msn.com/ws/2004/09/oim/rsi\">");
        mess.append("    <messageIds>");

        for (int i = 0; i < ids.length; i++)
        {
            String id = ids[i];
            mess.append("    <messageId>" + id + "</messageId>");
        }

        mess.append("    </messageIds>");
        mess.append("    </DeleteMessages>");
        mess.append("  </soap:Body>");
        mess.append("</soap:Envelope>");

        HttpResponse resp = sendRequest(mess.toString(), oim_url, oim_deletemsgs_soap);

        String respStr = null;

        try
        {
            respStr = EntityUtils.toString(resp.getEntity());
        }
        catch (Exception e){}

        if(resp.getStatusLine().getStatusCode() != 200)
            logger.error("Error deleting offline msg! " + respStr);
    }

    private OfflineMsg parseMessage(String xml)
        throws Exception
    {
        Element el = XmlUtils.locateElement(getDocumentElem(xml),
            "GetMessageResponse",
            "xmlns",
            "http://www.hotmail.msn.com/ws/2004/09/oim/rsi");

        if(el == null)
        {
            logger.error("error parsing " + xml);
            return null;
        }

        Element el1 = XmlUtils.findChild(el, "GetMessageResult");

        if(el1 == null)
        {
            logger.error("error parsing " + xml);
            return null;
        }


        BufferedReader in =
            new BufferedReader(new StringReader(XmlUtils.getText(el1)));

        String from = null;
        String displayName = null;
        String contentType = null;
        String encoding = null;
        String body = null;
        Date date=null;
        int seqNum = -1;

        StringBuilder bodyBuilder = new StringBuilder();
        boolean isBodyReached = false;

        String line = null;
        while((line = in.readLine()) != null)
        {
            if(isBodyReached)
            {
                bodyBuilder.append(line);
            }
            else if(line.startsWith("From"))
            {
                int ix1 = line.indexOf("&lt;");
                int ix1len = 1;
                if(ix1 == -1)
                    ix1 = line.indexOf("<");
                else
                    ix1len = 4;

                int ix2 = line.indexOf("&gt;");
                if(ix2 == -1)
                    ix2 = line.indexOf(">");

                if(ix1 == -1)
                    from = line.replaceAll("From:", "").trim();
                else
                {
                    from = line.substring(ix1 + ix1len, ix2);
                }

            }
            else if(line.startsWith("Content-Type"))
            {
                line = line.replaceAll("Content-Type:", "");
                StringTokenizer toks = new StringTokenizer(line, ";");
                while (toks.hasMoreTokens())
                {
                    String t = toks.nextToken().trim();
                    if(t.contains("text"))
                        contentType = t;
                    else
                        if(t.contains("charset"))
                            encoding = t.replaceAll("charset=", "");
                }
            }
            else if(line.startsWith("X-OIM-Sequence-Num"))
            {
                try
                {
                    seqNum = Integer.parseInt(
                        line.replaceAll("X-OIM-Sequence-Num:", "").trim());
                }
                catch (Exception e)
                {
                }
            }
            else if(line.startsWith("X-OriginalArrivalTime: ")){
                /* Notice that the Date object always return the Machine
                 * Timezone. So it depends on the developer to handle timezone.
                 * (It's easy to reset the date with DateFormat to UTC after
                 * retrieving this)
                 */
                DateFormat df = new SimpleDateFormat ("dd MMM yyyy HH:mm:ss z");
                line= line.replaceAll("X-OriginalArrivalTime: ", "");
                line=line.substring(0,line.indexOf('.'));
                try{
                    date=df.parse((line+" UTC"));
                }catch(java.text.ParseException e){
                    logger.error("Error parsing OIM date");
                }
            } 
            else if(line.trim().length() == 0)
            {
                isBodyReached = true;
            }
        }

        body = new String(Base64.decode(bodyBuilder.toString()), encoding);

        MsnContactImpl contact = (MsnContactImpl)
            session.getMessenger().getContactList().
                getContactByEmail(Email.parseStr(from));

        if (contact == null) {
                contact = new MsnContactImpl(session.getMessenger().getContactList());
                contact.setEmail(Email.parseStr(from));
                contact.setDisplayName(displayName);
        }

        return new OfflineMsg(body, contentType, encoding, contact, seqNum, date);
    }

    public class OfflineMsg
        implements Comparable<OfflineMsg>
    {
        String body;
        String contentType;
        String encoding;
        Date date;
        MsnContact contact;
        int seqNum;

        public OfflineMsg(String body, String contentType, String encoding, MsnContact contact, int seqNum, Date date)
        {
            this.body = body;
            this.contentType = contentType;
            this.encoding = encoding;
            this.contact = contact;
            this.seqNum = seqNum;
            this.date=date;
        }

        public int compareTo(OfflineMsg o)
        {
            return seqNum - o.seqNum;
        }
    }

    private Element getDocumentElem(String xml)
    {
        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        dbfactory.setIgnoringComments(true);
        DocumentBuilder docBuilder;
        Document doc;
        try
        {

            docBuilder = dbfactory.newDocumentBuilder();

            ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            doc = docBuilder.parse(in);
        }
        catch (ParserConfigurationException ex)
        {
            Logger.getLogger(ContactList.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        catch (SAXException ex)
        {
            Logger.getLogger(ContactList.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        catch (IOException ex)
        {
            Logger.getLogger(ContactList.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        return doc.getDocumentElement();
    }

/*    private String getOfflineMsg(Email email, String txt)
    {
        StringBuilder mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");
        mess.append("<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\r\n xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
        mess.append("  <soap:Header>");

        mess.append("  <From memberName=\"" + session.getMessenger().getOwner().getEmail().getEmailAddress()
            + "\" friendlyName=\"=?utf-8?B?" + new String(
                Base64.encode(session.getMessenger().getOwner().getDisplayName().getBytes())) + "?=\"\r\n xml:lang=\"nl-nl\"\r\n proxy=\"MSNMSGR\"\r\n xmlns=\"http://messenger.msn.com/ws/2004/09/oim/\"\r\n msnpVer=\"MSNP15\"\r\n buildVer=\"8.5.1288.816\"/>");
        mess.append("    <To memberName=\""+ email.getEmailAddress() + "\" xmlns=\"http://messenger.msn.com/ws/2004/09/oim/\"/>");
        mess.append("    <Ticket passport=\"" + sso.getOimTicket().replaceAll("&", "&amp;") + "\"\r\n appid=\"PROD0119GSJUC$18\"\r\n lockkey=\"" + lockkey + "\"\r\n xmlns=\"http://messenger.msn.com/ws/2004/09/oim/\"/>");
        mess.append("    <Sequence xmlns=\"http://schemas.xmlsoap.org/ws/2003/03/rm\">\r\n");
        mess.append("      <Identifier xmlns=\"http://schemas.xmlsoap.org/ws/2002/07/utility\">http://messenger.msn.com</Identifier>\r\n");
        mess.append("      <MessageNumber>" + sentMsgNumber + "</MessageNumber>\r\n");
        mess.append("    </Sequence>\r\n");
        mess.append("  </soap:Header>\r\n");
        mess.append("  <soap:Body>\r\n");
        mess.append("    <MessageType xmlns=\"http://messenger.msn.com/ws/2004/09/oim/\">text</MessageType>\r\n");
        mess.append("    <Content xmlns=\"http://messenger.msn.com/ws/2004/09/oim/\">MIME-Version: 1.0\r\n");
        mess.append("Content-Type: text/plain; charset=UTF-8\r\n");
        mess.append("Content-Transfer-Encoding: base64\r\n");
        mess.append("X-OIM-Message-Type: OfflineMessage\r\n");
        mess.append("X-OIM-Run-Id: {3A3BE82C-684D-4F4F-8005-CBE8D4F82BAD}\r\n");
        mess.append("X-OIM-Sequence-Num: " + sentMsgNumber + "\r\n\r\n");
        mess.append("      " + new String(Base64.encode(txt.getBytes())) + "");
        mess.append("  </Content>");

        mess.append("  </soap:Body>");
        mess.append("</soap:Envelope>");

        return mess.toString();
    }*/
  
    private String getOfflineMsg(Email email, String txt)
    {
        StringBuilder mess = new StringBuilder();
				String displayName=new String(), finaltxt=new String();
				try
				{
					displayName=new String(Base64.encode(session.getMessenger().getOwner().getDisplayName().getBytes("UTF-8")));
					txt=new String(Base64.encode(txt.getBytes("UTF-8")));
					StringBuffer txtbuf= new StringBuffer(txt);
					for(int i=1;i<txtbuf.length();i++)
					    if(i%40==0) //not sure about 40, it should be 76 but it could be less! - BLuEGoD
					        txtbuf.insert(i,"\n");
					finaltxt=txtbuf.toString();
				}
				catch (UnsupportedEncodingException uee)
				{
					uee.printStackTrace();
				}
		        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");
		        mess.append("<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\r\n xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		        mess.append("  <soap:Header>");
		
		        mess.append("  <From memberName=\"" + session.getMessenger().getOwner().getEmail().getEmailAddress()
		            + "\" friendlyName=\"=?utf-8?B?" + displayName + "?=\"\r\n xml:lang=\"nl-nl\"\r\n proxy=\"MSNMSGR\"\r\n xmlns=\"http://messenger.msn.com/ws/2004/09/oim/\"\r\n msnpVer=\"MSNP15\"\r\n buildVer=\"8.5.1288.816\"/>");
		        mess.append("    <To memberName=\""+ email.getEmailAddress() + "\" xmlns=\"http://messenger.msn.com/ws/2004/09/oim/\"/>");
		        mess.append("    <Ticket passport=\"" + sso.getOimTicket().replaceAll("&", "&amp;") + "\"\r\n appid=\"PROD0119GSJUC$18\"\r\n lockkey=\"" + lockkey + "\"\r\n xmlns=\"http://messenger.msn.com/ws/2004/09/oim/\"/>");
		        mess.append("    <Sequence xmlns=\"http://schemas.xmlsoap.org/ws/2003/03/rm\">\r\n");
		        mess.append("      <Identifier xmlns=\"http://schemas.xmlsoap.org/ws/2002/07/utility\">http://messenger.msn.com</Identifier>\r\n");
		        mess.append("      <MessageNumber>" + sentMsgNumber + "</MessageNumber>\r\n");
		        mess.append("    </Sequence>\r\n");
		        mess.append("  </soap:Header>\r\n");
		        mess.append("  <soap:Body>\r\n");
		        mess.append("    <MessageType xmlns=\"http://messenger.msn.com/ws/2004/09/oim/\">text</MessageType>\r\n");
		        mess.append("    <Content xmlns=\"http://messenger.msn.com/ws/2004/09/oim/\">MIME-Version: 1.0\r\n");
		        mess.append("Content-Type: text/plain; charset=UTF-8\r\n");
		        mess.append("Content-Transfer-Encoding: base64\r\n");
		        mess.append("X-OIM-Message-Type: OfflineMessage\r\n");
		        mess.append("X-OIM-Run-Id: {3A3BE82C-684D-4F4F-8005-CBE8D4F82BAD}\r\n");
		        mess.append("X-OIM-Sequence-Num: " + sentMsgNumber + "\r\n\r\n");
		        mess.append("      " + finaltxt + "");
		        mess.append("  </Content>");
		
		        mess.append("  </soap:Body>");
		        mess.append("</soap:Envelope>");
		
		        return mess.toString();
    }


    private  static int sentMsgNumber = 1;
    private String lockkey = "e745d4e406790fb5c5ca70041fbe06df";

    public void sendOfflineMsg(Email email, String txt)
    {
        sentMsgNumber++;

        HttpResponse resp = sendRequest(getOfflineMsg(email, txt), oim_send_url, oim_send_soap);
        String respStr = null;

        try
        {
            respStr = EntityUtils.toString(resp.getEntity());
        }
        catch (Exception e){
            e.printStackTrace();
        }

        if(resp.getStatusLine().getStatusCode() == 500)
        {
            Element el =
            XmlUtils.locateElement(
                getDocumentElem(respStr),
                "faultcode",
                "xmlns:q0",
                "http://messenger.msn.com/ws/2004/09/oim/");
            if(el != null)
            {
                String fault = XmlUtils.getText(el).toLowerCase();
                //logger.info(fault);
                if(fault.indexOf("AuthenticationFailed".toLowerCase()) != -1)
                {
                    Element el1 =
                        XmlUtils.locateElement(
                            getDocumentElem(respStr),
                            "LockKeyChallenge",
                            "xmlns",
                            "http://messenger.msn.com/ws/2004/09/oim/");

                    if(el1 != null)
                    {
                        String k = XmlUtils.getText(el1).trim();

                        // use OutgoingQRY only to compute the
                        // lockkey , we are not sending the packet
                        lockkey =
                            new OutgoingQRY(
                                session.getMessenger().getActualMsnProtocol()).calc(k);

                        if(logger.isTraceEnabled())
                            logger.trace("new lockkey " + lockkey);

                        resp =
                            sendRequest(getOfflineMsg(email, txt), oim_send_url, oim_send_soap);
                        try
                        {
                            respStr = EntityUtils.toString(resp.getEntity());
                        }
                        catch (Exception e){}


                        if(resp.getStatusLine().getStatusCode() != 200)
                            logger.error("Error sending offline msg! " + respStr);
                        else
                        {
                            // no event for success
                        }
                    }
                }
                else    
                    logger.error("Unexpected 500 error");
            }
            else
                logger.error("Error sending offline msg! " + respStr);

        }
        else if(resp.getStatusLine().getStatusCode() == 200)
        {
            // no event for success
        }
        else
        {
            logger.error("Error sending offline msg! " + respStr);
        }
    }

    ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();
    DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
    HttpParams params = null;
    BasicHttpProcessor httpproc = null;

    private synchronized HttpResponse sendRequest(String body, String address, String soapAddress)
    {

        try
        {
            if(logger.isTraceEnabled())
                logger.trace("Will send body: " + body);

            URL url = new URL(address);
            if(params == null)
            {
                params = new BasicHttpParams();
                HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
                HttpProtocolParams.setContentCharset(params, "UTF-8");
                //HttpProtocolParams.setUserAgent(params, "MSN Explorer/9.0 (MSN 8.0; TmstmpExt)");
                HttpProtocolParams.setUseExpectContinue(params, false);

                httpproc = new BasicHttpProcessor();
                // Required protocol interceptors
                httpproc.addInterceptor(new RequestContent());
                httpproc.addInterceptor(new RequestTargetHost());
                // Recommended protocol interceptors
                httpproc.addInterceptor(new RequestConnControl());
                httpproc.addInterceptor(new RequestUserAgent());
                httpproc.addInterceptor(new RequestExpectContinue());
            }

            HttpRequestExecutor httpexecutor = new HttpRequestExecutor();

            HttpContext context = new BasicHttpContext(null);

            HttpHost host = new HttpHost(url.getHost(), 443, "https");

            //ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();

            context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);


            if (!conn.isOpen())
            {
                SSLContext sc = SSLContext.getInstance("SSLv3");
                TrustManager[] tma = {new DummyTrustManager()};
                sc.init(null, tma, null);

                SSLSocketFactory factory = sc.getSocketFactory();

                Socket socket = factory.createSocket(host.getHostName(), host.getPort());

                conn.bind(socket, params);
            }

            BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST",
                        url.getPath());
            request.setEntity(new XmlEntity(body));

            context.setAttribute(ExecutionContext.HTTP_REQUEST, request);
            request.setParams(params);

            if(soapAddress != null)
                request.addHeader("SOAPAction", soapAddress);

            request.addHeader("Host", url.getHost());

            httpexecutor.preProcess(request, httpproc, context);

            HttpResponse response = httpexecutor.execute(request, conn, context);
            httpexecutor.postProcess(response, httpproc, context);

            if (!connStrategy.keepAlive(response, context))
                conn.close();

            return response;
        }
        catch(Exception e)
        {
            Logger.getLogger(ContactList.class.getName()).log(Level.SEVERE, null, e);
        }

        return null;
    }

    /**
     * @param postDel Set to true to delete offline messages after retrieval
     */
    public void setPostDel(boolean postDel) {
        this.postDel = postDel;
    }

    /**
     * @return the postDel value
     */
    public boolean isPostDel() {
        return postDel;
    }

}
