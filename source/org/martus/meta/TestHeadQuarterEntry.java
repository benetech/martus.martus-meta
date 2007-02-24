/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2005-2007, Beneficent
Technology, Inc. (The Benetech Initiative).

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

import org.martus.client.swingui.HeadQuarterEntry;
import org.martus.client.test.MockMartusApp;
import org.martus.clientside.test.MockUiLocalization;
import org.martus.common.HQKey;
import org.martus.common.HQKeys;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MockMartusSecurity;
import org.martus.util.TestCaseEnhanced;

public class TestHeadQuarterEntry extends TestCaseEnhanced 
{

	public TestHeadQuarterEntry(String name) 
	{
		super(name);
	}
	
	public void setUp() throws Exception
	{
		super.setUp();
	}
	
	public void tearDown() throws Exception
	{
	   	super.tearDown();
	}

	public void testBasics() throws Exception
	{
		MartusCrypto appSecurityAndHQ = MockMartusSecurity.createHQ();
		MockUiLocalization localization = new MockUiLocalization();
		MockMartusApp app = MockMartusApp.create(appSecurityAndHQ, localization);

		String publickey = "1234.4363.1233.3432.8823";
		String label1 = "key1 label";
		HQKey key1 = new HQKey(publickey, label1);
		HQKeys HQKeysAuthorized = new HQKeys(key1); 
		app.setAndSaveHQKeys(HQKeysAuthorized, HQKeysAuthorized);
		HeadQuarterEntry entry1 = new HeadQuarterEntry(key1);
		entry1.setSelected(false);
		assertEquals(label1, entry1.getLabel());
		String newLabel = "New Label Key1";
		entry1.setLabel(newLabel);
		assertEquals(newLabel, entry1.getLabel());
		assertEquals(key1.getPublicCode(), entry1.getPublicCode());
		assertEquals(key1, entry1.getKey());
		assertFalse("Entry is selected?", entry1.isSelected());
		
		HQKey key2 = new HQKey(appSecurityAndHQ.getPublicKeyString());
		key2.setLabel(app.getHQLabelIfPresent(key2));
		HeadQuarterEntry entry2 = new HeadQuarterEntry(key2);
		entry2.setSelected(true);
		String label2 = MartusCrypto.computeFormattedPublicCode(appSecurityAndHQ.getPublicKeyString()) + " " + localization.getFieldLabel("HQNotConfigured");
		assertEquals(label2, entry2.getLabel());
		assertEquals(key2.getPublicCode(), entry2.getPublicCode());
		assertEquals(key2, entry2.getKey());
		assertTrue("Entry is not selected?", entry2.isSelected());
		app.deleteAllFiles();
	}

}

