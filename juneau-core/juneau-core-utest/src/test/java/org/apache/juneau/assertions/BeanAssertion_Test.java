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

	public static class A {
		public int f1 = 1, f2 = 2;
	}

	@Test
	public void a01_basic() throws Exception {
		A a = new A();

		assertThrown(()->assertBean((Object)null).exists()).is("Value was null.");
		assertBean(a).exists();

		assertCollection(null).doesNotExist();
		assertThrown(()->assertBean(a).doesNotExist()).is("Value was not null.");

		assertBean(a).field("f1").asInteger().is(1);
		assertBean(a).field("x").asInteger().isNull();
		assertThrown(()->assertBean((Object)null).field("x")).is("Value was null.");

		assertBean(a).fields("f2,f1").asJson().is("{f2:2,f1:1}");
		assertBean(a).fields("x").asJson().is("{}");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->BeanAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		BeanAssertion.create(null).stdout().stderr();
	}
}
