/*
 * This file is part of TinySip. 
 * http://code.google.com/p/de-tiny-sip/
 * 
 * Created 2011 by Sebastian R�sch <flowfire@sebastianroesch.de>
 * 
 * This software is licensed under the Apache License 2.0.
 */

package de.tinysip.sip;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;

import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import android.util.Log;

import de.javawi.jstun.test.DiscoveryInfo;

/**
 * SipManager handles the session establishment, teardown and authorization. Notifies state changes by raising SipManagerStatusChangedEvent, SipManagerCallStatusEvent,
 * SipManagerSessionEvent.
 * 
 * @author Sebastian
 * 
 */
public class SipManager implements SipListener {
	private static String TAG = "tSIP";
	private static SipManager sipManager = null;
	private SipMessageHandler sipMessageHandler = null;

	private SipManagerState currentState = null;
	private SipSession currentSession = null;
	private SipContact currentContact = null;
	private Request currentRequest = null;

	private int registerTryCount = 0;

	private List<SipManagerStatusListener> statusListeners = null;

	/**
	 * Create a SipManager by specifying the LocalSipProfile and the STUN DiscoveryInfo for NAT traversal.
	 * 
	 * @param localSipProfile
	 *            the local user's SIP profile to register with the provider
	 * @param discoveryInfo
	 *            the STUN DiscoveryInfo for NAT traversal
	 */
	private SipManager(LocalSipProfile localSipProfile, DiscoveryInfo discoveryInfo) {
		try {
			sipMessageHandler = SipMessageHandler.createInstance(localSipProfile, discoveryInfo);
			sipMessageHandler.getSipProvider().addSipListener(this);
			statusListeners = new ArrayList<SipManagerStatusListener>();
			currentState = SipManagerState.IDLE;
		} catch (Exception e) {
		}
	}

	/**
	 * Create a singleton instance of SipManager by specifying the LocalSipProfile and the STUN DiscoveryInfo for NAT traversal.
	 * 
	 * @param localSipProfile
	 *            the local user's SIP profile to register with the provider
	 * @param discoveryInfo
	 *            the STUN DiscoveryInfo for NAT traversal
	 * @return the created instance of SipManager
	 */
	public static SipManager createInstance(LocalSipProfile localSipProfile, DiscoveryInfo discoveryInfo) {
		if (sipManager == null)
			sipManager = new SipManager(localSipProfile, discoveryInfo);

		return sipManager;
	}

	/**
	 * Get a singleton instance of the SipManager. Use createInstance() first to create SipManager.
	 * 
	 * @return an instance of SipManager or null if none was created
	 */
	public static SipManager getInstance() {
		return sipManager;
	}

	/**
	 * Register the local sip profile with the sip provider.
	 * 
	 * @throws InvalidArgumentException
	 * @throws TooManyListenersException
	 * @throws ParseException
	 * @throws SipException
	 */
	public void registerProfile() throws InvalidArgumentException, TooManyListenersException, ParseException, SipException {
		setStatusChanged(SipManagerState.REGISTERING);
		sipMessageHandler.register(SipRequestState.REGISTER);
	}

	/**
	 * Unregister the local sip profile from the sip provider.
	 * 
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 * @throws SipException
	 */
	public void unregisterProfile() throws ParseException, InvalidArgumentException, SipException {
		setStatusChanged(SipManagerState.UNREGISTERING);
		sipMessageHandler.register(SipRequestState.UNREGISTER);
	}

	/**
	 * Start calling the specified SipContact.
	 * 
	 * @param contact
	 *            the SIP contact to call
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 * @throws SipException
	 * @throws NullPointerException
	 * @throws SdpException
	 */
	public void sendInvite(SipContact contact) throws ParseException, InvalidArgumentException, SipException, NullPointerException, SdpException {
		currentContact = contact;
		currentSession = null;
		setStatusChanged(SipManagerState.CALLING);
		sipMessageHandler.sendInvite(currentContact, SipRequestState.REGISTER);
	}

	@Override
	public void processDialogTerminated(DialogTerminatedEvent arg0) {
	}

	@Override
	public void processIOException(IOExceptionEvent arg0) {
	}

