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
public class BooleanAssertion_Test {

	private BooleanAssertion test(Boolean value) {
		return assertBoolean(value).silent();
	}

	@Test
	public void a01_basic() throws Exception {

		assertThrown(()->test((Boolean)null).exists()).message().is("Value was null.");
		test(true).exists();
		test(true).exists();

		assertThrown(()->test(true).isNull()).message().is("Value was not null.");

		test(true).isEqual(true);

		test(true).isTrue();
		assertThrown(()->test(true).isFalse()).message().is("Value was true.");
		test(false).isFalse();
		assertThrown(()->test(false).isTrue()).message().is("Value was false.");

		assertThrown(()->test(true).isEqual(false)).message().contains("Unexpected value.");

		test(true).isNot("true");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test((Boolean)null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test((Boolean)null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
		test((Boolean)null).stdout();
	}
}
