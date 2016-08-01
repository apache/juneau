/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import static com.ibm.juno.server.annotation.Inherit.*;

import java.io.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.encoders.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.annotation.Properties;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testInheritance",
	serializers={TestInheritance.S1.class,TestInheritance.S2.class},
	parsers={TestInheritance.P1.class,TestInheritance.P2.class},
	encoders={TestInheritance.E1.class,TestInheritance.E2.class},
	filters={TestInheritance.F1.class},
	properties={@Property(name="p1",value="v1"), @Property(name="p2",value="v2")}
)
public class TestInheritance extends RestServlet {
	private static final long serialVersionUID = 1L;

	@RestResource(
		serializers={S3.class,S4.class},
		parsers={P3.class,P4.class},
		encoders={E3.class,E4.class},
		filters={F2.class},
		properties={@Property(name="p2",value="v2a"), @Property(name="p3",value="v3"), @Property(name="p4",value="v4")}
	)
	public static class Sub extends TestInheritance {
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
	@RestResource(path="/testInheritanceFilters", serializers=JsonSerializer.Simple.class)
	public static class TestFilters extends Sub {
		private static final long serialVersionUID = 1L;

		// Should show ['F1','F2','Foo3']
		@RestMethod(name="GET", path="/test1")
		public Object[] test1() {
			return new Object[]{new Foo1(), new Foo2(), new Foo3()};
		}

		// Should show ['F1','F2','F3']
		// Inherited serializer already has parent filters applied.
		@RestMethod(name="GET", path="/test2", filters=F3.class)
		public Object[] test2() {
			return new Object[]{new Foo1(), new Foo2(), new Foo3()};
		}

		// Should show ['F1','F2','F3']
		@RestMethod(name="GET", path="/test3", filters=F3.class, serializersInherit=FILTERS)
		public Object[] test3() {
			return new Object[]{new Foo1(), new Foo2(), new Foo3()};
		}

		// Should show ['Foo1','Foo2','F3']
		// Overriding serializer does not have parent filters applied.
		@RestMethod(name="GET", path="/test4", serializers=JsonSerializer.Simple.class, filters=F3.class)
		public Object[] test4() {
			return new Object[]{new Foo1(), new Foo2(), new Foo3()};
		}

		// Should show ['F1','F2','F3']
		// Overriding serializer does have parent filters applied.
		@RestMethod(name="GET", path="/test5", serializers=JsonSerializer.Simple.class, filters=F3.class, serializersInherit=FILTERS)
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
			return filter(properties);
		}

		// Should show {p1:'v1',p2:'v2a',p3:'v3',p4:'v4a',p5:'v5'} when override is false.
		// Should show {p1:'x',p2:'x',p3:'x',p4:'x',p5:'x'} when override is true.
		@RestMethod(name="GET", path="/test2",
			properties={@Property(name="p4",value="v4a"), @Property(name="p5", value="v5")})
		public ObjectMap test2(@Properties ObjectMap properties, @HasParam("override") boolean override) {
			if (override) {
				properties.put("p1", "x");
				properties.put("p2", "x");
				properties.put("p3", "x");
				properties.put("p4", "x");
				properties.put("p5", "x");
			}
			return filter(properties);
		}

		private ObjectMap filter(ObjectMap properties) {
			ObjectMap m = new ObjectMap();
			for (Map.Entry<String,Object> e : properties.entrySet()) {
				if (e.getKey().startsWith("p"))
					m.put(e.getKey(), e.getValue());
			}
			return m;
		}
	}

	public static class DummyParser extends ReaderParser {
		@Override /* Parser */
		protected <T> T doParse(Reader in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException {
			return null;
		}
	}

	public static class DummySerializer extends WriterSerializer {
		@Override /* Serializer */
		protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
			out.write(o.toString());
		}
	}

	@Consumes("text/p1")
	public static class P1 extends DummyParser{}

	@Consumes("text/p2")
	public static class P2 extends DummyParser{}

	@Consumes("text/p3")
	public static class P3 extends DummyParser{}

	@Consumes("text/p4")
	public static class P4 extends DummyParser{}

	@Consumes("text/p5")
	public static class P5 extends DummyParser{}

	@Produces("text/s1")
	public static class S1 extends DummySerializer{}

	@Produces("text/s2")
	public static class S2 extends DummySerializer{}

	@Produces("text/s3")
	public static class S3 extends DummySerializer{}

	@Produces("text/s4")
	public static class S4 extends DummySerializer{}

	@Produces("text/s5")
	public static class S5 extends DummySerializer{}

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

	public static class F1 extends PojoFilter<Foo1,String> {
		@Override /* PojoFilter */
		public String filter(Foo1 o) throws SerializeException {
			return "F1";
		}
	}

	public static class F2 extends PojoFilter<Foo2,String> {
		@Override /* PojoFilter */
		public String filter(Foo2 o) throws SerializeException {
			return "F2";
		}
	}

	public static class F3 extends PojoFilter<Foo3,String> {
		@Override /* PojoFilter */
		public String filter(Foo3 o) throws SerializeException {
			return "F3";
		}
	}
}
