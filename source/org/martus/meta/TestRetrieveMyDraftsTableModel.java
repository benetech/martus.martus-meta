/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2003-2004, Beneficent
Technology, Inc. (Benetech).

Martus is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later
version with the additions and exceptions described in the
accompanying Martus license file entitled "license.txt".

It is distributed WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, including warranties of fitness of purpose or
merchantability.  See the accompanying Martus License and
GPL license for more details on the required license terms
for this software.

You should have received a copy of the GNU General Public
License along with this program; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA 02111-1307, USA.

*/

package org.martus.meta;

import java.io.StringWriter;
import java.util.Vector;

import org.martus.client.core.BulletinSummary;
import org.martus.client.swingui.tablemodels.RetrieveMyDraftsTableModel;
import org.martus.client.test.MockMartusApp;
import org.martus.common.MartusConstants;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.clientside.UiBasicLocalization;
import org.martus.common.clientside.test.MockUiLocalization;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MockMartusSecurity;
import org.martus.common.network.NetworkInterface;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NonSSLNetworkAPI;
import org.martus.common.packet.FieldDataPacket;
import org.martus.common.packet.UniversalId;
import org.martus.server.forclients.MockMartusServer;
import org.martus.server.forclients.ServerForClients;
import org.martus.server.forclients.ServerSideNetworkHandler;
import org.martus.server.forclients.ServerSideNetworkHandlerForNonSSL;
import org.martus.util.TestCaseEnhanced;

public class TestRetrieveMyDraftsTableModel extends TestCaseEnhanced
{
	public TestRetrieveMyDraftsTableModel(String name) 
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
		b0.setDraft();
		app.getStore().saveBulletin(b0);
		b1 = app.createBulletin();
		b1.set(Bulletin.TAGTITLE, title1);
		b1.setSealed();
		app.getStore().saveBulletin(b1);
		b2 = app.createBulletin();
		b2.set(Bulletin.TAGTITLE, title2);
		b2.setDraft();
		app.getStore().saveBulletin(b2);

		testServer = new MockServer();
		testServer.verifyAndLoadConfigurationFiles();
		
