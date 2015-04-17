<h2>Welcome to the TinySip project</h2>
TinySip is an easy-to-use SIP UA implementation for Android. It includes a SIP stack, a STUN library and handles all the important features to set up a simple VoIP-call.

Android features a SIP API from version 2.3.3. But this API does not support any NAT traversal and is limited in function. This is why I decided to implement my own SIP API and include the NAT traversal tools.

Please feel free to use this code as you wish, according to the Apache License 2.0. I would appreciate any feedback if TinySip helped your project. Since the project is still under development, I am glad for every spotted error and I am also grateful for every contribution. Please read the ToDo-list and issue tracker for missing features and errors.

Note: this project does only include SIP session setup tools and does not handle media streams. Please take a look at RTP for media streams here: http://en.wikipedia.org/wiki/Real-time_Transport_Protocol <br>
An Android port of the jlibrtp can be found here: <a href='http://www.hsc.com/tabid/87/Default.aspx'>http://www.hsc.com/tabid/87/Default.aspx</a>

<h2>How it works</h2>
The TinySip project combines a SIP stack with a STUN library for Android to make simple SIP signalling and call setup.<br>
<br>
Please have a look at the Wikipedia articles for SIP and STUN here:<br>
SIP: <a href='http://en.wikipedia.org/wiki/Session_Initiation_Protocol'>http://en.wikipedia.org/wiki/Session_Initiation_Protocol</a><br>
STUN: <a href='http://en.wikipedia.org/wiki/Session_Traversal_Utilities_for_NAT'>http://en.wikipedia.org/wiki/Session_Traversal_Utilities_for_NAT</a>

STUN handles a large amount of NATs and firewalls and makes it easier for a SIP session setup to pass the NAT. STUN is also required for audio and video streams to reach the application behind a NAT.<br>
<br>
<h2>Requirements:</h2>

<b>JAIN-SIP-1.2 Stack</b><br>
Description: <a href='http://jsip.java.net/'>http://jsip.java.net/</a><br>
Sources: <a href='https://hudson.jboss.org/jenkins/job/jain-sip/lastSuccessfulBuild/artifact/'>https://hudson.jboss.org/jenkins/job/jain-sip/lastSuccessfulBuild/artifact/</a><br>

