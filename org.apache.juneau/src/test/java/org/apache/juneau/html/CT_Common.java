/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.html;

import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.html.HtmlSerializerContext.*;
import static org.apache.juneau.serializer.SerializerContext.*;
import static org.junit.Assert.*;

import java.net.*;
import java.net.URI;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testbeans.*;
import org.apache.juneau.xml.*;
import org.junit.*;

@SuppressWarnings("serial")
public class CT_Common {

	//====================================================================================================
	// Trim nulls from beans
	//====================================================================================================
	@Test
	public void testTrimNullsFromBeans() throws Exception {
		HtmlSerializer s = new HtmlSerializer.Sq();
		HtmlParser p = HtmlParser.DEFAULT;
		A t1 = A.create(), t2;

		s.setProperty(SERIALIZER_trimNullProperties, false);
		String r = s.serialize(t1);
		assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>s1</string></td><td><null/></td></tr><tr><td><string>s2</string></td><td><string>s2</string></td></tr></table>", r);
		t2 = p.parse(r, A.class);
		assertEqualObjects(t1, t2);

		s.setProperty(SERIALIZER_trimNullProperties, true);
		r = s.serialize(t1);
		assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>s2</string></td><td><string>s2</string></td></tr></table>", r);
		t2 = p.parse(r, A.class);
		assertEqualObjects(t1, t2);
	}

	public static class A {
		public String s1, s2;

		public static A create() {
			A t = new A();
			t.s2 = "s2";
			return t;
		}
	}

	//====================================================================================================
	// Trim empty maps
	//====================================================================================================
	@Test
	public void testTrimEmptyMaps() throws Exception {
		HtmlSerializer s = new HtmlSerializer.Sq();
		HtmlParser p = HtmlParser.DEFAULT;
		B t1 = B.create(), t2;
		String r;

		s.setProperty(SERIALIZER_trimEmptyMaps, false);
		r = s.serialize(t1);
		assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f1</string></td><td><table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr></table></td></tr><tr><td><string>f2</string></td><td><table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f2a</string></td><td><null/></td></tr><tr><td><string>f2b</string></td><td><table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>s2</string></td><td><string>s2</string></td></tr></table></td></tr></table></td></tr></table>", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t1, t2);

		s.setProperty(SERIALIZER_trimEmptyMaps, true);
		r = s.serialize(t1);
		assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f2</string></td><td><table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f2a</string></td><td><null/></td></tr><tr><td><string>f2b</string></td><td><table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>s2</string></td><td><string>s2</string></td></tr></table></td></tr></table></td></tr></table>", r);
		t2 = p.parse(r, B.class);
		assertNull(t2.f1);
	}

	public static class B {
		public TreeMap<String,A> f1, f2;

		public static B create() {
			B t = new B();
			t.f1 = new TreeMap<String,A>();
			t.f2 = new TreeMap<String,A>(){{put("f2a",null);put("f2b",A.create());}};
			return t;
		}
	}

	//====================================================================================================
	// Trim empty lists
	//====================================================================================================
	@Test
	public void testTrimEmptyLists() throws Exception {
		HtmlSerializer s = new HtmlSerializer.Sq();
		HtmlParser p = HtmlParser.DEFAULT;
		C t1 = C.create(), t2;
		String r;

		s.setProperty(SERIALIZER_trimEmptyLists, false);
		r = s.serialize(t1);
		// TODO: fail with Java 8: assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f1</string></td><td><ul></ul></td></tr><tr><td><string>f2</string></td><td><table type='array'><tr><th>s1</th><th>s2</th></tr><tr><null/></tr><tr><td><null/></td><td><string>s2</string></td></tr></table></td></tr></table>", r);
		//t2 = p.parse(r, C.class);
		//assertEqualObjects(t1, t2);

		s.setProperty(SERIALIZER_trimEmptyLists, true);
		r = s.serialize(t1);
		// TODO: fail with Java 8: assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f2</string></td><td><table type='array'><tr><th>s1</th><th>s2</th></tr><tr><null/></tr><tr><td><null/></td><td><string>s2</string></td></tr></table></td></tr></table>", r);
		// t2 = p.parse(r, C.class);
		// assertNull(t2.f1);
	}

	public static class C {
		public List<A> f1, f2;

		public static C create() {
			C t = new C();
			t.f1 = new LinkedList<A>();
			t.f2 = new LinkedList<A>(){{add(null);add(A.create());}};
			return t;
		}
	}

