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

import org.martus.client.swingui.tablemodels.DeleteMyServerDraftsTableModel;
import org.martus.client.test.MockMartusApp;
import org.martus.common.MartusConstants;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.clientside.test.MockUiLocalization;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MockMartusSecurity;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.packet.FieldDataPacket;
import org.martus.common.packet.UniversalId;
import org.martus.server.forclients.MockMartusServer;
import org.martus.server.forclients.ServerSideNetworkHandler;
import org.martus.util.TestCaseEnhanced;

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
		assertEquals(4, modelWithoutData.getColumnCount());
		assertEquals(4, modelWithData.getColumnCount());
	}
	
	public void testGetColumnName()
	{
		assertEquals(localization.getFieldLabel("DeleteFlag"), modelWithData.getColumnName(DeleteMyServerDraftsTableModel.COLUMN_DELETE_FLAG));
		assertEquals(localization.getFieldLabel(Bulletin.TAGTITLE), modelWithData.getColumnName(DeleteMyServerDraftsTableModel.COLUMN_TITLE));
		assertEquals(localization.getFieldLabel("BulletinDateSaved"), modelWithData.getColumnName(DeleteMyServerDraftsTableModel.COLUMN_LAST_DATE_SAVED));
		assertEquals(localization.getFieldLabel("BulletinSize"), modelWithData.getColumnName(DeleteMyServerDraftsTableModel.COLUMN_BULLETIN_SIZE));
	}
	
	public void testGetColumnClass()
	{
		assertEquals(Boolean.class, modelWithData.getColumnClass(DeleteMyServerDraftsTableModel.COLUMN_DELETE_FLAG));
		assertEquals(String.class, modelWithData.getColumnClass(DeleteMyServerDraftsTableModel.COLUMN_TITLE));
		assertEquals(String.class, modelWithData.getColumnClass(DeleteMyServerDraftsTableModel.COLUMN_LAST_DATE_SAVED));
		assertEquals(Integer.class, modelWithData.getColumnClass(DeleteMyServerDraftsTableModel.COLUMN_BULLETIN_SIZE));
	}
	
	public void testRowCount()
	{
		assertEquals(0, modelWithoutData.getRowCount());
		assertEquals(3, modelWithData.getRowCount());
	}
	
	public void testGetAndSetValueAt()
	{
		assertEquals("start bool", false, ((Boolean)modelWithData.getValueAt(0,DeleteMyServerDraftsTableModel.COLUMN_DELETE_FLAG)).booleanValue());
		modelWithData.setValueAt(new Boolean(true), 0,DeleteMyServerDraftsTableModel.COLUMN_DELETE_FLAG);
		assertEquals("setget bool", true, ((Boolean)modelWithData.getValueAt(0,DeleteMyServerDraftsTableModel.COLUMN_DELETE_FLAG)).booleanValue());

		assertEquals("start title", title2, modelWithData.getValueAt(2,DeleteMyServerDraftsTableModel.COLUMN_TITLE));
		modelWithData.setValueAt(title2+title2, 2,DeleteMyServerDraftsTableModel.COLUMN_TITLE);
		assertEquals("keep title", title2, modelWithData.getValueAt(2,DeleteMyServerDraftsTableModel.COLUMN_TITLE));

		assertEquals("Date Saved", dateSaved1, modelWithData.getValueAt(1,DeleteMyServerDraftsTableModel.COLUMN_LAST_DATE_SAVED));
		modelWithData.setValueAt("today", 1,DeleteMyServerDraftsTableModel.COLUMN_LAST_DATE_SAVED);
		assertEquals("keep title", dateSaved1, modelWithData.getValueAt(1,DeleteMyServerDraftsTableModel.COLUMN_LAST_DATE_SAVED));
	}
	
	public void testSetAllFlags()
	{
		Boolean t = new Boolean(true);
		Boolean f = new Boolean(false);
		
		modelWithData.setAllFlags(true);
		for(int allTrueCounter = 0; allTrueCounter < modelWithData.getRowCount(); ++allTrueCounter)
			assertEquals("all true" + allTrueCounter, t, modelWithData.getValueAt(0,DeleteMyServerDraftsTableModel.COLUMN_DELETE_FLAG));

		modelWithData.setAllFlags(false);
		for(int allFalseCounter = 0; allFalseCounter < modelWithData.getRowCount(); ++allFalseCounter)
			assertEquals("all false" + allFalseCounter, f, modelWithData.getValueAt(0,DeleteMyServerDraftsTableModel.COLUMN_DELETE_FLAG));
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
				list.add(b0.getLocalId() + MartusConstants.regexEqualsDelimeter + 
						b0.getFieldDataPacket().getLocalId() +
						MartusConstants.regexEqualsDelimeter +
						"3000");
				
				list.add(b1.getLocalId() + MartusConstants.regexEqualsDelimeter + 
						b1.getFieldDataPacket().getLocalId() +
						MartusConstants.regexEqualsDelimeter + 
						"3100" + 
						MartusConstants.regexEqualsDelimeter + 
						"1083873923190");
				
				list.add(b2.getLocalId() + MartusConstants.regexEqualsDelimeter + 
						b2.getFieldDataPacket().getLocalId() + 
						MartusConstants.regexEqualsDelimeter + 
						"3200");
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
	final static String dateSaved1="05/06/2004 1:05 PM";

	MockUiLocalization localization;
	MockMartusApp app;
	MockServer testServer;
	DeleteMyServerDraftsTableModel modelWithData;
	DeleteMyServerDraftsTableModel modelWithoutData;
	
	Bulletin b0;
	Bulletin b1;
	Bulletin b2;
}
