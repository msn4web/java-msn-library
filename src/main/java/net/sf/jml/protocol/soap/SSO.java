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

import net.sf.jml.util.Base64;
import net.sf.jml.util.XmlUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.*;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.Socket;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages MSN15 Single Sign-On
 *
 * @author Damian Minkov
 */
public class SSO
{
    private static final Log logger = LogFactory.getLog(SSO.class);

    private String userName = null;
    private String password = null;
    private String policy = null;
    private String nonce = null;

    private String webTicket = null;
    private String contactTicket = null;
    private String oimTicket = null;
    private String spaceTicket = null;
    private String storageTicket = null;

    Pattern redirectPattern = Pattern.compile("<psf:redirectUrl>(.*)</psf:redirectUrl>");

    public SSO(String userName, String password, String policy, String nonce)
    {
        this.userName = userName;
        this.password = password;
        this.policy = policy;
        this.nonce = nonce;
    }

    public String getTicket()
    {
        return getTicket(null);
    }

    public String getTicket(String urlStr)
    {
        try
        {
            if(urlStr == null)
                urlStr = "http://login.live.com/RST.srf";

            URL url = new URL(urlStr);

            StringBuilder mess = new StringBuilder();

            mess.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
            mess.append("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"\r\n");
            mess.append(" xmlns:wsse=\"http://schemas.xmlsoap.org/ws/2003/06/secext\"\r\n");
            mess.append(" xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\"\r\n");
            mess.append(" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"\r\n");
            mess.append(" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"\r\n");
            mess.append(" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\"\r\n");
            mess.append(" xmlns:wssc=\"http://schemas.xmlsoap.org/ws/2004/04/sc\"\r\n");
            mess.append(" xmlns:wst=\"http://schemas.xmlsoap.org/ws/2004/04/trust\">\r\n");
            mess.append("<Header>\r\n");
            mess.append("  <ps:AuthInfo xmlns:ps=\"http://schemas.microsoft.com/Passport/SoapServices/PPCRL\" Id=\"PPAuthInfo\">\r\n");
            mess.append("    <ps:HostingApp>{7108E71A-9926-4FCB-BCC9-9A9D3F32E423}</ps:HostingApp>\r\n");
            mess.append("    <ps:BinaryVersion>4</ps:BinaryVersion>\r\n");
            mess.append("    <ps:UIVersion>1</ps:UIVersion>\r\n");
            mess.append("    <ps:Cookies></ps:Cookies>\r\n");
            mess.append("    <ps:RequestParams>AQAAAAIAAABsYwQAAAAxMDMz</ps:RequestParams>\r\n");
            mess.append("  </ps:AuthInfo>\r\n");
            mess.append("  <wsse:Security>\r\n");
            mess.append("    <wsse:UsernameToken Id=\"user\">\r\n");
            mess.append("      <wsse:Username>" + userName + "</wsse:Username>\r\n");
            mess.append("      <wsse:Password>" + password + "</wsse:Password>\r\n");
            mess.append("    </wsse:UsernameToken>\r\n");
            mess.append("  </wsse:Security>\r\n");
            mess.append("</Header>\r\n");
            mess.append("<Body>\r\n");

            mess.append("<ps:RequestMultipleSecurityTokens xmlns:ps=\"http://schemas.microsoft.com/Passport/SoapServices/PPCRL\" Id=\"RSTS\">\r\n");
            mess.append("  <wst:RequestSecurityToken Id=\"RST0\">\r\n");
            mess.append("    <wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>\r\n");
            mess.append("    <wsp:AppliesTo>\r\n");
            mess.append("      <wsa:EndpointReference>\r\n");
            mess.append("        <wsa:Address>http://Passport.NET/tb</wsa:Address>\r\n");
            mess.append("      </wsa:EndpointReference>\r\n");
            mess.append("    </wsp:AppliesTo>\r\n");
            mess.append("  </wst:RequestSecurityToken>\r\n");

            mess.append("<wst:RequestSecurityToken Id=\"RST1\">\r\n");
            mess.append("  <wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>\r\n");
            mess.append("  <wsp:AppliesTo>\r\n");
            mess.append("    <wsa:EndpointReference>\r\n");
            mess.append("      <wsa:Address>messengerclear.live.com</wsa:Address>\r\n");
            mess.append("     </wsa:EndpointReference>\r\n");
            mess.append("    </wsp:AppliesTo>\r\n");
            mess.append("   <wsse:PolicyReference URI=\"" + policy + "\"></wsse:PolicyReference>\r\n");
            mess.append("</wst:RequestSecurityToken>\r\n");

            mess.append("<wst:RequestSecurityToken Id=\"RST2\">\r\n");
            mess.append("  <wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>\r\n");
            mess.append("  <wsp:AppliesTo>\r\n");
            mess.append("   <wsa:EndpointReference>\r\n");
            mess.append("     <wsa:Address>messenger.msn.com</wsa:Address>\r\n");
            mess.append("   </wsa:EndpointReference>\r\n");
            mess.append("  </wsp:AppliesTo>\r\n");
            mess.append("  <wsse:PolicyReference URI=\"?id=507\"></wsse:PolicyReference>\r\n");
            mess.append("</wst:RequestSecurityToken>\r\n");

            mess.append("<wst:RequestSecurityToken Id=\"RST3\">\r\n");
            mess.append("  <wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>\r\n");
            mess.append("   <wsp:AppliesTo>\r\n");
            mess.append("        <wsa:EndpointReference>\r\n");
            mess.append("          <wsa:Address>contacts.msn.com</wsa:Address>\r\n");
            mess.append("        </wsa:EndpointReference>\r\n");
            mess.append("      </wsp:AppliesTo>\r\n");
            mess.append("      <wsse:PolicyReference URI=\"MBI\"></wsse:PolicyReference>\r\n");
            mess.append("    </wst:RequestSecurityToken>\r\n");

            mess.append("    <wst:RequestSecurityToken Id=\"RST4\">\r\n");
            mess.append("      <wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>\r\n");
            mess.append("      <wsp:AppliesTo>\r\n");
            mess.append("        <wsa:EndpointReference>\r\n");
            mess.append("          <wsa:Address>messengersecure.live.com</wsa:Address>\r\n");
            mess.append("        </wsa:EndpointReference>\r\n");
            mess.append("      </wsp:AppliesTo>\r\n");
            mess.append("      <wsse:PolicyReference URI=\"MBI_SSL\"></wsse:PolicyReference>\r\n");
            mess.append("    </wst:RequestSecurityToken>\r\n");

            mess.append("    <wst:RequestSecurityToken Id=\"RST5\">\r\n");
            mess.append("      <wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>\r\n");
            mess.append("      <wsp:AppliesTo>\r\n");
            mess.append("        <wsa:EndpointReference>\r\n");
            mess.append("          <wsa:Address>spaces.live.com</wsa:Address>\r\n");
            mess.append("        </wsa:EndpointReference>\r\n");
            mess.append("      </wsp:AppliesTo>\r\n");
            mess.append("      <wsse:PolicyReference URI=\"MBI\"></wsse:PolicyReference>\r\n");
            mess.append("    </wst:RequestSecurityToken>\r\n");

            mess.append("    <wst:RequestSecurityToken Id=\"RST6\">\r\n");
            mess.append("      <wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>\r\n");
            mess.append("      <wsp:AppliesTo>\r\n");
            mess.append("        <wsa:EndpointReference>\r\n");
            mess.append("          <wsa:Address>storage.msn.com</wsa:Address>\r\n");
            mess.append("        </wsa:EndpointReference>\r\n");
            mess.append("      </wsp:AppliesTo>\r\n");
            mess.append("      <wsse:PolicyReference URI=\"MBI\"></wsse:PolicyReference>\r\n");
            mess.append("    </wst:RequestSecurityToken>\r\n");
            mess.append("  </ps:RequestMultipleSecurityTokens>\r\n");
            mess.append("</Body>\r\n");
            mess.append("</Envelope>");

            DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
            HttpParams params = null;
            BasicHttpProcessor httpproc = null;

            params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, "UTF-8");
            HttpProtocolParams.setUserAgent(params, "MSN Explorer/9.0 (MSN 8.0; TmstmpExt)");
            HttpProtocolParams.setUseExpectContinue(params, false);

            httpproc = new BasicHttpProcessor();
            // Required protocol interceptors
            httpproc.addInterceptor(new RequestContent());
            httpproc.addInterceptor(new RequestTargetHost());
            // Recommended protocol interceptors
            httpproc.addInterceptor(new RequestConnControl());
            httpproc.addInterceptor(new RequestUserAgent());
            httpproc.addInterceptor(new RequestExpectContinue());

            HttpRequestExecutor httpexecutor = new HttpRequestExecutor();

            HttpContext context = new BasicHttpContext(null);

            HttpHost host = new HttpHost(url.getHost(), 443, "https");

            context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);


            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            Socket socket = factory.createSocket(host.getHostName(), host.getPort());

            conn.bind(socket, params);

            BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(
                "POST",
                url.getPath());
            request.setEntity(new XmlEntity(mess.toString()));

            context.setAttribute(ExecutionContext.HTTP_REQUEST, request);
            request.setParams(params);

            request.addHeader("Host", url.getHost());

            httpexecutor.preProcess(request, httpproc, context);

            HttpResponse response = httpexecutor.execute(request, conn, context);
            httpexecutor.postProcess(response, httpproc, context);

            if(logger.isDebugEnabled())
                logger.debug(response.getStatusLine());

            String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");

            if(logger.isDebugEnabled())
                logger.debug(response.getStatusLine() + " / " + responseStr);

            conn.close();

            int code = response.getStatusLine().getStatusCode();

            if(code > -1 && code != 200)
            {
                if(responseStr.indexOf("<faultcode>psf:Redirect</faultcode>") == -1)
                {
                    logger.error("*** Can't get passport ticket! http code = " + code);
                    return null;
                }


                Matcher m = redirectPattern.matcher(responseStr);
                if(!m.find())
                {
                    logger.error("*** redirect, but can't get redirect URL!");
                    return null;
                }
                else
                {
                    String redirectUrl = m.group(1);

                    if(urlStr.equals(redirectUrl))
                    {
                        logger.error("*** redirect, but redirect to same URL!");
                        return null;
                    }

                    return getTicket(redirectUrl);
                }
            }
            else
            {
                if(responseStr.indexOf("<faultcode>psf:Redirect</faultcode>") != -1)
                {
                    Matcher m = redirectPattern.matcher(responseStr);
                    if(m.find())
                    {
                        String redirectUrl = m.group(1);

                        if(urlStr.equals(redirectUrl))
                        {
                            logger.error("*** redirect, but redirect to same URL!");
                            return null;
                        }

                        return getTicket(redirectUrl);
                    }
                }
            }

            if(response.getStatusLine().getStatusCode() != 200)
            {
                logger.error("something wrong!", new Exception());
                return null;
            }

            return getTicketFromResponseXml(responseStr);
        }catch (Exception e)
        {
            logger.error("Login error ", e);
            // fire login error
        }

        return null;
    }

    private String getTicketFromResponseXml(String xml)
    {
        try
        {
            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
            dbfactory.setIgnoringComments(true);
            DocumentBuilder docBuilder;
            docBuilder = dbfactory.newDocumentBuilder();

            ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            Document doc = docBuilder.parse(in);

            Element el1 = XmlUtils.locateElement(doc.getDocumentElement(),
                "wsse:BinarySecurityToken", "Id", "Compact1");

            Element p1 = (Element) el1.getParentNode().getParentNode();
            Element p2 = XmlUtils.findChild(p1, "wst:RequestedProofToken");
            Element el2 = XmlUtils.findChild(p2, "wst:BinarySecret");

            String ticket = XmlUtils.getText(el1);
            String binSecret = XmlUtils.getText(el2).trim();

            Element webTicketEl = XmlUtils.locateElement(doc.getDocumentElement(),
                "wsse:BinarySecurityToken", "Id", "Compact2");
            webTicket = XmlUtils.getText(webTicketEl);

            Element contactTicketEl = XmlUtils.locateElement(doc.getDocumentElement(),
                "wsse:BinarySecurityToken", "Id", "Compact3");
            contactTicket = XmlUtils.getText(contactTicketEl);

            Element oimTicketEl = XmlUtils.locateElement(doc.getDocumentElement(),
                "wsse:BinarySecurityToken", "Id", "Compact4");
            oimTicket = XmlUtils.getText(oimTicketEl);

            Element spaceTicketEl = XmlUtils.locateElement(doc.getDocumentElement(),
                "wsse:BinarySecurityToken", "Id", "Compact5");
            spaceTicket = XmlUtils.getText(spaceTicketEl);

            Element storageTicketEl = XmlUtils.locateElement(doc.getDocumentElement(),
                "wsse:BinarySecurityToken", "Id", "Compact6");
            storageTicket = XmlUtils.getText(storageTicketEl);

            SSOticket SSOTicket = new SSOticket(binSecret, nonce.trim());

            //return the final ticket
            return ticket.trim() + " " + SSOTicket.value;
        } catch (Exception e)
        {
            logger.error("Login error ", e);
        }

        return null;
    }

    static class SSOticket
    {
        public String value;    // the ticket in form of a string

        private byte[] beginning;

        public SSOticket(String key, String nonce)
            throws Exception
        {
            // First of all, we need to create a structure of information, which elements' size is 4 bytes
            // To do that, we use an array which we can turn into a string, later...
            beginning = new byte[28];

            //StructHeaderSize = 28
            beginning[0] = 0x1c;
            beginning[1] = 0x00;
            beginning[2] = 0x00;
            beginning[3] = 0x00;

            //CryptMode = 1
            beginning[4] = 0x01;
            beginning[5] = 0x00;
            beginning[6] = 0x00;
            beginning[7] = 0x00;

            //CipherType = 0x6603
            beginning[8] = 0x03;
            beginning[9] = 0x66;
            beginning[10] = 0x00;
            beginning[11] = 0x00;

            //HashType = 0x8004
            beginning[12] = 0x04;
            beginning[13] = (byte)0x80;
            beginning[14] = 0x00;
            beginning[15] = 0x00;

            //IV length = 8
            beginning[16] = 0x08;
            beginning[17] = 0x00;
            beginning[18] = 0x00;
            beginning[19] = 0x00;

            //hash length = 20
            beginning[20] = 0x14;
            beginning[21] = 0x00;
            beginning[22] = 0x00;
            beginning[23] = 0x00;

            //cipher length = 72
            beginning[24] = 0x48;
            beginning[25] = 0x00;
            beginning[26] = 0x00;
            beginning[27] = 0x00;

            // now, we have to create a first, base64 decoded key, which we get from the input key
            byte[] key1 = Base64.decode(key);

            // then we calculate a second key through a specific algorithm (see function DeriveKey())
            byte[] key2 = deriveKey(key1, "WS-SecureConversationSESSION KEY HASH");

            // ...and a third key with the same algorithm...
            byte[] key3 = deriveKey(key1, "WS-SecureConversationSESSION KEY ENCRYPTION");

            // compute the hash
            byte[] hash = HMAC(key2, nonce.getBytes("UTF-8"));

            // now, we will use TrippleDES algorithm to transform the nonce to a block of 72 bytes...
            // create the initialization vector (which number's are not important, but better use random ;-))
            byte[] iv = { 0, 1, 2, 3, 4, 5, 6, 7 };

            // fill random iv
            //byte[] iv = new byte[8];
            //for (int i = 0; i < 8; i++)
            //{
            //    byte rand = (byte) Math.floor(Math.random() * 256);
            //    iv[i] = rand;
            //}

            // we have to fill the nonce with 8*8
            byte[] restOfNonce = { 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08 };

            byte[] output = DES3(key3, combine(nonce.getBytes("UTF-8"), restOfNonce), iv);

            // the final key will be a base64 encoded structure, composed by the beginning of the structure, the initialization vector, the SHA1 - Hash and the transformed block
            // string struc = Encoding.Default.GetString(Beginning) + Encoding.Default.GetString(iv) + Encoding.Default.GetString(hash) + Encoding.Default.GetString(output);
            String struc = new String(beginning, "ISO-8859-1") + new String(iv, "ISO-8859-1") + new String(hash, "ISO-8859-1") + new String(output, "ISO-8859-1");


            value = new String(Base64.encode(struc.getBytes("ISO-8859-1")));
        }

        // combine two byte arrays
        private byte[] combine(byte[] a, byte[] b)
        {
            byte[] c = new byte[a.length + b.length];
            System.arraycopy(a, 0, c, 0, a.length);
            System.arraycopy(b, 0, c, a.length, b.length);

            return c;
        }

        // specific algorithm to calculate a key...
        private byte[] deriveKey(byte[] key, String magic)
            throws Exception
        {
            byte hash1[] = HMAC(key, magic.getBytes("UTF-8"));
            byte hash2[] = HMAC(key, combine(hash1, magic.getBytes("UTF-8")));
            byte hash3[] = HMAC(key, hash1);
            byte hash4[] = HMAC(key, combine(hash3, magic.getBytes("UTF-8")));
            byte out[] = new byte[4];
            out[0] = hash4[0];
            out[1] = hash4[1];
            out[2] = hash4[2];
            out[3] = hash4[3];
            return combine(hash2, out);
        }

        private byte[] HMAC(byte[] key, byte[] subject)
        {
            try
            {
                Mac mac = Mac.getInstance("HmacSHA1");
                SecretKeySpec sk = new SecretKeySpec(key, "HmacSHA1");

                mac.init(sk);
                return mac.doFinal(subject);
            }
            catch (NoSuchAlgorithmException ex)
            {
                ex.printStackTrace();
            }
            catch (InvalidKeyException ex)
            {
                ex.printStackTrace();
            }

            return null;
        }

        private byte[] DES3(byte[] key, byte[] subject, byte[] iv)
        {
            try
            {
                Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
                SecretKeySpec sk = new SecretKeySpec(key, "DESede");

                IvParameterSpec sr = new IvParameterSpec(iv);
                cipher.init(Cipher.ENCRYPT_MODE, sk, sr);
                return cipher.doFinal(subject);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
            return null;
        }
    }

    public String getContactTicket()
    {
        return contactTicket;
    }

    public String getOimTicket()
    {
        return oimTicket;
    }

    public String getSpaceTicket()
    {
        return spaceTicket;
    }

    public String getStorageTicket()
    {
        return storageTicket;
    }

    public String getWebTicket()
    {
        return webTicket;
    }
}
