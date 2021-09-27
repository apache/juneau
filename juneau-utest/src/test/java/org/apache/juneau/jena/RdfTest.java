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
package org.apache.juneau.jena;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.*;
import org.junit.*;

@SuppressWarnings({"serial"})
@FixMethodOrder(NAME_ASCENDING)
public class RdfTest {

	@Test
	public void testCollectionFormatProperties() throws Exception {
		A a = new A().init(), a2;
		String rdfXml;
		String expected;

		RdfSerializerBuilder s = RdfSerializer.create().xmlabbrev()
			.rdfxml_tab(3)
			.sq()
			.addRootProperty();
		RdfParser p = RdfParser.create().xml().build();

		//-------------------------------------------------------------------------------------------------------------
		// Normal format - Sequence
		//-------------------------------------------------------------------------------------------------------------
		expected =
			"<rdf:RDF a='http://ns/' j='http://www.apache.org/juneau/' jp='http://www.apache.org/juneaubp/' rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"
			+ "\n   <rdf:Description about='http://test/a'>"
			+ "\n      <a:f2>"
			+ "\n         <rdf:Seq>"
			+ "\n            <rdf:li>f2a</rdf:li>"
			+ "\n            <rdf:li>f2b</rdf:li>"
			+ "\n         </rdf:Seq>"
			+ "\n      </a:f2>"
			+ "\n      <a:f3>"
			+ "\n         <rdf:Seq>"
			+ "\n            <rdf:li>1</rdf:li>"
			+ "\n            <rdf:li>2</rdf:li>"
			+ "\n         </rdf:Seq>"
			+ "\n      </a:f3>"
			+ "\n      <j:root>true</j:root>"
			+ "\n   </rdf:Description>"
			+ "\n</rdf:RDF>";
		rdfXml = s.build().serialize(a);
		XmlUtils.assertXmlEquals(expected, rdfXml);

		a2 = p.parse(rdfXml, A.class);
		assertObject(a).isSameJsonAs(a2);

		//-------------------------------------------------------------------------------------------------------------
		// Explicit sequence
		//-------------------------------------------------------------------------------------------------------------
		s.collectionFormat(RdfCollectionFormat.SEQ);
		expected =
			"<rdf:RDF a='http://ns/' j='http://www.apache.org/juneau/' jp='http://www.apache.org/juneaubp/' rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"
			+ "\n   <rdf:Description about='http://test/a'>"
			+ "\n      <a:f2>"
			+ "\n         <rdf:Seq>"
			+ "\n            <rdf:li>f2a</rdf:li>"
			+ "\n            <rdf:li>f2b</rdf:li>"
			+ "\n         </rdf:Seq>"
			+ "\n      </a:f2>"
			+ "\n      <a:f3>"
			+ "\n         <rdf:Seq>"
			+ "\n            <rdf:li>1</rdf:li>"
			+ "\n            <rdf:li>2</rdf:li>"
			+ "\n         </rdf:Seq>"
			+ "\n      </a:f3>"
			+ "\n      <j:root>true</j:root>"
			+ "\n   </rdf:Description>"
			+ "\n</rdf:RDF>";
		rdfXml = s.build().serialize(a);
		XmlUtils.assertXmlEquals(expected, rdfXml);

		a2 = p.parse(rdfXml, A.class);
		assertObject(a).isSameJsonAs(a2);

		//-------------------------------------------------------------------------------------------------------------
		// Bag
		//-------------------------------------------------------------------------------------------------------------
		s.collectionFormat(RdfCollectionFormat.BAG);
		expected =
			"<rdf:RDF a='http://ns/' j='http://www.apache.org/juneau/' jp='http://www.apache.org/juneaubp/' rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"
			+ "\n   <rdf:Description about='http://test/a'>"
			+ "\n      <a:f2>"
			+ "\n         <rdf:Bag>"
			+ "\n            <rdf:li>f2a</rdf:li>"
			+ "\n            <rdf:li>f2b</rdf:li>"
			+ "\n         </rdf:Bag>"
			+ "\n      </a:f2>"
			+ "\n      <a:f3>"
			+ "\n         <rdf:Bag>"
			+ "\n            <rdf:li>1</rdf:li>"
			+ "\n            <rdf:li>2</rdf:li>"
			+ "\n         </rdf:Bag>"
			+ "\n      </a:f3>"
			+ "\n      <j:root>true</j:root>"
			+ "\n   </rdf:Description>"
			+ "\n</rdf:RDF>";
		rdfXml = s.build().serialize(a);
		XmlUtils.assertXmlEquals(expected, rdfXml);

		a2 = p.parse(rdfXml, A.class);
		assertObject(a).isSameJsonAs(a2);

		//-------------------------------------------------------------------------------------------------------------
		// List
		//-------------------------------------------------------------------------------------------------------------
		s.collectionFormat(RdfCollectionFormat.LIST);
		expected =
			"<rdf:RDF a='http://ns/' j='http://www.apache.org/juneau/' jp='http://www.apache.org/juneaubp/' rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"
			+ "\n   <rdf:Description about='http://test/a'>"
			+ "\n      <a:f2 parseType='Resource'>"
			+ "\n         <rdf:first>f2a</rdf:first>"
			+ "\n         <rdf:rest parseType='Resource'>"
			+ "\n            <rdf:first>f2b</rdf:first>"
			+ "\n            <rdf:rest resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>"
			+ "\n         </rdf:rest>"
			+ "\n      </a:f2>"
			+ "\n      <a:f3 parseType='Resource'>"
			+ "\n         <rdf:first>1</rdf:first>"
			+ "\n         <rdf:rest parseType='Resource'>"
			+ "\n            <rdf:first>2</rdf:first>"
			+ "\n            <rdf:rest resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>"
			+ "\n         </rdf:rest>"
			+ "\n      </a:f3>"
			+ "\n      <j:root>true</j:root>"
			+ "\n   </rdf:Description>"
			+ "\n</rdf:RDF>";
		rdfXml = s.build().serialize(a);
		XmlUtils.assertXmlEquals(expected, rdfXml);

		a2 = p.parse(rdfXml, A.class);
		assertObject(a).isSameJsonAs(a2);

		//-------------------------------------------------------------------------------------------------------------
		// Multi-properties
		//-------------------------------------------------------------------------------------------------------------
		s.collectionFormat(RdfCollectionFormat.MULTI_VALUED);
		expected =
			"<rdf:RDF a='http://ns/' j='http://www.apache.org/juneau/' jp='http://www.apache.org/juneaubp/' rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"
			+ "\n   <rdf:Description about='http://test/a'>"
			+ "\n      <a:f2>f2a</a:f2>"
			+ "\n      <a:f2>f2b</a:f2>"
			+ "\n      <a:f3>1</a:f3>"
			+ "\n      <a:f3>2</a:f3>"
			+ "\n      <j:root>true</j:root>"
			+ "\n   </rdf:Description>"
			+ "\n</rdf:RDF>";
		rdfXml = s.build().serialize(a);
		XmlUtils.assertXmlEquals(expected, rdfXml);

		// Note - Must specify collection format on parser for it to be able to understand this layout.
		p = RdfParser.create().xml().collectionFormat(RdfCollectionFormat.MULTI_VALUED).build();
		a2 = p.parse(rdfXml, A.class);
		assertObject(a).isSameJsonAs(a2);
	}

