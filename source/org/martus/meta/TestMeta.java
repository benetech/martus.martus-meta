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

		suite.addTest(TestMetaQuick.suite());
		suite.addTestSuite(TestThreads.class);
		
		return suite;
	}	
}