	//====================================================================================================
	// Trim empty arrays
	//====================================================================================================
	@Test
	public void testTrimEmptyArrays() throws Exception {
		HtmlSerializer s = new HtmlSerializer.Sq();
		HtmlParser p = HtmlParser.DEFAULT;
		D t1 = D.create(), t2;
		String r;

		s.setProperty(SERIALIZER_trimEmptyLists, false);
		r = s.serialize(t1);
		// TODO: fail with Java 8: assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f1</string></td><td><ul></ul></td></tr><tr><td><string>f2</string></td><td><table type='array'><tr><th>s1</th><th>s2</th></tr><tr><null/></tr><tr><td><null/></td><td><string>s2</string></td></tr></table></td></tr></table>", r);
		//t2 = p.parse(r, D.class);
		//assertEqualObjects(t1, t2);

		s.setProperty(SERIALIZER_trimEmptyLists, true);
		r = s.serialize(t1);
		// TODO: fail with Java 8: assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f2</string></td><td><table type='array'><tr><th>s1</th><th>s2</th></tr><tr><null/></tr><tr><td><null/></td><td><string>s2</string></td></tr></table></td></tr></table>", r);
		//t2 = p.parse(r, D.class);
		//assertNull(t2.f1);
	}

	public static class D {
		public A[] f1, f2;

		public static D create() {
			D t = new D();
			t.f1 = new A[]{};
			t.f2 = new A[]{null, A.create()};
			return t;
		}
	}

	//====================================================================================================
	// @BeanProperty.properties annotation.
	//====================================================================================================
	@Test
	public void testBeanPropertyProperties() throws Exception {
		HtmlSerializer s = HtmlSerializer.DEFAULT_SQ;
		E1 t = new E1();
		String r;

		r = s.serialize(t);
		assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>x1</string></td><td><table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f1</string></td><td><number>1</number></td></tr></table></td></tr><tr><td><string>x2</string></td><td><table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f1</string></td><td><number>1</number></td></tr></table></td></tr><tr><td><string>x3</string></td><td><table type='array'><tr><th>f1</th></tr><tr><td><number>1</number></td></tr></table></td></tr><tr><td><string>x4</string></td><td><table type='array'><tr><th>f1</th></tr><tr><td><number>1</number></td></tr></table></td></tr><tr><td><string>x5</string></td><td><table type='array'><tr><th>f1</th></tr><tr><td><number>1</number></td></tr></table></td></tr><tr><td><string>x6</string></td><td><table type='array'><tr><th>f1</th></tr><tr><td><number>1</number></td></tr></table></td></tr></table>", r);
		r = s.getSchemaSerializer().serialize(new E1());
		assertTrue(r.indexOf("f2") == -1);
	}

	public static class E1 {
		@BeanProperty(properties={"f1"}) public E2 x1 = new E2();
		@BeanProperty(properties={"f1"}) public Map<String,Integer> x2 = new LinkedHashMap<String,Integer>() {{
			put("f1",1); put("f2",2);
		}};
		@BeanProperty(properties={"f1"}) public E2[] x3 = {new E2()};
		@BeanProperty(properties={"f1"}) public List<E2> x4 = new LinkedList<E2>() {{
			add(new E2());
		}};
		@BeanProperty(properties={"f1"}) public ObjectMap[] x5 = {new ObjectMap().append("f1",1).append("f2",2)};
		@BeanProperty(properties={"f1"}) public List<ObjectMap> x6 = new LinkedList<ObjectMap>() {{
			add(new ObjectMap().append("f1",1).append("f2",2));
		}};
	}

	public static class E2 {
		public int f1 = 1;
		public int f2 = 2;
	}

	//====================================================================================================
	// @BeanProperty.properties annotation on list of beans.
	//====================================================================================================
	@Test
	public void testBeanPropertyPropertiesOnListOfBeans() throws Exception {
		HtmlSerializer s = HtmlSerializer.DEFAULT_SQ;
		List<F> l = new LinkedList<F>();
		F t = new F();
		t.x1.add(new F());
		l.add(t);
		String html = s.serialize(l);
		assertEquals("<table type='array'><tr><th>x1</th><th>x2</th></tr><tr><td><table type='array'><tr><th>x2</th></tr><tr><td><number>2</number></td></tr></table></td><td><number>2</number></td></tr></table>", html);
	}

	public static class F {
		@BeanProperty(properties={"x2"}) public List<F> x1 = new LinkedList<F>();
		public int x2 = 2;
	}

