package org.martus.meta;

import java.util.Vector;

import org.martus.client.core.ClientSideNetworkHandlerUsingXmlRpc;
import org.martus.common.crypto.MockMartusSecurity;
import org.martus.common.network.MartusSecureWebServer;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.common.network.SimpleX509TrustManager;
import org.martus.common.test.TestCaseEnhanced;
import org.martus.server.forclients.MockMartusServer;
import org.martus.server.forclients.ServerForClients;
import org.martus.server.forclients.ServerSideNetworkHandler;



public class TestSSL extends TestCaseEnhanced 
{
	public TestSSL(String name) 
	{
		super(name);
	}

	public void setUp() throws Exception
	{
		super.setUp();
		if(mockSecurityForServer == null)
		{
			int[] nonSslPorts = {1988};
			int[] sslPorts = {1987};
			mockSecurityForServer = MockMartusSecurity.createServer();
			mockServer = new MockMartusServer();
			mockServer.verifyAndLoadConfigurationFiles();
			mockServer.setSecurity(mockSecurityForServer);
			MartusSecureWebServer.security = mockSecurityForServer;
			
			serverForClients = new ServerForClients(mockServer);
			serverForClients.handleNonSSL(nonSslPorts);
			serverForClients.handleSSL(sslPorts);
			
//			XmlRpc.debug = true;
			proxy1 = new ClientSideNetworkHandlerUsingXmlRpc("localhost", sslPorts);
//			proxy2 = new ClientSideNetworkHandlerUsingXmlRpc("localhost", testport);
		}
	}
	
	public void tearDown() throws Exception
	{
		mockServer.deleteAllFiles();
		serverForClients.prepareToShutdown();
		super.tearDown();
	}

	
	public void testBasics()
	{
		verifyBadCertBeforeGoodCertHasBeenAccepted();
		verifyGoodCertAndItWillNotBeReverifiedThisSession();

	}
	
	public void verifyBadCertBeforeGoodCertHasBeenAccepted()
	{
		SimpleX509TrustManager trustManager = proxy1.getSimpleX509TrustManager();
		assertNull("Already trusted?", trustManager.getExpectedPublicKey());

		proxy1.getSimpleX509TrustManager().setExpectedPublicCode("Not a valid code");
		trustManager.clearCalledCheckServerTrusted();
		assertNull("accepted bad cert?", proxy1.getServerInfo(new Vector()));
		assertTrue("Never checked ssl cert!", trustManager.wasCheckServerTrustedCalled());
	}
	
	public void verifyGoodCertAndItWillNotBeReverifiedThisSession()
	{
		String serverAccountId = mockSecurityForServer.getPublicKeyString();
		SimpleX509TrustManager trustManager = proxy1.getSimpleX509TrustManager();
		trustManager.setExpectedPublicKey(serverAccountId);

		Vector parameters = new Vector();
		NetworkResponse result = new NetworkResponse(proxy1.getServerInfo(parameters));
		assertEquals(NetworkInterfaceConstants.OK, result.getResultCode());
		assertEquals(NetworkInterfaceConstants.VERSION, result.getResultVector().get(0));
		assertEquals(serverAccountId, trustManager.getExpectedPublicKey());

		NetworkResponse response = new NetworkResponse(proxy1.getServerInfo(new Vector()));
		assertEquals(NetworkInterfaceConstants.OK, response.getResultCode());
		assertEquals(NetworkInterfaceConstants.VERSION, response.getResultVector().get(0));
	}
	
	static MockMartusSecurity mockSecurityForServer;
	static MockMartusServer mockServer;
	static ServerSideNetworkHandler mockSSLServerInterface;
	static ClientSideNetworkHandlerUsingXmlRpc proxy1;
	static ServerForClients serverForClients;
}