	@Override
	public void processRequest(RequestEvent requestEvent) {
		Request request = requestEvent.getRequest();
		String method = ((CSeqHeader) request.getHeader(CSeqHeader.NAME)).getMethod();
		Log.d(TAG, "Incoming " + method + " request");

		if (method.equals(Request.INVITE)) {
			try {
				if (currentSession == null) {
					SessionDescription sdpSession = SdpFactory.getInstance().createSessionDescription(new String(request.getRawContent()));
					currentSession = SipMessageHandler.createSipSession(request, sdpSession);

					setStatusChanged(SipManagerState.INCOMING);
					setCallStatus("Incoming call from " + currentSession.getCallerNumber());
					Log.d(TAG, currentSession.toString());

					currentRequest = request;
					sipMessageHandler.sendRinging(request);
				} else {
					sipMessageHandler.sendBusyHere(request);
				}
			} catch (Exception e) {
				setStatusChanged(SipManagerState.ERROR);
			}
		} else if (method.equals(Request.CANCEL)) {
			reset();
		} else if (method.equals(Request.ACK)) {
			if (currentSession != null) {
				sipMessageHandler.setDialog(requestEvent.getDialog());
				setStatusChanged(SipManagerState.ESTABLISHED);
				setSessionChanged(currentSession);
			}
		} else if (method.equals(Request.BYE)) {
			try {
				sipMessageHandler.sendOK(request, requestEvent.getServerTransaction());
			} catch (Exception e) {
				setStatusChanged(SipManagerState.ERROR);
			}
			// no need to send 200 OK, SipStack should do that automatically
			reset();
		}
	}

