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

import java.util.*;

import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests for BeanTraverseContext properties.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class BeanTraversePropertiesComboTest extends ComboRoundTripTest {

	public static class A {
		public int f;

		public A init() {
			f = 1;
			return this;
		}
	}

	public static class B {
		public Object f;

		public B initRecursion() {
			f = this;
			return this;
		}

		public B initA() {
			f = new A().init();
			return this;
		}
	}

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<>(
					"BEANTRAVERSE_initialDepth",
					A.class,
					new A().init()
				)
				.json("{f:1}")
				.jsonT("{f:1}")
				.jsonR("\t\t{\n\t\t\tf: 1\n\t\t}")
				.xml("<object><f>1</f></object>")
				.xmlT("<object><f>1</f></object>")
				.xmlR("\t\t<object>\n\t\t\t<f>1</f>\n\t\t</object>\n")
				.xmlNs("<object><f>1</f></object>")
				.html("<table><tr><td>f</td><td>1</td></tr></table>")
				.htmlT("<table><tr><td>f</td><td>1</td></tr></table>")
				.htmlR("\t\t\t\t<table>\n\t\t\t\t\t<tr>\n\t\t\t\t\t\t<td>f</td>\n\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t</tr>\n\t\t\t\t</table>\n")
				.uon("(f=1)")
				.uonT("(f=1)")
				.uonR("\t\t(\n\t\t\tf=1\n\t\t)")
				.urlEnc("f=1")
				.urlEncT("f=1")
				.urlEncR("\t\tf=1")
				.msgPack("81A16601")
				.msgPackT("81A16601")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>1</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
				.apply(BeanTraverseContext.Builder.class, x -> x.initialDepth(2))
			},
			{ 	/* 1 */
				new ComboInput<>(
					"BEANTRAVERSE_detectRecursions",
					B.class,
					new B().initRecursion()
				)
				.json("x")
				.jsonT("x")
				.jsonR("x")
				.xml("x")
				.xmlT("x")
				.xmlR("x")
				.xmlNs("x")
				.html("x")
				.htmlT("x")
				.htmlR("x")
				.uon("x")
				.uonT("x")
				.uonR("x")
				.urlEnc("x")
				.urlEncT("x")
				.urlEncR("x")
				.msgPack("x")
				.msgPackT("x")
				.rdfXml("x")
				.rdfXmlT("x")
				.rdfXmlR("x")
				.apply(BeanTraverseContext.Builder.class, x -> x.detectRecursions())
				.exceptionMsg("Recursion occurred")
			},
			{ 	/* 2 */
				new ComboInput<>(
					"BEANTRAVERSE_ignoreRecursions",
					B.class,
					new B().initRecursion()
				)
				.json("{}")
				.jsonT("{}")
				.jsonR("{\n}")
				.xml("<object/>")
				.xmlT("<object/>")
				.xmlR("<object/>\n")
				.xmlNs("<object/>")
				.html("<table></table>")
				.htmlT("<table></table>")
				.htmlR("<table>\n</table>\n")
				.uon("()")
				.uonT("()")
				.uonR("(\n)")
				.urlEnc("")
				.urlEncT("")
				.urlEncR("")
				.msgPack("80")
				.msgPackT("80")
				.rdfXml("<rdf:RDF>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n</rdf:RDF>\n")
				.apply(BeanTraverseContext.Builder.class, x -> x.ignoreRecursions())
			},
			{ 	/* 3 */
				new ComboInput<>(
					"BEANTRAVERSE_maxDepth",
					B.class,
					new B().initA()
				)
				.json("{}")
				.jsonT("{}")
				.jsonR("{\n}")
				.xml("<object/>")
				.xmlT("<object/>")
				.xmlR("<object/>\n")
				.xmlNs("<object/>")
				.html("<table></table>")
				.htmlT("<table></table>")
				.htmlR("<table>\n</table>\n")
				.uon("()")
				.uonT("()")
				.uonR("(\n)")
				.urlEnc("")
				.urlEncT("")
				.urlEncR("")
				.msgPack("80")
				.msgPackT("80")
				.rdfXml("<rdf:RDF>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n</rdf:RDF>\n")
				.apply(BeanTraverseContext.Builder.class, x -> x.maxDepth(1))
			},
		});
	}

	public BeanTraversePropertiesComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}
}
