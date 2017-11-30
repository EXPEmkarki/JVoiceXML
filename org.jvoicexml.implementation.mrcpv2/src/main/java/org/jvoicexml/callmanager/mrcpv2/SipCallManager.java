/*
 * JVoiceXML - A free VoiceXML implementation.
 *
 * Copyright (C) 2009-2017 JVoiceXML group - http://jvoicexml.sourceforge.net
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
package org.jvoicexml.callmanager.mrcpv2;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import javax.json.Json;
import javax.sip.Dialog;
import javax.sip.ObjectInUseException;
import javax.sip.SipException;
import javax.sip.address.Address;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvoicexml.CallManager;
import org.jvoicexml.JVoiceXml;
import org.jvoicexml.Session;
import org.jvoicexml.SessionListener;
import org.jvoicexml.client.mrcpv2.Mrcpv2ConnectionInformation;
import org.jvoicexml.event.ErrorEvent;
import org.jvoicexml.event.error.NoresourceError;
import org.mrcp4j.client.MrcpChannel;
import org.mrcp4j.client.MrcpInvocationException;
import org.speechforge.cairo.client.NoMediaControlChannelException;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechClientImpl;
import org.speechforge.cairo.rtp.server.RTPStreamReplicator;
import org.speechforge.cairo.sip.SipSession;
import org.speechforge.zanzibar.sip.SipServer;
import org.speechforge.zanzibar.speechlet.SessionProcessor;
import org.speechforge.zanzibar.speechlet.SpeechletContext;
import org.speechforge.zanzibar.speechlet.SpeechletService;
import org.speechforge.zanzibar.telephony.TelephonyClient;

import com.spokentech.speechdown.client.rtp.RtpTransmitter;

import net.sourceforge.halef.HalefDbWriter;

/**
 * A SIP call manager.
 * @author Spencer Lord
 * @author Dirk Schnelle-Walka
 * @since 0.7.3
 */
