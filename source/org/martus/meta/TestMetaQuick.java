package org.martus.meta;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.martus.common.test.TestCaseEnhanced;

public class TestMetaQuick extends TestCaseEnhanced
{

	public TestMetaQuick(String name)
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

		suite.addTestSuite(TestBackgroundUploader.class);
		suite.addTestSuite(TestDatabase.class);
		suite.addTestSuite(TestDatabaseHiddenRecords.class);
		suite.addTestSuite(TestDeleteDraftsTableModel.class);
		suite.addTestSuite(TestMartusApp_WithServer.class);
		suite.addTestSuite(TestRetrieveHQDraftsTableModel.class);
		suite.addTestSuite(TestRetrieveHQTableModel.class);
		suite.addTestSuite(TestRetrieveMyDraftsTableModel.class);
		suite.addTestSuite(TestRetrieveMyTableModel.class);
		suite.addTestSuite(TestRetrieveTableModel.class);
		suite.addTestSuite(TestScrubFile.class);
		suite.addTestSuite(TestSimpleX509TrustManager.class);
		suite.addTestSuite(TestSSL.class);
		
		return suite;
	}	
}
