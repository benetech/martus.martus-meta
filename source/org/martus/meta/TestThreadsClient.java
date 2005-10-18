/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2001-2004, Beneficent
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
import java.io.Writer;
import java.util.zip.ZipFile;

import junit.framework.TestSuite;

import org.martus.client.bulletinstore.BulletinFolder;
import org.martus.client.bulletinstore.ClientBulletinStore;
import org.martus.client.test.MockBulletinStore;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletin.BulletinZipUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.database.DatabaseKey;
import org.martus.common.database.ReadableDatabase;
import org.martus.common.packet.Packet;
import org.martus.common.packet.UniversalId;
import org.martus.swing.Utilities;
import org.martus.util.TestCaseEnhanced;
import org.martus.util.inputstreamwithseek.InputStreamWithSeek;
import org.martus.util.inputstreamwithseek.StringInputStreamWithSeek;


public class TestThreadsClient extends TestCaseEnhanced
{
	private static int ITERATIONS = 10;
	private static int THREAD_COUNT = 10;

	public static void main(String[] args)
	{
		if(args.length ==1)
			scaleFactor = Integer.parseInt(args[0]);
		junit.textui.TestRunner.run (new TestSuite(TestThreadsClient.class));
	}		

	public TestThreadsClient(String name)
	{
		super(name);
		//Java HACK under MSWindows and maybe Mac (Testing?)
		//Under Java 1.4.2_03 Iterations of 20 with a threadCount of 20 causes a hotspot error
		//Under Java 1.5 with Iterations of 9 with a threadCount of 9 causes the hotspot error
		//Under Java 1.6 with Iterations of 4 with a threadCount of 4 causes the hotspot error
		//Under Linux no hotspot errors occure at any Iteration # of threadCount #, but since threads are more important for a server
		//Than a client we are making this change so that the tests will pass on the Win2K build machine
		//And for us developers with win2K machines.
		
		
		if(Utilities.isMSWindows())
		{
			ITERATIONS = 3;
			THREAD_COUNT = 3;
		}
		else
		{
			ITERATIONS = 5;
			THREAD_COUNT = 50;
		}
	}

	public void testThreadedBulletinActivity() throws Throwable
	{
		final int threadCount = THREAD_COUNT * scaleFactor;
		final int iterations = ITERATIONS * scaleFactor;
		ThreadFactory factory = new BulletinThreadFactory();
		launchTestThreads(factory, threadCount, iterations);
		factory.tearDown();
	}

	
	public void testThreadedPacketWriting() throws Throwable
	{
		final int threadCount = THREAD_COUNT * scaleFactor;
		final int iterations = ITERATIONS * scaleFactor;
		ThreadFactory factory = new PacketWriteThreadFactory();
		launchTestThreads(factory, threadCount, iterations);
		factory.tearDown();
	}
	
	public void testThreadedExporting() throws Throwable
	{
		final int threadCount = THREAD_COUNT * scaleFactor;
		final int iterations = ITERATIONS * scaleFactor;
		ThreadFactory factory = new ExportThreadFactory();
		launchTestThreads(factory, threadCount, iterations);
		factory.tearDown();
	}

	public void testThreadedImporting() throws Throwable
	{
		final int threadCount = THREAD_COUNT * scaleFactor;
		final int iterations = ITERATIONS * scaleFactor;
		ThreadFactory factory = new ImportThreadFactory();
		launchTestThreads(factory, threadCount, iterations);
		factory.tearDown();
	}
	
	public void testThreadedFolderListActivity() throws Throwable
	{
		final int threadCount = THREAD_COUNT * scaleFactor;
		final int iterations = ITERATIONS * scaleFactor;
		FolderListThreadFactory factory = new FolderListThreadFactory();
		launchTestThreads(factory, threadCount, iterations);
		factory.tearDown();
	}

	public void testThreadedFolderContentsActivity() throws Throwable
	{
		final int threadCount = THREAD_COUNT * scaleFactor;
		final int iterations = ITERATIONS * scaleFactor;
		FolderContentsThreadFactory factory = new FolderContentsThreadFactory();
		launchTestThreads(factory, threadCount, iterations);
		factory.tearDown();
	}

	class BulletinThreadFactory extends ThreadFactory
	{
		BulletinThreadFactory() throws Exception
		{
			store = new MockBulletinStore();
			//store.maxCachedBulletinCount = 10;
			
			for (int i = 0; i < 10; i++)
			{
				Bulletin b = store.createEmptyBulletin();
				store.saveBulletin(b);
			}
		}
		
		public TestingThread createThread(int copies) throws Exception
		{
			return new BulletinTester(store, copies);
		}

