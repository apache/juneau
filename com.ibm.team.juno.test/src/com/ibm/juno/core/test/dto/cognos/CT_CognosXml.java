/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.dto.cognos;

import static com.ibm.juno.core.serializer.SerializerProperties.*;
import static com.ibm.juno.core.test.TestUtils.*;
import static com.ibm.juno.core.xml.XmlSerializerProperties.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.dto.cognos.*;
import com.ibm.juno.core.xml.*;

public class CT_CognosXml {

	//====================================================================================================
	// test
	//====================================================================================================
	@Test
	public void test() throws Exception {
		String expected = ""
			+ "<dataset xmlns='http://developer.cognos.com/schemas/xmldata/1/' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n"
			+ "	<metadata>\n"
			+ "		<item name='asOfDate' type='xs:string' length='12'/>\n"
			+ "		<item name='rateOfReturn' type='xs:double'/>\n"
			+ "		<item name='famAcctIndex' type='xs:string' length='3'/>\n"
			+ "		<item name='rowID' type='xs:string' length='1'/>\n"
			+ "		<item name='brM' type='xs:string' length='1'/>\n"
			+ "		<item name='productLineCode' type='xs:int'/>\n"
			+ "	</metadata>\n"
			+ "	<data>\n"
			+ "		<row>\n"
			+ "			<value>Apr 26, 2002</value>\n"
			+ "			<value>0.21006642</value>\n"
			+ "			<value>JA1</value>\n"
			+ "			<value>F</value>\n"
			+ "			<value>B</value>\n"
			+ "			<value>1</value>\n"
			+ "		</row>\n"
			+ "		<row>\n"
			+ "			<value>Apr 27, 2002</value>\n"
			+ "			<value>0.1111111</value>\n"
			+ "			<value>BBB</value>\n"
			+ "			<value>G</value>\n"
			+ "			<value>B</value>\n"
			+ "			<value>2</value>\n"
			+ "		</row>\n"
			+ "	</data>\n"
			+ "</dataset>\n";
			
		List<Object> rows = new LinkedList<Object>();
		rows.add(new ObjectMap("{asOfDate:'Apr 26, 2002',rateOfReturn:0.210066429,famAcctIndex:'JA1',rowID:'F',brM:'B',productLineCode:1}"));
		rows.add(new Item("Apr 27, 2002", 0.1111111, "BBB", "G", "B", 2));
		
		Column[] c = {
			new Column("asOfDate", "xs:string", 12),
			new Column("rateOfReturn", "xs:double"),
			new Column("famAcctIndex", "xs:string", 3),
			new Column("rowID", "xs:string", 1),
			new Column("brM", "xs:string", 1),
			new Column("productLineCode", "xs:int")
		};
		
		XmlSerializer s = new XmlSerializer().setProperty(SERIALIZER_useIndentation, true).setProperty(SERIALIZER_quoteChar, '\'').setProperty(XML_defaultNamespaceUri, "cognos");
		
		DataSet ds = new DataSet(c, rows, BeanContext.DEFAULT);
		
		String out = s.serialize(ds);
		
		assertEquals(expected, out);
		
		// Make sure we can parse it back into a POJO.
		DataSet ds2 = XmlParser.DEFAULT.parse(out, DataSet.class);
		assertEqualObjects(ds, ds2);
	}

	public static class Item {
		public String asOfDate;
		public double rateOfReturn;
		public String famAcctIndex;
		public String rowID;
		public String brM;
		public int productLineCode;
		
		public Item(String asOfDate, double rateOfReturn, String famAcctIndex, String rowID, String brM, int productLineCode) {
			this.asOfDate = asOfDate;
			this.rateOfReturn = rateOfReturn;
			this.famAcctIndex = famAcctIndex;
			this.rowID = rowID;
			this.brM = brM;
			this.productLineCode = productLineCode;
		}
	}
}