	@Rdf(prefix="a", namespace="http://ns/")
	public static class A {
		@Rdf(beanUri=true) public URI f1;
		public String[] f2;
		public List<Integer> f3;

		public A init() throws Exception {
			f1 = new URI("http://test/a");
			f2 = new String[]{"f2a","f2b"};
			f3 = Arrays.asList(new Integer[]{1,2});
			return this;
		}
	}

	@Test
	public void testCollectionFormatAnnotations() throws Exception {
		B b = new B().init(), b2;
		String rdfXml, expected;
		RdfSerializerBuilder s = RdfSerializer.create().xmlabbrev()
			.rdfxml_tab(3)
			.sq()
			.addRootProperty();
		RdfParser p = RdfXmlParser.DEFAULT;

		//-------------------------------------------------------------------------------------------------------------
		// Normal format - Sequence
		//-------------------------------------------------------------------------------------------------------------

		expected =
			"<rdf:RDF b='http://ns/' j='http://www.apache.org/juneau/' jp='http://www.apache.org/juneaubp/' rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"
			+ "\n   <rdf:Description about='http://test/b'>"
			+ "\n      <b:f2>"
			+ "\n         <rdf:Seq>"
			+ "\n            <rdf:li>f2a</rdf:li>"
			+ "\n            <rdf:li>f2b</rdf:li>"
			+ "\n         </rdf:Seq>"
			+ "\n      </b:f2>"
			+ "\n      <b:f3>"
			+ "\n         <rdf:Bag>"
			+ "\n            <rdf:li>f3a</rdf:li>"
			+ "\n            <rdf:li>f3b</rdf:li>"
			+ "\n         </rdf:Bag>"
			+ "\n      </b:f3>"
			+ "\n      <b:f4 parseType='Resource'>"
			+ "\n         <rdf:first>f4a</rdf:first>"
			+ "\n         <rdf:rest parseType='Resource'>"
			+ "\n            <rdf:first>f4b</rdf:first>"
			+ "\n            <rdf:rest resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>"
			+ "\n         </rdf:rest>"
			+ "\n      </b:f4>"
			+ "\n      <b:f5>f5a</b:f5>"
			+ "\n      <b:f5>f5b</b:f5>"
			+ "\n      <b:f6>"
			+ "\n         <rdf:Seq>"
			+ "\n            <rdf:li>f6a</rdf:li>"
			+ "\n            <rdf:li>f6b</rdf:li>"
			+ "\n         </rdf:Seq>"
			+ "\n      </b:f6>"
			+ "\n      <b:f7>"
			+ "\n         <rdf:Seq>"
			+ "\n            <rdf:li>f7a</rdf:li>"
			+ "\n            <rdf:li>f7b</rdf:li>"
			+ "\n         </rdf:Seq>"
			+ "\n      </b:f7>"
			+ "\n      <b:f8>"
			+ "\n         <rdf:Bag>"
			+ "\n            <rdf:li>f8a</rdf:li>"
			+ "\n            <rdf:li>f8b</rdf:li>"
			+ "\n         </rdf:Bag>"
			+ "\n      </b:f8>"
			+ "\n      <b:f9 parseType='Resource'>"
			+ "\n         <rdf:first>f9a</rdf:first>"
			+ "\n         <rdf:rest parseType='Resource'>"
			+ "\n            <rdf:first>f9b</rdf:first>"
			+ "\n            <rdf:rest resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>"
			+ "\n         </rdf:rest>"
			+ "\n      </b:f9>"
			+ "\n      <b:fa>faa</b:fa>"
			+ "\n      <b:fa>fab</b:fa>"
			+ "\n      <b:fb>"
			+ "\n         <rdf:Seq>"
			+ "\n            <rdf:li>fba</rdf:li>"
			+ "\n            <rdf:li>fbb</rdf:li>"
			+ "\n         </rdf:Seq>"
			+ "\n      </b:fb>"
			+ "\n      <j:root>true</j:root>"
			+ "\n   </rdf:Description>"
			+ "\n</rdf:RDF>";
		rdfXml = s.build().serialize(b);
		XmlUtils.assertXmlEquals(expected, rdfXml);

		b2 = p.parse(rdfXml, B.class);
		assertObject(b).isSameSortedJsonAs(b2);

		//-------------------------------------------------------------------------------------------------------------
		// Default is Bag - Should only affect DEFAULT properties.
		//-------------------------------------------------------------------------------------------------------------
		s.collectionFormat(RdfCollectionFormat.BAG);
		expected =
			"<rdf:RDF b='http://ns/' j='http://www.apache.org/juneau/' jp='http://www.apache.org/juneaubp/' rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"
			+ "\n   <rdf:Description about='http://test/b'>"
			+ "\n      <b:f2>"
			+ "\n         <rdf:Seq>"
			+ "\n            <rdf:li>f2a</rdf:li>"
			+ "\n            <rdf:li>f2b</rdf:li>"
			+ "\n         </rdf:Seq>"
			+ "\n      </b:f2>"
			+ "\n      <b:f3>"
			+ "\n         <rdf:Bag>"
			+ "\n            <rdf:li>f3a</rdf:li>"
			+ "\n            <rdf:li>f3b</rdf:li>"
			+ "\n         </rdf:Bag>"
			+ "\n      </b:f3>"
			+ "\n      <b:f4 parseType='Resource'>"
			+ "\n         <rdf:first>f4a</rdf:first>"
			+ "\n         <rdf:rest parseType='Resource'>"
			+ "\n            <rdf:first>f4b</rdf:first>"
			+ "\n            <rdf:rest resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>"
			+ "\n         </rdf:rest>"
			+ "\n      </b:f4>"
			+ "\n      <b:f5>f5a</b:f5>"
			+ "\n      <b:f5>f5b</b:f5>"
			+ "\n      <b:f6>"
			+ "\n         <rdf:Bag>"
			+ "\n            <rdf:li>f6a</rdf:li>"
			+ "\n            <rdf:li>f6b</rdf:li>"
			+ "\n         </rdf:Bag>"
			+ "\n      </b:f6>"
			+ "\n      <b:f7>"
			+ "\n         <rdf:Seq>"
			+ "\n            <rdf:li>f7a</rdf:li>"
			+ "\n            <rdf:li>f7b</rdf:li>"
			+ "\n         </rdf:Seq>"
			+ "\n      </b:f7>"
			+ "\n      <b:f8>"
			+ "\n         <rdf:Bag>"
			+ "\n            <rdf:li>f8a</rdf:li>"
			+ "\n            <rdf:li>f8b</rdf:li>"
			+ "\n         </rdf:Bag>"
			+ "\n      </b:f8>"
			+ "\n      <b:f9 parseType='Resource'>"
			+ "\n         <rdf:first>f9a</rdf:first>"
			+ "\n         <rdf:rest parseType='Resource'>"
			+ "\n            <rdf:first>f9b</rdf:first>"
			+ "\n            <rdf:rest resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>"
			+ "\n         </rdf:rest>"
			+ "\n      </b:f9>"
			+ "\n      <b:fa>faa</b:fa>"
			+ "\n      <b:fa>fab</b:fa>"
			+ "\n      <b:fb>"
			+ "\n         <rdf:Bag>"
			+ "\n            <rdf:li>fba</rdf:li>"
			+ "\n            <rdf:li>fbb</rdf:li>"
			+ "\n         </rdf:Bag>"
			+ "\n      </b:fb>"
			+ "\n      <j:root>true</j:root>"
			+ "\n   </rdf:Description>"
			+ "\n</rdf:RDF>";

		rdfXml = s.build().serialize(b);
		XmlUtils.assertXmlEquals(expected, rdfXml);

		b2 = p.parse(rdfXml, B.class);
		assertObject(b).isSameSortedJsonAs(b2);
	}

