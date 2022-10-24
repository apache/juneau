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
import static org.junit.runners.MethodSorters.*;

import java.beans.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicBeanTests {

	//------------------------------------------------------------------------------------------------------------------
	// Beans with transient fields and methods.
	//------------------------------------------------------------------------------------------------------------------

	public static class A1 {
		public int f1;
		public transient int f2;

		public static A1 create() {
			A1 x = new A1();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}
	}

	@BeanConfig(disableIgnoreTransientFields="true")
	public static class A {}

	@Test
	public void a01_testTransientFieldsIgnored() {
		assertObject(A1.create()).asJson().is("{f1:1}");
	}

	@Test
	public void a02_testTransientFieldsIgnored_overrideSetting() {
		JsonSerializer s = Json5Serializer.DEFAULT.copy().disableIgnoreTransientFields().build();
		assertObject(A1.create()).asString(s).is("{f1:1,f2:2}");
	}

	@Test
	public void a03_testTransientFieldsIgnored_overrideAnnotation() {
		JsonSerializer s = Json5Serializer.DEFAULT.copy().applyAnnotations(A.class).build();
		assertObject(A1.create()).asString(s).is("{f1:1,f2:2}");
	}

	public static class A2 {
		public int f1;

		private int f2;

		public void setF2(int f2) {
			this.f2 = f2;
		}

		@Transient
		public int getF2() {
			return f2;
		}

		public static A2 create() {
			A2 x = new A2();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}
	}

	@Test
	public void a04_testTransientMethodsIgnored() {
		assertObject(A2.create()).asJson().is("{f1:1}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Bean with dyna property
	//------------------------------------------------------------------------------------------------------------------

	public static class B {
		@Beanp(name="*")
		public Map<String,Integer> f1 = new TreeMap<>();

		public static B create() {
			B x = new B();
			x.f1.put("a", 1);
			return x;
		}
	}

	@Test
	public void b01_beanWithDynaProperty() throws Exception {
		assertObject(B.create()).asJson().is("{a:1}");

		B b = JsonParser.DEFAULT.parse("{a:1}", B.class);
		assertObject(b).asJson().is("{a:1}");
	}
}