public final class SipCallManager
    implements CallManager, SpeechletService, SessionListener {
    /** Logger instance. */
    private static final Logger LOGGER =
        LogManager.getLogger(SipCallManager.class);

    //TODO Better management (clean out orphaned sessions, or leases/timeouts)
    /** Map of sessions. */
    private  Map<String, SipCallManagerSession> sessions;

    //TODO make the ids the same.  Perhaps set the voicexml session id with
    //sip id (rather than have it create its own UUID) or maybe there
    // is a way to attach the voicexml id to the sip session...
    /** Map of sip id's to voicexml session ids. **/
    private  Map<String, String> ids;

    /** Map of terminal names associated to an application. */
    private Map<String, String> applications;

    /** The local SIP server. */
    private SipServer sipServer;

    /** The URL of the cloud. */
    private String cloudUrl;

    /** Reference to JVoiceXML. */
    private JVoiceXml jvxml;

    /**
     * @return the cloudUrl
     */
    public String getCloudUrl() {
        return cloudUrl;
    }

    /**
     * @param url the cloudUrl to set
     */
    public void setCloudUrl(final String url) {
        cloudUrl = url;
    }

    /**
     * Sets the SIP server.
     * @param server the sipServer to set
     */
    public void setSipServer(final SipServer server) {
        sipServer = server;
    }

    /**
     * Sets the configured applications.
     * @param apps the configured applications
     */
    public void setApplications(final Map<String, String> apps) {
        applications = apps;
        LOGGER.info("loaded applications:");
        for (String id : applications.keySet()) {
            final String url = applications.get(id);
            LOGGER.info(" - " + id + ": " + url);
        }
    }

    /**
     * {@inheritDoc}
     * TODO Rename this method to "stopDialog"  Need to change the interface
     * first in the thirdparty jar.
     */
    @Override
    public void StopDialog(final SipSession pbxSession) throws SipException {
        final String id = pbxSession.getId();
        cleanupSession(id);
    }

    /**
     *  Cleanup the session after the call ended.
     * @param id the session id.
     */
    private void cleanupSession(final String id) {

        final SipCallManagerSession session = sessions.get(id);
        if (session == null) {
            LOGGER.error("no session given. unable to cleanup session");
            return;
        }

        try {
            final SpeechClient client = session.getSpeechClient();
            client.stopActiveRecognitionRequests();
            client.shutdown();

            //need to check for null mrcp session (in speechcloud case it
            //will be null)
            final SipSession mrcpsession = session.getMrcpSession();
            if (mrcpsession != null) {
                mrcpsession.bye();
            }

            final SipSession pbxsession = session.getPbxSession();
            pbxsession.bye();

        } catch (MrcpInvocationException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (NoMediaControlChannelException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (SipException e) {
            LOGGER.error(e.getMessage(), e);
        }

        session.getJvxmlSession().hangup();

        //TODO Clean up after telephony client?
        //session.getTelephonyClient();

        //remove the session from the map
        sessions.remove(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dtmf(final SipSession session, final char dtmf) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startNewMrcpDialog(final SipSession pbxSession,
                                   final SipSession mrcpSession) throws Exception {
        final MrcpChannel ttsChannel = mrcpSession.getTtsChannel();
        final MrcpChannel asrChannel = mrcpSession.getRecogChannel();
        final SpeechClient speechClient =
            new SpeechClientImpl(ttsChannel, asrChannel);
        final TelephonyClient telephonyClient = null;
        //new TelephonyClientImpl(pbxSession.getChannelName());
        final Dialog dialog = pbxSession.getSipDialog();
        final Address localParty = dialog.getLocalParty();
        final String displayName = localParty.getDisplayName();
        final String calledNumber;
        //separate the scheme and port from the address
        if ((displayName == null) || displayName.startsWith("sip:")) {
            final String uri = localParty.getURI().toString();
            String[] parts = uri.split(":");
            // get the first part of the address, which is the number that
            // was called.
            String[] parts2 = parts[1].split("@");
            calledNumber = parts2[0];
        } else {
            calledNumber = displayName;
        }

        // Create a session (so we can get other signals from the caller)
        // and release resources upon call completion
        final String id = pbxSession.getId();
        final SipCallManagerSession session =
            new SipCallManagerSession(id, pbxSession, mrcpSession,
                                      speechClient, telephonyClient);
        try {
            final Address remoteParty = dialog.getRemoteParty();
            final String callingNumber = remoteParty.getURI().toString();
            final URI calledDevice = new URI(calledNumber);
            final URI callingDevice = new URI(callingNumber);
            final Mrcpv2ConnectionInformation remote =
                new Mrcpv2ConnectionInformation(callingDevice,
                                                calledDevice);
            remote.setTtsClient(speechClient);
            remote.setAsrClient(speechClient);

            // Create a jvoicxml session and initiate a call at JVoiceXML.
            final Session jsession = jvxml.createSession(remote);


            //add a listener to capture the end of voicexml session event
            jsession.addSessionListener(this);

            //add the jvoicexml session to the session bag
            session.setJvxmlSession(jsession);
            synchronized (sessions) {
                sessions.put(id, session);
            }

            //workaround to deal with two id's
            //maps the voicexml sessionid to sip session id
            //needed for case when the voicxml session ends before a hang up
            // and need to get to close the sip session
            synchronized (ids) {
                ids.put(jsession.getSessionID(), id);
            }

            // Get the random code
            final String randomCode;
            if (callingNumber.startsWith("sip:")) {
                String[] parts = callingNumber.split(":");
                String[] parts2 = parts[1].split("@");
                String number = parts2[0];
                if (number.length() > 8) {
                    randomCode = number.substring(8);
                } else {
                    randomCode = "";
                    LOGGER.warn("No randomCode used.");
                }
            } else {
                randomCode = "";
                LOGGER.warn("No randomCode used.");
            }

            // Write real-time information for Halef system
            // remote party display name (set to Asterisk SIP callId)
            // JVoiceXML sessionID
            // JVoiceXML SIP callId
            // randomCode use by client
            final String remoteDisplayName = remoteParty.getDisplayName();
            final String asteriskCallID;
            final String use_case_name;
            if ((remoteDisplayName == null) ||
                    remoteDisplayName.startsWith("sip:")) {
                final String uri = remoteParty.getURI().toString();
                String[] parts = uri.split(":");
                String[] parts2 = parts[1].split("@");
                asteriskCallID = parts2[0];
                LOGGER.warn(String.format("The remote party display name seems to be an"
                                          + " invalid asterisk SIP callId (\"%s\")",
                                          asteriskCallID));
                use_case_name = "NO_USE_CASE";
            } else {
                String[] parts = remoteDisplayName.split("\\|");
                if (parts.length != 2) {
                    asteriskCallID = "WRONG DIALPLAN";
                    use_case_name = "WRONG DIALPLAN";
                    LOGGER.warn("DIALPLAN sets CALLERID(name) wrong.");
                } else {
                    asteriskCallID = parts[0];
                    use_case_name = parts[1];
                }
            }
            final String jCallID = dialog.getCallId().getCallId().split("@")[0];
            final String cCallID = mrcpSession.getSipDialog().getCallId().getCallId().split("@")[0];

            // Append the sessionId to the application uri
            final String appUri = applications.get(calledNumber);
            String path = new URL(appUri).getPath();
            path = path.replaceFirst("/", "");
            String appName = path.split("/")[0];
            final String applicationUri = appUri
                                          + "?session_id="
                                          + jsession.getSessionID()
                                          + "&client_session_id="
                                          + randomCode
                                          + "&origin=jvoicexml";


            String requestContent;

            HalefDbWriter.setJVXMLSessionId(jsession.getSessionID());
            HalefDbWriter.setJVXMLCallId(jCallID);
            HalefDbWriter.setCairoCallId(cCallID);
            HalefDbWriter.setClientSessionId(randomCode);

            if (randomCode.equals("")) {
              LOGGER.error("NO client_session_id! Logging will not work for this call. ");
            } else {
                requestContent = Json.createObjectBuilder()
                                 .add("api_version", 1)
                                 .add("event_type", "JVXML_CALL_START")
                                 .add("client_session_id", randomCode)
                                 .add("use_case_name", use_case_name)
                                 .add("jvxml_session_id", jsession.getSessionID())
                                 .add("asterisk_call_id", asteriskCallID)
                                 .add("jvxml_call_id", jCallID)
                                 .add("jvxml_extension", calledNumber)
                                 .add("jvxml_server_ip", System.getenv("IP"))
                                 .add("cairo_call_id", cCallID)
                                 .add("cairo_server_ip", System.getenv("IP")).build().toString();

                HalefDbWriter.logGanesha("log", requestContent);
                LOGGER.info(requestContent);
            }






            //use the number for looking up the application
            LOGGER.info("called number: '" + calledNumber + "'");
            LOGGER.info("calling application '" + applicationUri + "'...");

            //start the application
            final URI uri = new URI(applicationUri);
            jsession.call(uri);
        }  catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        } catch (ErrorEvent e) {
            LOGGER.error(e.getMessage(), e);
            throw new Exception(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startNewCloudDialog(final SipSession pbxSession,
                                    final RTPStreamReplicator rtpReplicator,
                                    final RtpTransmitter rtpTransmitter) throws Exception {

    }

    /**
     * Retrieves the reference to the interpreter.
     * @return the interpreter
     */
    public JVoiceXml getJVoiceXml() {
        return jvxml;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setJVoiceXml(final JVoiceXml jvoicexml) {
        jvxml = jvoicexml;
    }


    // TODO startup/shutdown are in the DialogManagerInterface
    // and start/stop are in the CallManager Interface -- don't need both sets.

    /**
     * {@inheritDoc}
     */
    @Override
    public void startup() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        LOGGER.info("shutdown mrcp sip callManager");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws NoresourceError, IOException {
        LOGGER.info("startup mrcp sip callManager");
        sessions = new java.util.HashMap<String, SipCallManagerSession>();
        ids = new java.util.HashMap<String, String>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        try {
            if (sipServer != null) {
                sipServer.shutdown();
            }
        } catch (ObjectInUseException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getLocalizedMessage(), e);
            }
        } finally {
            sipServer = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionStarted(final Session session) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionEnded(final Session session) {
        String id = session.getSessionID();
        //workaround to deal with two id's
        //maps the voicexml sessionid to sip session id
        //needed for case when the voicxml session ends before a hang up and
        // need to get to close the sip session

        // get the sip sesison id
        LOGGER.debug("SipCallManager.sessionEnded was called.");
        //remove the session id mapping
        final String sipId;
        synchronized (ids) {
            sipId = ids.get(id);
            ids.remove(id);
        }

        //clean up the session
        cleanupSession(sipId);
    }


    //@Override
    public SessionProcessor startNewDialog(SpeechletContext arg0)
    throws Exception {
        // TODO Auto-generated method stub
        return null;
    }
}