<b>Modified JSTUN</b><br>
Description: <a href='http://jstun.javawi.de/'>http://jstun.javawi.de/</a><br>
Sources: <a href='http://code.google.com/p/de-tiny-sip/source/browse/#svn%2Ftrunk%2Fsrc%2Fde%2Fjavawi%2Fjstun'>http://code.google.com/p/de-tiny-sip/source/browse/#svn%2Ftrunk%2Fsrc%2Fde%2Fjavawi%2Fjstun</a><br>
(Get the original sources here: <a href='http://www.hsc.com/tabid/87/Default.aspx'>http://www.hsc.com/tabid/87/Default.aspx</a>)<br>

<b>Log4J</b><br>
Description: <a href='http://logging.apache.org/log4j/1.2/index.html'>http://logging.apache.org/log4j/1.2/index.html</a><br>
Download: <a href='http://logging.apache.org/log4j/1.2/download.html'>http://logging.apache.org/log4j/1.2/download.html</a><br>

<b>SLF4J</b><br>
Description: <a href='http://www.slf4j.org/'>http://www.slf4j.org/</a><br>
Download: <a href='http://www.slf4j.org/dist/slf4j-1.6.1.zip'>http://www.slf4j.org/dist/slf4j-1.6.1.zip</a><br>

<h2>Getting Started</h2>
<h3>Demo Project Setup</h3>
To get startet, check out the project using the SVN path in the Source-tab. It sets up an Android project in Eclipse which contains all neccessary files and dependencies.<br>
<br>
Next, go to the <code>TinySipDemoActivity.java</code> and provide your SIP provider's details to register your account. Also, you need to specify the SIP user you want your application to call as soon as it registered your account.<br>
<br>
Now you're good to go. The Android application should start and display the current state. Watch out: STUN discovery can take quite some time, depending on the type of NAT you use.<br>
<br>
<h3>Getting the sources</h3>
The TinySip project is located inside the de.tinysip.<code>*</code> namespace. You can find the sources in the Source-tab and then browse to the de.tinysip.<code>*</code> package or check out the files using SVN.<br>
<br>
The de.tinysip.sip package contains all classes necessary for the SIP handling and message creation. Also, the user and contact details are included here.<br>
<br>
In the de.tinysip.stun package are all classes needed for the STUN discovery. The de.tinysip.demo package contains the files needed for the Android demo project.<br>
<br>
All other packages contain the classes and files referenced by the TinySip project. These include the jSIP stack and the jSTUN lib. The logging tools are required by both the jSIP and the jSTUN libs. These are located in the assets-folder and need to be included into the Java Build Path (already done in the demo project).<br>
<br>
<h3>Implementation</h3>
This section explains a typical usage of the TinySip project in your own application. All values are examples only and need to be replaced by your or the user's provider's details.<br>
<br>
First, create a LocalSipProfile with the user's credentials:<br><br>

<pre><code>// the local user's credentials<br>
LocalSipProfile profile = new LocalSipProfile("SIP UserName", "SIP Domain", "SIP Password", SIP Registrar Port, "Display Name");<br>
</code></pre>

Then, add your applications supported audio formats to the local profile.<br>
<pre><code>// create a list of supported audio formats for the local user agent<br>
ArrayList&lt;SipAudioFormat&gt; audioFormats = new ArrayList&lt;SipAudioFormat&gt;();<br>
audioFormats.add(new SipAudioFormat(SdpConstants.PCMA, "PCMA", 8000));<br>
localSipProfile.setAudioFormats(audioFormats);<br>
        <br>
// set ports for rtp and rtcp<br>
localSipProfile.setLocalRtpPort(5071);<br>
localSipProfile.setLocalRtcpPort(5072);<br>
</code></pre>

Next, you need to create the SipContact with the phone number or SIP account you want to call:<br>
<pre><code>// the sip contact to call<br>
SipContact contact = new SipContact("SIP User", "SIP Domain", true);<br>
</code></pre>
for a SIP URI or<br>
<pre><code>SipContact contact = new SipContact("Phone Number", "Local SIP Domain", false);<br>
</code></pre>
for a regular phone number.<br><br>

To start the STUN discovery for NAT traversal, do the following:<br>
<pre><code>// the STUN server and port for NAT traversal<br>
STUNInfo sipPortInfo = new STUNInfo(STUNInfo.TYPE_SIP, "stun.sipgate.net", 10000);<br>
sipPortInfo.setLocalPort(5070); // local port to use for SIP<br>
STUNDiscoveryTask sipPortTask = new STUNDiscoveryTask();<br>
sipPortTask.addResultListener(this);<br>
sipPortTask.execute(sipPortInfo);<br>
</code></pre>

The STUNDiscoveryTask will raise an event (STUNDiscoveryResultEvent) when it has finished testing the specified ports and NAT. According to the results, you can then start the SIP registration. To do so, you need to create a SipManager instance to handle the SIP session, states and messages.<br>
<pre><code>@Override<br>
public void STUNDiscoveryResultChanged(STUNDiscoveryResultEvent event) {<br>
  if (event.getDiscoveryInfo() != null)<br>
    {<br>
      DiscoveryInfo discoveryInfo = event.getDiscoveryInfo();<br>
      STUNInfo stunInfo = event.getStunInfo();<br>
<br>
      switch (stunInfo.getType())<br>
	{<br>
	  case STUNInfo.TYPE_SIP:<br>
	    sipDiscoveryInfo = discoveryInfo;<br>
	    // STUN test was completed, start SIP registration now<br>
	    startSipRegistration();<br>
	    break;<br>
        }<br>
    }		<br>
}<br>
<br>
private void startSipRegistration(){<br>
  sipManager = SipManager.createInstance(localSipProfile, sipDiscoveryInfo);<br>
  sipManager.addStatusListener(this);<br>
    <br>
  try<br>
  {<br>
    sipManager.registerProfile();<br>
  }<br>
  catch (Exception e)<br>
  {<br>
  }<br>
}<br>
</code></pre>

Now the SipManager is trying to register the local user's account with the provider. As soon as this was successfull, the SipManager will raise a SipManagerStatusChanged event. This is how you listen to these events:<br>
<pre><code>@Override<br>
public void SipManagerStatusChanged(SipManagerStatusChangedEvent event) {<br>
  final SipManagerStatusChangedEvent fevent = event;<br>
  this.runOnUiThread(new Runnable()<br>
  {<br>
    @Override<br>
    public void run()<br>
    {<br>
      state = fevent.getState();<br>
<br>
      switch (state)<br>
      {<br>
      case IDLE:<br>
	break;<br>
      //....		<br>
      case READY:<br>
	try <br>
        {<br>
          // start calling the remote SIP contact<br>
	  sipManager.sendInvite(sipContact);<br>
	} <br>
        catch (Exception e) <br>
        {<br>
        }<br>
      }<br>
  });		<br>
}<br>
</code></pre>
<h2>Tested providers</h2>
Since SIP is an open standard, compatibility is not always guaranteed. Depending on the SIP provider, some features might work differently. That makes it necessary to test the TinySip project with a large amount of providers to make sure it works correctly.<br>
<br>
The current functions of TinySip project have been successfully tested with the following providers:<br>
<ul>
<blockquote><li>1&1 Germany</li>
<li>Sipgate</li>
</ul></blockquote>

<h2>ToDo</h2>
<ul>
<blockquote><li>Test with different providers</li>
<li>Add support for different provider authentication schemes</li>
<li>Increase amount of SIP features supported by TinySip, like call forwarding and conference calls</li>
<li>Improve stability</li>
</ul></blockquote>