	@Override
	public void processResponse(ResponseEvent responseEvent) {
		Response response = (Response) responseEvent.getResponse();
		int status = response.getStatusCode();
		Log.d(TAG, "Response Status Code: " + status);

		switch (status) {
		case 180: // Ringing
			try {
				if (currentState.equals(SipManagerState.CALLING)) {
					setStatusChanged(SipManagerState.RINGING);
				}
			} catch (Exception e) {
				setStatusChanged(SipManagerState.ERROR);
			}
		case 200: // OK
			try {
				if ((currentState.equals(SipManagerState.RINGING) || currentState.equals(SipManagerState.CALLING)) && response.getRawContent() != null
						&& responseEvent.getDialog() != null) {
					SessionDescription sdpSession = SdpFactory.getInstance().createSessionDescription(new String(response.getRawContent()));
					currentSession = SipMessageHandler.createSipSession(response, sdpSession);

					sipMessageHandler.sendAck();

					setStatusChanged(SipManagerState.ESTABLISHED);
					setSessionChanged(currentSession);
				} else if (currentState.equals(SipManagerState.REGISTERING)) {
					setStatusChanged(SipManagerState.READY);
					registerTryCount = 0;
				} else if (currentState.equals(SipManagerState.UNREGISTERING)) {
					setStatusChanged(SipManagerState.IDLE);
					registerTryCount = 0;
				}
			} catch (Exception e) {
				setStatusChanged(SipManagerState.ERROR);
			}

			break;

		case 401: // Unauthorized
			try {
				registerTryCount++;

				WWWAuthenticateHeader authHeader = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);
				sipMessageHandler.getLocalSipProfile().setNonce(authHeader.getNonce());
				sipMessageHandler.getLocalSipProfile().setRealm(authHeader.getRealm());

				if (currentState.equals(SipManagerState.REGISTERING))
					sipMessageHandler.register(SipRequestState.AUTHORIZATION);
				else if (currentState.equals(SipManagerState.UNREGISTERING))
					sipMessageHandler.register(SipRequestState.UNREGISTER_AUTHORIZATION);
			} catch (Exception e) {
				setStatusChanged(SipManagerState.ERROR);
			}

			break;

		case 404: // Not found
			break;

		case 407: // Proxy Authentication required
			try {
				registerTryCount++;

				if (currentSession == null && currentContact != null) {
					ProxyAuthenticateHeader authHeader = (ProxyAuthenticateHeader) response.getHeader(ProxyAuthenticateHeader.NAME);
					sipMessageHandler.getLocalSipProfile().setNonce(authHeader.getNonce());
					sipMessageHandler.getLocalSipProfile().setRealm(authHeader.getRealm());

					if (currentState.equals(SipManagerState.CALLING))
						sipMessageHandler.sendInvite(currentContact, SipRequestState.AUTHORIZATION);
				}
			} catch (Exception e) {
				setStatusChanged(SipManagerState.ERROR);
			}

			break;

		case 486: // Busy
			reset();

			break;

		case 603: // Decline
			reset();

			break;

		case 500: // Too many clients
			try {
				setStatusChanged(SipManagerState.ERROR);
			} catch (Exception e) {
				setStatusChanged(SipManagerState.ERROR);
			}

		default:
			break;
		}

	}

	@Override
	public void processTimeout(TimeoutEvent arg0) {
	}

	@Override
	public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
	}

	/**
	 * Accept the incoming call.
	 * 
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 * @throws SdpException
	 */
	public void acceptCall() throws SipException, InvalidArgumentException, ParseException, SdpException {
		if (currentRequest != null)
			sipMessageHandler.sendOK(currentRequest, null);

		setStatusChanged(SipManagerState.ESTABLISHED);
	}

	/**
	 * Decline the incoming call and reset the SipManager.
	 * 
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 * @throws SdpException
	 */
	public void declineCall() throws SipException, InvalidArgumentException, ParseException, SdpException {
		if (currentRequest != null)
			sipMessageHandler.sendDecline(currentRequest);

		reset();
	}

	/**
	 * End the current call and reset the SipManager.
	 * 
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 * @throws SdpException
	 */
	public void endCall() throws SipException, InvalidArgumentException, ParseException, SdpException {
		if (currentState.equals(SipManagerState.ESTABLISHED))
			sipMessageHandler.sendBye();
		else
			sipMessageHandler.sendCancel();

		reset();
	}

	/**
	 * Reset all connections.
	 */
	private void reset() {
		currentContact = null;
		currentRequest = null;
		currentSession = null;
		registerTryCount = 0;
		sipMessageHandler.reset();

		if (currentState.equals(SipManagerState.ESTABLISHED))
			setSessionChanged(null);

		setStatusChanged(SipManagerState.READY);
	}

	/**
	 * Close the SipManager and reset all connections.
	 */
	public void close() {
		reset();
	}

	/**
	 * Raise a SipManagerSessionEvent.
	 * 
	 * @param session
	 *            the new SipSession information
	 */
	private void setSessionChanged(SipSession session) {
		SipManagerSessionEvent event = new SipManagerSessionEvent(this, session);

		synchronized (statusListeners) {
			for (SipManagerStatusListener item : statusListeners) {
				item.SipManagerSessionChanged(event);
			}
		}
	}

	/**
	 * Raises a SipManagerCallStatusEvent.
	 * 
	 * @param message
	 *            the message to send
	 */
	private void setCallStatus(String message) {
		SipManagerCallStatusEvent event = new SipManagerCallStatusEvent(this, message);

		synchronized (statusListeners) {
			for (SipManagerStatusListener item : statusListeners) {
				item.SipManagerCallStatusChanged(event);
			}
		}
	}

	/**
	 * Set the new state of the SipManager. Raises a SipManagerStatusChangedEvent.
	 * 
	 * @param state
	 *            the new state to signal
	 */
	private void setStatusChanged(SipManagerState state) {
		if (!currentState.equals(state)) {
			Log.d(TAG, "New State: " + state.toString());
			currentState = state;
			SipManagerStatusChangedEvent event = new SipManagerStatusChangedEvent(this, state);

			synchronized (statusListeners) {
				for (SipManagerStatusListener item : statusListeners) {
					item.SipManagerStatusChanged(event);
				}
			}
		}
	}

	/**
	 * Add a SipManagerStatusListener to this SipManager.
	 * 
	 * @param listener
	 *            the listener to register for the raised events
	 */
	public synchronized void addStatusListener(SipManagerStatusListener listener) {
		statusListeners.add(listener);
	}

	/**
	 * Remove a SipManagerStatusListener from this SipManager.
	 * 
	 * @param listener
	 *            the listener to unregister from the events
	 */
	public synchronized void removeStatusListener(SipManagerStatusListener listener) {
		statusListeners.remove(listener);
	}

}
