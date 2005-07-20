/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2003-2005, Beneficent
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

import java.io.File;
import java.io.StringWriter;
import java.util.Vector;

import org.martus.client.bulletinstore.ClientBulletinStore;
import org.martus.client.swingui.tablemodels.RetrieveHQDraftsTableModel;
import org.martus.client.test.MockMartusApp;
import org.martus.clientside.test.MockUiLocalization;
import org.martus.common.BulletinSummary;
import org.martus.common.HQKey;
import org.martus.common.HQKeys;
import org.martus.common.MartusUtilities;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletin.BulletinZipUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MockMartusSecurity;
import org.martus.common.database.DatabaseKey;
import org.martus.common.database.ReadableDatabase;
import org.martus.common.network.NetworkInterface;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.packet.BulletinHistory;
import org.martus.common.packet.FieldDataPacket;
import org.martus.common.packet.UniversalId;
import org.martus.server.forclients.MockMartusServer;
import org.martus.server.forclients.ServerForClients;
import org.martus.server.forclients.ServerSideNetworkHandler;
import org.martus.util.TestCaseEnhanced;

public class TestRetrieveHQDraftsTableModel extends TestCaseEnhanced
{
	public TestRetrieveHQDraftsTableModel(String name) 
	{
		super(name);
	}
	
	public void setUp() throws Exception
	{
		super.setUp();
		if(localization!=null)
			return;
		MartusCrypto hqSecurity = MockMartusSecurity.createHQ();
		localization = new MockUiLocalization();
		hqApp = MockMartusApp.create(hqSecurity);

		MartusCrypto fieldSecurity1 = MockMartusSecurity.createClient();
		fieldApp1 = MockMartusApp.create(fieldSecurity1);
		final ClientBulletinStore store1 = fieldApp1.getStore();
		ReadableDatabase db1 = store1.getDatabase();

		MartusCrypto fieldSecurity2 = MockMartusSecurity.createOtherClient();
		fieldApp2 = MockMartusApp.create(fieldSecurity2);
		final ClientBulletinStore store2 = fieldApp2.getStore();
		ReadableDatabase db2 = store2.getDatabase();

		assertNotEquals("account Id's equal?", fieldApp1.getAccountId(), fieldApp2.getAccountId());

		b0 = fieldApp1.createBulletin();
		b0.set(Bulletin.TAGTITLE, title0);
		b0.set(Bulletin.TAGAUTHOR, author0);
		b0.setAllPrivate(true);
		HQKeys hqKey = new HQKeys();
		HQKey key = new HQKey(hqApp.getAccountId());
		hqKey.add(key);
		b0.setAuthorizedToReadKeys(hqKey);
		store1.saveBulletin(b0);
		b0Size = MartusUtilities.getBulletinSize(store1.getDatabase(), b0.getBulletinHeaderPacket());

		b1 = fieldApp1.createBulletin();
		b1.set(Bulletin.TAGTITLE, title1);
		b1.set(Bulletin.TAGAUTHOR, author1);
		b1.setAllPrivate(false);
		b1.setAuthorizedToReadKeys(hqKey);
		b1.setSealed();
		store1.saveBulletin(b1);
		b1Size = MartusUtilities.getBulletinSize(store1.getDatabase(), b1.getBulletinHeaderPacket());

		b2 = fieldApp2.createBulletin();
		b2.set(Bulletin.TAGTITLE, title2);
		b2.set(Bulletin.TAGAUTHOR, author2);
		b2.setAllPrivate(true);
		b2.setAuthorizedToReadKeys(hqKey);
		BulletinHistory history2 = new BulletinHistory();
		history2.add(b1.getLocalId());
		historyId = UniversalId.createFromAccountAndLocalId(b2.getAccount(), b1.getLocalId());
		b2.setHistory(history2);
		b2.setDraft();
		store2.saveBulletin(b2);
		b2Size = 2300;
		testServer = new MockServer();
		testServer.verifyAndLoadConfigurationFiles();
		testSSLServerInterface = new ServerSideNetworkHandler(testServer.serverForClients);
		hqApp.setSSLNetworkInterfaceHandlerForTesting(testSSLServerInterface);
		modelWithData = new RetrieveHQDraftsTableModel(hqApp,localization);
		modelWithData.initialize(null);
		
		importBulletinFromFieldOfficeToHq(db1, b0, fieldSecurity1);
		importBulletinFromFieldOfficeToHq(db1, b1, fieldSecurity1);
		importBulletinFromFieldOfficeToHq(db2, b2, fieldSecurity2);
		
		modelWithoutData = new RetrieveHQDraftsTableModel(hqApp, localization);
		modelWithoutData.initialize(null);
	}
	
