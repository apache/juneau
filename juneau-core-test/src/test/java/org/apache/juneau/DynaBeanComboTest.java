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
package org.apache.juneau;

import static org.apache.juneau.TestUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests DynaBean support.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({"javadoc"})
public class DynaBeanComboTest extends ComboTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<A>(
					"A",
					A.class,
					new A().init(),
					/* Json */		"{f1:1,f2a:'a',f2b:'b',f3:3}",
					/* JsonT */		"{f1:1,f2a:'a',f2b:'b',f3:3}",
					/* JsonR */		"{\n\tf1: 1,\n\tf2a: 'a',\n\tf2b: 'b',\n\tf3: 3\n}",
					/* Xml */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* XmlT */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* XmlR */		"<object>\n\t<f1>1</f1>\n\t<f2a>a</f2a>\n\t<f2b>b</f2b>\n\t<f3>3</f3>\n</object>\n",
					/* XmlNs */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* Html */		"<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>",
					/* HtmlT */		"<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2a</td>\n\t\t<td>a</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2b</td>\n\t\t<td>b</td>\n\t</tr>\n\t<tr>\n\t\t<td>f3</td>\n\t\t<td>3</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f1=1,f2a=a,f2b=b,f3=3)",
					/* UonT */		"(f1=1,f2a=a,f2b=b,f3=3)",
					/* UonR */		"(\n\tf1=1,\n\tf2a=a,\n\tf2b=b,\n\tf3=3\n)",
					/* UrlEnc */	"f1=1&f2a=a&f2b=b&f3=3",
					/* UrlEncT */	"f1=1&f2a=a&f2b=b&f3=3",
					/* UrlEncR */	"f1=1\n&f2a=a\n&f2b=b\n&f3=3",
					/* MsgPack */	"84A2663101A3663261A161A3663262A162A2663303",
					/* MsgPackT */	"84A2663101A3663261A161A3663262A162A2663303",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f1>1</jp:f1>\n    <jp:f2a>a</jp:f2a>\n    <jp:f2b>b</jp:f2b>\n    <jp:f3>3</jp:f3>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					public void verify(A o) {
						assertType(A.class, o);
					}
				}
			},
		});
	}
	
	public DynaBeanComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}
	
	@Override
	protected Serializer applySettings(Serializer s) throws Exception {
		return s.builder().trimNullProperties(false).build();
	}
	
	@Override
	protected Parser applySettings(Parser p) throws Exception {
		return p.builder().build();
	}
	
	public static class A {
		public int f1;
		@BeanProperty(name="*")
		public Map<String,Object> f2 = new LinkedHashMap<String,Object>();
		public int f3;
	
		public A init() {
			this.f1 = 1;
			this.f2 = new ObjectMap().append("f2a", "a").append("f2b", "b");
			this.f3 = 3;
			return this;
		}
	}
}