/*The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2005, Beneficent
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

import org.martus.client.swingui.HeadQuarterEntry;
import org.martus.client.swingui.HeadQuartersTableModelConfiguration;
import org.martus.client.test.MockMartusApp;
import org.martus.common.HQKey;
import org.martus.common.HQKeys;
import org.martus.common.clientside.test.MockUiLocalization;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MockMartusSecurity;
import org.martus.util.TestCaseEnhanced;
import org.martus.util.Base64.InvalidBase64Exception;

public class TestHeadQuartersTableModelConfiguration extends TestCaseEnhanced
{

	public TestHeadQuartersTableModelConfiguration(String name) 
	{
		super(name);
	}
	
	public void setUp() throws Exception
	{
		super.setUp();
		if(localization!=null)
			return;
		localization = new MockUiLocalization();
		appSecurityAndHQ = MockMartusSecurity.createHQ();
		app = MockMartusApp.create(appSecurityAndHQ, localization);

		modelWithData = new HeadQuartersTableModelConfiguration(localization);
		key1 = new HQKey(publicCode1, label1);
		HQKeys HQKeysAuthorized = new HQKeys(key1); 
		app.setAndSaveHQKeys(HQKeysAuthorized);
		app.addHQLabelsWherePossible(HQKeysAuthorized);
		
		HeadQuarterEntry entry1 = new HeadQuarterEntry(key1);
		modelWithData.addNewHeadQuarterEntry(entry1);
		
		key2 = new HQKey(appSecurityAndHQ.getPublicKeyString());
		key2.setLabel(app.getHQLabelIfPresent(key2));
		HeadQuarterEntry entry2 = new HeadQuarterEntry(key2);
		modelWithData.addNewHeadQuarterEntry(entry2);

		modelWithoutData = new HeadQuartersTableModelConfiguration(localization);
	}

	public void tearDown() throws Exception
	{
		app.deleteAllFiles();
	   	super.tearDown();
	}
	
	public void testGetColumnName()
	{
		assertEquals(localization.getFieldLabel("ConfigureHeadQuartersDefault"), modelWithData.getColumnName(0));
		assertEquals(localization.getFieldLabel("ConfigureHQColumnHeaderPublicCode"), modelWithData.getColumnName(1));
		assertEquals(localization.getFieldLabel("BulletinHeadQuartersHQLabel"), modelWithData.getColumnName(2));
	}
	
	public void testGetColumnCount()
	{
		assertEquals(3, modelWithoutData.getColumnCount());
		assertEquals(3, modelWithData.getColumnCount());
	}
	
	public void testGetRowCount()
	{
		assertEquals(0, modelWithoutData.getRowCount());
		assertEquals(2, modelWithData.getRowCount());
	}
	
	public void testIsCellEditable()
	{
		assertEquals("select hq not editable?", true, modelWithData.isCellEditable(1,modelWithData.COLUMN_DEFAULT));
		assertEquals("Public code is editable?", false, modelWithData.isCellEditable(1,modelWithData.COLUMN_PUBLIC_CODE));
		assertEquals("label is editable?", false, modelWithData.isCellEditable(1,modelWithData.COLUMN_LABEL));
	}
	
	public void testGetColumnClass()
	{
		assertEquals(Boolean.class, modelWithData.getColumnClass(modelWithData.COLUMN_DEFAULT));
		assertEquals(String.class, modelWithData.getColumnClass(modelWithData.COLUMN_PUBLIC_CODE));
		assertEquals(String.class, modelWithData.getColumnClass(modelWithData.COLUMN_LABEL));
	}
	
	public void testKeyLabelNames() throws InvalidBase64Exception
	{
		assertEquals(label1, modelWithData.getValueAt(0,2));
		String label2 = MartusCrypto.computeFormattedPublicCode(appSecurityAndHQ.getPublicKeyString()) + " " + localization.getFieldLabel("HQNotConfigured");
		assertEquals(label2, modelWithData.getValueAt(1,2));
	}
	
	public void testKeyPublicCodes() throws InvalidBase64Exception
	{
		assertEquals(key1.getPublicCode(), modelWithData.getValueAt(0,1));
		assertEquals(key2.getPublicCode(), modelWithData.getValueAt(1,1));
	}

	public void testGetAllSelectedHeadQuarterKeys()
	{
		assertEquals(0, modelWithoutData.getAllDefaultHeadQuarterKeys().size());
		assertEquals(0, modelWithoutData.getAllDefaultHeadQuarterKeys().size());
		
		modelWithData.setValueAt(Boolean.TRUE, 0,0);
		HQKeys allDefaultHeadQuarterKeys = modelWithData.getAllDefaultHeadQuarterKeys();
		assertEquals(1, allDefaultHeadQuarterKeys.size());
		assertTrue(((Boolean)modelWithData.getValueAt(0,0)).booleanValue());
		assertEquals(key1, allDefaultHeadQuarterKeys.get(0));

		modelWithData.setValueAt(Boolean.FALSE, 0,0);
		assertFalse(((Boolean)modelWithData.getValueAt(0,0)).booleanValue());
		assertEquals(0, modelWithData.getAllDefaultHeadQuarterKeys().size());
		modelWithData.setValueAt(Boolean.TRUE, 1,0);
		assertTrue(((Boolean)modelWithData.getValueAt(1,0)).booleanValue());
		allDefaultHeadQuarterKeys = modelWithData.getAllDefaultHeadQuarterKeys();
		assertEquals(1, allDefaultHeadQuarterKeys.size());
		assertEquals(key2, allDefaultHeadQuarterKeys.get(0));
		
		modelWithData.setValueAt(Boolean.TRUE, 0,0);
		assertTrue(((Boolean)modelWithData.getValueAt(0,0)).booleanValue());
		assertEquals(2, modelWithData.getAllDefaultHeadQuarterKeys().size());
	}
	
	public void testGetSetHQLabels()
	{
		String newLabel1 = "new HQ Label 1";
		String newLabel2 = "new HQ Label 2";
			
		int labelColumn = modelWithData.COLUMN_LABEL;
		modelWithData.setValueAt(newLabel1, 0,labelColumn);
		assertEquals(newLabel1, modelWithData.getValueAt(0, labelColumn));
		modelWithData.setValueAt(label1, 0,labelColumn);
		assertEquals(label1, modelWithData.getValueAt(0, labelColumn));
		
		modelWithData.setValueAt(newLabel2, 1,labelColumn);
		assertEquals(newLabel2, modelWithData.getValueAt(1, labelColumn));
		modelWithData.setValueAt("", 1,labelColumn);
		assertEquals("", modelWithData.getValueAt(1, labelColumn));
	}
	
	public void testRemoveRow()
	{
		HQKey key3 = new HQKey("123.public.key.3");
		String key3Label = "key3";
		key3.setLabel(key3Label);
		HeadQuarterEntry newEntry = new HeadQuarterEntry(key3);
		modelWithData.addNewHeadQuarterEntry(newEntry);
		assertEquals(3, modelWithData.getRowCount());
		assertEquals(key3Label, modelWithData.getValueAt(2, modelWithData.COLUMN_LABEL));
		modelWithData.removeRow(1);
		assertEquals(2, modelWithData.getRowCount());
		assertEquals(label1, modelWithData.getValueAt(0, modelWithData.COLUMN_LABEL));
		assertEquals(key3Label, modelWithData.getValueAt(1, modelWithData.COLUMN_LABEL));
		
		
	}
	
	
	
	static MockUiLocalization localization;
	static MockMartusApp app;
	static MartusCrypto appSecurityAndHQ;
	static HeadQuartersTableModelConfiguration modelWithData;
	static HeadQuartersTableModelConfiguration modelWithoutData;
	
	static String publicCode1 = "123.436";
	static String label1 = "key1 label";
	static HQKey key1;
	static HQKey key2;
}



