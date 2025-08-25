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
package org.apache.juneau.annotation;

import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;

class Bean_Test extends SimpleTestBase {

	static VarResolverSession vr = VarResolver.create().vars(XVar.class).build().createSession();

	//------------------------------------------------------------------------------------------------------------------
	// @Bean annotation overrides visibility rules on class and constructor.
	//------------------------------------------------------------------------------------------------------------------

	@Bean
	@SuppressWarnings("unused")
	private static class A1 {
		public int f1;

		public static A1 create() {
			var a = new A1();
			a.f1 = 1;
			return a;
		}
	}

	@Test void testBeanAnnotationOverridesPrivate() throws Exception {
		String json = Json5.of(A1.create());
		assertEquals("{f1:1}", json);
		A1 a = Json5.DEFAULT.read(json, A1.class);
		json = Json5.of(a);
		assertEquals("{f1:1}", json);
	}

	@Bean(on="Dummy1")
	@Bean(on="A2")
	@Bean(on="Dummy2")
	private static class A2Config {}

	@SuppressWarnings("unused")
	private static class A2 {
		public int f1;

		public static A2 create() {
			var a = new A2();
			a.f1 = 1;
			return a;
		}
	}
	static ClassInfo a2ci = ClassInfo.of(A2Config.class);

	@Test void testBeanAnnotationOverridesPrivate_usingConfig() throws Exception {
		var al = AnnotationWorkList.of(a2ci.getAnnotationList());
		var js = Json5Serializer.create().apply(al).build();
		var jp = JsonParser.create().apply(al).build();

		String json = js.serialize(A2.create());
		assertEquals("{f1:1}", json);
		A2 a = jp.parse(json, A2.class);
		json = js.serialize(a);
		assertEquals("{f1:1}", json);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Beanc and @Beanp annotations overrides visibility rules on constructors/properties.
	//------------------------------------------------------------------------------------------------------------------

	public static class B1 {

		@Beanp
		private int f1;

		private int f2;

		@Beanp
		private void setF2(int f2) {
			this.f2 = f2;
		}

		@Beanp
		private int getF2() {
			return f2;
		}

		@Beanc
		private B1() {}

		public static B1 create() {
			var b = new B1();
			b.f1 = 1;
			b.f2 = 2;
			return b;
		}
	}

	@Test void testBeanxAnnotationOverridesPrivate() throws Exception {
		String json = Json5.of(B1.create());
		assertEquals("{f1:1,f2:2}", json);
		B1 b = Json5.DEFAULT.read(json, B1.class);
		json = Json5.of(b);
		assertEquals("{f1:1,f2:2}", json);
	}

	@Beanc(on="B2()")
	@Beanp(on="B2.f1")
	@Beanp(on="B2.setF2")
	@Beanp(on="B2.getF2")
	private static class B2Config {}

	@SuppressWarnings("unused")
	public static class B2 {

		private int f1;

		private int f2;

		private void setF2(int f2) {
			this.f2 = f2;
		}

		private int getF2() {
			return f2;
		}

		private B2() {}

		public static B2 create() {
			var b = new B2();
			b.f1 = 1;
			b.f2 = 2;
			return b;
		}
	}
	static ClassInfo b2ci = ClassInfo.of(B2Config.class);

	@Test void testBeanxAnnotationOverridesPrivate_usingConfig() throws Exception {
		var al = AnnotationWorkList.of(b2ci.getAnnotationList());
		var js = Json5Serializer.create().apply(al).build();
		var jp = JsonParser.create().apply(al).build();

		String json = js.serialize(B2.create());
		assertEquals("{f1:1,f2:2}", json);
		B2 b = jp.parse(json, B2.class);
		json = js.serialize(b);
		assertEquals("{f1:1,f2:2}", json);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Bean(on=X,properties=) should override @Bean(properties=)
	//-----------------------------------------------------------------------------------------------------------------

	@Bean(properties="a,b,c", excludeProperties="b")
	static class D1 {
		public int a, b, c, d;

		public static D1 create() {
			var d = new D1();
			d.a = 1;
			d.b = 2;
			d.c = 3;
			d.d = 4;
			return d;
		}
	}

	@Bean(p="a,b,c", xp="b")
	static class D2 {
		public int a, b, c, d;

		public static D2 create() {
			var d = new D2();
			d.a = 1;
			d.b = 2;
			d.c = 3;
			d.d = 4;
			return d;
		}
	}

	@Bean(on="Dummy", p="b,c,d", xp="c")
	@Bean(on="D1", properties="b,c,d", excludeProperties="c")
	@Bean(on="D2", p="b,c,d", xp="c")
	static class DConfig {}

	private static ClassInfo dConfig = ClassInfo.of(DConfig.class);

	@Test void d01_beanPropertiesExcludePropertiesCombined_noBeanConfig() throws Exception {
		String json = Json5.of(D1.create());
		assertEquals("{a:1,c:3}", json);
		D1 x = Json5.DEFAULT.read(json, D1.class);
		json = Json5.of(x);
		assertEquals("{a:1,c:3}", json);
	}

	@Test void d02_beanPXpCombined_noBeanConfig() throws Exception {
		String json = Json5.of(D2.create());
		assertEquals("{a:1,c:3}", json);
		D2 x = Json5.DEFAULT.read(json, D2.class);
		json = Json5.of(x);
		assertEquals("{a:1,c:3}", json);
	}

	@Test void d03_beanPropertiesExcludePropertiesCombined_beanConfigOverride() throws Exception {
		var al = AnnotationWorkList.of(vr, dConfig.getAnnotationList());
		var js = Json5Serializer.create().apply(al).build();
		var jp = JsonParser.create().apply(al).build();

		String json = js.serialize(D1.create());
		assertEquals("{b:2,d:4}", json);
		D1 d = jp.parse(json, D1.class);
		json = js.serialize(d);
		assertEquals("{b:2,d:4}", json);
	}

	@Test void d04_beanPXpCombined_beanConfigOverride() throws Exception {
		var al = AnnotationWorkList.of(vr, dConfig.getAnnotationList());
		var js = Json5Serializer.create().apply(al).build();
		var jp = JsonParser.create().apply(al).build();

		String json = js.serialize(D2.create());
		assertEquals("{b:2,d:4}", json);
		D2 d = jp.parse(json, D2.class);
		json = js.serialize(d);
		assertEquals("{b:2,d:4}", json);
	}

	@Test void d05_beanPropertiesExcludePropertiesCombined_beanContextBuilderOverride() throws Exception {
		Bean ba = BeanAnnotation.create("D1").properties("b,c,d").excludeProperties("c").build();
		var js = Json5Serializer.create().annotations(ba).build();
		var jp = JsonParser.create().annotations(ba).build();

		String json = js.serialize(D1.create());
		assertEquals("{b:2,d:4}", json);
		D1 d = jp.parse(json, D1.class);
		json = js.serialize(d);
		assertEquals("{b:2,d:4}", json);
	}

	@Test void d06_beanPXpCombined_beanContextBuilderOverride() throws Exception {
		Bean ba = BeanAnnotation.create("D2").p("b,c,d").xp("c").build();
		var js = Json5Serializer.create().annotations(ba).build();
		var jp = JsonParser.create().annotations(ba).build();

		String json = js.serialize(D2.create());
		assertEquals("{b:2,d:4}", json);
		D2 d = jp.parse(json, D2.class);
		json = js.serialize(d);
		assertEquals("{b:2,d:4}", json);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @BeanConfig(bpi/bpx) should override @Bean(bpi/bpx)
	//-----------------------------------------------------------------------------------------------------------------

	@Bean(properties="a,b,c")
	static class E1a {
		public int a, b, c, d;
	}

	@Bean(excludeProperties="b")
	static class E1 extends E1a {

		public static E1 create() {
			var e = new E1();
			e.a = 1;
			e.b = 2;
			e.c = 3;
			e.d = 4;
			return e;
		}
	}

	@Bean(p="a,b,c")
	static class E2a {
		public int a, b, c, d;
	}

	@Bean(xp="b")
	static class E2 extends E2a {

		public static E2 create() {
			var e = new E2();
			e.a = 1;
			e.b = 2;
			e.c = 3;
			e.d = 4;
			return e;
		}
	}

	@Bean(on="Dummy", p="b,c,d", xp="c")
	@Bean(on="E1", properties="b,c,d", excludeProperties="c")
	@Bean(on="E2", p="b,c,d", xp="c")
	static class EConfig {}

	private static ClassInfo eConfig = ClassInfo.of(EConfig.class);

	@Test void e01_beanPropertiesExcludePropertiesCombined_multipleBeanAnnotations_noBeanConfig() throws Exception {
		String json = Json5.of(E1.create());
		assertEquals("{a:1,c:3}", json);
		E1 e = Json5.DEFAULT.read(json, E1.class);
		json = Json5.of(e);
		assertEquals("{a:1,c:3}", json);
	}

	@Test void e02_beanPXpCombined_multipleBeanAnnotations_noBeanConfig() throws Exception {
		String json = Json5.of(E2.create());
		assertEquals("{a:1,c:3}", json);
		E2 e = Json5.DEFAULT.read(json, E2.class);
		json = Json5.of(e);
		assertEquals("{a:1,c:3}", json);
	}

	@Test void e03_beanPropertiesExcludePropertiesCombined_multipleBeanAnnotations_beanConfigOverride() throws Exception {
		var al = AnnotationWorkList.of(vr, eConfig.getAnnotationList());
		var js = Json5Serializer.create().apply(al).build();
		var jp = JsonParser.create().apply(al).build();

		String json = js.serialize(E1.create());
		assertEquals("{b:2,d:4}", json);
		E1 e = jp.parse(json, E1.class);
		json = js.serialize(e);
		assertEquals("{b:2,d:4}", json);
	}

	@Test void e04_beanPXpCombined_multipleBeanAnnotations_beanConfigOverride() throws Exception {
		var al = AnnotationWorkList.of(vr, eConfig.getAnnotationList());
		var js = Json5Serializer.create().apply(al).build();
		var jp = JsonParser.create().apply(al).build();

		String json = js.serialize(E2.create());
		assertEquals("{b:2,d:4}", json);
		E2 e = jp.parse(json, E2.class);
		json = js.serialize(e);
		assertEquals("{b:2,d:4}", json);
	}

	@Test void e05_beanPropertiersExcludePropertiesCombined_multipleBeanAnnotations_beanContextBuilderOverride() throws Exception {
		Bean ba = BeanAnnotation.create("E1").properties("b,c,d").excludeProperties("c").build();
		var js = Json5Serializer.create().annotations(ba).build();
		var jp = JsonParser.create().annotations(ba).build();

		String json = js.serialize(E1.create());
		assertEquals("{b:2,d:4}", json);
		E1 e = jp.parse(json, E1.class);
		json = js.serialize(e);
		assertEquals("{b:2,d:4}", json);
	}

	@Test void e06_beanBpiBpxCombined_multipleBeanAnnotations_beanContextBuilderOverride() throws Exception {
		Bean ba = BeanAnnotation.create("E2").p("b,c,d").xp("c").build();
		var js = Json5Serializer.create().annotations(ba).build();
		var jp = JsonParser.create().annotations(ba).build();

		String json = js.serialize(E2.create());
		assertEquals("{b:2,d:4}", json);
		E2 e = jp.parse(json, E2.class);
		json = js.serialize(e);
		assertEquals("{b:2,d:4}", json);
	}
}

