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
package org.apache.juneau.transforms;

import static org.apache.juneau.assertions.Verify.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
@SuppressWarnings({})
public class BuilderComboTest extends ComboRoundTripTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<A>(
					"A",
					A.class,
					new A(null).init()
				)
				.json("{a:1}")
				.jsonT("{a:1}")
				.jsonR("{\n\ta: 1\n}")
				.xml("<object><a>1</a></object>")
				.xmlT("<object><a>1</a></object>")
				.xmlR("<object>\n\t<a>1</a>\n</object>\n")
				.xmlNs("<object><a>1</a></object>")
				.html("<table><tr><td>a</td><td>1</td></tr></table>")
				.htmlT("<table><tr><td>a</td><td>1</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("(a=1)")
				.uonT("(a=1)")
				.uonR("(\n\ta=1\n)")
				.urlEnc("a=1")
				.urlEncT("a=1")
				.urlEncR("a=1")
				.msgPack("81A16101")
				.msgPackT("81A16101")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:a>1</jp:a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(A.class))
				.verify(x -> verify(x.createdByBuilder).isTrue())
			},
			{ 	/* 1 */
				new ComboInput<B>(
					"B",
					B.class,
					new B(null).init()
				)
				.json("{a:1}")
				.jsonT("{a:1}")
				.jsonR("{\n\ta: 1\n}")
				.xml("<object><a>1</a></object>")
				.xmlT("<object><a>1</a></object>")
				.xmlR("<object>\n\t<a>1</a>\n</object>\n")
				.xmlNs("<object><a>1</a></object>")
				.html("<table><tr><td>a</td><td>1</td></tr></table>")
				.htmlT("<table><tr><td>a</td><td>1</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("(a=1)")
				.uonT("(a=1)")
				.uonR("(\n\ta=1\n)")
				.urlEnc("a=1")
				.urlEncT("a=1")
				.urlEncR("a=1")
				.msgPack("81A16101")
				.msgPackT("81A16101")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:a>1</jp:a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(B.class))
				.verify(x -> verify(x.createdByBuilder).isTrue())
			},
			{ 	/* 2 */
				new ComboInput<C>(
					"C",
					C.class,
					new C(null).init()
				)
				.json("{a:1}")
				.jsonT("{a:1}")
				.jsonR("{\n\ta: 1\n}")
				.xml("<object><a>1</a></object>")
				.xmlT("<object><a>1</a></object>")
				.xmlR("<object>\n\t<a>1</a>\n</object>\n")
				.xmlNs("<object><a>1</a></object>")
				.html("<table><tr><td>a</td><td>1</td></tr></table>")
				.htmlT("<table><tr><td>a</td><td>1</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("(a=1)")
				.uonT("(a=1)")
				.uonR("(\n\ta=1\n)")
				.urlEnc("a=1")
				.urlEncT("a=1")
				.urlEncR("a=1")
				.msgPack("81A16101")
				.msgPackT("81A16101")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:a>1</jp:a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(C.class))
				.verify(x -> verify(x.createdByBuilder).isTrue())
			},
			{ 	/* 3 */
				new ComboInput<D>(
					"D",
					D.class,
					new D(null).init()
				)
				.json("{a:1}")
				.jsonT("{a:1}")
				.jsonR("{\n\ta: 1\n}")
				.xml("<object><a>1</a></object>")
				.xmlT("<object><a>1</a></object>")
				.xmlR("<object>\n\t<a>1</a>\n</object>\n")
				.xmlNs("<object><a>1</a></object>")
				.html("<table><tr><td>a</td><td>1</td></tr></table>")
				.htmlT("<table><tr><td>a</td><td>1</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("(a=1)")
				.uonT("(a=1)")
				.uonR("(\n\ta=1\n)")
				.urlEnc("a=1")
				.urlEncT("a=1")
				.urlEncR("a=1")
				.msgPack("81A16101")
				.msgPackT("81A16101")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:a>1</jp:a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(D.class))
				.verify(x -> verify(x.createdByBuilder).isTrue())
			},
			{ 	/* 4 */
				new ComboInput<E>(
					"E",
					E.class,
					new E(null).init()
				)
				.json("{a:1}")
				.jsonT("{a:1}")
				.jsonR("{\n\ta: 1\n}")
				.xml("<object><a>1</a></object>")
				.xmlT("<object><a>1</a></object>")
				.xmlR("<object>\n\t<a>1</a>\n</object>\n")
				.xmlNs("<object><a>1</a></object>")
				.html("<table><tr><td>a</td><td>1</td></tr></table>")
				.htmlT("<table><tr><td>a</td><td>1</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("(a=1)")
				.uonT("(a=1)")
				.uonR("(\n\ta=1\n)")
				.urlEnc("a=1")
				.urlEncT("a=1")
				.urlEncR("a=1")
				.msgPack("81A16101")
				.msgPackT("81A16101")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:a>1</jp:a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(E.class))
				.verify(x -> verify(x.createdByBuilder).isTrue())
			},
			{ 	/* 5 */
				new ComboInput<H>(
					"H",
					H.class,
					new H(null).init()
				)
				.json("{fooBar:1}")
				.jsonT("{fooBar:1}")
				.jsonR("{\n\tfooBar: 1\n}")
				.xml("<object><fooBar>1</fooBar></object>")
				.xmlT("<object><fooBar>1</fooBar></object>")
				.xmlR("<object>\n\t<fooBar>1</fooBar>\n</object>\n")
				.xmlNs("<object><fooBar>1</fooBar></object>")
				.html("<table><tr><td>fooBar</td><td>1</td></tr></table>")
				.htmlT("<table><tr><td>fooBar</td><td>1</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>fooBar</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("(fooBar=1)")
				.uonT("(fooBar=1)")
				.uonR("(\n\tfooBar=1\n)")
				.urlEnc("fooBar=1")
				.urlEncT("fooBar=1")
				.urlEncR("fooBar=1")
				.msgPack("81A6666F6F42617201")
				.msgPackT("81A6666F6F42617201")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:fooBar>1</jp:fooBar>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:fooBar>1</jp:fooBar>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:fooBar>1</jp:fooBar>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(H.class))
				.verify(x -> verify(x.createdByBuilder).isTrue())
			},
		});
	}

	public BuilderComboTest(ComboInput<?> comboInput) {
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

	//-----------------------------------------------------------------------------------------------------------------
	// Typical builder scenario
	//-----------------------------------------------------------------------------------------------------------------

	public static class A {
		public int a;
		boolean createdByBuilder;

		public A(ABuilder x) {
			if (x != null)
				this.a = x.a;
		}

		public A init() {
			a = 1;
			return this;
		}

		public static ABuilder create() {
			return new ABuilder();
		}
	}

	public static class ABuilder {
		public int a;

		public A build() {
			A x = new A(this);
			x.createdByBuilder = true;
			return x;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder detected through POJO constructor.
	//-----------------------------------------------------------------------------------------------------------------
	public static class B {
		public int a;
		boolean createdByBuilder;

		public B(BBuilder x) {
			if (x != null) {
				this.a = x.a;
				createdByBuilder = true;
			}
		}

		public B init() {
			a = 1;
			return this;
		}
	}

	public static class BBuilder implements org.apache.juneau.swap.Builder<B> {
		public int a;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Same as B, but should Builder.build() method.
	//-----------------------------------------------------------------------------------------------------------------

	public static class C {
		public int a;
		boolean createdByBuilder;

		public C(CBuilder x) {
			if (x != null) {
				this.a = x.a;
			}
		}

		public C init() {
			a = 1;
			return this;
		}
	}

	public static class CBuilder implements org.apache.juneau.swap.Builder<B> {
		public int a;

		public C build() {
			C x = new C(this);
			x.createdByBuilder = true;
			return x;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Builder annotation on POJO class.
	//-----------------------------------------------------------------------------------------------------------------

	@org.apache.juneau.annotation.Builder(DBuilder.class)
	public static class D {
		public int a;
		boolean createdByBuilder;

		public D(DBuilder x) {
			if (x != null) {
				this.a = x.a;
				createdByBuilder = true;
			}
		}

		public D init() {
			a = 1;
			return this;
		}
	}

	public static class DBuilder {
		public int a;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Builder annotation on POJO class, but uses build() method on builder.
	//-----------------------------------------------------------------------------------------------------------------

	@org.apache.juneau.annotation.Builder(EBuilder.class)
	public static class E {
		public int a;
		boolean createdByBuilder;

		public E(EBuilder x) {
			if (x != null) {
				this.a = x.a;
			}
		}

		public E init() {
			a = 1;
			return this;
		}
	}

	public static class EBuilder {
		public int a;

		public E build() {
			E x = new E(this);
			x.createdByBuilder = true;
			return x;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder with typical method setters.
	//-----------------------------------------------------------------------------------------------------------------

	public static class H {
		public int fooBar;
		boolean createdByBuilder;

		public H(HBuilder x) {
			if (x != null)
				this.fooBar = x.fooBar;
		}

		public H init() {
			fooBar = 1;
			return this;
		}

		public static HBuilder create() {
			return new HBuilder();
		}
	}

	public static class HBuilder {
		private int fooBar;

		public H build() {
			H x = new H(this);
			x.createdByBuilder = true;
			return x;
		}

		@Beanp
		public HBuilder fooBar(int fooBar) {
			this.fooBar = fooBar;
			return this;
		}
	}
}
