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

import java.util.Vector;

import org.martus.client.core.BackgroundUploader;
import org.martus.client.core.BulletinStore;
import org.martus.client.core.BulletinSummary;
import org.martus.client.swingui.tablemodels.RetrieveHQDraftsTableModel;
import org.martus.client.swingui.tablemodels.RetrieveHQTableModel;
import org.martus.client.swingui.tablemodels.RetrieveMyDraftsTableModel;
import org.martus.client.swingui.tablemodels.RetrieveMyTableModel;
import org.martus.client.test.MockMartusApp;
import org.martus.client.test.NullProgressMeter;
import org.martus.common.MartusUtilities;
import org.martus.common.ProgressMeterInterface;
import org.martus.common.MartusUtilities.ServerErrorException;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.clientside.UiBasicLocalization;
import org.martus.common.clientside.test.MockUiLocalization;
import org.martus.common.crypto.MockMartusSecurity;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.server.forclients.MockMartusServer;
import org.martus.server.forclients.ServerSideNetworkHandler;
import org.martus.util.TestCaseEnhanced;

public class TestRetrieveTableModel extends TestCaseEnhanced
{

	public TestRetrieveTableModel(String name) 
	{
		super(name);
	}
	
	public void setUp() throws Exception
	{
		super.setUp();
		if(localization == null)
			localization = new MockUiLocalization();
	 
		if(mockSecurityForApp == null)
			mockSecurityForApp = MockMartusSecurity.createClient();
		
		if(mockSecurityForServer == null)
			mockSecurityForServer = MockMartusSecurity.createServer();

		mockServer = new MockMartusServer();
		mockServer.loadBannedClients();
		mockServer.verifyAndLoadConfigurationFiles();
		mockServer.setSecurity(mockSecurityForServer);
		mockSSLServerHandler = new MockServerInterfaceHandler(mockServer);

		appWithoutServer = MockMartusApp.create(mockSecurityForApp);
		MockServerNotAvailable mockServerNotAvailable = new MockServerNotAvailable();
		appWithoutServer.setSSLNetworkInterfaceHandlerForTesting(new ServerSideNetworkHandler(mockServerNotAvailable));

		appWithServer = MockMartusApp.create(mockSecurityForApp);
		appWithServer.setServerInfo("mock", mockServer.getAccountId(), "");
		appWithServer.setSSLNetworkInterfaceHandlerForTesting(mockSSLServerHandler);
		
		appWithAccount = MockMartusApp.create(mockSecurityForApp);
		appWithAccount.setServerInfo("mock", mockServer.getAccountId(), "");
		appWithAccount.setSSLNetworkInterfaceHandlerForTesting(mockSSLServerHandler);
		
		ProgressMeterInterface nullProgressMeter = new NullProgressMeter();
		uploader = new BackgroundUploader(appWithAccount, nullProgressMeter);

		mockServer.deleteAllData();
		
		mockServerNotAvailable.deleteAllFiles();
	}

	public void tearDown() throws Exception
	{
		appWithoutServer.deleteAllFiles();
		appWithServer.deleteAllFiles();
		appWithAccount.deleteAllFiles();
		mockServer.deleteAllFiles();
		super.tearDown();
	}
	
