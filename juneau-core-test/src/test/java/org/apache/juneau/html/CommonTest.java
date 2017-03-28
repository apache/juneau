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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.html.HtmlSerializerContext.*;
import static org.junit.Assert.*;

import java.net.*;
import java.net.URI;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testbeans.*;
import org.apache.juneau.utils.*;
import org.junit.*;

@SuppressWarnings({"serial","javadoc"})
public class CommonTest {

	//====================================================================================================
	// Trim nulls from beans
	//====================================================================================================
	@Test
	public void testTrimNullsFromBeans() throws Exception {
		HtmlSerializerBuilder s = new HtmlSerializerBuilder().sq().addKeyValueTableHeaders(true);
		HtmlParser p = HtmlParser.DEFAULT;
		A t1 = A.create(), t2;

		s.trimNullProperties(false);
		String r = s.build().serialize(t1);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>s1</td><td><null/></td></tr><tr><td>s2</td><td>s2</td></tr></table>", r);
		t2 = p.parse(r, A.class);
		assertEqualObjects(t1, t2);

		s.trimNullProperties(true);
		r = s.build().serialize(t1);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>s2</td><td>s2</td></tr></table>", r);
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
		HtmlSerializerBuilder s = new HtmlSerializerBuilder().sq().addKeyValueTableHeaders(true);
		HtmlParser p = HtmlParser.DEFAULT;
		B t1 = B.create(), t2;
		String r;

		s.trimEmptyMaps(false);
		r = s.build().serialize(t1);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f1</td><td><table><tr><th>key</th><th>value</th></tr></table></td></tr><tr><td>f2</td><td><table><tr><th>key</th><th>value</th></tr><tr><td>f2a</td><td><null/></td></tr><tr><td>f2b</td><td><table><tr><th>key</th><th>value</th></tr><tr><td>s2</td><td>s2</td></tr></table></td></tr></table></td></tr></table>", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t1, t2);

		s.trimEmptyMaps(true);
		r = s.build().serialize(t1);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f2</td><td><table><tr><th>key</th><th>value</th></tr><tr><td>f2a</td><td><null/></td></tr><tr><td>f2b</td><td><table><tr><th>key</th><th>value</th></tr><tr><td>s2</td><td>s2</td></tr></table></td></tr></table></td></tr></table>", r);
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
		HtmlSerializerBuilder s = new HtmlSerializerBuilder().sq().addKeyValueTableHeaders(true);
		HtmlParser p = HtmlParser.DEFAULT;
		C t1 = C.create(), t2;
		String r;

		s.trimEmptyCollections(false);
		r = s.build().serialize(t1);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f1</td><td><ul></ul></td></tr><tr><td>f2</td><td><table _type='array'><tr><th>s2</th></tr><tr><null/></tr><tr><td>s2</td></tr></table></td></tr></table>", r);
		t2 = p.parse(r, C.class);
		assertEqualObjects(t1, t2);

		s.trimEmptyCollections(true);
		r = s.build().serialize(t1);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f2</td><td><table _type='array'><tr><th>s2</th></tr><tr><null/></tr><tr><td>s2</td></tr></table></td></tr></table>", r);
		t2 = p.parse(r, C.class);
		assertNull(t2.f1);
	}

	public static class C {
		public List<A> f1, f2;

		public static C create() {
			C t = new C();
			t.f1 = new AList<A>();
			t.f2 = new AList<A>().append(null).append(A.create());
			return t;
		}
	}

	//====================================================================================================
	// Trim empty arrays
	//====================================================================================================
	@Test
	public void testTrimEmptyArrays() throws Exception {
		HtmlSerializerBuilder s = new HtmlSerializerBuilder().sq().addKeyValueTableHeaders(true);
		HtmlParser p = HtmlParser.DEFAULT;
		D t1 = D.create(), t2;
		String r;

		s.trimEmptyCollections(false);
		r = s.build().serialize(t1);
		assertEquals(
			"<table>"
				+"<tr><th>key</th><th>value</th></tr>"
				+"<tr>"
					+"<td>f1</td>"
					+"<td><ul></ul></td>"
				+"</tr>"
				+"<tr>"
					+"<td>f2</td>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>s2</th></tr>"
							+"<tr><null/></tr>"
							+"<tr><td>s2</td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
			+"</table>",
			r);
		t2 = p.parse(r, D.class);
		assertEqualObjects(t1, t2);

		s.trimEmptyCollections(true);
		r = s.build().serialize(t1);
		assertEquals(
			"<table>"
				+"<tr><th>key</th><th>value</th></tr>"
				+"<tr>"
					+"<td>f2</td>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>s2</th></tr>"
							+"<tr><null/></tr>"
							+"<tr><td>s2</td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
			+"</table>",
			r);
		t2 = p.parse(r, D.class);
		assertNull(t2.f1);
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
		HtmlSerializer s = new HtmlSerializerBuilder().sq().addKeyValueTableHeaders(true).build();
		E1 t = new E1();
		String r;

		r = s.serialize(t);
		assertEquals(
			"<table>"
				+"<tr>"
					+"<th>key</th>"
					+"<th>value</th>"
				+"</tr>"
				+"<tr>"
					+"<td>x1</td>"
					+"<td>"
						+"<table>"
							+"<tr><th>key</th><th>value</th></tr>"
							+"<tr><td>f1</td><td>1</td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
				+"<tr>"
					+"<td>x2</td>"
					+"<td>"
						+"<table>"
							+"<tr><th>key</th><th>value</th></tr>"
							+"<tr><td>f1</td><td>3</td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
				+"<tr>"
					+"<td>x3</td>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>f1</th></tr>"
							+"<tr><td>1</td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
				+"<tr>"
					+"<td>x4</td>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>f1</th></tr>"
							+"<tr><td>1</td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
				+"<tr>"
					+"<td>x5</td>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>f1</th></tr>"
							+"<tr><td><number>5</number></td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
				+"<tr>"
					+"<td>x6</td>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>f1</th></tr>"
							+"<tr><td><number>7</number></td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
			+"</table>",
		r);
		r = s.getSchemaSerializer().serialize(new E1());
		assertTrue(r.indexOf("f2") == -1);
	}

