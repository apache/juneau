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

import org.apache.juneau.collections.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BeanListAssertion_Test {

	private BeanListAssertion<?> test(Object...value) {
		return assertBeanList(value == null ? null : AList.of(value)).silent();
	}

	public static class A {
		public Integer f1, f2;
		public A() {}
		public A(Integer f1, Integer f2) {
			this.f1 = f1;
			this.f2 = f2;
		}
		public static A of(Integer f1, Integer f2) {
			return new A(f1, f2);
		}
	}

	@Test
	public void a01_basic() throws Exception {
		A a = A.of(1,2);

		assertThrown(()->test((Object[])null).exists()).message().is("Value was null.");
		test(a).exists();
	}

	@Test
	public void a02_extract() throws Exception {
		A a1 = A.of(1,2), a2 = A.of(3,4);
		test(a1, a2).extract("f1").asJson().is("[{f1:1},{f1:3}]");
		test(a1, a2).extract("f1,f2").asJson().is("[{f1:1,f2:2},{f1:3,f2:4}]");
		test(a1, a2).extract("f1","f2").asJson().is("[{f1:1,f2:2},{f1:3,f2:4}]");
		test(a1, a2).extract("bad").asJson().is("[{},{}]");
		test(a1, a2).extract((String)null).asJson().is("[{},{}]");
	}

	@Test
	public void a03_property() throws Exception {
		A a1 = A.of(1,2), a2 = A.of(3,4);
		test(a1, a2).property("f1").asJson().is("[1,3]");
		test(a1, a2).property("bad").asJson().is("[null,null]");
		test(a1, a2).property(null).asJson().is("[null,null]");
	}

	@Test
	public void b01_other() throws Exception {
		assertThrown(()->test((Object[])null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test((Object[])null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
		test((Object[])null).stdout();
	}
}
