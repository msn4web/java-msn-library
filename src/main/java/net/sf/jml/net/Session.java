/*
 * SocketSession.java
 *
 * Created on March 13, 2007, 6:13 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.jml.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import net.sf.jml.util.ByteBufferUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author damencho
 */
public class Session
{
    private static final Log logger = LogFactory.getLog(Session.class);

    private final Collection<SessionListener> sessionListeners = new CopyOnWriteArrayList<SessionListener>();

    private Socket socket = null;
    private InputStream in = null;
    private OutputStream out = null;

    private boolean isStarted = false;

    private SocketAddress socketAddress = null;

    private Object attachment = null;

    private MessageRecognizer messagerecognizer = null;

    private boolean isClosing = false;

    private boolean isAvailable = false;

    private MsgSender msgSender = null;
    private MsgDispatcher msgDispatcher = null;

    private Timer timoutTimer = null;

    /** Creates a new instance of SocketSession */
    public Session()
    {
    }

    public Socket getSocket()
    {
        return socket;
    }

    public void setSocketAddress(SocketAddress socketaddress)
        throws IllegalStateException
    {
        if(isStarted)
        {
            throw new IllegalStateException("can't set socket address after session started");
        } else
        {
            this.socketAddress = socketaddress;
        }
    }

    public Object getAttachment()
    {
        return attachment;
    }

    public void setAttachment(Object o)
    {
        this.attachment = o;
    }

    public void setMessageRecognizer(MessageRecognizer messagerecognizer)
    {
        this.messagerecognizer = messagerecognizer;
    }

    public void addSessionListener(SessionListener sessionlistener)
    {
        sessionListeners.add(sessionlistener);
    }

    public void removeSessionListener(SessionListener sessionlistener)
    {
        sessionListeners.remove(sessionlistener);
    }

    public boolean isAvailable()
    {
        return isAvailable;
    }

    public void start(boolean flag)
        throws IllegalStateException
    {
        msgDispatcher = new MsgDispatcher();
        Thread msgDispatcherThread = new Thread(msgDispatcher, "net.sf.jml.net.SocketSession.msgDispatcher");
        msgDispatcherThread.start();

        msgSender = new MsgSender();
        Thread msgSenderThread = new Thread(msgSender, "net.sf.jml.net.SocketSession.msgSender");
        msgSenderThread.start();

        new Thread
        (
            new Runnable()
        {
            public void run()
            {
                try
                {
                    socket = new Socket();

                    socket.connect(socketAddress);
                    socket.setKeepAlive(true);
                    isAvailable = true;

                    in = socket.getInputStream();
                    out = socket.getOutputStream();

                    fireSessionEstablished();

                } catch (Exception ex)
                {
                    logger.error("error establishing connection " , ex );

                    firExceptionCaught(ex);

                    return;
                }

                ByteBuffer readBuffer = ByteBufferUtils.allocate(0x20000, false);

                byte[] buff;
                byte[] readBytes;
                try
                {
                    int readBytesLen;
                    do
                    {
                        synchronized (readBuffer)
                        {
                            if(readBuffer == null)
                            {
                                readBuffer = ByteBufferUtils.allocate(
                                    0x20000, false);
                            }
                            else if(!readBuffer.hasRemaining())
                            {
                                readBuffer = ByteBufferUtils.increaseCapacity(
                                    readBuffer, 0x20000);
                            }

                            buff = new byte[4096];

                            readBytesLen = in.read(buff);

                            if(readBytesLen < 0)
                                return;

                            readBytes = new byte[readBytesLen];
                            System.arraycopy(buff, 0, readBytes, 0, readBytesLen);

                            readBuffer.put(readBytes);

                            readBuffer.flip();

                            recognizeMessageAndDispatch(readBuffer);
                            readBuffer.compact();
                        }
                    }while(readBytesLen > 0);
                    }
                    catch(SocketException ex)
                    {
                        if(isClosing)
                        {
                            isClosing = false;
                        }
                        else
                        {
                            logger.debug("Smth happen to connection", ex);
                            fireSessionClosed();
                        }
                    }
                    catch (IOException ex)
                    {
                        logger.error("Smth happen to connection - IO ex", ex);
                        if(!isClosing)
                        {
                            fireSessionClosed();
                        }
                    }
                }
        },
        "net.sf.jml.net.SocketSession.reader").start();
    }

    protected void recognizeMessageAndDispatch(ByteBuffer bytebuffer)
    {
        do
        {
            if(!bytebuffer.hasRemaining())
                break;
            Message message = recognizeMessage(bytebuffer);
            if(message == null)
                break;
            msgDispatcher.dispacthMsg(message);
        } while(true);
    }

    protected Message recognizeMessage(ByteBuffer bytebuffer)
    {
        Message message;
        boolean flag;

        message = messagerecognizer.recognize(this, bytebuffer.asReadOnlyBuffer());
        if(message == null) return null;

        flag = message.readFromBuffer(bytebuffer);

        if(flag)
            return message;

        return null;
    }

    private class MsgDispatcher extends Thread
    {
        final Vector<Message> queue = new Vector<Message>();

        boolean isRunning = true;

        byte[] notDispatchedBuffer = null;

        public void stopDispatcher() {
            isRunning = false;
            this.interrupt();
            // Flush the queue
            while (!queue.isEmpty()) {
                Message msg = queue.remove(0);
                fireMessageReceived(msg);
            }

            synchronized(queue)
            {
                queue.notifyAll();
            }
        }