	public static class E1 {
		@BeanProperty(properties="f1") public E2 x1 = new E2();
		@BeanProperty(properties="f1") public Map<String,Integer> x2 = new AMap<String,Integer>().append("f1",3).append("f2",4);
		@BeanProperty(properties="f1") public E2[] x3 = {new E2()};
		@BeanProperty(properties="f1") public List<E2> x4 = new AList<E2>().append(new E2());
		@BeanProperty(properties="f1") public ObjectMap[] x5 = {new ObjectMap().append("f1",5).append("f2",6)};
		@BeanProperty(properties="f1") public List<ObjectMap> x6 = new AList<ObjectMap>().append(new ObjectMap().append("f1",7).append("f2",8));
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
		assertEquals(
			"<table _type='array'>"
				+"<tr><th>x1</th><th>x2</th></tr>"
				+"<tr>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>x2</th></tr>"
							+"<tr><td>2</td></tr>"
						+"</table>"
					+"</td>"
					+"<td>2</td>"
				+"</tr>"
			+"</table>", html);
	}

	public static class F {
		@BeanProperty(properties="x2") public List<F> x1 = new LinkedList<F>();
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
		public URI uri;
		public URI f1;
		public URL f2;
	}

	//====================================================================================================
	// Test URIs with URI_CONTEXT and URI_AUTHORITY
	//====================================================================================================
	@Test
	public void testUris() throws Exception {
		HtmlSerializerBuilder s = new HtmlSerializerBuilder().sq().uriAnchorText(PROPERTY_NAME).useWhitespace(false).addKeyValueTableHeaders(true);
		TestURI t = new TestURI();
		String r;
		String expected;

		s.relativeUriBase(null);
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0</a>"
			+"\n[f1]=<a href='f1/x1'>f1</a>"
			+"\n[f2]=<a href='/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>f3</a>"
			+"\n[f4]=<a href='f4/x4'>f4</a>"
			+"\n[f5]=<a href='/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>f7</a>"
			+"\n[f8]=<a href='f8/x8'>f8</a>"
			+"\n[f9]=<a href='f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.relativeUriBase("");  // Same as null.
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0</a>"
			+"\n[f1]=<a href='f1/x1'>f1</a>"
			+"\n[f2]=<a href='/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>f3</a>"
			+"\n[f4]=<a href='f4/x4'>f4</a>"
			+"\n[f5]=<a href='/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>f7</a>"
			+"\n[f8]=<a href='f8/x8'>f8</a>"
			+"\n[f9]=<a href='f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.relativeUriBase("/cr");
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='/cr/f0/x0'>f0</a>"
			+"\n[f1]=<a href='/cr/f1/x1'>f1</a>"
			+"\n[f2]=<a href='/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>f3</a>"
			+"\n[f4]=<a href='/cr/f4/x4'>f4</a>"
			+"\n[f5]=<a href='/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>f7</a>"
			+"\n[f8]=<a href='/cr/f8/x8'>f8</a>"
			+"\n[f9]=<a href='/cr/f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.relativeUriBase("/cr/");  // Same as above
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='/cr/f0/x0'>f0</a>"
			+"\n[f1]=<a href='/cr/f1/x1'>f1</a>"
			+"\n[f2]=<a href='/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>f3</a>"
			+"\n[f4]=<a href='/cr/f4/x4'>f4</a>"
			+"\n[f5]=<a href='/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>f7</a>"
			+"\n[f8]=<a href='/cr/f8/x8'>f8</a>"
			+"\n[f9]=<a href='/cr/f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.relativeUriBase("/");
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='/f0/x0'>f0</a>"
			+"\n[f1]=<a href='/f1/x1'>f1</a>"
			+"\n[f2]=<a href='/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>f3</a>"
			+"\n[f4]=<a href='/f4/x4'>f4</a>"
			+"\n[f5]=<a href='/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>f7</a>"
			+"\n[f8]=<a href='/f8/x8'>f8</a>"
			+"\n[f9]=<a href='/f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.relativeUriBase(null);

		s.absolutePathUriBase("http://foo");
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0</a>"
			+"\n[f1]=<a href='f1/x1'>f1</a>"
			+"\n[f2]=<a href='http://foo/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>f3</a>"
			+"\n[f4]=<a href='f4/x4'>f4</a>"
			+"\n[f5]=<a href='http://foo/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>f7</a>"
			+"\n[f8]=<a href='f8/x8'>f8</a>"
			+"\n[f9]=<a href='f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.absolutePathUriBase("http://foo/");
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0</a>"
			+"\n[f1]=<a href='f1/x1'>f1</a>"
			+"\n[f2]=<a href='http://foo/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>f3</a>"
			+"\n[f4]=<a href='f4/x4'>f4</a>"
			+"\n[f5]=<a href='http://foo/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>f7</a>"
			+"\n[f8]=<a href='f8/x8'>f8</a>"
			+"\n[f9]=<a href='f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);

		s.absolutePathUriBase("");  // Same as null.
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0</a>"
			+"\n[f1]=<a href='f1/x1'>f1</a>"
			+"\n[f2]=<a href='/f2/x2'>f2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>f3</a>"
			+"\n[f4]=<a href='f4/x4'>f4</a>"
			+"\n[f5]=<a href='/f5/x5'>f5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>f6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>f7</a>"
			+"\n[f8]=<a href='f8/x8'>f8</a>"
			+"\n[f9]=<a href='f9/x9'>f9</a>"
			+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>fa</a>"
			+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&foo=bar'>fd</a>"
			+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&label2=MY_LABEL'>fe</a>"
		;
		assertEquals(expected, r);
	}

