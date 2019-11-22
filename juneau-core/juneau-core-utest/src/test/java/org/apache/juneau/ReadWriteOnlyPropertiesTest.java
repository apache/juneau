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

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;
import org.junit.*;


public class ReadWriteOnlyPropertiesTest {

	//------------------------------------------------------------------------------------------------------------------
	// @Beanp(ro/wo)
	//------------------------------------------------------------------------------------------------------------------

	public static class A {
		@Beanp(ro="true")
		public int f1;

		@Beanp(wo="true")
		public int f2;

		static A create() {
			A x = new A();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}
	}

	@Test
	public void a01_beanpOnPrimitiveFields_serializer() throws Exception {
		assertObjectEquals("{f1:1}", A.create());
	}

	@Test
	public void a02_beanpOnPrimitiveFields_parser() throws Exception {
		A x = SimpleJson.DEFAULT.read("{f1:1,f2:2}", A.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Bean(bpro/bpwo)
	//------------------------------------------------------------------------------------------------------------------

	@Bean(bpro="f1", bpwo="f2")
	public static class B {
		@Beanp(ro="true")
		public int f1;

		@Beanp(wo="true")
		public int f2;

		static B create() {
			B x = new B();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}
	}

	@Test
	public void b01_beanAnnotation_serializer() throws Exception {
		assertObjectEquals("{f1:1}", B.create());
	}

	@Test
	public void b02_beanAnnotationParser() throws Exception {
		B x = SimpleJson.DEFAULT.read("{f1:1,f2:2}", B.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @BeanContext.bpro()/bpwo()
	//------------------------------------------------------------------------------------------------------------------

	public static class C {
		public int f1;
		public int f2;

		static C create() {
			C x = new C();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}
	}

	@Test
	public void c01_beanContext_serializer() throws Exception {
		WriterSerializer sw;

		sw = SimpleJsonSerializer.DEFAULT.builder()
			.bpro(C.class.getName(), "f1")
			.bpwo(C.class.getName(), "f2")
			.build();
		assertEquals("{f1:1}", sw.toString(C.create()));

		sw = SimpleJsonSerializer.DEFAULT.builder()
			.bpro("ReadWriteOnlyPropertiesTest$C", "f1")
			.bpwo("ReadWriteOnlyPropertiesTest$C", "f2")
			.build();
		assertEquals("{f1:1}", sw.toString(C.create()));

		sw = SimpleJsonSerializer.DEFAULT.builder()
			.bpro(C.class, "f1")
			.bpwo(C.class, "f2")
			.build();
		assertEquals("{f1:1}", sw.toString(C.create()));

		sw = SimpleJsonSerializer.DEFAULT.builder()
			.bpro(AMap.create(C.class.getName(), "f1"))
			.bpwo(AMap.create(C.class.getName(), "f2"))
			.build();
		assertEquals("{f1:1}", sw.toString(C.create()));

		sw = SimpleJsonSerializer.DEFAULT.builder()
			.bpro(AMap.create("ReadWriteOnlyPropertiesTest$C", "f1"))
			.bpwo(AMap.create("ReadWriteOnlyPropertiesTest$C", "f2"))
			.build();
		assertEquals("{f1:1}", sw.toString(C.create()));
	}

	@Test
	public void c02_beanAnnotationParser() throws Exception {
		ReaderParser rp;
		C x;

		rp = JsonParser.DEFAULT.builder()
			.bpro(C.class.getName(), "f1")
			.bpwo(C.class.getName(), "f2")
			.build();
		x = rp.parse("{f1:1,f2:2}", C.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);

		rp = JsonParser.DEFAULT.builder()
			.bpro("ReadWriteOnlyPropertiesTest$C", "f1")
			.bpwo("ReadWriteOnlyPropertiesTest$C", "f2")
			.build();
		x = rp.parse("{f1:1,f2:2}", C.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);

		rp = JsonParser.DEFAULT.builder()
			.bpro(C.class, "f1")
			.bpwo(C.class, "f2")
			.build();
		x = rp.parse("{f1:1,f2:2}", C.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);

		rp = JsonParser.DEFAULT.builder()
			.bpro(AMap.create(C.class.getName(), "f1"))
			.bpwo(AMap.create(C.class.getName(), "f2"))
			.build();
		x = rp.parse("{f1:1,f2:2}", C.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);

		rp = JsonParser.DEFAULT.builder()
			.bpro(AMap.create("ReadWriteOnlyPropertiesTest$C", "f1"))
			.bpwo(AMap.create("ReadWriteOnlyPropertiesTest$C", "f2"))
			.build();
		x = rp.parse("{f1:1,f2:2}", C.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Bean(bpro="*")
	//------------------------------------------------------------------------------------------------------------------

	@Bean(bpro="*")
	public static class D {
		public int f1;
		public int f2;

		static D create() {
			D x = new D();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}
	}

	@Test
	public void d01_beanAnnotation_bproAll_serializer() throws Exception {
		assertObjectEquals("{f1:1,f2:2}", D.create());
	}

	@Test
	public void b02_beanAnnotation_bproAll_Parser() throws Exception {
		D x = SimpleJson.DEFAULT.read("{f1:1,f2:2}", D.class);
		assertEquals(0, x.f1);
		assertEquals(0, x.f2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Bean(bpwo="*")
	//------------------------------------------------------------------------------------------------------------------

	@Bean(bpwo="*")
	public static class E {
		public int f1;
		public int f2;

		static E create() {
			E x = new E();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}
	}

	@Test
	public void e01_beanAnnotation_bpwoAll_serializer() throws Exception {
		assertObjectEquals("{}", E.create());
	}

	@Test
	public void e02_beanAnnotation_bpwoAll_Parser() throws Exception {
		E x = SimpleJson.DEFAULT.read("{f1:1,f2:2}", E.class);
		assertEquals(1, x.f1);
		assertEquals(2, x.f2);
	}
}