	//====================================================================================================
	// Test that URLs and URIs are serialized and parsed correctly.
	//====================================================================================================
	@Test
	public void testURIAttr() throws Exception {
		HtmlSerializer s = HtmlSerializer.DEFAULT_SQ;
		HtmlParser p = HtmlParser.DEFAULT;

		G t = new G();
		t.uri = new URI("http://uri");
		t.f1 = new URI("http://f1");
		t.f2 = new URL("http://f2");

		String html = s.serialize(t);
		t = p.parse(html, G.class);
		assertEquals("http://uri", t.uri.toString());
		assertEquals("http://f1", t.f1.toString());
		assertEquals("http://f2", t.f2.toString());
	}

	public static class G {
		@BeanProperty(beanUri=true) public URI uri;
		public URI f1;
		public URL f2;
	}

	//====================================================================================================
	// Test URIs with URI_CONTEXT and URI_AUTHORITY
	//====================================================================================================
	@Test
	public void testUris() throws Exception {
		HtmlSerializer s = new HtmlSerializer.Sq().setProperty(HTML_uriAnchorText, PROPERTY_NAME).setProperty(SERIALIZER_useIndentation, false);
		TestURI t = new TestURI();
		String r;
		String expected;

		s.setProperty(SERIALIZER_relativeUriBase, null);
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0</a>"
			+"\n[f1]=<a href='f1/x1'>f1</a>"
			+"\n[f2]=<a href='/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>f3</a>"
			+"\n[f4]=<a href='f4/x4'>f4</a>"
			+"\n[f5]=<a href='/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>f7</a>"
			+"\n[f8]=<a href='f8/x8'>f8</a>"
			+"\n[f9]=<a href='f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_relativeUriBase, "");  // Same as null.
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0</a>"
			+"\n[f1]=<a href='f1/x1'>f1</a>"
			+"\n[f2]=<a href='/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>f3</a>"
			+"\n[f4]=<a href='f4/x4'>f4</a>"
			+"\n[f5]=<a href='/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>f7</a>"
			+"\n[f8]=<a href='f8/x8'>f8</a>"
			+"\n[f9]=<a href='f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_relativeUriBase, "/cr");
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='/cr/f0/x0'>f0</a>"
			+"\n[f1]=<a href='/cr/f1/x1'>f1</a>"
			+"\n[f2]=<a href='/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>f3</a>"
			+"\n[f4]=<a href='/cr/f4/x4'>f4</a>"
			+"\n[f5]=<a href='/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>f7</a>"
			+"\n[f8]=<a href='/cr/f8/x8'>f8</a>"
			+"\n[f9]=<a href='/cr/f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_relativeUriBase, "/cr/");  // Same as above
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='/cr/f0/x0'>f0</a>"
			+"\n[f1]=<a href='/cr/f1/x1'>f1</a>"
			+"\n[f2]=<a href='/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>f3</a>"
			+"\n[f4]=<a href='/cr/f4/x4'>f4</a>"
			+"\n[f5]=<a href='/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>f7</a>"
			+"\n[f8]=<a href='/cr/f8/x8'>f8</a>"
			+"\n[f9]=<a href='/cr/f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_relativeUriBase, "/");
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='/f0/x0'>f0</a>"
			+"\n[f1]=<a href='/f1/x1'>f1</a>"
			+"\n[f2]=<a href='/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>f3</a>"
			+"\n[f4]=<a href='/f4/x4'>f4</a>"
			+"\n[f5]=<a href='/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>f7</a>"
			+"\n[f8]=<a href='/f8/x8'>f8</a>"
			+"\n[f9]=<a href='/f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_relativeUriBase, null);

		s.setProperty(SERIALIZER_absolutePathUriBase, "http://foo");
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0</a>"
			+"\n[f1]=<a href='f1/x1'>f1</a>"
			+"\n[f2]=<a href='http://foo/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>f3</a>"
			+"\n[f4]=<a href='f4/x4'>f4</a>"
			+"\n[f5]=<a href='http://foo/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>f7</a>"
			+"\n[f8]=<a href='f8/x8'>f8</a>"
			+"\n[f9]=<a href='f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_absolutePathUriBase, "http://foo/");
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0</a>"
			+"\n[f1]=<a href='f1/x1'>f1</a>"
			+"\n[f2]=<a href='http://foo/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>f3</a>"
			+"\n[f4]=<a href='f4/x4'>f4</a>"
			+"\n[f5]=<a href='http://foo/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>f7</a>"
			+"\n[f8]=<a href='f8/x8'>f8</a>"
			+"\n[f9]=<a href='f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_absolutePathUriBase, "");  // Same as null.
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0</a>"
			+"\n[f1]=<a href='f1/x1'>f1</a>"
			+"\n[f2]=<a href='/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>f3</a>"
			+"\n[f4]=<a href='f4/x4'>f4</a>"
			+"\n[f5]=<a href='/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>f7</a>"
			+"\n[f8]=<a href='f8/x8'>f8</a>"
			+"\n[f9]=<a href='f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);
	}