	private String strip(String html) {
		return html
			.replace("<table><tr><th>key</th><th>value</th></tr>", "")
			.replace("</table>", "")
			.replace("<tr><td>", "\n[")
			.replace("</td><td>", "]=")
			.replace("</td></tr>", "");
	}

	//====================================================================================================
	// Recursion
	//====================================================================================================
	@Test
	public void testRecursion() throws Exception {
		HtmlSerializerBuilder s = new HtmlSerializerBuilder().sq().addKeyValueTableHeaders(true);

		R1 r1 = new R1();
		R2 r2 = new R2();
		R3 r3 = new R3();
		r1.r2 = r2;
		r2.r3 = r3;
		r3.r1 = r1;

		// No recursion detection
		try {
			s.build().serialize(r1);
			fail("Exception expected!");
		} catch (Exception e) {
			String msg = e.getLocalizedMessage();
			assertTrue(msg.contains("It's recommended you use the SerializerContext.SERIALIZER_detectRecursions setting to help locate the loop."));
		}

		// Recursion detection, no ignore
		s.detectRecursions(true);
		try {
			s.build().serialize(r1);
			fail("Exception expected!");
		} catch (Exception e) {
			String msg = e.getLocalizedMessage();
			assertTrue(msg.contains("[0]<noname>:org.apache.juneau.html.CommonTest$R1"));
			assertTrue(msg.contains("->[1]r2:org.apache.juneau.html.CommonTest$R2"));
			assertTrue(msg.contains("->[2]r3:org.apache.juneau.html.CommonTest$R3"));
			assertTrue(msg.contains("->[3]r1:org.apache.juneau.html.CommonTest$R1"));
		}

		s.ignoreRecursions(true);
		assertEquals(
			"<table><tr><th>key</th><th>value</th></tr><tr><td>name</td><td>foo</td></tr><tr><td>r2</td><td><table><tr><th>key</th><th>value</th></tr><tr><td>name</td><td>bar</td></tr><tr><td>r3</td><td><table><tr><th>key</th><th>value</th></tr><tr><td>name</td><td>baz</td></tr></table></td></tr></table></td></tr></table>",
			s.build().serialize(r1)
		);

		// Make sure this doesn't blow up.
		s.build().getSchemaSerializer().serialize(r1);
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
		WriterSerializer s = new HtmlSerializerBuilder().sq().trimNullProperties(false).sortProperties(true).addKeyValueTableHeaders(true).build();

		J a = new J();
		a.setF1("J");
		a.setF2(100);
		a.setF3(true);
		assertEquals(
			"<table>"
				+"<tr><th>key</th><th>value</th></tr>"
				+"<tr><td>f1</td><td>J</td></tr>"
				+"<tr><td>f2</td><td>100</td></tr>"
				+"<tr><td>f3</td><td>true</td></tr>"
			+"</table>",
			s.serialize(a));
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