	void importBulletinFromFieldOfficeToHq(ReadableDatabase db, Bulletin b, MartusCrypto sigVerifier) throws Exception
	{
		File tempFile = createTempFile();
		DatabaseKey headerKey = DatabaseKey.createKey(b.getUniversalId(), b.getStatus());
		BulletinZipUtilities.exportBulletinPacketsFromDatabaseToZipFile(db, headerKey, tempFile, sigVerifier);
		hqApp.getStore().importZipFileToStoreWithSameUids(tempFile);
	}
	
	public void tearDown() throws Exception
	{
		testServer.deleteAllFiles();
    	fieldApp1.deleteAllFiles();
    	fieldApp2.deleteAllFiles();
    	hqApp.deleteAllFiles();
    	super.tearDown();
    }

	public void testGetColumnName()
	{
		assertEquals(localization.getFieldLabel("retrieveflag"), modelWithData.getColumnName(0));
		assertEquals(localization.getFieldLabel(Bulletin.TAGTITLE), modelWithData.getColumnName(1));
		assertEquals(localization.getFieldLabel(Bulletin.TAGAUTHOR), modelWithData.getColumnName(2));
		assertEquals(localization.getFieldLabel(Bulletin.TAGLASTSAVED), modelWithData.getColumnName(3));
		assertEquals(localization.getFieldLabel("BulletinVersionNumber"), modelWithData.getColumnName(4));
		assertEquals(localization.getFieldLabel("BulletinSize"), modelWithData.getColumnName(5));
	}
	
	public void testGetColumnCount()
	{
		assertEquals(6, modelWithoutData.getColumnCount());
		assertEquals(6, modelWithData.getColumnCount());
	}
	
	public void testGetRowCount()
	{
		assertEquals(0, modelWithoutData.getRowCount());
		assertEquals(2, modelWithData.getRowCount());
	}
	
	public void testIsCellEditable()
	{
		assertEquals("flag", true, modelWithData.isCellEditable(1,modelWithData.COLUMN_RETRIEVE_FLAG));
		assertEquals("title", false, modelWithData.isCellEditable(1,modelWithData.COLUMN_TITLE));
		assertEquals("author", false, modelWithData.isCellEditable(1,modelWithData.COLUMN_AUTHOR));
		assertEquals("size", false, modelWithData.isCellEditable(1,modelWithData.COLUMN_LAST_DATE_SAVED));
		assertEquals("date", false, modelWithData.isCellEditable(1,modelWithData.COLUMN_BULLETIN_SIZE));
	}
	
	public void testGetColumnClass()
	{
		assertEquals(Boolean.class, modelWithData.getColumnClass(modelWithData.COLUMN_RETRIEVE_FLAG));
		assertEquals(String.class, modelWithData.getColumnClass(modelWithData.COLUMN_TITLE));
		assertEquals(String.class, modelWithData.getColumnClass(modelWithData.COLUMN_AUTHOR));
		assertEquals(String.class, modelWithData.getColumnClass(modelWithData.COLUMN_LAST_DATE_SAVED));
		assertEquals(Integer.class, modelWithData.getColumnClass(modelWithData.COLUMN_BULLETIN_SIZE));
	}
	