        public void run()
        {
            while(isRunning)
            {
                synchronized (queue) {
                    if(queue.isEmpty())
                        try {
                            queue.wait();
                        }
                        catch (InterruptedException ex) {
                            return;
                        }
                }
                if(queue.isEmpty())
                    continue;

                Message msg = queue.remove(0);
                fireMessageReceived(msg);
            }
        }

        void dispacthMsg(Message msg)
        {
            synchronized (queue)
            {
                queue.add(msg);
                queue.notifyAll();
            }
        }
    }

    private void fireMessageReceived(Message msg)
    {
        for (SessionListener sessionListener : sessionListeners) {
            try {
                sessionListener.messageReceived(Session.this, msg);
            } catch (Exception ex) {
                logger.error("error firing events for msg received", ex);
            }
        }
    }

    private void fireMessageSent(Message msg)
    {
        for (SessionListener sessionListener : sessionListeners) {
            try {
                sessionListener.messageSent(Session.this, msg);
            } catch (Exception ex) {
                logger.error("error firing events for msg sent", ex);
            }
        }
    }

    private void fireSessionClosed()
    {
        for (SessionListener sessionListener : sessionListeners) {
            try {
                sessionListener.sessionClosed(this);
            } catch (Exception ex) {
                logger.error("error firing events for close", ex);
            }
        }
    }

    private void firExceptionCaught(Throwable e)
    {
        for (SessionListener sessionListener : sessionListeners) {
            try {
                sessionListener.exceptionCaught(this, e);
            } catch (Exception ex) {
                logger.error("error firing events for firExceptionCaught", ex);
            }
        }
    }

    private void fireSessionEstablished()
    {
        for (SessionListener sessionListener : sessionListeners) {
            try {
                sessionListener.sessionEstablished(this);
            } catch (Exception ex) {
                logger.error("error firing events for sessionEstablished", ex);
            }
        }
    }

    private void fireSessionTimeout()
    {
        for (SessionListener sessionListener : sessionListeners) {
            try {
                sessionListener.sessionTimeout(this);
            } catch (Exception ex) {
                logger.error("error firing events for sessionEstablished", ex);
            }
        }
    }

    public void close()
    {
        this.close(true);
    }

    public void close(boolean flag)
    {
        if(isClosing) return;

        isClosing = true;

        msgDispatcher.stopDispatcher();
        msgSender.stopSender();

        try {
            socket.shutdownInput();
        }
        catch (IOException e) {
            logger.error("error shutting down input on socket", e);
        }

        try {
            socket.getOutputStream().flush();
        }
        catch (IOException e) {
            logger.error("error flushing remaining output on socket", e);
        }

        if(timoutTimer != null)
        {
            // cancel all left tasks, or we will get "Socket closed" exception
            // as we will close the socket
            timoutTimer.cancel();
            timoutTimer = null;
        }

        try
        {
            socket.close();
        } catch (IOException ex)
        {
            logger.error("error closing socket", ex);
        }

        isAvailable = false;

        fireSessionClosed();
    }

    public void write(Message message)
        throws IllegalArgumentException, IllegalStateException
    {
        msgSender.sendMsg(message);
    }

    public boolean blockWrite(Message message)
        throws IllegalArgumentException, IllegalStateException
    {
        try {
            sendMessage(message);
        }
        catch (IOException ex) {
            logger.error("error sending msg", ex);
            return false;
        }

        fireMessageSent(message);

        return true;
    }

     private class MsgSender extends Thread
    {
        final Vector<Message> queue = new Vector<Message>();
        boolean isRunning = true;

        public void stopSender() {
            isRunning = false;
            this.interrupt();
            // Flush the remains of the queue
            while (!queue.isEmpty()) {
                Message message = queue.remove(0);
                try {
                    Session.this.sendMessage(message);
                    fireMessageSent(message);
                }
                catch (IOException ex) {
                    logger.error("error sending msg: "+message, ex);
                }
            }

            synchronized(queue)
            {
                queue.notifyAll();
            }
        }

        public void run()
        {
            while(isRunning)
            {
                synchronized (queue) {
                    if (queue.isEmpty())
                        try {
                            queue.wait();
                        }
                        catch (InterruptedException ex) {
                            return;
                        }

                    // Secondary check just in case we have a slip-by
                    if (queue.isEmpty()) return;

                    Message message = queue.remove(0);
                    try
                    {
                        Session.this.sendMessage(message);
                        fireMessageSent(message);
                    }
                    catch (IOException ex)
                    {
                        logger.error("error sending msg: "+message, ex);
                        ex.printStackTrace();
                    }
                }
            }
        }

        void sendMsg(Message message)
        {
            synchronized (queue)
            {
                queue.add(message);
                queue.notifyAll();
            }
        }
    }

    private synchronized void sendMessage(Message message)
        throws IOException
    {
        ByteArrayOutputStream o = new ByteArrayOutputStream();

        ByteBuffer[] toSendBuffs = message.toByteBuffer();
        for (ByteBuffer buf : toSendBuffs) {
            byte[] bsToWrite = new byte[buf.limit()];
            buf.get(bsToWrite, 0, bsToWrite.length);

            o.write(bsToWrite);
        }

        o.writeTo(out);
        out.flush();
    }

    public void setSessionTimeout(int i)
    {
        logger.debug("setSessionTimeout:" + i);

        if(socket != null)
        {
            if(timoutTimer == null)
                timoutTimer = new Timer();

            timoutTimer.schedule(new TimeoutFire(), i);
        }
//            try
//            {
//                socket.setSoTimeout(i);
//            } catch (SocketException ex)
//            {
//                logger.error("cannot set timeout", ex);
//            }
    }

    private class TimeoutFire
        extends TimerTask
    {
        public void run()
        {
            fireSessionTimeout();
        }
    }

}