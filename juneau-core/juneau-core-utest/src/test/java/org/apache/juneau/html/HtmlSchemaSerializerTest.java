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
package org.apache.juneau.html;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.*;

/**
 * Most of the heavy testing for HtmlSchemaSerializer is done in JsonSchemaGeneratorTest.
 */
@FixMethodOrder(NAME_ASCENDING)
public class HtmlSchemaSerializerTest {

	//====================================================================================================
	// Simple objects
	//====================================================================================================
	@Test
	public void simpleObjects() throws Exception {
		HtmlSchemaSerializer s = HtmlSchemaSerializer.DEFAULT_SIMPLE;

		assertEquals("<table><tr><td>type</td><td>integer</td></tr><tr><td>format</td><td>int16</td></tr></table>", s.serialize((short)1));
		assertEquals("<table><tr><td>type</td><td>integer</td></tr><tr><td>format</td><td>int32</td></tr></table>", s.serialize(1));
		assertEquals("<table><tr><td>type</td><td>integer</td></tr><tr><td>format</td><td>int64</td></tr></table>", s.serialize(1l));
		assertEquals("<table><tr><td>type</td><td>number</td></tr><tr><td>format</td><td>float</td></tr></table>", s.serialize(1f));
		assertEquals("<table><tr><td>type</td><td>number</td></tr><tr><td>format</td><td>double</td></tr></table>", s.serialize(1d));
		assertEquals("<table><tr><td>type</td><td>boolean</td></tr></table>", s.serialize(true));
		assertEquals("<table><tr><td>type</td><td>string</td></tr></table>", s.serialize("foo"));
		assertEquals("<table><tr><td>type</td><td>string</td></tr></table>", s.serialize(new StringBuilder("foo")));
		assertEquals("<table><tr><td>type</td><td>string</td></tr></table>", s.serialize('c'));
		assertEquals("<table><tr><td>type</td><td>string</td></tr><tr><td>enum</td><td><ul><li>one</li><li>two</li><li>three</li></ul></td></tr></table>", s.serialize(TestEnumToString.ONE));
		assertEquals("<table><tr><td>type</td><td>object</td></tr><tr><td>properties</td><td><table><tr><td>f1</td><td><table><tr><td>type</td><td>string</td></tr></table></td></tr></table></td></tr></table>", s.serialize(new SimpleBean()));
	}

	public static class SimpleBean {
		public String f1;
	}

	//====================================================================================================
	// Documentation examples
	//====================================================================================================

	@Bean(properties="name,birthDate,addresses")
	public static class Person {
		public String name;
		public Calendar birthDate;
		public List<Address> addresses;
	}

	@Bean(properties="street,city,state,zip,isCurrent")
	public static class Address {
		public String street, city;
		public StateEnum state;
		public int zip;
		public boolean isCurrent;
	}

	public static enum StateEnum {
		AL,PA,NC
	}

	@Test
	public void documentationExample() throws Exception {
		HtmlSchemaSerializer s = HtmlSchemaSerializer.DEFAULT_SIMPLE;
		assertEquals("<table><tr><td>type</td><td>object</td></tr><tr><td>properties</td><td><table><tr><td>name</td><td><table><tr><td>type</td><td>string</td></tr></table></td></tr><tr><td>birthDate</td><td><table><tr><td>type</td><td>string</td></tr></table></td></tr><tr><td>addresses</td><td><table><tr><td>type</td><td>array</td></tr><tr><td>items</td><td><table><tr><td>type</td><td>object</td></tr><tr><td>properties</td><td><table><tr><td>street</td><td><table><tr><td>type</td><td>string</td></tr></table></td></tr><tr><td>city</td><td><table><tr><td>type</td><td>string</td></tr></table></td></tr><tr><td>state</td><td><table><tr><td>type</td><td>string</td></tr><tr><td>enum</td><td><ul><li>AL</li><li>PA</li><li>NC</li></ul></td></tr></table></td></tr><tr><td>zip</td><td><table><tr><td>type</td><td>integer</td></tr><tr><td>format</td><td>int32</td></tr></table></td></tr><tr><td>isCurrent</td><td><table><tr><td>type</td><td>boolean</td></tr></table></td></tr></table></td></tr></table></td></tr></table></td></tr></table></td></tr></table>", s.serialize(Person.class));
	}

}