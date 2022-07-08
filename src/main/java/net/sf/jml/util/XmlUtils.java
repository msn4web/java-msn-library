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

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Various xml utils.
 * @author Damian Minkov
 */
public class XmlUtils
{
    /**
     * Looks through all child elements of the specified root (recursively)
     * and returns the elements that corresponds to all parameters.
     *
     * @param root the Element where the search should begin
     * @param tagName the name of the node we're looking for
     * @param keyAttributeName the name of an attribute that the node has to
     * have
     * @param keyAttributeValue the value that attribute must have
     * @return list of Elements in the tree under root that match the specified
     * parameters.
     * @throws NullPointerException if any of the arguments is null.
     */
    public static List locateElements(Element root,
                                        String tagName,
                                        String keyAttributeName,
                                        String keyAttributeValue)
    {
        ArrayList result = new ArrayList();
        NodeList nodes = root.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for(int i = 0; i < len; i++)
        {
            node = nodes.item(i);
            if(node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            // is this the node we're looking for?
            if(node.getNodeName().equals(tagName))
            {
                String attr = ((Element)node).getAttribute(keyAttributeName);

                if(    attr!= null
                    && attr.equals(keyAttributeValue))
                    result.add(node);
            }

            //look inside.
            
            List childs = locateElements( (Element) node, tagName
                          , keyAttributeName, keyAttributeValue);

            if (childs != null)
                 result.addAll(childs);

        }

        return result;
    }
    
    public static Element locateElement(Element root,
        String tagName,
        String keyAttributeName,
        String keyAttributeValue)
    {
        NodeList nodes = root.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for (int i = 0; i < len; i++)
        {
            node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;            // is this the node we're looking for?
            }
            if (node.getNodeName().equals(tagName))
            {
                String attr = ((Element) node).getAttribute(keyAttributeName);
                if (attr != null && attr.equals(keyAttributeValue))
                {
                    return (Element) node;
                }
            }

            //look inside.
            Element child = locateElement((Element) node, tagName, keyAttributeName, keyAttributeValue);

            if (child != null)
            {
                return child;
            }
        }

        return null;
    }

    public static String getText(Element parentNode)
    {
        Text text = getTextNode(parentNode);

        if (text == null)
        {
            return null;
        }
        else
        {
            return text.getData();
        }
    }

    public static Text getTextNode(Element element)
    {
        return (Text) getChildByType(element, Node.TEXT_NODE);
    }

    public static Node getChildByType(Element element, short nodeType)
    {
        if (element == null)
        {
            return null;
        }
        NodeList nodes = element.getChildNodes();
        if (nodes == null || nodes.getLength() < 1)
        {
            return null;
        }
        Node node;
        String data;
        for (int i = 0; i < nodes.getLength(); i++)
        {
            node = nodes.item(i);
            short type = node.getNodeType();
            if (type == nodeType)
            {
                if (type == Node.TEXT_NODE ||
                    type == Node.CDATA_SECTION_NODE)
                {
                    data = ((Text) node).getData();
                    if (data == null || data.trim().length() < 1)
                    {
                        continue;
                    }
                }

                return node;
            }
        }

        return null;
    }

    public static Element findChild(Element parent, String tagName)
    {
        if (parent == null || tagName == null)
        {
            throw new NullPointerException("Parent or tagname were null! " + "parent = " + parent + "; tagName = " + tagName);
        }
        NodeList nodes = parent.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for (int i = 0; i < len; i++)
        {
            node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getNodeName().equals(tagName))
            {
                return (Element) node;
            }
        }

        return null;
    }
    
    public static Element findChildByChain(Element parent, String[] tagNames)
    {
        if (parent == null || tagNames == null)
        {
            throw new NullPointerException("Parent or tagname were null! " + 
                "parent = " + parent + "; tagName = ");
        }
        
        if(tagNames.length == 0)
            return parent;
        
        NodeList nodes = parent.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for (int i = 0; i < len; i++)
        {
            node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getNodeName().equals(tagNames[0]))
            {
                String[] newTags = new String[tagNames.length - 1];
                System.arraycopy(tagNames, 1, newTags, 0, newTags.length);
                
                return findChildByChain((Element) node, newTags);
            }
        }

        return null;
    }
    
    public static List findChildrenByChain(Element parent, String[] tagNames)
    {
        if (parent == null || tagNames == null)
        {
            throw new NullPointerException("Parent or tagname were null! " + 
                "parent = " + parent + "; tagName = ");
        }
        
        ArrayList result = new ArrayList();
        
        if(tagNames.length == 0)
        {
            result.add(parent);
            return result;
        }
        
        NodeList nodes = parent.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for (int i = 0; i < len; i++)
        {
            node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getNodeName().equals(tagNames[0]))
            {
                String[] newTags = new String[tagNames.length - 1];
                System.arraycopy(tagNames, 1, newTags, 0, newTags.length);
                
                result.addAll(findChildrenByChain((Element) node, newTags));
            }
        }

        return result;
    }
    
    /**
     * Returns the children elements with the specified tagName for the specified
     * parent element.
     * @param parent The parent whose children we're looking for.
     * @param tagName the name of the child to find
     * @return List of the children with the specified name 
     * @throws NullPointerException if parent or tagName are null
     */
    public static List findChildren(Element parent, String tagName)
    {
        if(parent == null || tagName == null)
            throw new NullPointerException("Parent or tagname were null! "
                + "parent = " + parent + "; tagName = " + tagName);

        ArrayList result = new ArrayList();
        NodeList nodes = parent.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for(int i = 0; i < len; i++)
        {
            node = nodes.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE
               && ((Element)node).getNodeName().equals(tagName))
                result.add(node);
        }

        return result;
    }
}
