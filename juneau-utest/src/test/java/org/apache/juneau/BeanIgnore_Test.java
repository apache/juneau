/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.json5.*;
import org.junit.jupiter.api.*;

class BeanIgnore_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Test @BeanIgnore on properties (fields, methods).
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

	@Test void a01_beanIgnoreOnProperties() {
		assertJson("{a:'a',c:'c'}", new A());
	}

	@BeanIgnoreApply(on="Ac.getB",value=@BeanIgnore())
	@BeanIgnoreApply(on="Ac.d",value=@BeanIgnore())
	private static class AcConfig {}

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

	@Test void a02_beanIgnoreOnProperties_usingConfig() {
		assertSerialized(new Ac(), Json5Serializer.DEFAULT.copy().applyAnnotations(AcConfig.class).build(), "{a:'a',c:'c'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @BeanIgnore on private field: suppress accessor pair from bean metadata (default + JavaBean introspector).
	//------------------------------------------------------------------------------------------------------------------

	public static class PrivateFieldIgnoredWithAccessors {
		public String visible = "ok";

		@BeanIgnore(ignoreAccessors = true)
		private String foo = "secret";

		public String getVisible() {
			return visible;
		}

		public void setVisible(String value) {
			visible = value;
		}

		public String getFoo() {
			return foo;
		}

		public void setFoo(String value) {
			foo = value;
		}
	}

	@Test void a03_beanIgnoreOnPrivateFieldSuppressesGetterProperty() {
		var bm = MarshallingContext.DEFAULT.getBeanMeta(PrivateFieldIgnoredWithAccessors.class);
		assertFalse(bm.getProperties().containsKey("foo"), () -> "properties: " + bm.getProperties().keySet());
		assertJson("{visible:'ok'}", new PrivateFieldIgnoredWithAccessors());
	}

	@Test void a04_beanIgnoreOnPrivateField_suppressedWithJavaBeanIntrospector() {
		var bc = MarshallingContext.create().useJavaBeanIntrospector().build();
		var s = Json5Serializer.DEFAULT.copy().marshallingContext(bc).build();
		assertFalse(bc.getBeanMeta(PrivateFieldIgnoredWithAccessors.class).getProperties().containsKey("foo"));
		assertEquals("{visible:'ok'}", s.serialize(new PrivateFieldIgnoredWithAccessors()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// @BeanIgnore on TYPE — marks the class as not a bean.
	// The marshaller falls through to other type detection (e.g. toString() via AutoStringSwap).
	//------------------------------------------------------------------------------------------------------------------

	@BeanIgnore
	public static class C1 {
		public int f = 1;

		@Override
		public String toString() {
			return "xxx";
		}
	}

	public static class C {
		public int f2 = 2;
		public C1 f3 = new C1();

		public C1 getF4() {
			return new C1();
		}
	}

	@Test void a05_beanIgnoreOnType_fallsThroughToToString() {
		assertJson("{f2:2,f3:'xxx',f4:'xxx'}", new C());
	}
}
