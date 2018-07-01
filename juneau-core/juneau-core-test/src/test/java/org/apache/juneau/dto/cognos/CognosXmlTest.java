// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.dto.cognos;

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.xml.*;
import org.junit.*;

public class CognosXmlTest {

	//====================================================================================================
	// test
	//====================================================================================================
	@Test
	public void test() throws Exception {
		String expected = ""
			+ "<dataset xmlns='http://developer.cognos.com/schemas/xmldata/1/'>\n"
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

		List<Object> rows = new LinkedList<>();
		rows.add(new ObjectMap("{asOfDate:'Apr 26, 2002',rateOfReturn:0.21006642,famAcctIndex:'JA1',rowID:'F',brM:'B',productLineCode:1}"));
		rows.add(new Item("Apr 27, 2002", 0.1111111, "BBB", "G", "B", 2));

		Column[] c = {
			new Column("asOfDate", "xs:string", 12),
			new Column("rateOfReturn", "xs:double"),
			new Column("famAcctIndex", "xs:string", 3),
			new Column("rowID", "xs:string", 1),
			new Column("brM", "xs:string", 1),
			new Column("productLineCode", "xs:int")
		};

		XmlSerializer s = XmlSerializer.create()
			.ws()
			.sq()
			.defaultNamespace("cognos")
			.ns()
			.addNamespaceUrisToRoot()
			.build();

		DataSet ds = new DataSet(c, rows, BeanContext.DEFAULT.createSession());

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