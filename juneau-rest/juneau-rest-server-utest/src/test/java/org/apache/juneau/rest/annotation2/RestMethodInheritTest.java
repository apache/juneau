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
package org.apache.juneau.rest.annotation2;

import java.io.IOException;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.annotation.HasQuery;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests that validate the behavior of @RestMethod(inherit).
 */
@SuppressWarnings({})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestMethodInheritTest {

	//=================================================================================================================
	// Setup classes
	//=================================================================================================================

	public static class DummyParser extends ReaderParser {
		public DummyParser(String...consumes) {
			super(PropertyStore.DEFAULT, consumes);
		}
		@Override /* Parser */
		public ReaderParserSession createSession(ParserSessionArgs args) {
			return new ReaderParserSession(args) {
				@Override /* ParserSession */
				protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
					return null;
				}
			};
		}
	}

	public static class DummySerializer extends WriterSerializer {
		public DummySerializer(String produces) {
			super(PropertyStore.DEFAULT, produces, null);
		}
		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {
				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
					out.getWriter().write(o.toString());
				}
			};
		}
	}

	public static class P1 extends DummyParser{ public P1(PropertyStore ps) {super("text/p1");} }
	public static class P2 extends DummyParser{ public P2(PropertyStore ps) {super("text/p2");} }
	public static class P3 extends DummyParser{ public P3(PropertyStore ps) {super("text/p3");} }
	public static class P4 extends DummyParser{ public P4(PropertyStore ps) {super("text/p4");} }
	public static class P5 extends DummyParser{ public P5(PropertyStore ps) {super("text/p5");} }

	public static class S1 extends DummySerializer{ public S1(PropertyStore ps) {super("text/s1");} }
	public static class S2 extends DummySerializer{ public S2(PropertyStore ps) {super("text/s2");} }
	public static class S3 extends DummySerializer{ public S3(PropertyStore ps) {super("text/s3");} }
	public static class S4 extends DummySerializer{ public S4(PropertyStore ps) {super("text/s4");} }
	public static class S5 extends DummySerializer{ public S5(PropertyStore ps) {super("text/s5");} }

	public static class E1 extends IdentityEncoder {
		@Override public String[] getCodings() {
			return new String[]{"e1"};
		}
	}
	public static class E2 extends IdentityEncoder {
		@Override public String[] getCodings() {
			return new String[]{"e2"};
		}
	}
	public static class E3 extends IdentityEncoder {
		@Override public String[] getCodings() {
			return new String[]{"e3"};
		}
	}
	public static class E4 extends IdentityEncoder {
		@Override public String[] getCodings() {
			return new String[]{"e4"};
		}
	}

	public static class Foo1 {@Override public String toString(){return "Foo1";}}
	public static class Foo2 {@Override public String toString(){return "Foo2";}}
	public static class Foo3 {@Override public String toString(){return "Foo3";}}

	public static class F1Swap extends StringSwap<Foo1> {
		@Override /* PojoSwap */
		public String swap(BeanSession session, Foo1 o) throws SerializeException {
			return "F1";
		}
	}
	public static class F2Swap extends StringSwap<Foo2> {
		@Override /* PojoSwap */
		public String swap(BeanSession session, Foo2 o) throws SerializeException {
			return "F2";
		}
	}
	public static class F3Swap extends StringSwap<Foo3> {
		@Override /* PojoSwap */
		public String swap(BeanSession session, Foo3 o) throws SerializeException {
			return "F3";
		}
	}

	//=================================================================================================================
	// Test serializer inheritance.
	//=================================================================================================================

	@Rest(serializers={S1.class,S2.class})
	public static class A {}

	@Rest(serializers={S3.class,S4.class,Inherit.class})
	public static class A01 extends A {}

	@Rest
	public static class A02 extends A01 {
		@RestMethod(path="/default")
		public ObjectList a01(RestResponse res) {
			// Should show ['text/s3','text/s4','text/s1','text/s2']
			return new ObjectList(res.getSupportedMediaTypes());
		}
		@RestMethod(path="/onMethod", serializers=S5.class)
		public ObjectList a02(RestResponse res) {
			// Should show ['text/s5']
			return new ObjectList(res.getSupportedMediaTypes());
		}
		@RestMethod(path="/onMethodInherit", serializers={S5.class,Inherit.class})
		public ObjectList a03(RestResponse res) {
			// Should show ['text/s5','text/s3','text/s4','text/s1','text/s2']
			return new ObjectList(res.getSupportedMediaTypes());
		}
	}
	static MockRest a = MockRest.build(A02.class, null);

	@Test
	public void a01_serializers_default() throws Exception {
		a.get("/default").execute().assertBody("['text/s3','text/s4','text/s1','text/s2']");
	}
	@Test
	public void a02_serializers_onMethod() throws Exception {
		a.get("/onMethod").execute().assertBody("['text/s5']");
	}
	@Test
	public void a03_serializers_onMethodInherit() throws Exception {
		a.get("/onMethodInherit").execute().assertBody("['text/s5','text/s3','text/s4','text/s1','text/s2']");
	}

	//=================================================================================================================
	// Test parser inheritance.
	//=================================================================================================================

	@Rest(parsers={P1.class,P2.class})
	public static class B {}

	@Rest(parsers={P3.class,P4.class,Inherit.class})
	public static class B01 extends B {}

	@Rest
	public static class B02 extends B01 {
		@RestMethod(path="/default")
		public ObjectList b01(RestRequest req) {
			// Should show ['text/p3','text/p4','text/p1','text/p2']
			return new ObjectList(req.getConsumes());
		}
		@RestMethod(path="/onMethod", parsers=P5.class)
		public ObjectList b02(RestRequest req) {
			// Should show ['text/p5']
			return new ObjectList(req.getConsumes());
		}
		@RestMethod(path="/onMethodInherit", parsers={P5.class,Inherit.class})
		public ObjectList bo3(RestRequest req) {
			// Should show ['text/p5','text/p3','text/p4','text/p1','text/p2']
			return new ObjectList(req.getConsumes());
		}
	}
	static MockRest b = MockRest.build(B02.class, null);

	@Test
	public void b01_parsers_default() throws Exception {
		b.get("/default").execute().assertBody("['text/p3','text/p4','text/p1','text/p2']");
	}
	@Test
	public void b02_parsers_onMethod() throws Exception {
		b.get("/onMethod").execute().assertBody("['text/p5']");
	}
	@Test
	public void b03_parsers_onMethodInherit() throws Exception {
		b.get("/onMethodInherit").execute().assertBody("['text/p5','text/p3','text/p4','text/p1','text/p2']");
	}

	//=================================================================================================================
	// Test filter inheritance.
	//=================================================================================================================

	@Rest(pojoSwaps={F1Swap.class})
	public static class D {}

	@Rest(pojoSwaps={F2Swap.class,Inherit.class})
	public static class D01 extends D {}

	@Rest(serializers=SimpleJsonSerializer.class)
	public static class D02 extends D01 {
		@RestMethod
		public Object[] d01() {
			// Should show ['F1','F2','Foo3']
			return new Object[]{new Foo1(), new Foo2(), new Foo3()};
		}
		@RestMethod(pojoSwaps={F3Swap.class,Inherit.class})
		public Object[] d02() {
			// Should show ['F1','F2','F3']
			return new Object[]{new Foo1(), new Foo2(), new Foo3()};
		}
		@RestMethod(serializers=SimpleJsonSerializer.class, pojoSwaps=F3Swap.class)
		public Object[] d03() {
			// Should show ['Foo1','Foo2','F3']"
			// Overriding serializer does not have parent filters applied.
			return new Object[]{new Foo1(), new Foo2(), new Foo3()};
		}
		@RestMethod(serializers=SimpleJsonSerializer.class, pojoSwaps={F3Swap.class,Inherit.class})
		public Object[] d04() {
			// Should show ['F1','F2','F3']
			return new Object[]{new Foo1(), new Foo2(), new Foo3()};
		}
	}
	static MockRest d = MockRest.build(D02.class);

	@Test
	public void d01_transforms_default() throws Exception {
		d.get("/d01").json().execute().assertBody("['F1','F2','Foo3']");
	}
	@Test
	public void d02_transforms_inheritTransforms() throws Exception {
		d.get("/d02").json().execute().assertBody("['F1','F2','F3']");
	}
	@Test
	public void d03_transforms_overrideSerializer() throws Exception {
		d.get("/d03").json().execute().assertBody("['Foo1','Foo2','F3']");
	}
	@Test
	public void d04_transforms_overrideSerializerInheritTransforms() throws Exception {
		d.get("/d04").json().execute().assertBody("['F1','F2','F3']");
	}

	//=================================================================================================================
	// Test properties inheritance.
	//=================================================================================================================

	@Rest(attrs={"p1:v1","p2:v2"})
	public static class E {}

	@Rest(attrs={"p2:v2a","p3:v3","p4:v4"})
	public static class E01 extends E {}

	@Rest
	public static class E02 extends E01 {
		@RestMethod
		public ObjectMap e01(RequestAttributes attrs) {
			// Should show {p1:'v1',p2:'v2a',p3:'v3',p4:'v4'}
			return transform(attrs);
		}
		@RestMethod(attrs={"p4:v4a","p5:v5"})
		public ObjectMap e02(RequestAttributes attrs, @HasQuery("override") boolean override) {
			// Should show {p1:'v1',p2:'v2a',p3:'v3',p4:'v4a',p5:'v5'} when override is false.
			// Should show {p1:'x',p2:'x',p3:'x',p4:'x',p5:'x'} when override is true.
			if (override) {
				attrs.put("p1", "x");
				attrs.put("p2", "x");
				attrs.put("p3", "x");
				attrs.put("p4", "x");
				attrs.put("p5", "x");
			}
			return transform(attrs);
		}

		private ObjectMap transform(RequestAttributes attrs) {
			ObjectMap m = new ObjectMap();
			for (Map.Entry<String,Object> e : attrs.entrySet()) {
				if (e.getKey().startsWith("p"))
					m.put(e.getKey(), e.getValue());
			}
			return m;
		}
	}
	static MockRest e = MockRest.build(E02.class, null);

	@Test
	public void e01_properties_default() throws Exception {
		e.get("/e01").execute().assertBody("{p1:'v1',p2:'v2a',p3:'v3',p4:'v4'}");
	}
	@Test
	public void e02a_properties_override_false() throws Exception {
		e.get("/e02").execute().assertBody("{p1:'v1',p2:'v2a',p3:'v3',p4:'v4a',p5:'v5'}");
	}
	@Test
	public void e02b_properties_override_true() throws Exception {
		e.get("/e02?override").execute().assertBody("{p1:'x',p2:'x',p3:'x',p4:'x',p5:'x'}");
	}
}
