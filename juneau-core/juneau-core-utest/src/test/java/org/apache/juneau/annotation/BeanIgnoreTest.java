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

import static org.apache.juneau.assertions.ObjectAssertion.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.json.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BeanIgnoreTest {

	//------------------------------------------------------------------------------------------------------------------
	// Test @BeanIgnore on properties
	//------------------------------------------------------------------------------------------------------------------

	public static class A {
		public String getA() {
			return "a";
		}

		@BeanIgnore
		public String getB() {
			return "b";
		}

		public String c = "c";

		@BeanIgnore public String d = "d";
	}

	@Test
	public void testBeanIgnoreOnProperties() throws Exception {
		assertObject(new A()).json().is("{c:'c',a:'a'}");
	}

	@BeanConfig(
		applyBeanIgnore={
			@BeanIgnore(on="Ac.getB"),
			@BeanIgnore(on="Ac.d")
		}
	)
	public static class Ac {
		public String getA() {
			return "a";
		}

		public String getB() {
			return "b";
		}

		public String c = "c";

		public String d = "d";
	}

	@Test
	public void testBeanIgnoreOnProperties_usingConfig() throws Exception {
		assertObject(new Ac()).serialized(SimpleJsonSerializer.DEFAULT.builder().applyAnnotations(Ac.class).build()).is("{c:'c',a:'a'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test @BeanIgnore on classes
	//------------------------------------------------------------------------------------------------------------------

	@BeanIgnore
	public static class B1 {
		public int f = 1;

		@Override
		public String toString() {
			return "xxx";
		}
	}

	public static class B {
		public int f2 = 2;
		public B1 f3 = new B1();

		public B1 getF4() {
			return new B1();
		}
	}

	@Test
	public void testBeanIgnoreOnBean() throws Exception {
		assertObject(new B()).json().is("{f2:2,f3:'xxx',f4:'xxx'}");
	}

	@BeanConfig(applyBeanIgnore=@BeanIgnore(on="B1c"))
	public static class B1c {
		public int f = 1;

		@Override
		public String toString() {
			return "xxx";
		}
	}

	public static class Bc {
		public int f2 = 2;
		public B1c f3 = new B1c();

		public B1c getF4() {
			return new B1c();
		}
	}

	@Test
	public void testBeanIgnoreOnBean_usingConfig() throws Exception {
		assertObject(new Bc()).serialized(SimpleJsonSerializer.DEFAULT.builder().applyAnnotations(B1c.class).build()).is("{f2:2,f3:'xxx',f4:'xxx'}");
	}
}

