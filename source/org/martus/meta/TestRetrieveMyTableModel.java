package org.martus.meta;

import java.io.StringWriter;
import java.util.Vector;

import org.martus.client.swingui.tablemodels.RetrieveMyTableModel;
import org.martus.client.test.*;
import org.martus.client.test.MockMartusApp;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.clientside.UiBasicLocalization;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MockMartusSecurity;
import org.martus.common.network.NetworkInterface;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkInterfaceForNonSSL;
import org.martus.common.packet.FieldDataPacket;
import org.martus.common.packet.UniversalId;
import org.martus.common.test.TestCaseEnhanced;
import org.martus.server.forclients.MockMartusServer;
import org.martus.server.forclients.ServerSideNetworkHandler;
import org.martus.server.forclients.ServerSideNetworkHandlerForNonSSL;

public class TestRetrieveMyTableModel extends TestCaseEnhanced
{
	public TestRetrieveMyTableModel(String name) 
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
		app.getStore().saveBulletin(b0);
		b1 = app.createBulletin();
		b1.set(Bulletin.TAGTITLE, title1);
		app.getStore().saveBulletin(b1);
		b2 = app.createBulletin();
		b2.set(Bulletin.TAGTITLE, title2);
		app.getStore().saveBulletin(b2);

		testServer = new MockServer();
		testServer.verifyAndLoadConfigurationFiles();
		
