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
package org.apache.juneau.rest.test;

import static org.apache.juneau.rest.annotation.Inherit.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Properties;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testInheritance",
	serializers={InheritanceResource.S1.class,InheritanceResource.S2.class},
	parsers={InheritanceResource.P1.class,InheritanceResource.P2.class},
	encoders={InheritanceResource.E1.class,InheritanceResource.E2.class},
	pojoSwaps={InheritanceResource.F1Swap.class},
	properties={@Property(name="p1",value="v1"), @Property(name="p2",value="v2")}
)
public class InheritanceResource extends RestServlet {
	private static final long serialVersionUID = 1L;

	@RestResource(
		serializers={S3.class,S4.class},
		parsers={P3.class,P4.class},
		encoders={E3.class,E4.class},
		pojoSwaps={F2Swap.class},
		properties={@Property(name="p2",value="v2a"), @Property(name="p3",value="v3"), @Property(name="p4",value="v4")}
	)
	public static class Sub extends InheritanceResource {
		private static final long serialVersionUID = 1L;
	}

	//====================================================================================================
	// Test serializer inheritance.
	//====================================================================================================
	@RestResource(path="/testInheritanceSerializers")
	public static class TestSerializers extends Sub {
		private static final long serialVersionUID = 1L;

		// Should show ['text/s3','text/s4','text/s1','text/s2']
		@RestMethod(
			name="GET",
			path="/test1"
		)
		public Reader test1(RestResponse res) {
			return new StringReader(new ObjectList(res.getSupportedMediaTypes()).toString());
		}

		// Should show ['text/s5']
		@RestMethod(
			name="GET",
			path="/test2",
			serializers=S5.class
		)
		public Reader test2(RestResponse res) {
			return new StringReader(new ObjectList(res.getSupportedMediaTypes()).toString());
		}

		// Should show ['text/s5','text/s3','text/s4','text/s1','text/s2']
		@RestMethod(
			name="GET",
			path="/test3",
			serializers=S5.class,
			serializersInherit=SERIALIZERS
		)
		public Reader test3(RestResponse res) {
			return new StringReader(new ObjectList(res.getSupportedMediaTypes()).toString());
		}
	}

	//====================================================================================================
	// Test parser inheritance.
	//====================================================================================================
	@RestResource(path="/testInheritanceParsers")
	public static class TestParsers extends Sub {
		private static final long serialVersionUID = 1L;

		// Should show ['text/p3','text/p4','text/p1','text/p2']
		@RestMethod(
			name="GET",
			path="/test1"
		)
		public Reader test1(RestRequest req) {
			return new StringReader(new ObjectList(req.getSupportedMediaTypes()).toString());
		}

		// Should show ['text/p5']
		@RestMethod(
			name="GET",
			path="/test2",
			parsers=P5.class
		)
		public Reader test2(RestRequest req) {
			return new StringReader(new ObjectList(req.getSupportedMediaTypes()).toString());
		}

		// Should show ['text/p5','text/p3','text/p4','text/p1','text/p2']
		@RestMethod(
			name="GET",
			path="/test3",
			parsers=P5.class,
			parsersInherit=PARSERS
		)
		public Reader test3(RestRequest req) {
			return new StringReader(new ObjectList(req.getSupportedMediaTypes()).toString());
		}
	}

	//====================================================================================================
	// Test encoder inheritance.
	//====================================================================================================
	@RestResource(path="/testInheritanceEncoders")
	public static class TestEncoders extends Sub {
		private static final long serialVersionUID = 1L;

		// Should show ['e3','e4','e1','e2','identity']
		@RestMethod(name="GET", path="/test")
		public Reader test(RestResponse res) throws RestServletException {
			return new StringReader(new ObjectList(res.getSupportedEncodings()).toString());
		}
	}

	//====================================================================================================
	// Test filter inheritance.
	//====================================================================================================
	@RestResource(path="/testInheritanceTransforms", serializers=JsonSerializer.Simple.class)
	public static class TestTransforms extends Sub {
		private static final long serialVersionUID = 1L;

		// Should show ['F1Swap','F2Swap','Foo3']
		@RestMethod(name="GET", path="/test1")
		public Object[] test1() {
			return new Object[]{new Foo1(), new Foo2(), new Foo3()};
		}

		// Should show ['F1Swap','F2Swap','F3Swap']
		// Inherited serializer already has parent filters applied.
		@RestMethod(name="GET", path="/test2", pojoSwaps=F3Swap.class)
		public Object[] test2() {
			return new Object[]{new Foo1(), new Foo2(), new Foo3()};
		}