	@Rdf(prefix="b", namespace="http://ns/")
	public static class B {
		@Rdf(beanUri=true) public URI f1;

		@Rdf(collectionFormat=RdfCollectionFormat.SEQ)
		public String[] f2;

		@Rdf(collectionFormat=RdfCollectionFormat.BAG)
		public String[] f3;

		@Rdf(collectionFormat=RdfCollectionFormat.LIST)
		public String[] f4;

		@Rdf(collectionFormat=RdfCollectionFormat.MULTI_VALUED)
		public String[] f5;

		@Rdf(collectionFormat=RdfCollectionFormat.DEFAULT)
		public String[] f6;

		public BA f7;
		public BB f8;
		public BC f9;
		public BD fa;
		public BE fb;

		public B init() throws Exception {
			f1 = new URI("http://test/b");
			f2 = new String[]{"f2a","f2b"};
			f3 = new String[]{"f3a","f3b"};
			f4 = new String[]{"f4a","f4b"};
			f5 = new String[]{"f5a","f5b"};
			f6 = new String[]{"f6a","f6b"};
			f7 = new BA().append("f7a","f7b");
			f8 = new BB().append("f8a","f8b");
			f9 = new BC().append("f9a","f9b");
			fa = new BD().append("faa","fab");
			fb = new BE().append("fba","fbb");
			return this;
		}
	}

