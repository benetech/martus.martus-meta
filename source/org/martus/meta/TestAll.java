package org.martus.meta;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.martus.client.test.TestClient;
import org.martus.common.test.TestCommon;
import org.martus.server.core.TestServer;

public class TestAll extends java.lang.Object 
{
    public TestAll() 
    {
    }

	public static void main (String[] args) 
	{
		int loop = 0;
		if(args.length > 0 && args[0].equalsIgnoreCase("LOOP"))
			loop = 1;
		do
		{
			if(loop>0)
			{
				System.out.println("\nTo exit tests type Control + C.");
				System.out.println("Loop:"+loop);
				loop++;
			}
			runTests();
		}while(loop>0);
	}

	public static void runTests () 
	{
		junit.textui.TestRunner.run (suite());
	}

	public static Test suite ( ) 
	{
		TestSuite suite= new TestSuite("All Martus Tests");

		suite.addTest(TestMeta.suite());
		
		// shared stuff
		suite.addTest(TestCommon.suite());
		suite.addTest(TestServer.suite());
		suite.addTest(TestClient.suite());

	    return suite;
	}
}