		// Should show ['F1Swap','F2Swap','F3Swap']
		@RestMethod(name="GET", path="/test3", pojoSwaps=F3Swap.class, serializersInherit=TRANSFORMS)
		public Object[] test3() {
			return new Object[]{new Foo1(), new Foo2(), new Foo3()};
		}

		// Should show ['Foo1','Foo2','F3Swap']
		// Overriding serializer does not have parent filters applied.
		@RestMethod(name="GET", path="/test4", serializers=JsonSerializer.Simple.class, pojoSwaps=F3Swap.class)
		public Object[] test4() {
			return new Object[]{new Foo1(), new Foo2(), new Foo3()};
		}

		// Should show ['F1Swap','F2Swap','F3Swap']
		// Overriding serializer does have parent filters applied.
		@RestMethod(name="GET", path="/test5", serializers=JsonSerializer.Simple.class, pojoSwaps=F3Swap.class, serializersInherit=TRANSFORMS)
		public Object[] test5() {
			return new Object[]{new Foo1(), new Foo2(), new Foo3()};
		}
	}

	//====================================================================================================
	// Test properties inheritance.
	//====================================================================================================
	@RestResource(path="/testInheritanceProperties", serializers=JsonSerializer.Simple.class)
	public static class TestProperties extends Sub {
		private static final long serialVersionUID = 1L;

		// Should show {p1:'v1',p2:'v2a',p3:'v3',p4:'v4'}
		@RestMethod(name="GET", path="/test1")
		public ObjectMap test1(@Properties ObjectMap properties) {
			return transform(properties);
		}

		// Should show {p1:'v1',p2:'v2a',p3:'v3',p4:'v4a',p5:'v5'} when override is false.
		// Should show {p1:'x',p2:'x',p3:'x',p4:'x',p5:'x'} when override is true.
		@RestMethod(name="GET", path="/test2",
			properties={@Property(name="p4",value="v4a"), @Property(name="p5", value="v5")})
		public ObjectMap test2(@Properties ObjectMap properties, @HasQuery("override") boolean override) {
			if (override) {
				properties.put("p1", "x");
				properties.put("p2", "x");
				properties.put("p3", "x");
				properties.put("p4", "x");
				properties.put("p5", "x");
			}
			return transform(properties);
		}

		private ObjectMap transform(ObjectMap properties) {
			ObjectMap m = new ObjectMap();
			for (Map.Entry<String,Object> e : properties.entrySet()) {
				if (e.getKey().startsWith("p"))
					m.put(e.getKey(), e.getValue());
			}
			return m;
		}
	}

	public static class DummyParser extends ReaderParser {

		public DummyParser() {
			super(PropertyStore.create());
		}

		@Override /* Parser */
		protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
			return null;
		}
	}

	public static class DummySerializer extends WriterSerializer {

		public DummySerializer() {
			super(PropertyStore.create());
		}

		@Override /* Serializer */
		protected void doSerialize(SerializerSession session, SerializerOutput out, Object o) throws Exception {
			out.getWriter().write(o.toString());
		}
	}

	@Consumes("text/p1")
	public static class P1 extends DummyParser{ public P1(PropertyStore ps) {super();} }

	@Consumes("text/p2")
	public static class P2 extends DummyParser{ public P2(PropertyStore ps) {super();} }

	@Consumes("text/p3")
	public static class P3 extends DummyParser{ public P3(PropertyStore ps) {super();} }

	@Consumes("text/p4")
	public static class P4 extends DummyParser{ public P4(PropertyStore ps) {super();} }

	@Consumes("text/p5")
	public static class P5 extends DummyParser{ public P5(PropertyStore ps) {super();} }

	@Produces("text/s1")
	public static class S1 extends DummySerializer{ public S1(PropertyStore ps) {super();} }

	@Produces("text/s2")
	public static class S2 extends DummySerializer{ public S2(PropertyStore ps) {super();} }

	@Produces("text/s3")
	public static class S3 extends DummySerializer{ public S3(PropertyStore ps) {super();} }

	@Produces("text/s4")
	public static class S4 extends DummySerializer{ public S4(PropertyStore ps) {super();} }

	@Produces("text/s5")
	public static class S5 extends DummySerializer{ public S5(PropertyStore ps) {super();} }

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
}