	@Rdf(prefix="ba", namespace="http://ns/", collectionFormat=RdfCollectionFormat.SEQ)
	public static class BA extends ArrayList<String> {
		public BA append(String...s) {
			this.addAll(Arrays.asList(s));
			return this;
		}
	}

	@Rdf(prefix="bb", namespace="http://ns/", collectionFormat=RdfCollectionFormat.BAG)
	public static class BB extends ArrayList<String> {
		public BB append(String...s) {
			this.addAll(Arrays.asList(s));
			return this;
		}
	}

	@Rdf(prefix="bc", namespace="http://ns/", collectionFormat=RdfCollectionFormat.LIST)
	public static class BC extends ArrayList<String> {
		public BC append(String...s) {
			this.addAll(Arrays.asList(s));
			return this;
		}
	}

	@Rdf(prefix="bd", namespace="http://ns/", collectionFormat=RdfCollectionFormat.MULTI_VALUED)
	public static class BD extends ArrayList<String> {
		public BD append(String...s) {
			this.addAll(Arrays.asList(s));
			return this;
		}
	}

	@Rdf(prefix="bd", namespace="http://ns/", collectionFormat=RdfCollectionFormat.DEFAULT)
	public static class BE extends ArrayList<String> {
		public BE append(String...s) {
			this.addAll(Arrays.asList(s));
			return this;
		}
	}

