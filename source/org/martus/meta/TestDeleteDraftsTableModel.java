package org.martus.meta;

import java.io.StringWriter;
import java.util.Vector;

import org.martus.client.swingui.tablemodels.DeleteMyServerDraftsTableModel;
import org.martus.client.test.MockMartusApp;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.clientside.test.MockUiLocalization;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MockMartusSecurity;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.packet.FieldDataPacket;
import org.martus.common.packet.UniversalId;
import org.martus.common.test.TestCaseEnhanced;
import org.martus.server.forclients.MockMartusServer;
import org.martus.server.forclients.ServerSideNetworkHandler;

public class TestDeleteDraftsTableModel extends TestCaseEnhanced
{

	public TestDeleteDraftsTableModel(String name)
	{
		super(name);
	}

	public void setUp() throws Exception
	{
		super.setUp();
		MartusCrypto appSecurity = MockMartusSecurity.createClient();
		localization = new MockUiLocalization();
		app = MockMartusApp.create(appSecurity);

		b0 = app.createBulletin();
		b0.set(Bulletin.TAGTITLE, title1);
		b1 = app.createBulletin();
		b1.set(Bulletin.TAGTITLE, title1);
		b2 = app.createBulletin();
		b2.set(Bulletin.TAGTITLE, title2);
		
		testServer = new MockServer();
		testServer.verifyAndLoadConfigurationFiles();
		MockMartusSecurity serverSecurity = MockMartusSecurity.createServer();
		testServer.setSecurity(serverSecurity);
		ServerSideNetworkHandler testSSLServerInterface = new ServerSideNetworkHandler(testServer);
		
		app.setSSLNetworkInterfaceHandlerForTesting(testSSLServerInterface);

		testServer.hasData = false;
		modelWithoutData = new DeleteMyServerDraftsTableModel(app, localization);
		modelWithoutData.initialize(null);
		
		testServer.hasData = true;
		modelWithData = new DeleteMyServerDraftsTableModel(app, localization);
		modelWithData.initialize(null);
	}
	
	public void tearDown() throws Exception
	{
		testServer.deleteAllFiles();
    	app.deleteAllFiles();
    	super.tearDown();
    }

	public void testGetColumnCount()
	{
		assertEquals(3, modelWithoutData.getColumnCount());
		assertEquals(3, modelWithData.getColumnCount());
	}
	
	public void testGetColumnName()
	{
		assertEquals(localization.getFieldLabel("DeleteFlag"), modelWithData.getColumnName(0));
		assertEquals(localization.getFieldLabel(Bulletin.TAGTITLE), modelWithData.getColumnName(1));
		assertEquals(localization.getFieldLabel("BulletinSize"), modelWithData.getColumnName(2));
	}
	
	public void testGetColumnClass()
	{
		assertEquals(Boolean.class, modelWithData.getColumnClass(0));
		assertEquals(String.class, modelWithData.getColumnClass(1));
		assertEquals(Integer.class, modelWithData.getColumnClass(2));
	}
	
	public void testRowCount()
	{
		assertEquals(0, modelWithoutData.getRowCount());
		assertEquals(3, modelWithData.getRowCount());
	}
	
	public void testGetAndSetValueAt()
	{
		assertEquals("start bool", false, ((Boolean)modelWithData.getValueAt(0,0)).booleanValue());
		modelWithData.setValueAt(new Boolean(true), 0,0);
		assertEquals("setget bool", true, ((Boolean)modelWithData.getValueAt(0,0)).booleanValue());

		assertEquals("start title", title2, modelWithData.getValueAt(2,1));
		modelWithData.setValueAt(title2+title2, 2,1);
		assertEquals("keep title", title2, modelWithData.getValueAt(2,1));
	}
	
	class MockServer extends MockMartusServer
	{
		MockServer() throws Exception
		{
			super();
		}
		
		public Vector listMyDraftBulletinIds(String clientId, Vector retrieveTags)
		{
			Vector result = new Vector();
			result.add(NetworkInterfaceConstants.OK);
			Vector list = new Vector();
			if(hasData)
			{
				list.add(b0.getLocalId() + "=" + b0.getFieldDataPacket().getLocalId() + "=3000");
				list.add(b1.getLocalId() + "=" + b1.getFieldDataPacket().getLocalId() + "=3100");
				list.add(b2.getLocalId() + "=" + b2.getFieldDataPacket().getLocalId() + "=3200");
			}
			result.add(list);
			return result;
		}
		
		public Vector getPacket(String hqAccountId, String authorAccountId, String bulletinLocalId, String packetLocalId)
		{
			Vector result = new Vector();
			try 
			{
				UniversalId uid = UniversalId.createFromAccountAndLocalId(authorAccountId, packetLocalId);
				FieldDataPacket fdp = null;
				MartusCrypto security = app.getSecurity();
				if(uid.equals(b0.getFieldDataPacket().getUniversalId()))
					fdp = b0.getFieldDataPacket();
				if(uid.equals(b1.getFieldDataPacket().getUniversalId()))
					fdp = b1.getFieldDataPacket();
				if(uid.equals(b2.getFieldDataPacket().getUniversalId()))
					fdp = b2.getFieldDataPacket();
				StringWriter writer = new StringWriter();
				fdp.writeXml(writer, security);
				result.add(NetworkInterfaceConstants.OK);
				result.add(writer.toString());
				writer.close();
			} 
			catch (Exception e) 
			{
				result.add(NetworkInterfaceConstants.SERVER_ERROR);
			}
			return result;
		}

		public boolean hasData;
		
	}
	
	final static String title1 = "This is a cool title";
	final static String title2 = "Even cooler";

	MockUiLocalization localization;
	MockMartusApp app;
	MockServer testServer;
	DeleteMyServerDraftsTableModel modelWithData;
	DeleteMyServerDraftsTableModel modelWithoutData;
	
	Bulletin b0;
	Bulletin b1;
	Bulletin b2;
}
