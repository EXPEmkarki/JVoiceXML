/*
 * JVoiceXML - A free VoiceXML implementation.
 *
 * Copyright (C) 2013-2017 JVoiceXML group - http://jvoicexml.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jvoicexml.client.text;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvoicexml.client.text.protobuf.TextMessageOuterClass.TextMessage;
import org.jvoicexml.client.text.protobuf.TextMessageOuterClass.TextMessage.TextMessageType;
import org.jvoicexml.xml.ssml.SsmlDocument;

/**
 * Test cases for the {@link TextServer}.
 * 
 * @author Raphael Groner
 * @author Dirk Schnelle-Walka
 * @since 0.7.6
 */
public class TestTextServer implements TextListener {

    private static final int PORT = 4711;

    private TextServer server;
    private TextConnectionInformation info;
    private Socket socket;

    private final Object lock = new Object();

    private TextMessage rcvd;

    /**
     * Set up the test environment
     * 
     * @throws java.lang.Exception
     *             test failed
     */
    @Before
    public void setUp() throws Exception {
        server = new TextServer(PORT);
        info = (TextConnectionInformation) server.getConnectionInformation();

        server.addTextListener(this);
        server.start();
        synchronized (lock) {
            lock.wait();
        }
        final InetAddress address = info.getAddress();
        final int port = info.getPort();
        socket = new Socket(address, port);
        server.waitConnected();
    }

    /**
     * Tear down the test environment
     * 
     * @throws java.lang.Exception
     *             tear down failed
     */
    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Test method for
     * {@link org.jvoicexml.client.text.TextServer#getConnectionInformation()}.
     * 
     * @throws UnknownHostException
     */
    @Test
    public void testGetConnectionInformation() {
        Assert.assertEquals(PORT, info.getPort());
    }

    /**
     * Test method for {@link org.jvoicexml.client.text.TextServer#isStarted()}.
     */
    @Test
    public void testIsStarted() {
        Assert.assertTrue(server.isStarted());
    }

    /**
     * Test method for
     * {@link org.jvoicexml.client.text.TextServer#sendInput(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test(timeout = 5000)
    public void testSendInput() throws Exception {
        Assert.assertTrue(server.isStarted());
        String input = "Test123";
        final Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                    final InputStream in = socket.getInputStream();
                    System.out.println("1");
                    rcvd = TextMessage.parseDelimitedFrom(in);
                    System.out.println(2);
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                } catch (Exception e) {
                    Assert.fail(e.getMessage());
                }
            }
        };
        thread.start();
        synchronized (lock) {
            lock.wait();
        }
        server.sendInput(input);
        if (thread.isAlive()) {
            synchronized (lock) {
                lock.wait();
            }
        }
        final TextMessage message = TextMessage.newBuilder()
                .setType(TextMessageType.USER).setData(input)
                .setSequenceNumber(0).build();
        Assert.assertEquals(message, rcvd);
    }

    /**
     * Test method for {@link org.jvoicexml.client.text.TextServer#stopServer()}
     * .
     */
    @Test
    public void testStopServer() {
        Assert.assertTrue(server.isStarted());
        server.stopServer();
        Assert.assertFalse(server.isStarted());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void started() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connected(InetSocketAddress remote) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void outputSsml(final TextMessageEvent event,
            final SsmlDocument document) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void expectingInput(final TextMessageEvent event) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inputClosed(final TextMessageEvent event) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnected(final TextMessageEvent event) {
    }

}