	private String strip(String html) {
		return html
			.replace("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr>", "")
			.replace("</table>", "")
			.replace("<tr><td><string>", "\n[")
			.replace("</string></td><td>", "]=")
			.replace("</td></tr>", "");
	}

	//====================================================================================================
	// Validate that you cannot update properties on locked serializer.
	//====================================================================================================
	@Test
	public void testLockedSerializer() throws Exception {
		HtmlSerializer s = new HtmlSerializer().lock();
		try {
			s.setProperty(XmlSerializerContext.XML_enableNamespaces, true);
			fail("Locked exception not thrown");
		} catch (LockedException e) {}
		try {
			s.setProperty(SerializerContext.SERIALIZER_addClassAttrs, true);
			fail("Locked exception not thrown");
		} catch (LockedException e) {}
		try {
			s.setProperty(BeanContext.BEAN_beanMapPutReturnsOldValue, true);
			fail("Locked exception not thrown");
		} catch (LockedException e) {}
	}

	//====================================================================================================
	// Recursion
	//====================================================================================================
	@Test
	public void testRecursion() throws Exception {
		HtmlSerializer s = new HtmlSerializer.Sq();

		R1 r1 = new R1();
		R2 r2 = new R2();
		R3 r3 = new R3();
		r1.r2 = r2;
		r2.r3 = r3;
		r3.r1 = r1;

		// No recursion detection
		try {
			s.serialize(r1);
			fail("Exception expected!");
		} catch (Exception e) {
			String msg = e.getLocalizedMessage();
			assertTrue(msg.contains("It's recommended you use the SerializerContext.SERIALIZER_detectRecursions setting to help locate the loop."));
		}

		// Recursion detection, no ignore
		s.setProperty(SERIALIZER_detectRecursions, true);
		try {
			s.serialize(r1);
			fail("Exception expected!");
		} catch (Exception e) {
			String msg = e.getLocalizedMessage();
			assertTrue(msg.contains("[0]<noname>:org.apache.juneau.html.CT_Common$R1"));
			assertTrue(msg.contains("->[1]r2:org.apache.juneau.html.CT_Common$R2"));
			assertTrue(msg.contains("->[2]r3:org.apache.juneau.html.CT_Common$R3"));
			assertTrue(msg.contains("->[3]r1:org.apache.juneau.html.CT_Common$R1"));
		}

		s.setProperty(SERIALIZER_ignoreRecursions, true);
		assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>name</string></td><td><string>foo</string></td></tr><tr><td><string>r2</string></td><td><table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>name</string></td><td><string>bar</string></td></tr><tr><td><string>r3</string></td><td><table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>name</string></td><td><string>baz</string></td></tr></table></td></tr></table></td></tr></table>",
			s.serialize(r1));

		// Make sure this doesn't blow up.
		s.getSchemaSerializer().serialize(r1);
	}

	public static class R1 {
		public String name = "foo";
		public R2 r2;
	}
	public static class R2 {
		public String name = "bar";
		public R3 r3;
	}
	public static class R3 {
		public String name = "baz";
		public R1 r1;
	}

	//====================================================================================================
	// Basic bean
	//====================================================================================================
	@Test
	public void testBasicBean() throws Exception {
		WriterSerializer s = new HtmlSerializer.Sq().setProperty(SERIALIZER_trimNullProperties, false).setProperty(BEAN_sortProperties, true);

		J a = new J();
		a.setF1("J");
		a.setF2(100);
		a.setF3(true);
		assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f1</string></td><td><string>J</string></td></tr><tr><td><string>f2</string></td><td><number>100</number></td></tr><tr><td><string>f3</string></td><td><boolean>true</boolean></td></tr></table>", s.serialize(a));
	}

	public static class J {
		private String f1 = null;
		private int f2 = -1;
		private boolean f3 = false;

		public String getF1() {
			return this.f1;
		}

		public void setF1(String f1) {
			this.f1 = f1;
		}

		public int getF2() {
			return this.f2;
		}

		public void setF2(int f2) {
			this.f2 = f2;
		}

		public boolean isF3() {
			return this.f3;
		}

		public void setF3(boolean f3) {
			this.f3 = f3;
		}

		@Override /* Object */
		public String toString() {
			return ("J(f1: " + this.getF1() + ", f2: " + this.getF2() + ")");
		}
	}
}