		public void tearDown() throws Exception
		{
			store.deleteAllData();
		}

		ClientBulletinStore store;
	}
	
	class ExportThreadFactory extends ThreadFactory
	{
		ExportThreadFactory() throws Exception
		{
			store = new MockBulletinStore();
			
			b = store.createEmptyBulletin();
			store.saveBulletin(b);
		}
		
		public TestingThread createThread(int copies) throws Exception
		{
			return new Exporter(store, b, copies);
		}
		
		public void tearDown() throws Exception
		{
			store.deleteAllData();
		}

		ClientBulletinStore store;
		Bulletin b;
	}
	
	class ImportThreadFactory extends ThreadFactory
	{
		ImportThreadFactory() throws Exception
		{
			store = new MockBulletinStore();
		}
		
		public TestingThread createThread(int copies) throws Exception
		{
			return new Importer(store, copies);
		}

		public void tearDown() throws Exception
		{
			store.deleteAllData();
		}

		MockBulletinStore store;		
	}
	
	class PacketWriteThreadFactory extends ThreadFactory
	{
		PacketWriteThreadFactory() throws Exception
		{
			store = new MockBulletinStore();
		}
		
		public TestingThread createThread(int copies) throws Exception
		{
			return new PacketWriter(store, copies);
		}

		public void tearDown() throws Exception
		{
			store.deleteAllData();
		}

		ClientBulletinStore store;		
	}
	
	class FolderListThreadFactory extends ThreadFactory
	{
		FolderListThreadFactory() throws Exception
		{
			store = new MockBulletinStore();
		}
		
		public TestingThread createThread(int copies) throws Exception
		{
			return new FolderListTester(store, copies, nextId++);
		}
		
		public void tearDown() throws Exception
		{
			store.deleteAllData();
		}

		ClientBulletinStore store;
		int nextId;	
	}
	
	class FolderContentsThreadFactory extends ThreadFactory
	{
		FolderContentsThreadFactory() throws Exception
		{
			store = new MockBulletinStore();
		}
		
		public TestingThread createThread(int copies) throws Exception
		{
			return new FolderContentsTester(store, copies);
		}
		
		public void tearDown() throws Exception 
		{
			store.deleteAllData();
		}

		ClientBulletinStore store;
	}
	
	class BulletinTester extends TestingThread
	{
		BulletinTester(ClientBulletinStore storeToUse, int copiesToDo) throws Exception
		{
			store = storeToUse;
			copies = copiesToDo;
		}
		
		public void run()
		{
			try 
			{
				for(int i=0; i < copies; ++i)
				{
					Bulletin b = store.createEmptyBulletin();
					DatabaseKey key = b.getDatabaseKeyForLocalId(b.getLocalId());

					store.saveBulletin(b);
					assertTrue("not found after save?", store.doesBulletinRevisionExist(key));
					store.destroyBulletin(b);
					assertFalse("found after remove?", store.doesBulletinRevisionExist(key));
				}
			}
			catch (Throwable e)
			{
System.out.println(folderName + ": " + e);
System.out.flush();
				result = e;
			}
		}
		
		ClientBulletinStore store;
		int copies;
		String folderName;
		Bulletin[] bulletins;
	}

	class Exporter extends TestingThread
	{
		Exporter(ClientBulletinStore store, Bulletin bulletinToExport, int copiesToExport) throws Exception
		{
			bulletin = bulletinToExport;
			file = createTempFile();
			copies = copiesToExport;
			db = store.getDatabase();
			security = store.getSignatureVerifier();
			headerKey = DatabaseKey.createKey(bulletin.getUniversalId(), bulletin.getStatus());
		}
		
		public void run()
		{
			try 
			{
				for(int i=0; i < copies; ++i)
					BulletinZipUtilities.exportBulletinPacketsFromDatabaseToZipFile(db, headerKey, file, security);
				} 
			catch (Exception e) 
			{
				result = e;
			}
		}
		
		Bulletin bulletin;
		File file;
		int copies;
		ReadableDatabase db;
		MartusCrypto security;
		DatabaseKey headerKey;
	}

	class Importer extends TestingThread
	{
		Importer(MockBulletinStore storeToUse, int copiesToDo) throws Exception
		{
			copies = copiesToDo;
			store = storeToUse;

			file = createTempFile();
			ReadableDatabase db = store.getDatabase();
			security = store.getSignatureVerifier();

			Bulletin b = store.createEmptyBulletin();
			store.saveBulletin(b);
			headerKey = DatabaseKey.createKey(b.getUniversalId(), b.getStatus());
			BulletinZipUtilities.exportBulletinPacketsFromDatabaseToZipFile(db, headerKey, file, security);
			store.destroyBulletin(b);
		}
		