		testServerInterface = new ServerSideNetworkHandlerForNonSSL(testServer.serverForClients);
		testSSLServerInterface = new ServerSideNetworkHandler(testServer.serverForClients);
		app.setSSLNetworkInterfaceHandlerForTesting(testSSLServerInterface);
		modelWithoutData = new RetrieveMyDraftsTableModel(app, localization);
		modelWithoutData.initialize(null);
		app.getStore().deleteAllData();
		assertFalse("deleteAllData didn't?", app.getStore().doesBulletinRevisionExist(b0.getUniversalId()));
		modelWithData = new RetrieveMyDraftsTableModel(app, localization);
		modelWithData.initialize(null);
	}
	
	public void tearDown() throws Exception
	{
		testServer.deleteAllFiles();
    	app.deleteAllFiles();
    	super.tearDown();
    }

	public void testGetColumnName() throws Exception
	{
		assertEquals(localization.getFieldLabel("retrieveflag"), modelWithData.getColumnName(RetrieveMyDraftsTableModel.COLUMN_RETRIEVE_FLAG));
		assertEquals(localization.getFieldLabel(Bulletin.TAGTITLE), modelWithData.getColumnName(RetrieveMyDraftsTableModel.COLUMN_TITLE));
		assertEquals(localization.getFieldLabel(Bulletin.TAGLASTSAVED), modelWithData.getColumnName(RetrieveMyDraftsTableModel.COLUMN_LAST_DATE_SAVED));
		assertEquals(localization.getFieldLabel("BulletinSize"), modelWithData.getColumnName(RetrieveMyDraftsTableModel.COLUMN_BULLETIN_SIZE));
	}
	
	public void testGetColumnCount()
	{
		assertEquals(4, modelWithoutData.getColumnCount());
		assertEquals(4, modelWithData.getColumnCount());
	}
	
	public void testGetRowCount()
	{
		assertEquals(0, modelWithoutData.getRowCount());
		assertEquals(2, modelWithData.getRowCount());
	}
	
	public void testIsCellEditable()
	{
		assertEquals("flag", true, modelWithData.isCellEditable(1,RetrieveMyDraftsTableModel.COLUMN_RETRIEVE_FLAG));
		assertEquals("title", false, modelWithData.isCellEditable(1,RetrieveMyDraftsTableModel.COLUMN_TITLE));
		assertEquals("size", false, modelWithData.isCellEditable(1,RetrieveMyDraftsTableModel.COLUMN_LAST_DATE_SAVED));
		assertEquals("date", false, modelWithData.isCellEditable(1,RetrieveMyDraftsTableModel.COLUMN_BULLETIN_SIZE));
	}
	
	public void testGetColumnClass()
	{
		assertEquals(Boolean.class, modelWithData.getColumnClass(RetrieveMyDraftsTableModel.COLUMN_RETRIEVE_FLAG));
		assertEquals(String.class, modelWithData.getColumnClass(RetrieveMyDraftsTableModel.COLUMN_TITLE));
		assertEquals(String.class, modelWithData.getColumnClass(RetrieveMyDraftsTableModel.COLUMN_LAST_DATE_SAVED));
		assertEquals(Integer.class, modelWithData.getColumnClass(RetrieveMyDraftsTableModel.COLUMN_BULLETIN_SIZE));
	}
	
	public void testGetAndSetValueAt()
	{
		assertEquals("start bool", false, ((Boolean)modelWithData.getValueAt(0,RetrieveMyDraftsTableModel.COLUMN_RETRIEVE_FLAG)).booleanValue());
		modelWithData.setValueAt(new Boolean(true), 0,RetrieveMyDraftsTableModel.COLUMN_RETRIEVE_FLAG);
		assertEquals("setget bool", true, ((Boolean)modelWithData.getValueAt(0,RetrieveMyDraftsTableModel.COLUMN_RETRIEVE_FLAG)).booleanValue());

		assertEquals("start title", title2, modelWithData.getValueAt(1,RetrieveMyDraftsTableModel.COLUMN_TITLE));
		modelWithData.setValueAt(title2+title2, 1,RetrieveMyDraftsTableModel.COLUMN_TITLE);
		assertEquals("keep title", title2, modelWithData.getValueAt(1,RetrieveMyDraftsTableModel.COLUMN_TITLE));

		assertEquals("b2 size", new Integer(b2Size/1000), modelWithData.getValueAt(1,RetrieveMyDraftsTableModel.COLUMN_BULLETIN_SIZE));

		String expectedDateSaved = localization.convertStoredDateTimeToDisplay(BulletinSummary.getLastDateTimeSaved(dateSavedInMillis2));
		assertEquals("start date", expectedDateSaved, modelWithData.getValueAt(1,RetrieveMyDraftsTableModel.COLUMN_LAST_DATE_SAVED));
		modelWithData.setValueAt("some date", 1,RetrieveMyDraftsTableModel.COLUMN_LAST_DATE_SAVED);
		assertEquals("keep date", expectedDateSaved, modelWithData.getValueAt(1,RetrieveMyDraftsTableModel.COLUMN_LAST_DATE_SAVED));
	}
	
	public void testSetAllFlags()
	{
		Boolean t = new Boolean(true);
		Boolean f = new Boolean(false);
		
		modelWithData.setAllFlags(true);
		for(int allTrueCounter = 0; allTrueCounter < modelWithData.getRowCount(); ++allTrueCounter)
			assertEquals("all true" + allTrueCounter, t, modelWithData.getValueAt(0,RetrieveMyDraftsTableModel.COLUMN_RETRIEVE_FLAG));

		modelWithData.setAllFlags(false);
		for(int allFalseCounter = 0; allFalseCounter < modelWithData.getRowCount(); ++allFalseCounter)
			assertEquals("all false" + allFalseCounter, f, modelWithData.getValueAt(0,RetrieveMyDraftsTableModel.COLUMN_RETRIEVE_FLAG));
	}
	
	public void testGetIdList()
	{
		modelWithData.setAllFlags(false);
		Vector emptyList = modelWithData.getUniversalIdList();
		assertEquals(0, emptyList.size());
		
		modelWithData.setAllFlags(true);
		modelWithData.setValueAt(new Boolean(false), 1, RetrieveMyDraftsTableModel.COLUMN_RETRIEVE_FLAG);
		Vector twoList = modelWithData.getUniversalIdList();
		assertEquals(1, twoList.size());
		assertEquals("b0 id", b0.getUniversalId(), twoList.get(0));
	}

	class MockServer extends MockMartusServer
	{
		MockServer() throws Exception
		{
			super();
		}
		
		public ServerForClients createServerForClients()
		{
			return new LocalMockServerForClients(this);
		}
		
		class LocalMockServerForClients extends ServerForClients
		{
			LocalMockServerForClients(MockMartusServer coreServer)
			{
				super(coreServer);
			}

			public Vector listMyDraftBulletinIds(String authorAccountId, Vector retrieveTags) 
			{
				Vector result = new Vector();
				result.add(NetworkInterfaceConstants.OK);
				Vector list = new Vector();
				list.add(b0.getLocalId() + "=" + b0.getFieldDataPacket().getLocalId() + "=" + b0Size);
				list.add(b2.getLocalId() + MartusConstants.regexEqualsDelimeter + 
						b2.getFieldDataPacket().getLocalId() +
						MartusConstants.regexEqualsDelimeter + 
						b2Size + 
						MartusConstants.regexEqualsDelimeter + 
						dateSavedInMillis2);

				
				result.add(list);
				return result;
			}

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
	
	final static String title1 = "This is a cool title";
	final static String title2 = "Even cooler";
	final static String dateSavedInMillis2 = "1083873923190";
	final static int b0Size = 3000;
	final static int b1Size = 5000;
	final static int b2Size = 8000;

	MockMartusServer testServer;
	NonSSLNetworkAPI testServerInterface;
	NetworkInterface testSSLServerInterface;
	UiBasicLocalization localization;
	MockMartusApp app;
	Bulletin b0;
	Bulletin b1;
	Bulletin b2;
	
	RetrieveMyDraftsTableModel modelWithData;
	RetrieveMyDraftsTableModel modelWithoutData;
}
