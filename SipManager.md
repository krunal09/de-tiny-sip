The SipManager is the instance that handles the session establishment, teardown and all messages.

The SipManager exports all necessary functions for your application to place, receive as well as decline and cancel a call. The exported methods are shown in the Javadoc or in the TinySipDemo.

It consists of a state machine that goes through certain states while setting up a SIP session. Each change of state is notified to your application by raising a SipManagerStatusChangedEvent.

Please refer to http://en.wikipedia.org/wiki/Session_Initiation_Protocol for further details about the states of a SIP session.