	@Test
	public void testCollectionFormatAnnotationOnClass() throws Exception {
		C c = new C().init(), c2;
		String rdfXml, expected;
		RdfSerializerBuilder s = RdfSerializer.create().xmlabbrev()
			.rdfxml_tab(3)
			.sq()
			.addRootProperty();
		RdfParser p = RdfXmlParser.DEFAULT;

		//-------------------------------------------------------------------------------------------------------------
		// Default on class is Bag - Should only affect DEFAULT properties.
		//-------------------------------------------------------------------------------------------------------------
		s.collectionFormat(RdfCollectionFormat.BAG);
		expected =
			"<rdf:RDF b='http://ns/' j='http://www.apache.org/juneau/' jp='http://www.apache.org/juneaubp/' rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"
			+ "\n   <rdf:Description about='http://test/b'>"
			+ "\n      <b:f2>"
			+ "\n         <rdf:Seq>"
			+ "\n            <rdf:li>f2a</rdf:li>"
			+ "\n            <rdf:li>f2b</rdf:li>"
			+ "\n         </rdf:Seq>"
			+ "\n      </b:f2>"
			+ "\n      <b:f3>"
			+ "\n         <rdf:Bag>"
			+ "\n            <rdf:li>f3a</rdf:li>"
			+ "\n            <rdf:li>f3b</rdf:li>"
			+ "\n         </rdf:Bag>"
			+ "\n      </b:f3>"
			+ "\n      <b:f4 parseType='Resource'>"
			+ "\n         <rdf:first>f4a</rdf:first>"
			+ "\n         <rdf:rest parseType='Resource'>"
			+ "\n            <rdf:first>f4b</rdf:first>"
			+ "\n            <rdf:rest resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>"
			+ "\n         </rdf:rest>"
			+ "\n      </b:f4>"
			+ "\n      <b:f5>f5a</b:f5>"
			+ "\n      <b:f5>f5b</b:f5>"
			+ "\n      <b:f6>"
			+ "\n         <rdf:Bag>"
			+ "\n            <rdf:li>f6a</rdf:li>"
			+ "\n            <rdf:li>f6b</rdf:li>"
			+ "\n         </rdf:Bag>"
			+ "\n      </b:f6>"
			+ "\n      <b:f7>"
			+ "\n         <rdf:Seq>"
			+ "\n            <rdf:li>f7a</rdf:li>"
			+ "\n            <rdf:li>f7b</rdf:li>"
			+ "\n         </rdf:Seq>"
			+ "\n      </b:f7>"
			+ "\n      <b:f8>"
			+ "\n         <rdf:Bag>"
			+ "\n            <rdf:li>f8a</rdf:li>"
			+ "\n            <rdf:li>f8b</rdf:li>"
			+ "\n         </rdf:Bag>"
			+ "\n      </b:f8>"
			+ "\n      <b:f9 parseType='Resource'>"
			+ "\n         <rdf:first>f9a</rdf:first>"
			+ "\n         <rdf:rest parseType='Resource'>"
			+ "\n            <rdf:first>f9b</rdf:first>"
			+ "\n            <rdf:rest resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>"
			+ "\n         </rdf:rest>"
			+ "\n      </b:f9>"
			+ "\n      <b:fa>faa</b:fa>"
			+ "\n      <b:fa>fab</b:fa>"
			+ "\n      <b:fb>"
			+ "\n         <rdf:Bag>"
			+ "\n            <rdf:li>fba</rdf:li>"
			+ "\n            <rdf:li>fbb</rdf:li>"
			+ "\n         </rdf:Bag>"
			+ "\n      </b:fb>"
			+ "\n      <j:root>true</j:root>"
			+ "\n   </rdf:Description>"
			+ "\n</rdf:RDF>";

		rdfXml = s.build().serialize(c);
		XmlUtils.assertXmlEquals(expected, rdfXml);

		c2 = p.parse(rdfXml, C.class);
		assertObject(c).isSameSortedJsonAs(c2);
	}

