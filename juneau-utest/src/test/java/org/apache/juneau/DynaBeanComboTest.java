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

import static org.apache.juneau.assertions.Verify.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swaps.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests DynaBean support.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class DynaBeanComboTest extends ComboRoundTripTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<BeanWithDynaField>(
					"BeanWithDynaField",
					BeanWithDynaField.class,
					new BeanWithDynaField().init()
				)
				.json("{f1:1,f2a:'a',f2b:'b',f3:3}")
				.jsonT("{f1:1,f2a:'a',f2b:'b',f3:3}")
				.jsonR("{\n\tf1: 1,\n\tf2a: 'a',\n\tf2b: 'b',\n\tf3: 3\n}")
				.xml("<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>")
				.xmlT("<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>")
				.xmlR("<object>\n\t<f1>1</f1>\n\t<f2a>a</f2a>\n\t<f2b>b</f2b>\n\t<f3>3</f3>\n</object>\n")
				.xmlNs("<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>")
				.html("<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>")
				.htmlT("<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2a</td>\n\t\t<td>a</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2b</td>\n\t\t<td>b</td>\n\t</tr>\n\t<tr>\n\t\t<td>f3</td>\n\t\t<td>3</td>\n\t</tr>\n</table>\n")
				.uon("(f1=1,f2a=a,f2b=b,f3=3)")
				.uonT("(f1=1,f2a=a,f2b=b,f3=3)")
				.uonR("(\n\tf1=1,\n\tf2a=a,\n\tf2b=b,\n\tf3=3\n)")
				.urlEnc("f1=1&f2a=a&f2b=b&f3=3")
				.urlEncT("f1=1&f2a=a&f2b=b&f3=3")
				.urlEncR("f1=1\n&f2a=a\n&f2b=b\n&f3=3")
				.msgPack("84A2663101A3663261A161A3663262A162A2663303")
				.msgPackT("84A2663101A3663261A161A3663262A162A2663303")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f1>1</jp:f1>\n    <jp:f2a>a</jp:f2a>\n    <jp:f2b>b</jp:f2b>\n    <jp:f3>3</jp:f3>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithDynaField.class))
			},
			{ 	/* 1 */
				new ComboInput<BeanWithDynaMethods>(
					"BeanWithDynaMethods",
					BeanWithDynaMethods.class,
					new BeanWithDynaMethods().init()
				)
				.json("{f1:1,f2a:'a',f2b:'b',f3:3}")
				.jsonT("{f1:1,f2a:'a',f2b:'b',f3:3}")
				.jsonR("{\n\tf1: 1,\n\tf2a: 'a',\n\tf2b: 'b',\n\tf3: 3\n}")
				.xml("<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>")
				.xmlT("<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>")
				.xmlR("<object>\n\t<f1>1</f1>\n\t<f2a>a</f2a>\n\t<f2b>b</f2b>\n\t<f3>3</f3>\n</object>\n")
				.xmlNs("<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>")
				.html("<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>")
				.htmlT("<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2a</td>\n\t\t<td>a</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2b</td>\n\t\t<td>b</td>\n\t</tr>\n\t<tr>\n\t\t<td>f3</td>\n\t\t<td>3</td>\n\t</tr>\n</table>\n")
				.uon("(f1=1,f2a=a,f2b=b,f3=3)")
				.uonT("(f1=1,f2a=a,f2b=b,f3=3)")
				.uonR("(\n\tf1=1,\n\tf2a=a,\n\tf2b=b,\n\tf3=3\n)")
				.urlEnc("f1=1&f2a=a&f2b=b&f3=3")
				.urlEncT("f1=1&f2a=a&f2b=b&f3=3")
				.urlEncR("f1=1\n&f2a=a\n&f2b=b\n&f3=3")
				.msgPack("84A2663101A3663261A161A3663262A162A2663303")
				.msgPackT("84A2663101A3663261A161A3663262A162A2663303")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f1>1</jp:f1>\n    <jp:f2a>a</jp:f2a>\n    <jp:f2b>b</jp:f2b>\n    <jp:f3>3</jp:f3>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithDynaMethods.class))
				.verify(x -> x.setterCalled ? null : "Setter not called")
			},
			{ 	/* 2 */
				new ComboInput<BeanWithDynaGetterOnly>(
					"BeanWithDynaGetterOnly",
					BeanWithDynaGetterOnly.class,
					new BeanWithDynaGetterOnly().init()
				)
				.json("{f1:1,f2a:'a',f2b:'b',f3:3}")
				.jsonT("{f1:1,f2a:'a',f2b:'b',f3:3}")
				.jsonR("{\n\tf1: 1,\n\tf2a: 'a',\n\tf2b: 'b',\n\tf3: 3\n}")
				.xml("<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>")
				.xmlT("<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>")
				.xmlR("<object>\n\t<f1>1</f1>\n\t<f2a>a</f2a>\n\t<f2b>b</f2b>\n\t<f3>3</f3>\n</object>\n")
				.xmlNs("<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>")
				.html("<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>")
				.htmlT("<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2a</td>\n\t\t<td>a</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2b</td>\n\t\t<td>b</td>\n\t</tr>\n\t<tr>\n\t\t<td>f3</td>\n\t\t<td>3</td>\n\t</tr>\n</table>\n")
				.uon("(f1=1,f2a=a,f2b=b,f3=3)")
				.uonT("(f1=1,f2a=a,f2b=b,f3=3)")
				.uonR("(\n\tf1=1,\n\tf2a=a,\n\tf2b=b,\n\tf3=3\n)")
				.urlEnc("f1=1&f2a=a&f2b=b&f3=3")
				.urlEncT("f1=1&f2a=a&f2b=b&f3=3")
				.urlEncR("f1=1\n&f2a=a\n&f2b=b\n&f3=3")
				.msgPack("84A2663101A3663261A161A3663262A162A2663303")
				.msgPackT("84A2663101A3663261A161A3663262A162A2663303")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f1>1</jp:f1>\n    <jp:f2a>a</jp:f2a>\n    <jp:f2b>b</jp:f2b>\n    <jp:f3>3</jp:f3>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithDynaGetterOnly.class))
			},
			{ 	/* 3 */
				new ComboInput<BeanWithDynaFieldSwapped>(
					"BeanWithDynaFieldSwapped",
					BeanWithDynaFieldSwapped.class,
					new BeanWithDynaFieldSwapped().init()
				)
				.json("{f1a:'1901-03-03T18:11:12Z'}")
				.jsonT("{f1a:'1901-03-03T18:11:12Z'}")
				.jsonR("{\n\tf1a: '1901-03-03T18:11:12Z'\n}")
				.xml("<object><f1a>1901-03-03T18:11:12Z</f1a></object>")
				.xmlT("<object><f1a>1901-03-03T18:11:12Z</f1a></object>")
				.xmlR("<object>\n\t<f1a>1901-03-03T18:11:12Z</f1a>\n</object>\n")
				.xmlNs("<object><f1a>1901-03-03T18:11:12Z</f1a></object>")
				.html("<table><tr><td>f1a</td><td>1901-03-03T18:11:12Z</td></tr></table>")
				.htmlT("<table><tr><td>f1a</td><td>1901-03-03T18:11:12Z</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f1a</td>\n\t\t<td>1901-03-03T18:11:12Z</td>\n\t</tr>\n</table>\n")
				.uon("(f1a=1901-03-03T18:11:12Z)")
				.uonT("(f1a=1901-03-03T18:11:12Z)")
				.uonR("(\n\tf1a=1901-03-03T18:11:12Z\n)")
				.urlEnc("f1a=1901-03-03T18:11:12Z")
				.urlEncT("f1a=1901-03-03T18:11:12Z")
				.urlEncR("f1a=1901-03-03T18:11:12Z")
				.msgPack("81A3663161B4313930312D30332D30335431383A31313A31325A")
				.msgPackT("81A3663161B4313930312D30332D30335431383A31313A31325A")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f1a>1901-03-03T18:11:12Z</jp:f1a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f1a>1901-03-03T18:11:12Z</jp:f1a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f1a>1901-03-03T18:11:12Z</jp:f1a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithDynaFieldSwapped.class))
				.verify(x -> verify(x.f1.get("f1a")).isType(Calendar.class))
			},
			{ 	/* 4 */
				new ComboInput<BeanWithDynaFieldStringList>(
					"BeanWithDynaFieldStringList",
					BeanWithDynaFieldStringList.class,
					new BeanWithDynaFieldStringList().init()
				)
				.json("{f1a:['foo','bar']}")
				.jsonT("{f1a:['foo','bar']}")
				.jsonR("{\n\tf1a: [\n\t\t'foo',\n\t\t'bar'\n\t]\n}")
				.xml("<object><f1a><string>foo</string><string>bar</string></f1a></object>")
				.xmlT("<object><f1a><string>foo</string><string>bar</string></f1a></object>")
				.xmlR("<object>\n\t<f1a>\n\t\t<string>foo</string>\n\t\t<string>bar</string>\n\t</f1a>\n</object>\n")
				.xmlNs("<object><f1a><string>foo</string><string>bar</string></f1a></object>")
				.html("<table><tr><td>f1a</td><td><ul><li>foo</li><li>bar</li></ul></td></tr></table>")
				.htmlT("<table><tr><td>f1a</td><td><ul><li>foo</li><li>bar</li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f1a</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>foo</li>\n\t\t\t\t<li>bar</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f1a=@(foo,bar))")
				.uonT("(f1a=@(foo,bar))")
				.uonR("(\n\tf1a=@(\n\t\tfoo,\n\t\tbar\n\t)\n)")
				.urlEnc("f1a=@(foo,bar)")
				.urlEncT("f1a=@(foo,bar)")
				.urlEncR("f1a=@(\n\tfoo,\n\tbar\n)")
				.msgPack("81A366316192A3666F6FA3626172")
				.msgPackT("81A366316192A3666F6FA3626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f1a>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:f1a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f1a>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:f1a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f1a>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li>bar</rdf:li>\n      </rdf:Seq>\n    </jp:f1a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithDynaFieldStringList.class))
			},
			{ 	/* 5 */
				new ComboInput<BeanWithDynaMethodsAndExtraKeys>(
					"BeanWithDynaMethodsAndExtraKeys",
					BeanWithDynaMethodsAndExtraKeys.class,
					new BeanWithDynaMethodsAndExtraKeys().init()
				)
				.json("{f1:1,f2a:'a',f2b:'b',f3:3}")
				.jsonT("{f1:1,f2a:'a',f2b:'b',f3:3}")
				.jsonR("{\n\tf1: 1,\n\tf2a: 'a',\n\tf2b: 'b',\n\tf3: 3\n}")
				.xml("<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>")
				.xmlT("<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>")
				.xmlR("<object>\n\t<f1>1</f1>\n\t<f2a>a</f2a>\n\t<f2b>b</f2b>\n\t<f3>3</f3>\n</object>\n")
				.xmlNs("<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>")
				.html("<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>")
				.htmlT("<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2a</td>\n\t\t<td>a</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2b</td>\n\t\t<td>b</td>\n\t</tr>\n\t<tr>\n\t\t<td>f3</td>\n\t\t<td>3</td>\n\t</tr>\n</table>\n")
				.uon("(f1=1,f2a=a,f2b=b,f3=3)")
				.uonT("(f1=1,f2a=a,f2b=b,f3=3)")
				.uonR("(\n\tf1=1,\n\tf2a=a,\n\tf2b=b,\n\tf3=3\n)")
				.urlEnc("f1=1&f2a=a&f2b=b&f3=3")
				.urlEncT("f1=1&f2a=a&f2b=b&f3=3")
				.urlEncR("f1=1\n&f2a=a\n&f2b=b\n&f3=3")
				.msgPack("84A2663101A3663261A161A3663262A162A2663303")
				.msgPackT("84A2663101A3663261A161A3663262A162A2663303")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f1>1</jp:f1>\n    <jp:f2a>a</jp:f2a>\n    <jp:f2b>b</jp:f2b>\n    <jp:f3>3</jp:f3>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithDynaMethodsAndExtraKeys.class))
				.verify(x -> x.setterCalled, "Setter not called")
			},
		});
	}

	public DynaBeanComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	@Override
	protected Serializer applySettings(Serializer s) throws Exception {
		return s.copy().keepNullProperties().build();
	}

	@Override
	protected Parser applySettings(Parser p) throws Exception {
		return p;
	}

	@Bean(sort=true)
	public static class BeanWithDynaField {
		public int f1;
		@Beanp(name="*")
		public Map<String,Object> f2 = new LinkedHashMap<>();
		public int f3;

		public BeanWithDynaField init() {
			this.f1 = 1;
			this.f2 = JsonMap.of("f2a", "a", "f2b", "b");
			this.f3 = 3;
			return this;
		}
	}

	@Bean(sort=true)
	public static class BeanWithDynaMethods {

		private int f1, f3;
		private Map<String,Object> f2 = new LinkedHashMap<>();
		private boolean setterCalled = false;

		public int getF1() {
			return f1;
		}
		public void setF1(int f1) {
			this.f1 = f1;
		}
		public int getF3() {
			return f3;
		}
		public void setF3(int f3) {
			this.f3 = f3;
		}

		@Beanp(name="*")
		public Map<String, Object> xxx() {
			return f2;
		}

		@Beanp(name="*")
		public void setYYY(String name, Object o) {
			setterCalled = true;
			this.f2.put(name, o);
		}

		public BeanWithDynaMethods init() {
			this.f1 = 1;
			this.f2 = JsonMap.of("f2a", "a", "f2b", "b");
			this.f3 = 3;
			return this;
		}
	}

	@Bean(sort=true)
	public static class BeanWithDynaMethodsAndExtraKeys {

		private int f1, f3;
		private Map<String,Object> f2 = new LinkedHashMap<>();
		private boolean setterCalled = false;

		public int getF1() {
			return f1;
		}
		public void setF1(int f1) {
			this.f1 = f1;
		}
		public int getF3() {
			return f3;
		}
		public void setF3(int f3) {
			this.f3 = f3;
		}

		@Beanp(name="*")
		public Object get(String name) {
			return f2.get(name);
		}

		@Beanp(name="*")
		public void set(String name, Object o) {
			setterCalled = true;
			this.f2.put(name, o);
		}

		@Beanp(name="*")
		public Collection<String> getExtraKeys() {
			return f2.keySet();
		}

		public BeanWithDynaMethodsAndExtraKeys init() {
			this.f1 = 1;
			this.f2 = JsonMap.of("f2a", "a", "f2b", "b");
			this.f3 = 3;
			return this;
		}
	}

	@Bean(sort=true)
	public static class BeanWithDynaGetterOnly {

		private int f1, f3;
		private Map<String,Object> f2 = new LinkedHashMap<>();

		public int getF1() {
			return f1;
		}
		public void setF1(int f1) {
			this.f1 = f1;
		}
		public int getF3() {
			return f3;
		}
		public void setF3(int f3) {
			this.f3 = f3;
		}

		@Beanp(name="*")
		public Map<String, Object> xxx() {
			return f2;
		}

		public BeanWithDynaGetterOnly init() {
			this.f1 = 1;
			this.f2 = JsonMap.of("f2a", "a", "f2b", "b");
			this.f3 = 3;
			return this;
		}
	}

	private static Calendar singleDate = new GregorianCalendar(TimeZone.getTimeZone("PST"));
	static {
		singleDate.setTimeInMillis(0);
		singleDate.set(1901, 2, 3, 10, 11, 12);
	}

	@Bean(sort=true)
	public static class BeanWithDynaFieldSwapped {
		@Beanp(name="*")
		@Swap(TemporalCalendarSwap.IsoInstant.class)
		public Map<String,Calendar> f1 = new LinkedHashMap<>();

		public BeanWithDynaFieldSwapped init() {
			this.f1.put("f1a", singleDate);
			return this;
		}
	}

	@Bean(sort=true)
	public static class BeanWithDynaFieldStringList {
		@Beanp(name="*")
		public Map<String,List<String>> f1 = new LinkedHashMap<>();

		public BeanWithDynaFieldStringList init() {
			this.f1.put("f1a", Arrays.asList(new String[]{"foo","bar"}));
			return this;
		}
	}
}