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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.Vector;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.jml.MsnContactPending;
import net.sf.jml.MsnGroup;
import net.sf.jml.Email;
import net.sf.jml.MsnContact;
import net.sf.jml.MsnList;
import net.sf.jml.MsnProtocol;
import net.sf.jml.impl.AbstractMessenger;
import net.sf.jml.impl.MsnContactImpl;
import net.sf.jml.impl.MsnContactListImpl;
import net.sf.jml.impl.MsnGroupImpl;
import net.sf.jml.protocol.MsnSession;
import net.sf.jml.protocol.outgoing.OutgoingADL;
import net.sf.jml.protocol.outgoing.OutgoingBLP;
import net.sf.jml.protocol.outgoing.OutgoingCHG;
import net.sf.jml.protocol.outgoing.OutgoingRML;
import net.sf.jml.protocol.outgoing.OutgoingUUX;
import net.sf.jml.util.XmlUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
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
 * Manages contact list through the webservices exposed for
 * MSN13 and later.
 *
 * @author Damian Minkov
 */
public class ContactList
{
    private static final Log logger = LogFactory.getLog(ContactList.class);

    private SSO sso;
    private MsnSession session;

    private static final String membership_url =
        "https://local-bay.contacts.msn.com/abservice/SharingService.asmx";

    private static final String membership_soap =
        "http://www.msn.com/webservices/AddressBook/FindMembership";
    private static final String membership_soap_delete =
        "http://www.msn.com/webservices/AddressBook/DeleteMember";
    private static final String membership_soap_add =
        "http://www.msn.com/webservices/AddressBook/AddMember";


    private static final String addressbook_url =
        "https://local-bay.contacts.msn.com/abservice/abservice.asmx";

    private static final String addressbook_action_findall =
        "http://www.msn.com/webservices/AddressBook/ABFindAll";
    private static final String addressbook_action_add =
        "http://www.msn.com/webservices/AddressBook/ABAdd";
    private static final String addressbook_action_groupadd =
        "http://www.msn.com/webservices/AddressBook/ABGroupAdd";
    private static final String addressbook_action_groupdelete =
        "http://www.msn.com/webservices/AddressBook/ABGroupDelete";
    private static final String addressbook_action_groupupdate =
        "http://www.msn.com/webservices/AddressBook/ABGroupUpdate";
    private static final String addressbook_action_contactadd =
        "http://www.msn.com/webservices/AddressBook/ABContactAdd";
    private static final String addressbook_action_groupContactAdd =
        "http://www.msn.com/webservices/AddressBook/ABGroupContactAdd";
    private static final String addressbook_action_groupContactDelete =
        "http://www.msn.com/webservices/AddressBook/ABGroupContactDelete";
    private static final String addressbook_action_contactdelete =
        "http://www.msn.com/webservices/AddressBook/ABContactDelete";
    private static final String addressbook_action_contactupdate =
        "http://www.msn.com/webservices/AddressBook/ABContactUpdate";

    private Hashtable membersRoles = new Hashtable();

    private String myDisplayName = null;

    private static final String PREF_HOTNAME_NODE_START = "<PreferredHostName>";
    private static final String PREF_HOTNAME_NODE_END = "</PreferredHostName>";
    
    private static boolean ADLSent=false;
    private static boolean firstADLReply=true;

    ContactList(){this(null);}

    public ContactList(MsnSession session)
    {
        this.sso = session.getSSO();
        this.session = session;

        membersRoles.put("Allow",
            new MemberRole("Allow", MsnList.AL));
        membersRoles.put("Block",
            new MemberRole("Block", MsnList.BL));
        membersRoles.put("Reverse",
            new MemberRole("Reverse", MsnList.RL));
        membersRoles.put("Pending",
            new MemberRole("Pending", MsnList.PL));
    }

    private synchronized String sendRequest(String body, String address, String soapAddress, String method)
    {
        return sendRequest(body, address, soapAddress, method, false);
    }

    private synchronized String sendRequest(String body, String address, String soapAddress, String method, boolean secondTry)
    {
        //ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();
        DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
        HttpParams params = null;
        BasicHttpProcessor httpproc = null;

        try
        {
            if(logger.isTraceEnabled())
                logger.trace("Will send body: " + body + " using address " + address);

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

            context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);


            if (!conn.isOpen())
            {
                // msn change their certificate with invalid one,
                // which prevents us from retreiving contacts and maging them
                // we install dummy trust man ager in order to fix it
                SSLContext sc = SSLContext.getInstance("SSLv3");
                TrustManager[] tma = {new DummyTrustManager()};
                sc.init(null, tma, null);

                SSLSocketFactory factory = sc.getSocketFactory();
                Socket socket = factory.createSocket(host.getHostName(), host.getPort());

                conn.bind(socket, params);
            }

            BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(
                method,
                url.getPath());
            request.setEntity(new XmlEntity(body));

            context.setAttribute(ExecutionContext.HTTP_REQUEST, request);
            request.setParams(params);

            if(soapAddress != null)
                request.addHeader("SOAPAction", soapAddress);

            request.addHeader("Host", url.getHost());
            request.addHeader("Accept", "text/*");

            httpexecutor.preProcess(request, httpproc, context);

            HttpResponse response = httpexecutor.execute(request, conn, context);
            httpexecutor.postProcess(response, httpproc, context);

            if(logger.isDebugEnabled())
            {
                for(org.apache.http.Header h : response.getAllHeaders())
                {
                    logger.debug("Header - " + h.getName() + ":" + h.getValue());
                }
            }

            String resultStr = EntityUtils.toString(response.getEntity());

            if(logger.isDebugEnabled())
                logger.debug(response.getStatusLine() + " / " + resultStr);

            //if (!connStrategy.keepAlive(response, context))
            try
            {
                conn.close();
            }
            catch (Exception e)
            {
                logger.error("error closing connection", e);
            }

            params = null;

            if(response.getStatusLine().getStatusCode() != 200)
            {
                // avoid loop by setting secondTry
                if(!secondTry)
                {
                    Header locationHeader;
                    if((locationHeader = response.getFirstHeader("Location"))
                        != null)
                    {
                        return sendRequest(
                                body,
                                locationHeader.getValue(),
                                soapAddress, method, true);
                    }
                    else
                    {
                        // check for PreferredHostName and test there
                        // TODO dirty check, fix it with xml parse
                        int ix = -1;
                        if((ix = resultStr.indexOf(PREF_HOTNAME_NODE_START)) != -1)
                        {
                            int ix2 = resultStr.indexOf(PREF_HOTNAME_NODE_END, ix);
                            String newHost =
                                resultStr.substring(ix + PREF_HOTNAME_NODE_START.length(), ix2);

                            String oldHost = new URL(address).getHost();
                            String newHostUrl = address.replace(oldHost, newHost);

                            return sendRequest(body, newHostUrl, soapAddress, method, true);
                        }
                    }
                }


                logger.error("something wrong!", new Exception());
                logger.info("Error xml:" + resultStr);
                return null;
            }

            return resultStr;
        }
        catch(Exception e)
        {
            logger.error("Sending request", e);
        }