		public void run()
		{
			try 
			{
				for(int i=0; i < copies; ++i)
				{
					ZipFile zip = new ZipFile(file);
					store.importBulletinZipFile(zip);
					zip.close();

					Bulletin b = store.getBulletinRevision(headerKey.getUniversalId());
					assertTrue("import didn't work?", store.doesBulletinRevisionExist(headerKey));
					store.destroyBulletin(b);
				}
			} 
			catch (Exception e) 
			{
				result = e;
			}
		}
		
		MockBulletinStore store;
		File file;
		int copies;
		MartusCrypto security;
		DatabaseKey headerKey;
	}

	class PacketWriter extends TestingThread
	{
		PacketWriter(ClientBulletinStore storeToUse, int copiesToDo) throws Exception
		{
			
			copies = copiesToDo;
			db = storeToUse.getDatabase();
			bulletin = storeToUse.createEmptyBulletin();
			security = storeToUse.getSignatureGenerator();
		}
		
		public void run()
		{
			try 
			{
				for(int i=0; i < copies; ++i)
				{
					Writer writer = new StringWriter();
					bulletin.getBulletinHeaderPacket().writeXml(writer, security);
					InputStreamWithSeek in = new StringInputStreamWithSeek(writer.toString());
					Packet.validateXml(in, bulletin.getAccount(), bulletin.getLocalId(), null, security);
				}
			} 
			catch (Exception e) 
			{
				result = e;
			}
		}
		
		Bulletin bulletin;
		ReadableDatabase db;
		MartusCrypto security;
		int copies;
	}

	class FolderListTester extends TestingThread
	{
		FolderListTester(ClientBulletinStore storeToUse, int copiesToDo, int id) throws Exception
		{
			store = storeToUse;
			copies = copiesToDo;
			folderName = Integer.toString(id);
		}
		
		public void run()
		{
			try 
			{
				for(int i=0; i < copies; ++i)
				{
//System.out.println("delete " + folderName);
//System.out.flush();
					store.deleteFolder(folderName);
					assertNull("found after delete1?", store.findFolder(folderName));
//System.out.println("create " + folderName);
//System.out.flush();
					store.createFolder(folderName);
					assertNotNull("not found after create?", store.findFolder(folderName));
//System.out.println("save " + folderName);
//System.out.flush();
					store.saveFolders();
					assertNotNull("not found after save?", store.findFolder(folderName));
//System.out.println("delete " + folderName);
//System.out.flush();
					store.deleteFolder(folderName);
					assertNull("found after delete2?", store.findFolder(folderName));
				}
			}
			catch (Throwable e)
			{
System.out.println(folderName + ": " + e);
System.out.flush();
				result = e;
			}
		}
		
		ClientBulletinStore store;
		int copies;
		String folderName;
	}

	class FolderContentsTester extends TestingThread
	{
		FolderContentsTester(ClientBulletinStore storeToUse, int copiesToDo) throws Exception
		{
			store = storeToUse;
			copies = copiesToDo;
			folderName = "test";
			store.createFolder(folderName);
			bulletins= new Bulletin[copies];
			for (int i = 0; i < bulletins.length; i++)
			{
				bulletins[i] = store.createEmptyBulletin();
				store.saveBulletin(bulletins[i]);
			}
		}
		
		public void run()
		{
			try 
			{
				for(int i=0; i < copies; ++i)
				{
					Bulletin b = bulletins[i];
					UniversalId uid = b.getUniversalId();
					BulletinFolder f = store.findFolder(folderName);
					assertEquals("Already in?", false, f.contains(b));
					store.addBulletinToFolder(f, uid);
					assertEquals("Not added?", true, f.contains(b));
					store.discardBulletin(f, b);
					assertEquals("Not discarded?", false, f.contains(b));
					store.moveBulletin(b, store.getFolderDiscarded(), f);
					assertEquals("Not moved back?", true, f.contains(b));
					store.removeBulletinFromFolder(f, b);
					assertEquals("Not removed?", false, f.contains(b));
					assertEquals("Not orphan?", true, store.isOrphan(b));
					store.addBulletinToFolder(f, uid);
				}
			}
			catch (Throwable e)
			{
System.out.println(folderName + ": " + e);
System.out.flush();
				result = e;
			}
		}
		ClientBulletinStore store;
		int copies;
		String folderName;
		Bulletin[] bulletins;
	}

	public static int scaleFactor = 1;
}
