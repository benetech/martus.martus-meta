package org.martus.meta;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.martus.common.test.TestCaseEnhanced;

public class TestMeta extends TestCaseEnhanced
{

	public TestMeta(String name)
	{
		super(name);
	}

	public static void main (String[] args)
	{
		runTests();
	}

	public static void runTests ()
	{
		junit.textui.TestRunner.run (suite());
	}

	public static Test suite()
	{
		TestSuite suite= new TestSuite("Meta Tests");

		suite.addTest(new TestSuite(TestBackgroundUploader.class));
		suite.addTest(new TestSuite(TestDatabase.class));
		suite.addTest(new TestSuite(TestDatabaseHiddenRecords.class));
		suite.addTest(new TestSuite(TestDeleteDraftsTableModel.class));
		suite.addTest(new TestSuite(TestMartusApp_WithServer.class));
		suite.addTest(new TestSuite(TestRetrieveHQDraftsTableModel.class));
		suite.addTest(new TestSuite(TestRetrieveHQTableModel.class));
		suite.addTest(new TestSuite(TestRetrieveMyDraftsTableModel.class));
		suite.addTest(new TestSuite(TestRetrieveMyTableModel.class));
		suite.addTest(new TestSuite(TestRetrieveTableModel.class));
		suite.addTest(new TestSuite(TestSimpleX509TrustManager.class));
		suite.addTest(new TestSuite(TestSSL.class));
		suite.addTest(new TestSuite(TestThreads.class));
		
		return suite;
	}	
}
