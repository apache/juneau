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

import static org.junit.Assert.assertEquals;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.reflect.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BeanAnnotationTest {

	//------------------------------------------------------------------------------------------------------------------
	// @Bean annotation overrides visibility rules on class and constructor.
	//------------------------------------------------------------------------------------------------------------------

	@Bean
	@SuppressWarnings("unused")
	private static class A1 {
		public int f1;

		public static A1 create() {
			A1 a = new A1();
			a.f1 = 1;
			return a;
		}
	}

	@Test
	public void testBeanAnnotationOverridesPrivate() throws Exception {
		String json = SimpleJson.DEFAULT.toString(A1.create());
		assertEquals("{f1:1}", json);
		A1 a = SimpleJson.DEFAULT.read(json, A1.class);
		json = SimpleJson.DEFAULT.toString(a);
		assertEquals("{f1:1}", json);
	}

	@BeanConfig(applyBean=@Bean(on="A2"))
	@SuppressWarnings("unused")
	private static class A2 {
		public int f1;

		public static A2 create() {
			A2 a = new A2();
			a.f1 = 1;
			return a;
		}
	}
	static ClassInfo a2ci = ClassInfo.of(A2.class);

	@Test
	public void testBeanAnnotationOverridesPrivate_usingConfig() throws Exception {
		AnnotationList al = a2ci.getAnnotationList();
		JsonSerializer js = JsonSerializer.create().simple().applyAnnotations(al, null).build();
		JsonParser jp = JsonParser.create().applyAnnotations(al, null).build();

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
			B1 b = new B1();
			b.f1 = 1;
			b.f2 = 2;
			return b;
		}
	}

	@Test
	public void testBeanxAnnotationOverridesPrivate() throws Exception {
		String json = SimpleJson.DEFAULT.toString(B1.create());
		assertEquals("{f1:1,f2:2}", json);
		B1 b = SimpleJson.DEFAULT.read(json, B1.class);
		json = SimpleJson.DEFAULT.toString(b);
		assertEquals("{f1:1,f2:2}", json);
	}

	@BeanConfig(applyBeanc=@Beanc(on="B2()"),applyBeanp={@Beanp(on="B2.f1"),@Beanp(on="B2.setF2"),@Beanp(on="B2.getF2")})
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
			B2 b = new B2();
			b.f1 = 1;
			b.f2 = 2;
			return b;
		}
	}
	static ClassInfo b2ci = ClassInfo.of(B2.class);

	@Test
	public void testBeanxAnnotationOverridesPrivate_usingConfig() throws Exception {
		AnnotationList al = b2ci.getAnnotationList();
		JsonSerializer js = JsonSerializer.create().simple().applyAnnotations(al, null).build();
		JsonParser jp = JsonParser.create().applyAnnotations(al, null).build();

		String json = js.serialize(B2.create());
		assertEquals("{f1:1,f2:2}", json);
		B2 b = jp.parse(json, B2.class);
		json = js.serialize(b);
		assertEquals("{f1:1,f2:2}", json);
	}
}