		testServerInterface = new ServerSideNetworkHandlerForNonSSL(testServer);
		testSSLServerInterface = new ServerSideNetworkHandler(testServer);
		app.setSSLNetworkInterfaceHandlerForTesting(testSSLServerInterface);
		modelWithoutData = new RetrieveMyTableModel(app, localization);
		modelWithoutData.initialize(null);
		app.getStore().deleteAllData();
		modelWithData = new RetrieveMyTableModel(app, localization);
		modelWithData.initialize(null);
	}
	
	public void tearDown() throws Exception
	{
		testServer.deleteAllFiles();
    	app.deleteAllFiles();
    	super.tearDown();
    }

	public void testGetColumnName()
	{
		assertEquals(localization.getFieldLabel("retrieveflag"), modelWithData.getColumnName(0));
		assertEquals(localization.getFieldLabel(Bulletin.TAGTITLE), modelWithData.getColumnName(1));
		assertEquals(localization.getFieldLabel("BulletinSize"), modelWithData.getColumnName(2));
	}
	
	public void testGetColumnCount()
	{
		assertEquals(3, modelWithoutData.getColumnCount());
		assertEquals(3, modelWithData.getColumnCount());
	}
	
	public void testGetRowCount()
	{
		assertEquals(0, modelWithoutData.getRowCount());
		assertEquals(3, modelWithData.getRowCount());
	}
	
	public void testIsCellEditable()
	{
		assertEquals("flag", true, modelWithData.isCellEditable(1,0));
		assertEquals("title", false, modelWithData.isCellEditable(1,1));
		assertEquals("size", false, modelWithData.isCellEditable(1,2));
	}
	
	public void testGetColumnClass()
	{
		assertEquals(Boolean.class, modelWithData.getColumnClass(0));
		assertEquals(String.class, modelWithData.getColumnClass(1));
		assertEquals(Integer.class, modelWithData.getColumnClass(2));
	}
	
	public void testGetAndSetValueAt()
	{
		assertEquals("start bool", false, ((Boolean)modelWithData.getValueAt(0,0)).booleanValue());
		modelWithData.setValueAt(new Boolean(true), 0,0);
		assertEquals("setget bool", true, ((Boolean)modelWithData.getValueAt(0,0)).booleanValue());

		assertEquals("start title", title2, modelWithData.getValueAt(2,1));
		modelWithData.setValueAt(title2+title2, 2,1);
		assertEquals("keep title", title2, modelWithData.getValueAt(2,1));

		assertEquals("b0 size", new Integer(b0Size/1000), modelWithData.getValueAt(0,2));
		assertEquals("b1 size", new Integer(b1Size/1000), modelWithData.getValueAt(1,2));
		assertEquals("b2 size", new Integer(b2Size/1000), modelWithData.getValueAt(2,2));
	}
	

	public void testSetAllFlags()
	{
		Boolean t = new Boolean(true);
		Boolean f = new Boolean(false);
		
		modelWithData.setAllFlags(true);
		for(int allTrueCounter = 0; allTrueCounter < modelWithData.getRowCount(); ++allTrueCounter)
			assertEquals("all true" + allTrueCounter, t, modelWithData.getValueAt(0,0));

		modelWithData.setAllFlags(false);
		for(int allFalseCounter = 0; allFalseCounter < modelWithData.getRowCount(); ++allFalseCounter)
			assertEquals("all false" + allFalseCounter, f, modelWithData.getValueAt(0,0));
	}
	
	public void testGetIdList()
	{
		modelWithData.setAllFlags(false);
		Vector emptyList = modelWithData.getUniversalIdList();
		assertEquals(0, emptyList.size());
		
		modelWithData.setAllFlags(true);
		modelWithData.setValueAt(new Boolean(false), 1, 0);
		Vector twoList = modelWithData.getUniversalIdList();
		assertEquals(2, twoList.size());
		assertEquals("b0 id", b0.getUniversalId(), twoList.get(0));
		assertEquals("b2 id", b2.getUniversalId(), twoList.get(1));
	}

	class MockServer extends MockMartusServer
	{
		MockServer() throws Exception
		{
			super();
		}
		
		public Vector listMySealedBulletinIds(String clientId, Vector retrieveTags)
		{
			Vector result = new Vector();
			result.add(NetworkInterfaceConstants.OK);
			Vector list = new Vector();
			list.add(b0.getLocalId() + "=" +  b0.getFieldDataPacket().getLocalId() + "=" + b0Size);
			list.add(b1.getLocalId() + "=" +  b1.getFieldDataPacket().getLocalId() + "=" + b1Size);
			list.add(b2.getLocalId() + "=" +  b2.getFieldDataPacket().getLocalId() + "=" + b2Size);
			result.add(list);
			Vector sizes = new Vector();
			if(retrieveTags.size() == 1)
			{
				sizes.add(new Integer(b0Size));
				sizes.add(new Integer(b1Size));
				sizes.add(new Integer(b2Size));
			}
			result.add(sizes);
			return result;
		}
		
		public Vector getPacket(String hqAccountId, String authorAccountId, String bulletinLocalId, String packetLocalId)
		{
			Vector result = new Vector();
			try 
			{
				UniversalId uid = UniversalId.createFromAccountAndLocalId(authorAccountId, packetLocalId);
				FieldDataPacket fdp = null;
				if(uid.equals(b0.getFieldDataPacket().getUniversalId()))
					fdp = b0.getFieldDataPacket();
				if(uid.equals(b1.getFieldDataPacket().getUniversalId()))
					fdp = b1.getFieldDataPacket();
				if(uid.equals(b2.getFieldDataPacket().getUniversalId()))
					fdp = b2.getFieldDataPacket();
				StringWriter writer = new StringWriter();
				MartusCrypto security = app.getSecurity();
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
	}
	
	String title1 = "This is a cool title";
	String title2 = "Even cooler";

	MockMartusServer testServer;
	NetworkInterfaceForNonSSL testServerInterface;
	NetworkInterface testSSLServerInterface;
	MockMartusApp app;
	UiBasicLocalization localization;
	Bulletin b0;
	Bulletin b1;
	Bulletin b2;
	int b0Size = 3000;
	int b1Size = 5000;
	int b2Size = 8000;

	RetrieveMyTableModel modelWithData;
	RetrieveMyTableModel modelWithoutData;
}