	public void testGetAndSetValueAt()
	{
		Vector authors = new Vector();
		authors.add(modelWithData.getValueAt(0,2));
		authors.add(modelWithData.getValueAt(1,2));
		assertContains("Author 0 missing?", b0.get(Bulletin.TAGAUTHOR), authors);
		assertContains("Author 2 missing?", b2.get(Bulletin.TAGAUTHOR), authors);
		
		assertEquals("start bool", false, ((Boolean)modelWithData.getValueAt(0,modelWithData.COLUMN_RETRIEVE_FLAG)).booleanValue());
		modelWithData.setValueAt(new Boolean(true), 0,0);
		assertEquals("setget bool", true, ((Boolean)modelWithData.getValueAt(0,modelWithData.COLUMN_RETRIEVE_FLAG)).booleanValue());

		assertEquals("start title", title2, modelWithData.getValueAt(1,modelWithData.COLUMN_TITLE));
		modelWithData.setValueAt(title2+title2, 1,1);
		assertEquals("keep title", title2, modelWithData.getValueAt(1,modelWithData.COLUMN_TITLE));

		assertTrue("B0 Size too small", ((Integer)(modelWithData.getValueAt(0,modelWithData.COLUMN_BULLETIN_SIZE))).intValue() > 1);
		assertTrue("B2 Size too small", ((Integer)(modelWithData.getValueAt(0,modelWithData.COLUMN_BULLETIN_SIZE))).intValue() > 1);

		assertEquals("start date1", "", modelWithData.getValueAt(0,modelWithData.COLUMN_LAST_DATE_SAVED));
		modelWithData.setValueAt("some date1", 0,modelWithData.COLUMN_LAST_DATE_SAVED);
		assertEquals("keep date1", "", modelWithData.getValueAt(0,modelWithData.COLUMN_LAST_DATE_SAVED));

		String expectedDateSaved = localization.formatDateTime(BulletinSummary.getLastDateTimeSaved(dateSavedInMillis2));
		assertEquals("start date2", expectedDateSaved, modelWithData.getValueAt(1,modelWithData.COLUMN_LAST_DATE_SAVED));
		modelWithData.setValueAt("some date2", 1,modelWithData.COLUMN_LAST_DATE_SAVED);
		assertEquals("keep date2", expectedDateSaved, modelWithData.getValueAt(1,modelWithData.COLUMN_LAST_DATE_SAVED));
	}
	
	public void testSetAllFlags()
	{
		Boolean t = new Boolean(true);
		Boolean f = new Boolean(false);
		
		modelWithData.setAllFlags(true);
		for(int allTrueCounter = 0; allTrueCounter < modelWithData.getRowCount(); ++allTrueCounter)
			assertEquals("all true" + allTrueCounter, t, modelWithData.getValueAt(0,modelWithData.COLUMN_RETRIEVE_FLAG));

		modelWithData.setAllFlags(false);
		for(int allFalseCounter = 0; allFalseCounter < modelWithData.getRowCount(); ++allFalseCounter)
			assertEquals("all false" + allFalseCounter, f, modelWithData.getValueAt(0,modelWithData.COLUMN_RETRIEVE_FLAG));
	}
	
