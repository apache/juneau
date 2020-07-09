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
package org.apache.juneau.assertions;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.json.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ObjectAssertion_Test {

	public static class A1 {
		public int getFoo() {
			throw new RuntimeException("foo");
		}
	}

	public static class A2 {
		public int getFoo() {
			return 1;
		}
		public int getBar() {
			return 2;
		}
	}

	@Test
	public void a01_basic() throws Exception {
		assertObject(null).doesNotExist();
		assertObject(1).exists();

		assertThrown(()->assertObject(null).isType(null)).is("Value was null.");
		assertThrown(()->assertObject("foo").isType(null)).is("Parameter cannot be null.");
		assertObject("foo").isType(String.class);
		assertObject("foo").isType(CharSequence.class);
		assertObject("foo").isType(Comparable.class);
		assertThrown(()->assertObject(1).isType(String.class)).is("Unexpected class.\n\tExpected=[java.lang.String]\n\tActual=[java.lang.Integer]");

		assertObject("foo").serialized(JsonSerializer.DEFAULT).is("\"foo\"");
		assertObject(null).serialized(JsonSerializer.DEFAULT).is("null");

		assertThrown(()->assertObject(new A1()).json()).contains("Could not call getValue() on property 'foo'");

		assertObject("foo").json().is("'foo'");
		assertObject(null).serialized(JsonSerializer.DEFAULT).is("null");

		assertObject(new A2()).jsonSorted().is("{bar:2,foo:1}");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->ObjectAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		ObjectAssertion.create(null).stdout().stderr();
	}
}