	@Rdf(collectionFormat=RdfCollectionFormat.BAG)
	public static class C extends B {
		@Override /* B */
		public C init() throws Exception {
			f1 = new URI("http://test/b");
			f2 = new String[]{"f2a","f2b"};
			f3 = new String[]{"f3a","f3b"};
			f4 = new String[]{"f4a","f4b"};
			f5 = new String[]{"f5a","f5b"};
			f6 = new String[]{"f6a","f6b"};
			f7 = new BA().append("f7a","f7b");
			f8 = new BB().append("f8a","f8b");
			f9 = new BC().append("f9a","f9b");
			fa = new BD().append("faa","fab");
			fb = new BE().append("fba","fbb");
			return this;
		}
	}

	@Test
	public void testLooseCollectionsOfBeans() throws Exception {
		WriterSerializer s = RdfSerializer.create().xmlabbrev().looseCollections().build();
		ReaderParser p = RdfParser.create().xml().looseCollections().build();
		String rdfXml, expected;

		List<D> l = new LinkedList<>();
		l.add(new D().init(1));
		l.add(new D().init(2));

		rdfXml = s.serialize(l);
		expected =
			"<rdf:RDF j='http://www.apache.org/juneau/' jp='http://www.apache.org/juneaubp/' rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"
			+ "\n   <rdf:Description about='http://localhost/f1/2'>"
			+ "\n      <jp:f2>f2</jp:f2>"
			+ "\n      <jp:f3 resource='http://localhost/f3/2'/>"
			+ "\n   </rdf:Description>"
			+ "\n   <rdf:Description about='http://localhost/f1/1'>"
			+ "\n      <jp:f2>f2</jp:f2>"
			+ "\n      <jp:f3 resource='http://localhost/f3/1'/>"
			+ "\n   </rdf:Description>"
			+ "\n</rdf:RDF>";
		XmlUtils.assertXmlEquals(expected, rdfXml);

		l = p.parse(rdfXml, LinkedList.class, D.class);
		D[] da = l.toArray(new D[l.size()]);
		rdfXml = s.serialize(da);
		expected =
			"<rdf:RDF j='http://www.apache.org/juneau/' jp='http://www.apache.org/juneaubp/' rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"
			+ "\n   <rdf:Description about='http://localhost/f1/2'>"
			+ "\n      <jp:f2>f2</jp:f2>"
			+ "\n      <jp:f3 resource='http://localhost/f3/2'/>"
			+ "\n   </rdf:Description>"
			+ "\n   <rdf:Description about='http://localhost/f1/1'>"
			+ "\n      <jp:f2>f2</jp:f2>"
			+ "\n      <jp:f3 resource='http://localhost/f3/1'/>"
			+ "\n   </rdf:Description>"
			+ "\n</rdf:RDF>";
		XmlUtils.assertXmlEquals(expected, rdfXml);

		da = p.parse(rdfXml, D[].class);
		rdfXml = s.serialize(da);
		expected =
			"<rdf:RDF j='http://www.apache.org/juneau/' jp='http://www.apache.org/juneaubp/' rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"
			+ "\n   <rdf:Description about='http://localhost/f1/2'>"
			+ "\n      <jp:f2>f2</jp:f2>"
			+ "\n      <jp:f3 resource='http://localhost/f3/2'/>"
			+ "\n   </rdf:Description>"
			+ "\n   <rdf:Description about='http://localhost/f1/1'>"
			+ "\n      <jp:f2>f2</jp:f2>"
			+ "\n      <jp:f3 resource='http://localhost/f3/1'/>"
			+ "\n   </rdf:Description>"
			+ "\n</rdf:RDF>";
		XmlUtils.assertXmlEquals(expected, rdfXml);
	}

	public static class D {
		@Rdf(beanUri=true) public URI f1;
		public String f2;
		public URI f3;

		public D init(int num) throws Exception {
			f1 = new URI("http://localhost/f1/" + num);
			f2 = "f2";
			f3 = new URI("http://localhost/f3/" + num);
			return this;
		}
	}
}
