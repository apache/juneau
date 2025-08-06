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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.api.*;

class ReadWriteOnlyPropertiesTest extends SimpleTestBase {

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
	public void a01_beanpOnPrimitiveFields_serializer() {
		assertJson(A.create(), "{f1:1}");
	}

	@Test
	public void a02_beanpOnPrimitiveFields_parser() {
		A x = Json5.DEFAULT.read("{f1:1,f2:2}", A.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Bean(bpro/bpwo)
	//------------------------------------------------------------------------------------------------------------------

	@Bean(readOnlyProperties="f1", writeOnlyProperties="f2")
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
	public void b01_beanAnnotation_serializer() {
		assertJson(B.create(), "{f1:1}");
	}

	@Test
	public void b02_beanAnnotationParser() {
		Bc x = JsonParser.DEFAULT.copy().applyAnnotations(BcConfig.class).build().parse("{f1:1,f2:2}", Bc.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);
	}

	@Bean(on="Dummy1", readOnlyProperties="f1", writeOnlyProperties="f2")
	@Bean(on="Bc", readOnlyProperties="f1", writeOnlyProperties="f2")
	@Bean(on="Dummy2", readOnlyProperties="f1", writeOnlyProperties="f2")
	@Beanp(on="Bc.f1", ro="true")
	@Beanp(on="Bc.f2", wo="true")
	private static class BcConfig {}

	public static class Bc {
		public int f1;
		public int f2;

		static Bc create() {
			Bc x = new Bc();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}
	}

	@Test
	public void b01_beanAnnotation_serializer_usingConfig() {
		assertJson(B.create(), "{f1:1}");
	}

	@Test
	public void b02_beanAnnotationParser_usingConfig() throws Exception {
		B x = Json5.DEFAULT.read("{f1:1,f2:2}", B.class);
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
	public void c01_beanContext_serializer() {
		WriterSerializer sw;

		sw = Json5Serializer.DEFAULT.copy()
			.beanPropertiesReadOnly(C.class.getName(), "f1")
			.beanPropertiesWriteOnly(C.class.getName(), "f2")
			.build();
		assertEquals("{f1:1}", sw.toString(C.create()));

		sw = Json5Serializer.DEFAULT.copy()
			.beanPropertiesReadOnly("ReadWriteOnlyPropertiesTest$C", "f1")
			.beanPropertiesWriteOnly("ReadWriteOnlyPropertiesTest$C", "f2")
			.build();
		assertEquals("{f1:1}", sw.toString(C.create()));

		sw = Json5Serializer.DEFAULT.copy()
			.beanPropertiesReadOnly(C.class, "f1")
			.beanPropertiesWriteOnly(C.class, "f2")
			.build();
		assertEquals("{f1:1}", sw.toString(C.create()));

		sw = Json5Serializer.DEFAULT.copy()
			.beanPropertiesReadOnly(map(C.class.getName(), "f1"))
			.beanPropertiesWriteOnly(map(C.class.getName(), "f2"))
			.build();
		assertEquals("{f1:1}", sw.toString(C.create()));

		sw = Json5Serializer.DEFAULT.copy()
			.beanPropertiesReadOnly(map("ReadWriteOnlyPropertiesTest$C", "f1"))
			.beanPropertiesWriteOnly(map("ReadWriteOnlyPropertiesTest$C", "f2"))
			.build();
		assertEquals("{f1:1}", sw.toString(C.create()));
	}

	@Test
	public void c02_beanAnnotationParser() throws Exception {
		ReaderParser rp;
		C x;

		rp = JsonParser.DEFAULT.copy()
			.beanPropertiesReadOnly(C.class.getName(), "f1")
			.beanPropertiesWriteOnly(C.class.getName(), "f2")
			.build();
		x = rp.parse("{f1:1,f2:2}", C.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);

		rp = JsonParser.DEFAULT.copy()
			.beanPropertiesReadOnly("ReadWriteOnlyPropertiesTest$C", "f1")
			.beanPropertiesWriteOnly("ReadWriteOnlyPropertiesTest$C", "f2")
			.build();
		x = rp.parse("{f1:1,f2:2}", C.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);

		rp = JsonParser.DEFAULT.copy()
			.beanPropertiesReadOnly(C.class, "f1")
			.beanPropertiesWriteOnly(C.class, "f2")
			.build();
		x = rp.parse("{f1:1,f2:2}", C.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);

		rp = JsonParser.DEFAULT.copy()
			.beanPropertiesReadOnly(map(C.class.getName(), "f1"))
			.beanPropertiesWriteOnly(map(C.class.getName(), "f2"))
			.build();
		x = rp.parse("{f1:1,f2:2}", C.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);

		rp = JsonParser.DEFAULT.copy()
			.beanPropertiesReadOnly(map("ReadWriteOnlyPropertiesTest$C", "f1"))
			.beanPropertiesWriteOnly(map("ReadWriteOnlyPropertiesTest$C", "f2"))
			.build();
		x = rp.parse("{f1:1,f2:2}", C.class);
		assertEquals(0, x.f1);
		assertEquals(2, x.f2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Bean(bpro="*")
	//------------------------------------------------------------------------------------------------------------------

	@Bean(readOnlyProperties="*")
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
	public void d01_beanAnnotation_bproAll_serializer() {
		assertJson(D.create(), "{f1:1,f2:2}");
	}

	@Test
	public void d02_beanAnnotation_bproAll_Parser() {
		D x = Json5.DEFAULT.read("{f1:1,f2:2}", D.class);
		assertEquals(0, x.f1);
		assertEquals(0, x.f2);
	}

	@Bean(on="Dc",readOnlyProperties="*")
	private static class DcConfig {}

	public static class Dc {
		public int f1;
		public int f2;

		static Dc create() {
			Dc x = new Dc();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}
	}

	@Test
	public void d03_beanAnnotation_bproAll_serializer_usingConfig() {
		assertObject(Dc.create()).asString(Json5Serializer.DEFAULT.copy().applyAnnotations(DcConfig.class).build()).is("{f1:1,f2:2}");
	}

	@Test
	public void d04_beanAnnotation_bproAll_Parser_usingConfig() throws Exception {
		Dc x = JsonParser.DEFAULT.copy().applyAnnotations(DcConfig.class).build().parse("{f1:1,f2:2}", Dc.class);
		assertEquals(0, x.f1);
		assertEquals(0, x.f2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Bean(bpwo="*")
	//------------------------------------------------------------------------------------------------------------------

	@Bean(writeOnlyProperties="*")
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
	public void e01_beanAnnotation_bpwoAll_serializer() {
		assertJson(E.create(), "{}");
	}

	@Test
	public void e02_beanAnnotation_bpwoAll_Parser() throws Exception {
		E x = Json5.DEFAULT.read("{f1:1,f2:2}", E.class);
		assertEquals(1, x.f1);
		assertEquals(2, x.f2);
	}

	@Bean(on="Ec", writeOnlyProperties="*")
	private static class EcConfig {}

	public static class Ec {
		public int f1;
		public int f2;

		static Ec create() {
			Ec x = new Ec();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}
	}

	@Test
	public void e03_beanAnnotation_bpwoAll_serializer_usingConfig() {
		assertObject(E.create()).asString(Json5Serializer.DEFAULT.copy().applyAnnotations(EcConfig.class).build()).is("{}");
	}

	@Test
	public void e04_beanAnnotation_bpwoAll_Parser_usingConfig() throws Exception {
		Ec x = JsonParser.DEFAULT.copy().applyAnnotations(EcConfig.class).build().parse("{f1:1,f2:2}", Ec.class);
		assertEquals(1, x.f1);
		assertEquals(2, x.f2);
	}
}