	public void testGetIdList()
	{
		modelWithData.setAllFlags(false);
		Vector emptyList = modelWithData.getSelectedUidsLatestVersion();
		assertEquals(0, emptyList.size());
		emptyList = modelWithData.getSelectedUidsFullHistory();
		assertEquals(0, emptyList.size());
		
		modelWithData.setAllFlags(true);

		Vector fullList = modelWithData.getSelectedUidsLatestVersion();
		assertEquals(2, fullList.size());
		assertNotEquals("hq account ID0?", hqApp.getAccountId(), ((UniversalId)fullList.get(0)).getAccountId());
		assertNotEquals("hq account ID2?", hqApp.getAccountId(), ((UniversalId)fullList.get(1)).getAccountId());

		assertContains("b0 Uid not in list?", b0.getUniversalId(), fullList);
		assertContains("b2 Uid not in list?", b2.getUniversalId(), fullList);
		Vector fullVersionList = modelWithData.getSelectedUidsFullHistory();
		assertEquals(3, fullVersionList.size());
		assertContains("History Uid not in list?", historyId, fullVersionList);
		
		
		modelWithData.setValueAt(new Boolean(false), 0, 0);
		String summary = (String)modelWithData.getValueAt(1,modelWithData.COLUMN_TITLE);
		assertEquals("Not correct summary?", title2, summary);
		Vector twoList = modelWithData.getSelectedUidsLatestVersion();
		assertEquals(1, twoList.size());
		assertEquals("b2 id not in LatestVersion", fullList.get(1), twoList.get(0));

		Vector fullHistoryList = modelWithData.getSelectedUidsFullHistory();
		assertEquals(2, fullHistoryList.size());
		assertEquals("History id not in FullHistory", historyId, fullHistoryList.get(1));
	}

	class MockServer extends MockMartusServer
	{
		MockServer() throws Exception
		{
			super();
			setSecurity(MockMartusSecurity.createServer());
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
			
			public Vector listFieldOfficeDraftBulletinIds(String hqAccountId, String authorAccountId, Vector retrieveTags) 
			{			
				Vector result = new Vector();
				result.add(NetworkInterfaceConstants.OK);
				Vector list = new Vector();
				if(authorAccountId.equals(b0.getAccount()))
					list.add(b0.getLocalId() + "=" + b0.getFieldDataPacket().getLocalId() + "=" + b0Size);
				if(authorAccountId.equals(b2.getAccount()))
					list.add(b2.getLocalId() + BulletinSummary.fieldDelimeter + 
						b2.getFieldDataPacket().getLocalId() +
						BulletinSummary.fieldDelimeter + 
						b2Size + 
						BulletinSummary.fieldDelimeter + 
						dateSavedInMillis2 +
						BulletinSummary.fieldDelimeter +
						b2.getHistory().get(0));

				result.add(list);
				return result;
			}

		}
		
		public Vector listFieldOfficeAccounts(String hqAccountId) 
		{
			Vector v = new Vector();
			v.add(NetworkInterfaceConstants.OK);
			v.add(fieldApp1.getAccountId());
			v.add(fieldApp2.getAccountId());
			return v;			
		}

		public Vector getPacket(String hqAccountId, String authorAccountId, String bulletinLocalId, String packetLocalId)
		{
			Vector result = new Vector();
			try 
			{
				UniversalId uid = UniversalId.createFromAccountAndLocalId(authorAccountId, packetLocalId);
				FieldDataPacket fdp = null;
				MartusCrypto security = fieldApp1.getSecurity();
				if(uid.equals(b0.getFieldDataPacket().getUniversalId()))
					fdp = b0.getFieldDataPacket();
				if(uid.equals(b1.getFieldDataPacket().getUniversalId()))
					fdp = b1.getFieldDataPacket();
				if(uid.equals(b2.getFieldDataPacket().getUniversalId()))
				{
					fdp = b2.getFieldDataPacket();
					security = fieldApp2.getSecurity();
				}

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
	}
	
	final static String title0 = "cool title";
	final static String title1 = "This is a cool title";
	final static String title2 = "Even cooler";
	final static String dateSavedInMillis2 = "1083873923190";

	final static String author0 = "Fred 0";
	final static String author1 = "Betty 1";
	final static String author2 = "Donna 2";

	static int b0Size;
	static int b1Size;
	static int b2Size;

	static MockMartusServer testServer;
	static NetworkInterface testSSLServerInterface;
	static MockMartusApp fieldApp1;
	static MockMartusApp fieldApp2;
	static MockMartusApp hqApp;
	static MockUiLocalization localization;
	
	static Bulletin b0;
	static Bulletin b1;
	static Bulletin b2;
	static UniversalId historyId;
	static RetrieveHQDraftsTableModel modelWithData;
	static RetrieveHQDraftsTableModel modelWithoutData;
}