        return null;
    }

    private void waitFor(long time)
    {
        Object o = new Object();
        synchronized(o)
        {
            try
            {
                o.wait(time);
            }
            catch (Exception e){}
        }
    }

    private String getRequestBody(boolean isAddressbook)
    {
        StringBuilder mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        mess.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\r\n");
        mess.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n");
        mess.append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\r\n");
        mess.append(" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">\r\n");
        mess.append("<soap:Header>\r\n");
        mess.append(" <ABApplicationHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
        mess.append("  <ApplicationId>996CDE1E-AA53-4477-B943-2BE802EA6166</ApplicationId>\r\n");
        mess.append("   <IsMigration>false</IsMigration>\r\n");
        mess.append("   <PartnerScenario>Initial</PartnerScenario>\r\n");
        mess.append("  </ABApplicationHeader>\r\n");
        mess.append("  <ABAuthHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
        mess.append("   <ManagedGroupRequest>false</ManagedGroupRequest>\r\n");
        mess.append("   <TicketToken>" + sso.getContactTicket().replaceAll("&", "&amp;") + "</TicketToken>\r\n");
        mess.append("  </ABAuthHeader>\r\n");
        mess.append("</soap:Header>\r\n");
        mess.append("<soap:Body>\r\n");

        if(isAddressbook)
        {
            mess.append("<ABFindAll xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
            mess.append(" <abId>00000000-0000-0000-0000-000000000000</abId>\r\n");
            mess.append(" <abView>Full</abView>\r\n");
            mess.append(" <deltasOnly>false</deltasOnly>\r\n");
            mess.append(" <lastChange>0001-01-01T00:00:00.0000000-08:00</lastChange>\r\n");
            mess.append("</ABFindAll>\r\n");
        }
        else
        {
            mess.append("<FindMembership xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
            mess.append(" <serviceFilter>\r\n");
            mess.append("  <Types>\r\n");
            mess.append("   <ServiceType>Messenger</ServiceType>\r\n");
            mess.append("   <ServiceType>Invitation</ServiceType>\r\n");
            mess.append("   <ServiceType>SocialNetwork</ServiceType>\r\n");
            mess.append("   <ServiceType>Space</ServiceType>\r\n");
            mess.append("   <ServiceType>Profile</ServiceType>\r\n");
            mess.append("  </Types>\r\n");
            mess.append(" </serviceFilter>\r\n");
            mess.append("</FindMembership>\r\n");
        }

        mess.append("</soap:Body>\r\n");
        mess.append("</soap:Envelope>");

        return mess.toString();
    }

    private String getRequestBodyAddressBookAdd()
    {
        StringBuilder mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");
        mess.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\r\n");
        mess.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n");
        mess.append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\r\n");
        mess.append(" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">\r\n");
        mess.append("<soap:Header>\r\n");
        mess.append("<ABApplicationHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
        mess.append("<ApplicationId>996CDE1E-AA53-4477-B943-2BE802EA6166</ApplicationId>\r\n");
        mess.append("<IsMigration>false</IsMigration>\r\n");
        mess.append("<PartnerScenario>Initial</PartnerScenario>\r\n");
        mess.append("</ABApplicationHeader>\r\n");
        mess.append("<ABAuthHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
        mess.append("<ManagedGroupRequest>false</ManagedGroupRequest>\r\n");
        mess.append("<TicketToken>" + sso.getContactTicket().replaceAll("&", "&amp;") + "</TicketToken>\r\n");
        mess.append("</ABAuthHeader>\r\n");
        mess.append("</soap:Header>\r\n");
        mess.append("<soap:Body>\r\n");

        mess.append("<ABAdd xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
        mess.append("<abInfo>\r\n");
        mess.append("<name/>\r\n");
        mess.append("<ownerPuid>0</ownerPuid>\r\n");
        mess.append("<ownerEmail> " + session.getMessenger().getOwner().getEmail().getEmailAddress() + " </ownerEmail>\r\n");
        mess.append("<fDefault>true</fDefault>\r\n");
        mess.append("</abInfo>\r\n");
        mess.append("</ABAdd>\r\n");

        mess.append("</soap:Body>");
        mess.append("</soap:Envelope>");

        return mess.toString();
    }

    public void dispatch()
    {
        String membersRes = sendRequest(
            getRequestBody(false),
            membership_url,
            membership_soap,
            "POST");

        processMembers(membersRes);

        waitFor(400);

        String addBookRes = sendRequest(
            getRequestBody(true),
            addressbook_url,
            addressbook_action_findall,
            "POST");

        if(addBookRes == null)
        {
            // book may not exist so create it
            sendRequest(
                getRequestBodyAddressBookAdd(),
                addressbook_url,
                addressbook_action_add,
                "POST");
            waitFor(400);
            // and request it again
            addBookRes = sendRequest(
                getRequestBody(true),
                addressbook_url,
                addressbook_action_findall,
                "POST");
        }

        processContactList(addBookRes);
        //processInit();

    }

    /**
     * Returns the contact id of the corresponding email
     *
     * @param xmlStr
     * @param conatctEmail
     * @return
     */
    private String getContactID(String xmlStr, String conatctEmail)
    {
        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        dbfactory.setIgnoringComments(true);
        DocumentBuilder docBuilder;
        Document doc;
        try
        {

            docBuilder = dbfactory.newDocumentBuilder();

            ByteArrayInputStream in = new ByteArrayInputStream(xmlStr.getBytes("UTF-8"));
            doc = docBuilder.parse(in);

        }catch (Exception ex)
        {
            logger.error("", ex);
            return null;
        }

        Element el1 =
            XmlUtils.locateElement(
                doc.getDocumentElement(),
                "ABFindAllResponse",
                "xmlns",
                "http://www.msn.com/webservices/AddressBook");
        Element contactListResults = XmlUtils.findChild(el1, "ABFindAllResult");

        Element contacts = XmlUtils.findChild(contactListResults, "contacts");

        // there maybe no contacts
        if(contacts == null)
            return null;

        List cs = XmlUtils.findChildren(contacts, "Contact");

        Iterator iter = cs.iterator();
        while (iter.hasNext())
        {
            Element el = (Element)iter.next();

            Element cIdEl = XmlUtils.findChild(el, "contactId");

            String contactId = XmlUtils.getText(cIdEl).trim();

            Element contactInfoEl = XmlUtils.findChild(el, "contactInfo");

            Element contactTypeEl = XmlUtils.findChild(contactInfoEl, "contactType");
            String cType = XmlUtils.getText(contactTypeEl);

            if(cType.equalsIgnoreCase("me"))
                continue;

            /* ignore non-messenger contacts */
            Element isMessengerUserEl = XmlUtils.findChild(contactInfoEl, "isMessengerUser");
            boolean isMessengerUser = Boolean.parseBoolean(XmlUtils.getText(isMessengerUserEl));

            if(!isMessengerUser)
                continue;

            Element emailNameEl = XmlUtils.findChild(contactInfoEl, "passportName");
            String email = XmlUtils.getText(emailNameEl);

            if(email == null)
            {
                // new
                Element isMessengerEnabledEl =
                    XmlUtils.findChildByChain(contactInfoEl,
                        new String[]{"emails", "ContactEmail", "isMessengerEnabled"});

                if(isMessengerEnabledEl == null ||
                    !Boolean.parseBoolean(XmlUtils.getText(isMessengerEnabledEl)))
                    continue;

                Element emailEl = XmlUtils.findChildByChain(contactInfoEl,
                    new String[]{"emails", "ContactEmail", "email"});

                if(emailEl == null)
                    continue;

                email = XmlUtils.getText(emailEl);

                if(email == null)
                    continue;
            }

            if(email.equals(conatctEmail))
                return contactId;
        }

        return null;
    }

    private void processContactList(String xmlStr)
    {
        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        dbfactory.setIgnoringComments(true);
        DocumentBuilder docBuilder;
        Document doc;
        try
        {

            docBuilder = dbfactory.newDocumentBuilder();

            ByteArrayInputStream in = new ByteArrayInputStream(xmlStr.getBytes("UTF-8"));
            doc = docBuilder.parse(in);
        }
        catch (ParserConfigurationException ex)
        {
            logger.error("", ex);
            return;
        }catch (SAXException ex)
        {
            logger.error("", ex);
            return;
        }catch (IOException ex)
        {
            logger.error("", ex);
            return;
        }

        Element el =
            XmlUtils.locateElement(
                doc.getDocumentElement(),
                "ABFindAllResponse",
                "xmlns",
                "http://www.msn.com/webservices/AddressBook");
        Element contactListResults = XmlUtils.findChild(el, "ABFindAllResult");

        Element groupsResults = XmlUtils.findChild(contactListResults, "groups");

        Element contactsResults = XmlUtils.findChild(contactListResults, "contacts");

        processGroups(groupsResults);
        processContacts(contactsResults);

        AbstractMessenger messenger = (AbstractMessenger)session.getMessenger();

        messenger.fireContactListSyncCompleted(); //Sync completed

        OutgoingBLP outgoing = new OutgoingBLP(messenger.getActualMsnProtocol());
        outgoing.setOnlyNotifyAllowList(false);
        messenger.send(outgoing);
        sendContactList();
    }
    
    //Solved ADL long-contact-list issue - BLuEGoD
    public void sendContactList(){
        AbstractMessenger messenger = (AbstractMessenger)session.getMessenger();
        MsnProtocol protocol = messenger.getActualMsnProtocol();

        if (protocol.after(MsnProtocol.MSNP12))
        {
            MsnContactListImpl contactList = (MsnContactListImpl) session
                .getMessenger().getContactList();
            MsnContact[] cs = contactList.getContacts();
            int cs_size = cs.length;
            Queue<MsnContact> csq = new LinkedList<MsnContact>();
            try
            {
                // don't know why 220 instead of ~250, but MSN server don't like it
                // we should put this lower if the server disconnects after an ADL is made - BLuEGoD
                // Each ADL command may contain up to 150 contacts - damencho
                    for(int i=0, j=1; i < cs_size; i++,j++)
                    {
                        if(cs[i]!=null)
                            csq.add(cs[i]);

                        if(j == 150 || i == cs_size-1)
                        {
                            //waitFor(6000);
                            OutgoingADL o1 = new OutgoingADL(messenger.getActualMsnProtocol());

                            if(firstADLReply)
                                ADLSent = (cs_size-1)==i?true:false;

                            o1.addContacts(csq.toArray(new MsnContact[j] ));
                            messenger.send(o1);
                            j=0;
                            csq.clear();
                        }
                    }

                    // continue contact list init process if there are no contacts
                    if(cs.length == 0 && firstADLReply)
                        session.getContactList().processInit();

                    firstADLReply=false; //prevents calling processInit again! Check: RFS Incoming
            }catch(Exception e){
                logger.error("Contact list process error: "+e.toString());
            }
            
        }
    }
    
    public boolean isADLSent(){
        return ADLSent;
    }
    
    public void processInit(){
        
        /* This should be for ex. at MsnOwner instead of in this method,
         * for call such method from both SYN and ADL reply.
         * if MSNP>13 then this is called from the ADL reply *it must*
         * else it's not called but a similar algorithm is in the SYN reply.
         * From <http://msnpiki.msnfanatic.com/> MSNP protocol the order
         * for this command in the ADL reply MUST be the one implemented here.
         * 
         * Note that several things like personal message, display name, etc
         * has been modified to let all the things works together. 
         * 
         * James Lopez (BLuEGoD) - <bluegod at bluegod.net>
         */
        
        AbstractMessenger messenger = (AbstractMessenger)session.getMessenger();
        MsnProtocol protocol = messenger.getActualMsnProtocol();
        messenger.getOwner().setDisplayName(messenger.getOwner().getDisplayName());
        
        protocol = messenger.getActualMsnProtocol();
        if (protocol.after(MsnProtocol.MSNP9)){
            OutgoingUUX uuxmessage = new OutgoingUUX(protocol);
            uuxmessage.setPersonalMessage(messenger.getOwner().getPersonalMessage());
            uuxmessage.setMachineGuid("{F26D1F07-95E2-403C-BC18-D4BFED493428}");
            messenger.send(uuxmessage);
        }
        
        OutgoingCHG message = new OutgoingCHG(protocol);
        message.setStatus(messenger.getOwner().getInitStatus());
        message.setClientId(messenger.getOwner().getClientId());
        message.setDisplayPicture(messenger.getOwner().getDisplayPicture());
        message.setFirstSend(true);
        messenger.send(message);
        
 
   
    }

    private void processGroups(Element groups)
    {
        // there maybe no groups
        if(groups == null)
            return;

        List gs = XmlUtils.findChildren(groups, "Group");

        MsnContactListImpl contactList = (MsnContactListImpl) session
                .getMessenger().getContactList();

        Iterator iter = gs.iterator();
        while (iter.hasNext())
        {
            Element el = (Element)iter.next();

            Element grIdEl = XmlUtils.findChild(el, "groupId");

            String grId = XmlUtils.getText(grIdEl).trim();

            Element grInfoEl = XmlUtils.findChild(el, "groupInfo");
            Element grNameEl = XmlUtils.findChild(grInfoEl, "name");
            String name = XmlUtils.getText(grNameEl);

            MsnGroupImpl group = new MsnGroupImpl(contactList);
            group.setGroupId(grId);
            group.setGroupName(name);
            contactList.addGroup(group);
        }
    }

    private void processContacts(Element contacts)
    {
        // there maybe no contacts
        if(contacts == null)
            return;

        List cs = XmlUtils.findChildren(contacts, "Contact");

        MsnContactListImpl contactList = (MsnContactListImpl) session
                .getMessenger().getContactList();

        Iterator iter = cs.iterator();
        while (iter.hasNext())
        {
            Element el = (Element)iter.next();

            Element cIdEl = XmlUtils.findChild(el, "contactId");

            MsnContactImpl contact = new MsnContactImpl(contactList);

            String contactId = XmlUtils.getText(cIdEl).trim();
            contact.setId(contactId);

            Element contactInfoEl = XmlUtils.findChild(el, "contactInfo");

            Element contactTypeEl = XmlUtils.findChild(contactInfoEl, "contactType");
            String cType = XmlUtils.getText(contactTypeEl);

            if(cType.equalsIgnoreCase("me"))
            {
                Element contactDisplayNameEl =
                    XmlUtils.findChild(contactInfoEl, "displayName");
                myDisplayName = XmlUtils.getText(contactDisplayNameEl);

                continue;
            }

            /* ignore non-messenger contacts */
            Element isMessengerUserEl = XmlUtils.findChild(contactInfoEl, "isMessengerUser");
            boolean isMessengerUser = Boolean.parseBoolean(XmlUtils.getText(isMessengerUserEl));

            if(!isMessengerUser)
                continue;

            Element emailNameEl = XmlUtils.findChild(contactInfoEl, "passportName");
            String email = XmlUtils.getText(emailNameEl);

            if(email == null)
            {
                // new
                Element isMessengerEnabledEl =
                    XmlUtils.findChildByChain(contactInfoEl,
                        new String[]{"emails", "ContactEmail", "isMessengerEnabled"});

                if(isMessengerEnabledEl == null ||
                    !Boolean.parseBoolean(XmlUtils.getText(isMessengerEnabledEl)))
                    continue;

                Element emailEl = XmlUtils.findChildByChain(contactInfoEl,
                    new String[]{"emails", "ContactEmail", "email"});

                if(emailEl == null)
                    continue;

                email = XmlUtils.getText(emailEl);

                if(email == null)
                    continue;
            }

            contact.setEmail(Email.parseStr(email));

            Element groupsEl = XmlUtils.findChild(contactInfoEl, "groupIds");
            if(groupsEl != null)
            {
                List grIdsEls = XmlUtils.findChildren(groupsEl, "guid");
                Iterator i = grIdsEls.iterator();
                while (i.hasNext())
                {
                    Element guid = (Element)i.next();
                    contact.addBelongGroup(XmlUtils.getText(guid).trim());
                }
            }

            Element displayNameEl = XmlUtils.findChild(contactInfoEl, "displayName");
            String displayName = XmlUtils.getText(displayNameEl);
            contact.setFriendlyName(displayName);
            contact.setDisplayName(displayName);

            // telephone ??

            // list number is sum of all lists that the contact is in.
            int listNumber = 0;
            Iterator i = membersRoles.entrySet().iterator();
            while (i.hasNext())
            {
                Entry e = (Entry)i.next();

                MemberRole mr = (MemberRole)e.getValue();

                if(mr.getMembers().contains(email))
                {
                    listNumber += mr.list.getListId();
                }
            }

            // something is wrong, contact in noone list
            if(listNumber == 0)
                continue;

            contact.setListNumber(listNumber);

            if(contact.isInList(MsnList.AL))
                contact.setInList(MsnList.FL, true);

            // Fix for error 241 (contact in allow and blocked list)
            if(contact.isInList(MsnList.AL) && contact.isInList(MsnList.BL))
            {
                contact.setInList(MsnList.FL, false);
                contact.setInList(MsnList.AL, false);
                contact.setInList(MsnList.PL, false);
            }
            
            /* It seems that such fix doesn't work as expected,
             * I leave it there anyway, but I see a l=5 in other clients raw log!
             * There's other strange 241 error I got that should be solved with the code below
             * (James Lopez - BLuEGoD)
             */
            if((listNumber | MsnList.RL.getListId())==MsnList.RL.getListId())
                contact.setInList(MsnList.PL, true);
            contactList.addContact(contact);
        }
    }

    private void processMembers(String xml)
    {
        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        dbfactory.setIgnoringComments(true);
        DocumentBuilder docBuilder;
        Document doc;
        try{

            docBuilder = dbfactory.newDocumentBuilder();

            ByteArrayInputStream in = new
            ByteArrayInputStream(xml.getBytes("UTF-8"));
            doc = docBuilder.parse(in);
        }
        catch (ParserConfigurationException ex){
            logger.error("",ex);
            return;
        }catch (SAXException ex){
            logger.error("",ex);
            return;
        }catch (IOException ex){
            logger.error("",ex);
            return;
        }

        Element el =
            XmlUtils.locateElement(doc.getDocumentElement(),"FindMembershipResponse","xmlns",
            "http://www.msn.com/webservices/AddressBook");

        List membershipsList = XmlUtils.findChildrenByChain(el,new String[] {
                "FindMembershipResult","Services","Service",
        "Memberships" });

        Iterator memberIter = membershipsList.iterator();
        while (memberIter.hasNext()){
            Element e1 = (Element)memberIter.next();
            List memberships = XmlUtils.findChildren(e1,"Membership");
            Iterator iter = memberships.iterator();
            while (iter.hasNext()){
                Element m = (Element)iter.next();
                Element mr = XmlUtils.findChild(m,"MemberRole");
                String role = XmlUtils.getText(mr);

                MemberRole memberRole = (MemberRole)membersRoles.get(role);

                // there maybe some member roles we are not interested for
                if (memberRole == null) continue;

                if (role.equals("Pending")
                        && XmlUtils.getText(
                                (Element)XmlUtils.findChildrenByChain((Element)e1.getParentNode(),
                                        new String[] { "Info","Handle","Type"
                                }).get(0)).equals("Messenger")){
                    List ms =
                        XmlUtils.locateElements(m,"Member","xsi:type","PassportMember");
                    Iterator i = ms.iterator();
                    while (i.hasNext()){
                        Element mem = (Element)i.next();

                        Element mn = XmlUtils.findChild(mem,"DisplayName");
                        String name = XmlUtils.getText(mn);
                        mn = XmlUtils.findChild(mem,"JoinedDate");
                        Date joinedDate = null;
                        try{
                            joinedDate =
                                DatatypeFactory.newInstance().newXMLGregorianCalendar(XmlUtils.getText(mn))

                                .toGregorianCalendar().getTime();
                        }catch (DatatypeConfigurationException e){
                            e.printStackTrace();
                        }
                        mn = XmlUtils.findChild(mem,"PassportName");
                        String email = XmlUtils.getText(mn);

                        ArrayList<MsnContactPending> pendingList =
                            ((AbstractMessenger)session.getMessenger()).getPendingList();
                        pendingList.add(new
                                MsnContactPending(Email.parseStr(email),name,joinedDate));
                       
                        ((AbstractMessenger) session.getMessenger())
                        .fireContactAddedMe(session.getMessenger().getPendingList()
                                .toArray(new MsnContactPending[] {}));
                    }
                }else{
                    List ms =
                        XmlUtils.locateElements(m,"Member","xsi:type","PassportMember");

                    Iterator i = ms.iterator();
                    while (i.hasNext()){
                        Element mem = (Element)i.next();

                        Element mn = XmlUtils.findChild(mem,"PassportName");
                        String name = XmlUtils.getText(mn);

                        memberRole.addMember(name);
                    }
                }
            }
        }
    }


    public void createGroup(String groupName)
    {
        StringBuilder mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        mess.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"");
        mess.append("               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        mess.append("               xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
        mess.append("               xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">");
        mess.append("<soap:Header>");
        mess.append("    <ABApplicationHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <ApplicationId>996CDE1E-AA53-4477-B943-2BE802EA6166</ApplicationId>");
        mess.append("        <IsMigration>false</IsMigration>");
        mess.append("        <PartnerScenario>GroupSave</PartnerScenario>");
        mess.append("    </ABApplicationHeader>");
        mess.append("    <ABAuthHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <ManagedGroupRequest>false</ManagedGroupRequest>");
        mess.append("        <TicketToken>" + sso.getContactTicket().replaceAll("&", "&amp;") + "</TicketToken>");
        mess.append("    </ABAuthHeader>");
        mess.append("</soap:Header>");
        mess.append("<soap:Body>");

        mess.append("    <ABGroupAdd xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <abId>00000000-0000-0000-0000-000000000000</abId>");
        mess.append("        <groupAddOptions>");
        mess.append("           <fRenameOnMsgrConflict>false</fRenameOnMsgrConflict>");
        mess.append("        </groupAddOptions>");
        mess.append("        <groupInfo>");
        mess.append("           <GroupInfo>");
        mess.append("               <name>" + groupName + "</name>");
        mess.append("               <groupType>"+ "C8529CE2-6EAD-434d-881F-341E17DB3FF8</groupType>");
        mess.append("               <fMessenger>false</fMessenger>");
        mess.append("               <annotations>");
        mess.append("                  <Annotation>");
        mess.append("                      <Name>MSN.IM.Display</Name>");
        mess.append("                      <Value>1</Value>");
        mess.append("                  </Annotation>");
        mess.append("               </annotations>");
        mess.append("           </GroupInfo>");
        mess.append("        </groupInfo>");
        mess.append("    </ABGroupAdd>");

        mess.append("</soap:Body>");
        mess.append("</soap:Envelope>");



        String res = sendRequest(
            mess.toString(), addressbook_url, addressbook_action_groupadd, "POST");

        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        dbfactory.setIgnoringComments(true);
        DocumentBuilder docBuilder;
        Document doc;
        try
        {

            docBuilder = dbfactory.newDocumentBuilder();

            ByteArrayInputStream in = new ByteArrayInputStream(res.getBytes("UTF-8"));
            doc = docBuilder.parse(in);
        }


        catch (ParserConfigurationException ex)
        {
            logger.error("", ex);
            return;
        }        catch (SAXException ex)
        {
            logger.error("", ex);
            return;
        }        catch (IOException ex)
        {
            logger.error("", ex);
            return;
        }

        Element el =
            XmlUtils.locateElement(
                doc.getDocumentElement(),
                "ABGroupAddResponse",
                "xmlns",
                "http://www.msn.com/webservices/AddressBook");

        Element e = XmlUtils.findChildByChain(el,
            new String[]{"ABGroupAddResult", "guid"});

        // todo : if something missing throw failed event
        if(e == null)
            return;

        String guid = XmlUtils.getText(e);

        MsnContactListImpl contactList = (MsnContactListImpl) session
                .getMessenger().getContactList();

        MsnGroupImpl group = new MsnGroupImpl(contactList);
        group.setGroupId(guid);
        group.setGroupName(groupName);
        contactList.addGroup(group);

        ((AbstractMessenger) session.getMessenger())
                .fireGroupAddCompleted(group);
    }

    public void removeGroup(String groupId)
    {
        StringBuilder mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");
        mess.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\r\n");
        mess.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n");
        mess.append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\r\n");
        mess.append(" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">\r\n");
        mess.append("<soap:Header>\r\n");
        mess.append("<ABApplicationHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
        mess.append("<ApplicationId>996CDE1E-AA53-4477-B943-2BE802EA6166</ApplicationId>\r\n");
        mess.append("<IsMigration>false</IsMigration>\r\n");
        mess.append("<PartnerScenario>Timer</PartnerScenario>\r\n");
        mess.append("</ABApplicationHeader>\r\n");
        mess.append("<ABAuthHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
        mess.append("<ManagedGroupRequest>false</ManagedGroupRequest>\r\n");
        mess.append("<TicketToken>" + sso.getContactTicket().replaceAll("&", "&amp;") + "</TicketToken>\r\n");
        mess.append("</ABAuthHeader>\r\n");
        mess.append("</soap:Header>\r\n");
        mess.append("<soap:Body>\r\n");

        mess.append("<ABGroupDelete xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
        mess.append("<abId>00000000-0000-0000-0000-000000000000</abId>\r\n");
        mess.append("<groupFilter>\r\n");
        mess.append("<groupIds><guid>" + groupId + "</guid></groupIds>\r\n");
        mess.append("</groupFilter>\r\n");
        mess.append("</ABGroupDelete>\r\n");

        mess.append("</soap:Body>\r\n");
        mess.append("</soap:Envelope>");

        String res = sendRequest(
            mess.toString(), addressbook_url, addressbook_action_groupdelete, "POST");

        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        dbfactory.setIgnoringComments(true);
        DocumentBuilder docBuilder;
        Document doc;
        try
        {

            docBuilder = dbfactory.newDocumentBuilder();

            ByteArrayInputStream in = new ByteArrayInputStream(res.getBytes("UTF-8"));
            doc = docBuilder.parse(in);
        }
        catch (ParserConfigurationException ex)
        {
            logger.error("", ex);
            return;
        }catch (SAXException ex)
        {
            logger.error("", ex);
            return;
        }catch (IOException ex)
        {
            logger.error("", ex);
            return;
        }

        Element el =
            XmlUtils.locateElement(
                doc.getDocumentElement(),
                "ABGroupDeleteResponse",
                "xmlns",
                "http://www.msn.com/webservices/AddressBook");

        // todo if something missing throw failed event
        if(el == null || el.getChildNodes().getLength() > 0)
            return;

        MsnContactListImpl contactList = (MsnContactListImpl) session
                .getMessenger().getContactList();

        MsnGroup group = contactList.getGroup(groupId);
        contactList.removeGroup(groupId);

        ((AbstractMessenger) session.getMessenger())
                .fireGroupRemoveCompleted(group);
    }

    public void renameGroup(String groupId, String newGroupName)
    {
        StringBuilder mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        mess.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"");
        mess.append("               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        mess.append("               xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
        mess.append("               xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">");
        mess.append("<soap:Header>");
        mess.append("    <ABApplicationHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <ApplicationId>996CDE1E-AA53-4477-B943-2BE802EA6166</ApplicationId>");
        mess.append("        <IsMigration>false</IsMigration>");
        mess.append("        <PartnerScenario>GroupSave</PartnerScenario>");
        mess.append("    </ABApplicationHeader>");
        mess.append("    <ABAuthHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <ManagedGroupRequest>false</ManagedGroupRequest>");
        mess.append("        <TicketToken>" + sso.getContactTicket().replaceAll("&", "&amp;") + "</TicketToken>");
        mess.append("    </ABAuthHeader>");
        mess.append("</soap:Header>");
        mess.append("<soap:Body>");

        mess.append("    <ABGroupUpdate xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <abId>00000000-0000-0000-0000-000000000000</abId>");
        mess.append("        <groups><Group>");
        mess.append("           <groupId>" + groupId + "</groupId>");
        mess.append("           <groupInfo><name>" + newGroupName + "</name></groupInfo>");
        mess.append("           <propertiesChanged>GroupName</propertiesChanged>");
        mess.append("        </Group></groups>");
        mess.append("    </ABGroupUpdate>");

        mess.append("</soap:Body>");
        mess.append("</soap:Envelope>");

        String res = sendRequest(
            mess.toString(), addressbook_url, addressbook_action_groupupdate, "POST");

        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        dbfactory.setIgnoringComments(true);
        DocumentBuilder docBuilder;
        Document doc;
        try
        {

            docBuilder = dbfactory.newDocumentBuilder();

            ByteArrayInputStream in = new ByteArrayInputStream(res.getBytes("UTF-8"));
            doc = docBuilder.parse(in);
        }


        catch (ParserConfigurationException ex)
        {
            logger.error("", ex);
            return;
        }catch (SAXException ex)
        {
            logger.error("", ex);
            return;
        }        catch (IOException ex)
        {
            logger.error("", ex);
            return;
        }

        Element el =
            XmlUtils.locateElement(
                doc.getDocumentElement(),
                "ABGroupDeleteResponse",
                "xmlns",
                "http://www.msn.com/webservices/AddressBook");

        // todo if something missing throw failed event
        if(el == null || el.getChildNodes().getLength() > 0)
            return;

        MsnContactListImpl contactList = (MsnContactListImpl) session
                .getMessenger().getContactList();

        MsnGroupImpl group = (MsnGroupImpl)contactList.getGroup(groupId);
        if (group != null)
            group.setGroupName(newGroupName);
    }

    public void addFriend(Email email, String friendlyName)
    {
        StringBuilder mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        mess.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"");
        mess.append("               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        mess.append("               xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
        mess.append("               xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">");
        mess.append("<soap:Header>");
        mess.append("    <ABApplicationHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <ApplicationId>996CDE1E-AA53-4477-B943-2BE802EA6166</ApplicationId>");
        mess.append("        <IsMigration>false</IsMigration>");
        mess.append("        <PartnerScenario>ContactSave</PartnerScenario>");
        mess.append("    </ABApplicationHeader>");
        mess.append("    <ABAuthHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <ManagedGroupRequest>false</ManagedGroupRequest>");
        mess.append("        <TicketToken>" + sso.getContactTicket().replaceAll("&", "&amp;") + "</TicketToken>");
        mess.append("    </ABAuthHeader>");
        mess.append("</soap:Header>");
        mess.append("<soap:Body>");

        mess.append("    <ABContactAdd xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <abId>00000000-0000-0000-0000-000000000000</abId>");
        mess.append("        <contacts>");
        mess.append("           <Contact xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("               <contactInfo>");
        mess.append("                   <contactType>LivePending</contactType>");
        mess.append("                   <passportName>" + email.getEmailAddress() + "</passportName>");
        mess.append("                   <isMessengerUser>true</isMessengerUser>");
        mess.append("                   <MessengerMemberInfo><DisplayName>" + friendlyName + "</DisplayName></MessengerMemberInfo>");
        mess.append("               </contactInfo>");
        mess.append("           </Contact>");
        mess.append("        </contacts>");
        mess.append("        <options><EnableAllowListManagement>true</EnableAllowListManagement></options>");
        mess.append("    </ABContactAdd>");

        mess.append("</soap:Body>");
        mess.append("</soap:Envelope>");

        String res = sendRequest(
            mess.toString(), addressbook_url, addressbook_action_contactadd, "POST");

        String guid = null;

        if(res != null)
        {
            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
            dbfactory.setIgnoringComments(true);
            DocumentBuilder docBuilder;
            Document doc;
            try
            {

                docBuilder = dbfactory.newDocumentBuilder();

                ByteArrayInputStream in = new ByteArrayInputStream(res.getBytes("UTF-8"));
                doc = docBuilder.parse(in);
            }
            catch (ParserConfigurationException ex)
            {
                logger.error("", ex);
                return;
            }catch (SAXException ex)
            {
                logger.error("", ex);
                return;
            }catch (IOException ex)
            {
                logger.error("", ex);
                return;
            }

            Element el =
                XmlUtils.locateElement(
                    doc.getDocumentElement(),
                    "ABContactAddResponse",
                    "xmlns",
                    "http://www.msn.com/webservices/AddressBook");

            Element e = XmlUtils.findChildByChain(el,
                new String[]{"ABContactAddResult", "guid"});

            // todo if something missing throw failed event
            if(e == null)
                return;

            guid = XmlUtils.getText(e);
        }
        else
        {
            // sometimes when removing and after that adding a contact
            // we receive from server 500 Error - Dynamic Item Already Exists
            // although we have received ok responses to all our delete requests
            // and if we request the whole addressbook again we can see that the
            // user we are trying to add is already there
            // so get its ID, create the contact and fire the event
            //
            // todo: the res also can be null due to other error like :
            // <soap:Body><soap:Fault><faultcode>soap:Client</faultcode><faultstring>Contact Already Exists </faultstring>
            // this way we replace the previous contact with new one and
            // forget about its status I will take care of the problem
            // in the begining of the method, but must make
            // sendRequest to return the error code and the error string from
            // the soap message
            String addBookRes = sendRequest(
                getRequestBody(true),
                addressbook_url,
                addressbook_action_findall,
                "POST");

            guid =
                getContactID(addBookRes, email.getEmailAddress());
        }

        MsnContactListImpl contactList = (MsnContactListImpl) session
                .getMessenger().getContactList();

        MsnContactImpl c = new MsnContactImpl(contactList);
        c.setEmail(email);
        c.setFriendlyName(friendlyName);
        c.setId(guid);

        contactList.addContact(c);

        addFriendToList(new MsnList[]{MsnList.AL, MsnList.FL}, c);
    }
    
    public void updateFriend(Email email, String id, String friendlyName)
    {
        // remove from group
        StringBuilder mess = new StringBuilder();
        String res = null;
        Document doc;

        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try
        {
            docBuilder = dbfactory.newDocumentBuilder();
        }
        catch (ParserConfigurationException ex)
        {
            logger.fatal("", ex);
            return;
        }

        dbfactory.setIgnoringComments(true);
        Element el = null;

        MsnContactListImpl contactList = (MsnContactListImpl) session
                    .getMessenger().getContactList();

        MsnContactImpl contact = (MsnContactImpl)contactList.getContactById(id);
        contact.setFriendlyName(friendlyName);

        // update in msn and addbook
        mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        mess.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\r\n");
        mess.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n");
        mess.append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\r\n");
        mess.append(" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">\r\n");
        mess.append("<soap:Header>\r\n");
        mess.append("<ABApplicationHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
        mess.append("<ApplicationId>996CDE1E-AA53-4477-B943-2BE802EA6166</ApplicationId>\r\n");
        mess.append("<IsMigration>false</IsMigration>\r\n");
        mess.append("<PartnerScenario>Timer</PartnerScenario>\r\n");
        mess.append("</ABApplicationHeader>\r\n");
        mess.append("<ABAuthHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
        mess.append("<ManagedGroupRequest>false</ManagedGroupRequest>\r\n");
        mess.append("<TicketToken>" + sso.getContactTicket().replaceAll("&", "&amp;") + "</TicketToken>\r\n");
        mess.append("</ABAuthHeader>\r\n");
        mess.append("</soap:Header>\r\n");
        mess.append("<soap:Body>\r\n");

        mess.append("    <ABContactUpdate xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <abId>00000000-0000-0000-0000-000000000000</abId>");
        mess.append("        <contacts>");
        mess.append("           <Contact xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("               <contactId>" + id + "</contactId>");
        mess.append("               <contactInfo>");
        mess.append("                   <displayName>" + friendlyName + "</displayName>");
        mess.append("               </contactInfo>");
        mess.append("               <propertiesChanged>DisplayName</propertiesChanged>");
        mess.append("           </Contact>");
        mess.append("        </contacts>");
        mess.append("    </ABContactUpdate>");

        mess.append("</soap:Body>\r\n");
        mess.append("</soap:Envelope>");

        res = sendRequest(
            mess.toString(), addressbook_url, addressbook_action_contactupdate, "POST");

        try
        {
            ByteArrayInputStream in = new ByteArrayInputStream(res.getBytes("UTF-8"));
            doc = docBuilder.parse(in);
        }
        catch (SAXException ex)
        {
            logger.error("", ex);
            return;
        }
        catch (IOException ex)
        {
            logger.error("", ex);
            return;
        }

        el =
            XmlUtils.locateElement(
                doc.getDocumentElement(),
                "ABContactUpdateResponse",
                "xmlns",
                "http://www.msn.com/webservices/AddressBook");
        // todo if something missing throw failed event
        if(el == null || el.getChildNodes().getLength() > 0)
            return;

        // update contact
        contactList.removeContactById(id);
        contactList.addContact(contact);
    }

    public void addFriendToList(MsnList[] lists, MsnContactImpl contact)
    {
        for (int i = 0; i < lists.length; i++)
        {
            MsnList msnList = lists[i];
            contact.setInList(msnList, true);
        }

        AbstractMessenger messenger = (AbstractMessenger)session.getMessenger();
        OutgoingADL o1 = new OutgoingADL(messenger.getActualMsnProtocol());
        o1.setContact(contact);
        messenger.send(o1);
    }


    public void removeFriend(MsnList list, Email email, String id, String groupId)
    {
        // remove from group
        StringBuilder mess = new StringBuilder();
        String res = null;
        Document doc;

        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try
        {
            docBuilder = dbfactory.newDocumentBuilder();
        }
        catch (ParserConfigurationException ex)
        {
            logger.fatal("", ex);
            return;
        }

        dbfactory.setIgnoringComments(true);
        Element el = null;

        MsnContactListImpl contactList = (MsnContactListImpl) session
                    .getMessenger().getContactList();

        MsnContactImpl contact = (MsnContactImpl)contactList.getContactById(id);

        if(groupId != null)
        {
            mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            mess.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\r\n");
            mess.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n");
            mess.append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\r\n");
            mess.append(" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">\r\n");
            mess.append("<soap:Header>\r\n");
            mess.append("<ABApplicationHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
            mess.append("<ApplicationId>CFE80F9D-180F-4399-82AB-413F33A1FA11</ApplicationId>\r\n");
            mess.append("<IsMigration>false</IsMigration>\r\n");
            mess.append("<PartnerScenario>Timer</PartnerScenario>\r\n");
            mess.append("</ABApplicationHeader>\r\n");
            mess.append("<ABAuthHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
            mess.append("<ManagedGroupRequest>false</ManagedGroupRequest>\r\n");
            mess.append("<TicketToken>" + sso.getContactTicket().replaceAll("&", "&amp;") + "</TicketToken>\r\n");
            mess.append("</ABAuthHeader>\r\n");
            mess.append("</soap:Header>\r\n");
            mess.append("<soap:Body>\r\n");

            mess.append("<ABGroupContactDelete xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
            mess.append("<abId>00000000-0000-0000-0000-000000000000</abId>\r\n");
            mess.append("<contacts><Contact><contactId>" + id + "</contactId></Contact></contacts>\r\n");

            mess.append("<groupFilter><groupIds>\r\n");
            mess.append("<guid>" + groupId + "</guid>\r\n");
            mess.append("</groupIds></groupFilter>\r\n");
            mess.append("</ABGroupContactDelete>\r\n");

            mess.append("</soap:Body>\r\n");
            mess.append("</soap:Envelope>");

            res = sendRequest(
                mess.toString(), addressbook_url, addressbook_action_groupContactDelete, "POST");

            try
            {
                ByteArrayInputStream in = new ByteArrayInputStream(res.getBytes());
                doc = docBuilder.parse(in);
            }
            catch (SAXException ex)
            {
                logger.error("", ex);
                return;
            }
            catch (IOException ex)
            {
                logger.error("", ex);
                return;
            }

            el =
                XmlUtils.locateElement(
                    doc.getDocumentElement(),
                    "ABGroupContactDeleteResponse",
                    "xmlns",
                    "http://www.msn.com/webservices/AddressBook");

            // todo if something missing throw failed event
            if(el == null || el.getChildNodes().getLength() > 0)
                logger.error("some error in " + res);
            else
            {
                contact.removeBelongGroup(groupId);

                MsnGroup group = contactList.getGroup(groupId);

                ((AbstractMessenger) session.getMessenger())
                    .fireContactRemoveFromGroupCompleted(contact, group);
            }
        }

        if(contact.getBelongGroups().length > 0)
            return;

        // remove from msn and addbook
        mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        mess.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\r\n");
        mess.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n");
        mess.append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\r\n");
        mess.append(" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">\r\n");
        mess.append("<soap:Header>\r\n");
        mess.append("<ABApplicationHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
        mess.append("<ApplicationId>996CDE1E-AA53-4477-B943-2BE802EA6166</ApplicationId>\r\n");
        mess.append("<IsMigration>false</IsMigration>\r\n");
        mess.append("<PartnerScenario>Timer</PartnerScenario>\r\n");
        mess.append("</ABApplicationHeader>\r\n");
        mess.append("<ABAuthHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
        mess.append("<ManagedGroupRequest>false</ManagedGroupRequest>\r\n");
        mess.append("<TicketToken>" + sso.getContactTicket().replaceAll("&", "&amp;") + "</TicketToken>\r\n");
        mess.append("</ABAuthHeader>\r\n");
        mess.append("</soap:Header>\r\n");
        mess.append("<soap:Body>\r\n");

        mess.append("<ABContactDelete xmlns=\"http://www.msn.com/webservices/AddressBook\">\r\n");
        mess.append("<abId>00000000-0000-0000-0000-000000000000</abId>\r\n");
        mess.append("<contacts><Contact><contactId>" + id + "</contactId></Contact></contacts>\r\n");
        mess.append("</ABContactDelete>\r\n");

        mess.append("</soap:Body>\r\n");
        mess.append("</soap:Envelope>");

        res = sendRequest(
            mess.toString(), addressbook_url, addressbook_action_contactdelete, "POST");

        try
        {
            ByteArrayInputStream in = new ByteArrayInputStream(res.getBytes("UTF-8"));
            doc = docBuilder.parse(in);
        }
        catch (SAXException ex)
        {
            logger.error("", ex);
            return;
        }
        catch (IOException ex)
        {
            logger.error("", ex);
            return;
        }

        el =
            XmlUtils.locateElement(
                doc.getDocumentElement(),
                "ABContactDeleteResponse",
                "xmlns",
                "http://www.msn.com/webservices/AddressBook");
        // todo if something missing throw failed event
        if(el == null || el.getChildNodes().getLength() > 0)
            return;

        contactList.removeContactById(id);

        // remove from list
        AbstractMessenger messenger = (AbstractMessenger)session.getMessenger();
        OutgoingRML message = new OutgoingRML(messenger.getActualMsnProtocol());
        message.setRemoveFromList(list, contact);
        messenger.send(message);
    }

    public void blockFriend(Email email)
    {
        StringBuilder mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        mess.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"");
        mess.append("               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        mess.append("               xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
        mess.append("               xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">");
        mess.append("<soap:Header>");
        mess.append("    <ABApplicationHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <ApplicationId>996CDE1E-AA53-4477-B943-2BE802EA6166</ApplicationId>");
        mess.append("        <IsMigration>false</IsMigration>");
        mess.append("        <PartnerScenario>BlockUnblock</PartnerScenario>");
        mess.append("    </ABApplicationHeader>");
        mess.append("    <ABAuthHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <ManagedGroupRequest>false</ManagedGroupRequest>");
        mess.append("        <TicketToken>" + sso.getContactTicket().replaceAll("&", "&amp;") + "</TicketToken>");
        mess.append("    </ABAuthHeader>");
        mess.append("</soap:Header>");
        mess.append("<soap:Body>");

        mess.append("    <DeleteMember xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <serviceHandle><Id>0</Id><Type>Messenger</Type><ForeignId></ForeignId></serviceHandle>");
        mess.append("        <memberships><Membership>");
        mess.append("           <MemberRole>Block</MemberRole>");
        mess.append("           <Members><Member xsi:type=\"PassportMember\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        mess.append("               <Type>Passport</Type><State>Accepted</State><PassportName>"
                                    + email.getEmailAddress() + "</PassportName></Member></Members>");
        mess.append("        </Membership></memberships>");
        mess.append("    </DeleteMember>");

        mess.append("</soap:Body>");
        mess.append("</soap:Envelope>");

        String res = sendRequest(
            mess.toString(), addressbook_url, membership_soap_delete, "POST");

        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        dbfactory.setIgnoringComments(true);
        DocumentBuilder docBuilder;
        Document doc;
        try
        {

            docBuilder = dbfactory.newDocumentBuilder();

            ByteArrayInputStream in = new ByteArrayInputStream(res.getBytes("UTF-8"));
            doc = docBuilder.parse(in);
        }


        catch (ParserConfigurationException ex)
        {
            logger.error("", ex);
            return;
        }catch (SAXException ex)
        {
            logger.error("", ex);
            return;
        }catch (IOException ex)
        {
            logger.error("", ex);
            return;
        }

        Element el =
            XmlUtils.locateElement(
                doc.getDocumentElement(),
                "DeleteMemberResponse",
                "xmlns",
                "http://www.msn.com/webservices/AddressBook");

        // todo if something missing throw failed event
        if(el == null || el.getChildNodes().getLength() > 0)
            return;

        MsnContactListImpl contactList = (MsnContactListImpl) session
                .getMessenger().getContactList();

        MsnContactImpl contact = (MsnContactImpl)contactList.getContactByEmail(email);
        contact.setInList(MsnList.AL, false);
        contact.setInList(MsnList.BL, true);
    }

    public void unblockFriend(Email email)
    {
        // disable it for now
        // complaining about wrong SoapAction
        if(true)
            return;

        StringBuilder mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        mess.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"");
        mess.append("               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        mess.append("               xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
        mess.append("               xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">");
        mess.append("<soap:Header>");
        mess.append("    <ABApplicationHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <ApplicationId>996CDE1E-AA53-4477-B943-2BE802EA6166</ApplicationId>");
        mess.append("        <IsMigration>false</IsMigration>");
        mess.append("        <PartnerScenario>BlockUnblock</PartnerScenario>");
        mess.append("    </ABApplicationHeader>");
        mess.append("    <ABAuthHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <ManagedGroupRequest>false</ManagedGroupRequest>");
        mess.append("        <TicketToken>" + sso.getContactTicket().replaceAll("&", "&amp;") + "</TicketToken>");
        mess.append("    </ABAuthHeader>");
        mess.append("</soap:Header>");
        mess.append("<soap:Body>");

        mess.append("    <AddMember xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <serviceHandle><Id>0</Id><Type>Messenger</Type><ForeignId></ForeignId></serviceHandle>");
        mess.append("        <memberships><Membership>");
        mess.append("           <MemberRole>Allow</MemberRole>");
        mess.append("           <Members><Member xsi:type=\"PassportMember\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        mess.append("               <Type>Passport</Type><State>Accepted</State><PassportName>"
                                    + email.getEmailAddress() + "</PassportName></Member></Members>");
        mess.append("        </Membership></memberships>");
        mess.append("    </AddMember>");

        mess.append("</soap:Body>");
        mess.append("</soap:Envelope>");

        String res = sendRequest(
            mess.toString(), addressbook_url, membership_soap_add, "POST");

        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        dbfactory.setIgnoringComments(true);
        DocumentBuilder docBuilder;
        Document doc;
        try
        {

            docBuilder = dbfactory.newDocumentBuilder();

            ByteArrayInputStream in = new ByteArrayInputStream(res.getBytes("UTF-8"));
            doc = docBuilder.parse(in);
        }


        catch (ParserConfigurationException ex)
        {
            logger.error("", ex);
            return;
        }catch (SAXException ex)
        {
            logger.error("", ex);
            return;
        }catch (IOException ex)
        {
            logger.error("", ex);
            return;
        }

        Element el =
            XmlUtils.locateElement(
                doc.getDocumentElement(),
                "AddMemberResponse",
                "xmlns",
                "http://www.msn.com/webservices/AddressBook");

        // todo if something missing throw failed event
        if(el == null || el.getChildNodes().getLength() > 0)
            return;

        MsnContactListImpl contactList = (MsnContactListImpl) session
                .getMessenger().getContactList();

        MsnContactImpl contact = (MsnContactImpl)contactList.getContactByEmail(email);
        contact.setInList(MsnList.AL, true);
        contact.setInList(MsnList.BL, false);
    }

    public void copyFriend(Email email, String groupId)
    {
        MsnContactListImpl contactList = (MsnContactListImpl) session
                .getMessenger().getContactList();

        MsnContactImpl contact = (MsnContactImpl)contactList.getContactByEmail(email);

        StringBuilder mess = new StringBuilder();

        mess.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        mess.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"");
        mess.append("               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        mess.append("               xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
        mess.append("               xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">");
        mess.append("<soap:Header>");
        mess.append("    <ABApplicationHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <ApplicationId>996CDE1E-AA53-4477-B943-2BE802EA6166</ApplicationId>");
        mess.append("        <IsMigration>false</IsMigration>");
        mess.append("        <PartnerScenario>GroupSave</PartnerScenario>");
        mess.append("    </ABApplicationHeader>");
        mess.append("    <ABAuthHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <ManagedGroupRequest>false</ManagedGroupRequest>");
        mess.append("        <TicketToken>" + sso.getContactTicket().replaceAll("&", "&amp;") + "</TicketToken>");
        mess.append("    </ABAuthHeader>");
        mess.append("</soap:Header>");
        mess.append("<soap:Body>");

        mess.append("    <ABGroupContactAdd xmlns=\"http://www.msn.com/webservices/AddressBook\">");
        mess.append("        <abId>00000000-0000-0000-0000-000000000000</abId>");
        mess.append("        <contacts><Contact><contactId>" + contact.getId() + "</contactId></Contact></contacts>");

        mess.append("        <groupFilter><groupIds>");
        mess.append("           <guid>" + groupId + "</guid>");
        mess.append("        </groupIds></groupFilter>");
        mess.append("    </ABGroupContactAdd>");

        mess.append("</soap:Body>");
        mess.append("</soap:Envelope>");

        String res = sendRequest(
            mess.toString(), addressbook_url, addressbook_action_groupContactAdd, "POST");

        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        dbfactory.setIgnoringComments(true);
        DocumentBuilder docBuilder;
        Document doc;
        try
        {

            docBuilder = dbfactory.newDocumentBuilder();

            ByteArrayInputStream in = new ByteArrayInputStream(res.getBytes("UTF-8"));
            doc = docBuilder.parse(in);
        }
        catch (ParserConfigurationException ex)
        {
            logger.error("", ex);
            return;
        }catch (SAXException ex)
        {
            logger.error("", ex);
            return;
        }catch (IOException ex)
        {
            logger.error("", ex);
            return;
        }

        Element el =
            XmlUtils.locateElement(
                doc.getDocumentElement(),
                "ABGroupContactAddResponse",
                "xmlns",
                "http://www.msn.com/webservices/AddressBook");

        // todo if something missing throw failed event
        if(el == null)
            return;

        el = XmlUtils.findChildByChain(el, new String[]{"ABGroupContactAddResult", "guid"});

        if(el != null)
        {
            contact.addBelongGroup(groupId);

            MsnGroup group = contactList.getGroup(groupId);

            ((AbstractMessenger) session.getMessenger())
                .fireContactAddInGroupCompleted(contact, group);
        }
    }

    public void moveFriend(Email email, String srcGroupId, String destGroupId)
    {
        MsnContactListImpl contactList = (MsnContactListImpl) session
                .getMessenger().getContactList();
        MsnContact contact = contactList.getContactByEmail(email);

        copyFriend(email, destGroupId);
        removeFriend(MsnList.FL, email, contact.getId(), srcGroupId);
    }

    private class MemberRole
    {
        String name;
        MsnList list;
        Vector members = new Vector();

        MemberRole(String name, MsnList list)
        {
            this.name = name;
            this.list = list;
        }

        void addMember(String name)
        {
            members.add(name);
        }

        Vector getMembers()
        {
            return members;
        }
    }
}
