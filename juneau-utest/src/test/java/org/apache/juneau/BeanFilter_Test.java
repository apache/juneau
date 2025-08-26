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

import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

class BeanFilter_Test extends SimpleTestBase {

	//====================================================================================================
	// Test sub types
	//====================================================================================================
	@Test void a01_subTypes() throws Exception {
		var s = Json5Serializer.DEFAULT.copy().addBeanTypes().addRootType().build();
		var p = JsonParser.DEFAULT;

		var a1 = new A1();
		a1.f1 = "f1";
		a1.fb = new B2();
		((B2)a1.fb).f2 = "f2";
		var r = s.serialize(a1);
		assertEquals("{_type:'A1',f0:'f0',fb:{_type:'B2',f0b:'f0b',f2:'f2'},f1:'f1'}", r);

		var a = p.parse(r, A.class);
		assertTrue(a instanceof A1);
		assertTrue(a.fb instanceof B2);
		assertEquals("f1", ((A1)a).f1);
		assertEquals("f2", ((B2)a.fb).f2);

		// Try out-of-order creation.
		r = "{f0:'f0',f1:'f1',_type:'A1',fb:{f0b:'f0b',f2:'f2',_type:'B2'}}";
		a = p.parse(r, A.class);
		assertTrue(a instanceof A1);
		assertTrue(a.fb instanceof B2);
		assertEquals("f1", ((A1)a).f1);
		assertEquals("f2", ((B2)a.fb).f2);
	}

	@Bean(dictionary={A1.class, A2.class})
	public abstract static class A {
		public String f0 = "f0";
		public B fb;
	}

	@Bean(typeName="A1")
	public static class A1 extends A {
		public String f1;
	}

	@Bean(typeName="A2")
	public static class A2 extends A {
		public String f2;
	}

	@Bean(dictionary={B1.class,B2.class})
	public abstract static class B {
		public String f0b = "f0b";
	}

	@Bean(typeName="B1")
	public static class B1 extends B {
		public String f1;
	}

	@Bean(typeName="B2")
	public static class B2 extends B {
		public String f2;
	}

	@Test void a02_subTypes_usingConfig() throws Exception {
		var s = Json5Serializer.DEFAULT.copy().addBeanTypes().addRootType().applyAnnotations(EConfig.class).build();
		var p = JsonParser.create().applyAnnotations(EConfig2.class).build();

		var e1 = new E1();
		e1.f1 = "f1";
		e1.fb = new F2();
		((F2)e1.fb).f2 = "f2";
		var r = s.serialize(e1);
		assertEquals("{_type:'E1',f0:'f0',fb:{_type:'F2',f0b:'f0b',f2:'f2'},f1:'f1'}", r);

		var e = p.parse(r, E.class);
		assertTrue(e instanceof E1);
		assertTrue(e.fb instanceof F2);
		assertEquals("f1", ((E1)e).f1);
		assertEquals("f2", ((F2)e.fb).f2);

		// Try out-of-order creation.
		r = "{f0:'f0',f1:'f1',_type:'E1',fb:{f0b:'f0b',f2:'f2',_type:'F2'}}";
		e = p.parse(r, E.class);
		assertTrue(e instanceof E1);
		assertTrue(e.fb instanceof F2);
		assertEquals("f1", ((E1)e).f1);
		assertEquals("f2", ((F2)e.fb).f2);
	}

	@Bean(on="E", dictionary={E1.class, E2.class})
	@Bean(on="E1", typeName="E1")
	@Bean(on="E2", typeName="E2")
	@Bean(on="F", dictionary={F1.class,F2.class})
	@Bean(on="F1", typeName="F1")
	@Bean(on="F2", typeName="F2")
	private static class EConfig {}

	private static class EConfig2 extends EConfig {}

	public abstract static class E {
		public String f0 = "f0";
		public F fb;
	}

	public static class E1 extends E {
		public String f1;
	}

	public static class E2 extends E {
		public String f2;
	}

	public abstract static class F {
		public String f0b = "f0b";
	}

	public static class F1 extends F {
		public String f1;
	}

	public static class F2 extends F {
		public String f2;
	}

	//====================================================================================================
	// Test parent class used as filter
	//====================================================================================================
	@Test void a03_parentClassFilter() throws Exception {
		var s = JsonSerializer.create().json5().interfaces(C1.class).build();

		var c1 = new C2();
		var r = s.serialize(c1);
		assertEquals("{f0:'f0'}", r);

		var l = new LinkedList<>();
		l.add(new C2());
		r = s.serialize(l);
		assertEquals("[{f0:'f0'}]", r);
	}

	public static class C1 {
		public String f0 = "f0";
	}

	public static class C2 extends C1 {
		public String f1 = "f1";
	}

	//====================================================================================================
	// Test non-static parent class used as filter
	//====================================================================================================
	@Test void a04_parentClassFilter2() throws Exception {
		var s = JsonSerializer.create().json5().interfaces(D1.class).build();

		var d1 = new D2();
		var r = s.serialize(d1);
		assertEquals("{f0:'f0'}", r);

		var l = new LinkedList<>();
		l.add(new D2());
		r = s.serialize(l);
		assertEquals("[{f0:'f0'}]", r);
	}

	public class D1 {
		public String f0 = "f0";
	}

	public class D2 extends D1 {
		public String f1 = "f1";
	}
}