	public void testGetMyBulletinSummariesWithServerError() throws Exception
	{
		String sampleSummary1 = "this is a basic summary";
		String sampleSummary2 = "another silly summary";
		String sampleSummary3 = "yet another!";

		Bulletin b1 = appWithAccount.createBulletin();
		b1.setAllPrivate(true);
		b1.set(Bulletin.TAGTITLE, sampleSummary1);
		b1.setSealed();
		appWithAccount.getStore().saveBulletin(b1);

		Bulletin b2 = appWithAccount.createBulletin();
		b2.setAllPrivate(false);
		b2.set(Bulletin.TAGTITLE, sampleSummary2);
		b2.setSealed();
		appWithAccount.getStore().saveBulletin(b2);

		Bulletin b3 = appWithAccount.createBulletin();
		b3.setAllPrivate(true);
		b3.set(Bulletin.TAGTITLE, sampleSummary3);
		b3.setSealed();
		appWithAccount.getStore().saveBulletin(b3);

		mockServer.allowUploads(appWithAccount.getAccountId());
		assertEquals("failed upload1?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b1));
		assertEquals("failed upload2?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b2));
		assertEquals("failed upload3?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b3));

		BulletinStore store = appWithAccount.getStore();
		store.destroyBulletin(b1);
		store.destroyBulletin(b2);
		store.destroyBulletin(b3);

		mockServer.countDownToGetPacketFailure = 2;

		RetrieveMyTableModel model = new RetrieveMyTableModel(appWithAccount, localization);
		try
		{
			model.initialize(null);
			model.checkIfErrorOccurred();
			fail("Didn't throw");
		}
		catch (ServerErrorException expectedExceptionToIgnore)
		{
		}
		Vector result = model.getDownloadableSummaries();
		assertEquals("wrong count?", 2, result.size());

		BulletinSummary s1 = (BulletinSummary)result.get(0);
		BulletinSummary s2 = (BulletinSummary)result.get(1);
		assertNotEquals(s1.getLocalId(), s2.getLocalId());
	}

	public void testGetMyBulletinSummariesNoServer() throws Exception
	{
		try
		{
			RetrieveMyTableModel model = new RetrieveMyTableModel(appWithoutServer, localization);
			model.initialize(null);
			model.getMySummaries();
			model.checkIfErrorOccurred();
			model.getDownloadableSummaries();
			fail("Got valid summaries?");
		}
		catch(MartusUtilities.ServerErrorException ignoreExpectedException)
		{
		}

		try
		{
			RetrieveMyDraftsTableModel model = new RetrieveMyDraftsTableModel(appWithoutServer, localization);
			model.initialize(null);
			model.getMyDraftSummaries();
			model.checkIfErrorOccurred();
			model.getDownloadableSummaries();
			fail("Got valid draft summaries?");
		}
		catch(MartusUtilities.ServerErrorException ignoreExpectedException)
		{
		}
	}
	
	public void testGetMyBulletinSummariesErrors() throws Exception
	{
		assertTrue("must be able to ping", appWithServer.isSSLServerAvailable());

		Vector desiredResult = new Vector();

		desiredResult.add(NetworkInterfaceConstants.REJECTED);
		mockServer.listMyResponse = desiredResult;
		try
		{
			RetrieveMyTableModel model = new RetrieveMyTableModel(appWithServer, localization);
			model.initialize(null);
			model.checkIfErrorOccurred();
			model.getDownloadableSummaries();
			fail("rejected didn't throw?");
		}
		catch(MartusUtilities.ServerErrorException ignoreExpectedException)
		{
		}
		mockServer.listMyResponse = null;
	}

	public void testGetMySummaries() throws Exception
	{
		String sampleSummary1 = "this is a basic summary";
		String sampleSummary2 = "another silly summary";
		String sampleSummary3 = "yet another!";
		
		Bulletin b1 = appWithAccount.createBulletin();
		b1.setAllPrivate(true);
		b1.set(Bulletin.TAGTITLE, sampleSummary1);
		b1.setSealed();
		appWithAccount.getStore().saveBulletin(b1);
		
		Bulletin b2 = appWithAccount.createBulletin();
		b2.setAllPrivate(false);
		b2.set(Bulletin.TAGTITLE, sampleSummary2);
		b2.setSealed();
		appWithAccount.getStore().saveBulletin(b2);
		
		Bulletin b3 = appWithAccount.createBulletin();
		b3.setAllPrivate(true);
		b3.set(Bulletin.TAGTITLE, sampleSummary3);
		b3.setDraft();
		appWithAccount.getStore().saveBulletin(b3);
		
		mockServer.allowUploads(appWithAccount.getAccountId());
		assertEquals("failed upload1?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b1));
		assertEquals("failed upload2?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b2));
		assertEquals("failed upload3?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b3));
		
		BulletinStore store = appWithAccount.getStore();
		store.destroyBulletin(b1);
		store.destroyBulletin(b2);
		store.destroyBulletin(b3);

		RetrieveMyTableModel model = new RetrieveMyTableModel(appWithAccount, localization);
		model.initialize(null);
		model.checkIfErrorOccurred();
		Vector result = model.getDownloadableSummaries();
		assertEquals("wrong count?", 2, result.size());
		
		BulletinSummary s1 = (BulletinSummary)result.get(0);
		BulletinSummary s2 = (BulletinSummary)result.get(1);

		Bulletin bulletins[] = new Bulletin[] {b1, b2};
		BulletinSummary summaries[] = new BulletinSummary[] {s1, s2};
		boolean found[] = new boolean[bulletins.length];
		
		for(int i = 0; i < bulletins.length; ++i)
		{
			for(int j = 0; j < summaries.length; ++j)
			{
				Bulletin b = bulletins[i];
				BulletinSummary s = summaries[j];
				if(b.getLocalId().equals(s.getLocalId()))
				{
					assertEquals(b.get(Bulletin.TAGTITLE), s.getTitle());
					found[i] = true;
				}
			}
		}
		
		assertTrue("Missing 1?", found[0]);
		assertTrue("Missing 2?", found[1]);
	}

	public void testGetAllMySummaries() throws Exception
	{
		String sampleSummary1 = "1 basic summary";
		String sampleSummary2 = "2 silly summary";
		String sampleSummary3 = "3 yet another!";
		
		appWithAccount.getStore().deleteAllData();
		
		Bulletin b1 = appWithAccount.createBulletin();
		b1.setAllPrivate(true);
		b1.set(Bulletin.TAGTITLE, sampleSummary1);
		b1.setSealed();
		appWithAccount.getStore().saveBulletin(b1);
		
		Bulletin b2 = appWithAccount.createBulletin();
		b2.setAllPrivate(false);
		b2.set(Bulletin.TAGTITLE, sampleSummary2);
		b2.setSealed();
		appWithAccount.getStore().saveBulletin(b2);
				
		Bulletin b3 = appWithAccount.createBulletin();
		b3.setAllPrivate(true);
		b3.set(Bulletin.TAGTITLE, sampleSummary3);
		b3.setSealed();
		appWithAccount.getStore().saveBulletin(b3);
		
		mockServer.allowUploads(appWithAccount.getAccountId());
		assertEquals("failed upload1?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b1));
		assertEquals("failed upload2?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b2));
		assertEquals("failed upload3?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b3));

		appWithAccount.getStore().destroyBulletin(b1);
		appWithAccount.getStore().destroyBulletin(b2);

		RetrieveMyTableModel model = new RetrieveMyTableModel(appWithAccount, localization);
		model.initialize(null);
		model.checkIfErrorOccurred();
		Vector allResult = model.getAllSummaries();
		assertEquals("wrong all summaries count?", 3, allResult.size());

		BulletinSummary allS1 = (BulletinSummary)allResult.get(0);
		BulletinSummary allS2 = (BulletinSummary)allResult.get(1);
		BulletinSummary allS3 = (BulletinSummary)allResult.get(2);
		Bulletin allBulletins[] = new Bulletin[] {b1, b2, b3};
		BulletinSummary allSummaries[] = new BulletinSummary[] {allS1, allS2, allS3};
		
		for(int i = 0; i < allBulletins.length; ++i)
		{
			for(int j = 0; j < allSummaries.length; ++j)
			{
				Bulletin b = allBulletins[i];
				BulletinSummary s = allSummaries[j];
				if(b.getLocalId().equals(s.getLocalId()))
				{
					if(b.equals(b1))
						assertTrue("B1 not downloadable?", s.isDownloadable());
					if(b.equals(b2))
						assertTrue("B2 not downloadable?", s.isDownloadable());
					if(b.equals(b3))
						assertFalse("B3 downloadable?", s.isDownloadable());
				}
			}
		}
		model.checkIfErrorOccurred();
		Vector result = model.getDownloadableSummaries();
		assertEquals("wrong count?", 2, result.size());
		
		BulletinSummary s1 = (BulletinSummary)result.get(0);
		BulletinSummary s2 = (BulletinSummary)result.get(1);

		Bulletin bulletins[] = new Bulletin[] {b1, b2};
		BulletinSummary summaries[] = new BulletinSummary[] {s1, s2};
		boolean found[] = new boolean[bulletins.length];
		
		for(int i = 0; i < bulletins.length; ++i)
		{
			for(int j = 0; j < summaries.length; ++j)
			{
				Bulletin b = bulletins[i];
				BulletinSummary s = summaries[j];
				if(b.getLocalId().equals(s.getLocalId()))
				{
					assertEquals(b.get(Bulletin.TAGTITLE), s.getTitle());
					found[i] = true;
				}
				assertTrue("Not downloadable?", s.isDownloadable());
			}
		}
		assertTrue("Missing 1?", found[0]);
		assertTrue("Missing 2?", found[1]);
	}


	public void testGetMyDraftBulletinSummariesErrors() throws Exception
	{
		assertTrue("must be able to ping", appWithServer.isSSLServerAvailable());

		Vector desiredResult = new Vector();

		desiredResult.add(NetworkInterfaceConstants.REJECTED);
		mockServer.listMyResponse = desiredResult;
		try
		{
			RetrieveMyDraftsTableModel model = new RetrieveMyDraftsTableModel(appWithServer, localization);
			model.initialize(null);
			model.checkIfErrorOccurred();
			model.getDownloadableSummaries();
			fail("rejected didn't throw?");
		}
		catch(MartusUtilities.ServerErrorException ignoreExpectedException)
		{
		}
		mockServer.listMyResponse = null;
	}

	public void testGetMyDraftSummaries() throws Exception
	{
		String sampleSummary1 = "this is a basic summary";
		String sampleSummary2 = "another silly summary";
		String sampleSummary3 = "yet another!";
		
		Bulletin b1 = appWithAccount.createBulletin();
		b1.setAllPrivate(true);
		b1.set(Bulletin.TAGTITLE, sampleSummary1);
		b1.setDraft();
		appWithAccount.getStore().saveBulletin(b1);
		
		Bulletin b2 = appWithAccount.createBulletin();
		b2.setAllPrivate(false);
		b2.set(Bulletin.TAGTITLE, sampleSummary2);
		b2.setDraft();
		appWithAccount.getStore().saveBulletin(b2);
		
		Bulletin b3 = appWithAccount.createBulletin();
		b3.setAllPrivate(true);
		b3.set(Bulletin.TAGTITLE, sampleSummary3);
		b3.setSealed();
		appWithAccount.getStore().saveBulletin(b3);
		
		mockServer.allowUploads(appWithAccount.getAccountId());
		assertEquals("failed upload1?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b1));
		assertEquals("failed upload2?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b2));
		assertEquals("failed upload3?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b3));

		BulletinStore store = appWithAccount.getStore();
		store.destroyBulletin(b1);
		store.destroyBulletin(b2);
		store.destroyBulletin(b3);

		RetrieveMyDraftsTableModel model = new RetrieveMyDraftsTableModel(appWithAccount, localization);
		model.initialize(null);
		model.checkIfErrorOccurred();
		Vector result = model.getDownloadableSummaries();
		assertEquals("wrong count?", 2, result.size());
		
		BulletinSummary s1 = (BulletinSummary)result.get(0);
		BulletinSummary s2 = (BulletinSummary)result.get(1);

		Bulletin bulletins[] = new Bulletin[] {b1, b2};
		BulletinSummary summaries[] = new BulletinSummary[] {s1, s2};
		boolean found[] = new boolean[bulletins.length];
		
		for(int i = 0; i < bulletins.length; ++i)
		{
			for(int j = 0; j < summaries.length; ++j)
			{
				Bulletin b = bulletins[i];
				BulletinSummary s = summaries[j];
				if(b.getLocalId().equals(s.getLocalId()))
				{
					assertEquals(b.get(Bulletin.TAGTITLE), s.getTitle());
					found[i] = true;
				}
			}
		}
		
		assertTrue("Missing 1?", found[0]);
		assertTrue("Missing 2?", found[1]);
	}
	
	public void testGetFieldOfficeBulletinSummariesNoServer() throws Exception
	{
		try
		{
			RetrieveHQTableModel model = new RetrieveHQTableModel(appWithoutServer, localization);
			model.initialize(null);
			model.getFieldOfficeSealedSummaries("");
			model.checkIfErrorOccurred();
			model.getDownloadableSummaries();
			fail("Got valid sealed summaries?");
		}
		catch(MartusUtilities.ServerErrorException ignoreExpectedException)
		{
		}
		
		try
		{
			RetrieveHQDraftsTableModel model = new RetrieveHQDraftsTableModel(appWithoutServer, localization);
			model.initialize(null);
			model.getFieldOfficeDraftSummaries("");
			model.checkIfErrorOccurred();
			model.getDownloadableSummaries();
			fail("Got valid draft summaries?");
		}
		catch(MartusUtilities.ServerErrorException ignoreExpectedException)
		{
		}
	}

	public void testGetFieldOfficeBulletinSummariesErrors() throws Exception
	{
		assertTrue("must be able to ping", appWithServer.isSSLServerAvailable());

		Vector desiredResult = new Vector();

		desiredResult.add(NetworkInterfaceConstants.REJECTED);
		mockServer.listFieldOfficeSummariesResponse = desiredResult;
		try
		{
			RetrieveHQTableModel model = new RetrieveHQTableModel(appWithServer, localization);
			model.initialize(null);
			model.getFieldOfficeSealedSummaries("");
			model.checkIfErrorOccurred();
			model.getDownloadableSummaries();
			fail("rejected sealed didn't throw?");
		}
		catch(MartusUtilities.ServerErrorException ignoreExpectedException)
		{
		}

		try
		{
			RetrieveHQDraftsTableModel model = new RetrieveHQDraftsTableModel(appWithServer, localization);
			model.initialize(null);
			model.getFieldOfficeDraftSummaries("");
			model.checkIfErrorOccurred();
			model.getDownloadableSummaries();
			fail("rejected draft didn't throw?");
		}
		catch(MartusUtilities.ServerErrorException ignoreExpectedException)
		{
		}

		mockServer.listFieldOfficeSummariesResponse = null;
	}

	public void testGetFieldOfficeSummaries() throws Exception
	{
		MockMartusSecurity hqSecurity = MockMartusSecurity.createHQ();	
		MockMartusApp hqApp = MockMartusApp.create(hqSecurity);
		hqApp.setServerInfo("mock", mockServer.getAccountId(), "");
		hqApp.setSSLNetworkInterfaceHandlerForTesting(mockSSLServerHandler);
		assertNotEquals("same public key?", appWithAccount.getAccountId(), hqApp.getAccountId());
		Vector keys = new Vector();
		keys.add(hqApp.getAccountId());
		appWithAccount.setAndSaveHQKeys(keys);

		String sampleSummary1 = "this is a basic summary";
		String sampleSummary2 = "another silly summary";
		String sampleSummary3 = "Draft summary";
		
		Bulletin b1 = appWithAccount.createBulletin();
		b1.setAllPrivate(true);
		b1.set(Bulletin.TAGTITLE, sampleSummary1);
		b1.setSealed();
		appWithAccount.setHQKeyInBulletin(b1);
		appWithAccount.getStore().saveBulletin(b1);
		
		Bulletin b2 = appWithAccount.createBulletin();
		b2.setAllPrivate(false);
		b2.set(Bulletin.TAGTITLE, sampleSummary2);
		b2.setSealed();
		appWithAccount.setHQKeyInBulletin(b2);
		appWithAccount.getStore().saveBulletin(b2);
		
		Bulletin b3 = appWithAccount.createBulletin();
		b3.setAllPrivate(false);
		b3.set(Bulletin.TAGTITLE, sampleSummary3);
		b3.setDraft();
		appWithAccount.setHQKeyInBulletin(b3);
		appWithAccount.getStore().saveBulletin(b3);

		mockServer.allowUploads(appWithAccount.getAccountId());
		assertEquals("failed upload1?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b1));
		assertEquals("failed upload2?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b2));
		assertEquals("failed upload3?", NetworkInterfaceConstants.OK, uploader.uploadBulletin(b3));

		Vector desiredSealedResult = new Vector();
		desiredSealedResult.add(NetworkInterfaceConstants.OK);
		Vector list = new Vector();
		list.add(b1.getLocalId() + "=" + b1.getFieldDataPacket().getLocalId()+"=2000");
		list.add(b2.getLocalId() + "=" + b2.getFieldDataPacket().getLocalId()+"=2000");
		desiredSealedResult.add(list);
		mockServer.listFieldOfficeSummariesResponse = desiredSealedResult;	

		RetrieveHQTableModel model = new RetrieveHQTableModel(hqApp, localization);
		model.initialize(null);
		model.checkIfErrorOccurred();
		Vector returnedSealedResults = model.getDownloadableSummaries();
		assertEquals("Wrong size?", 2, returnedSealedResults.size());
		BulletinSummary s1 = (BulletinSummary)returnedSealedResults.get(0);
		BulletinSummary s2 = (BulletinSummary)returnedSealedResults.get(1);
		boolean found1 = false;
		boolean found2 = false;
		found1 = s1.getLocalId().equals(b1.getLocalId());
		if(!found1)
			found1 = s2.getLocalId().equals(b1.getLocalId());
		found2 = s1.getLocalId().equals(b2.getLocalId());
		if(!found2)
			found2 = s2.getLocalId().equals(b2.getLocalId());
		assertTrue("not found S1?", found1);
		assertTrue("not found S2?", found2);

		Vector desiredDraftResult = new Vector();
		desiredDraftResult.add(NetworkInterfaceConstants.OK);
		Vector list2 = new Vector();
		list2.add(b3.getLocalId() + "=" + b3.getFieldDataPacket().getLocalId()+"=3400");
		desiredDraftResult.add(list2);
		mockServer.listFieldOfficeSummariesResponse = desiredDraftResult;	

		RetrieveHQDraftsTableModel model2 = new RetrieveHQDraftsTableModel(hqApp, localization);
		model2.initialize(null);
		model2.checkIfErrorOccurred();
		Vector returnedDraftResults = model2.getDownloadableSummaries();
		assertEquals("Wrong draft size?", 1, returnedDraftResults.size());
		BulletinSummary s3 = (BulletinSummary)returnedDraftResults.get(0);
		boolean found3 = false;
		found3 = s3.getLocalId().equals(b3.getLocalId());
		assertTrue("not found S3?", found3);
		mockServer.listFieldOfficeSummariesResponse = null;
		hqApp.deleteAllFiles();
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
			list.add(b0.getLocalId() + "= " + b0.get(Bulletin.TAGTITLE) + "=3000");
			list.add(b1.getLocalId() + "= " + b1.get(Bulletin.TAGTITLE) + "=3200");
			list.add(b2.getLocalId() + "= " + b2.get(Bulletin.TAGTITLE) + "=3100");
			result.add(list);
			return result;
		}
		
	}
	
	public class MockServerInterfaceHandler extends ServerSideNetworkHandler
	{
		MockServerInterfaceHandler(MockMartusServer serverToUse)
		{
			super(serverToUse);
		}
		
		public void nullGetFieldOfficeAccountIds(boolean shouldReturnNull)
		{
			nullGetFieldOfficeAccountIds = shouldReturnNull;
		}
		
		public Vector getFieldOfficeAccountIds(String myAccountId, Vector parameters, String signature)
		{
			if(nullGetFieldOfficeAccountIds)
				return null;
			return super.getFieldOfficeAccountIds(myAccountId, parameters, signature);
		}
		
		boolean nullGetFieldOfficeAccountIds;
	}

	public static class MockServerNotAvailable extends MockMartusServer
	{
		MockServerNotAvailable() throws Exception
		{
			super();
		}

		public String ping()
		{
			return null;
		}
		
	}


	Bulletin b0;
	Bulletin b1;
	Bulletin b2;

	String title1 = "This is a cool title";
	String title2 = "Even cooler";

	static UiBasicLocalization localization;
	private static MockMartusSecurity mockSecurityForApp;
	private static MockMartusSecurity mockSecurityForServer;

	private MockMartusApp appWithServer;
	private MockMartusApp appWithoutServer;
	private MockMartusApp appWithAccount;

	private MockMartusServer mockServer;
	private MockServerInterfaceHandler mockSSLServerHandler;
	
	BackgroundUploader uploader;
}
