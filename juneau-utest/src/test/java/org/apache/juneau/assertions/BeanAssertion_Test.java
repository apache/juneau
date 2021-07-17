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

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BeanAssertion_Test {

	private BeanAssertion<?> test(Object value) {
		return assertBean(value).silent();
	}

	public static class A {
		public int f1 = 1, f2 = 2;
	}

	@Test
	public void a01_basic() throws Exception {
		A a = new A();

		assertThrown(()->test((Object)null).exists()).message().is("Value was null.");
		test(a).exists();

		assertCollection(null).isNull();
		assertThrown(()->test(a).isNull()).message().is("Value was not null.");

		test(a).property("f1").asInteger().is(1);
		test(a).property("x").asInteger().isNull();
		assertThrown(()->test((Object)null).property("x")).message().is("Value was null.");

		test(a).extract("f2,f1").asJson().is("{f2:2,f1:1}");
		test(a).extract("x").asJson().is("{}");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test(null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test(null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
		test(null).stdout();
	}
}
