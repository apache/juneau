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
public class ArrayAssertion_Test {

	private <E> ArrayAssertion<E> test(E[] value) {
		return assertArray(value).silent();
	}

	@Test
	public void a01_basic() throws Exception {
		String[] x1={}, x2={"foo","bar"};

		assertThrown(()->test(null).exists()).is("Value was null.");
		test(x1).exists();

		test(null).doesNotExist();
		assertThrown(()->test(x1).doesNotExist()).is("Value was not null.");

		assertThrown(()->test(null).isSize(0)).is("Value was null.");
		test(x1).isSize(0);
		assertThrown(()->test(x1).isSize(2)).is("Array did not have the expected size.\n\tExpect=2.\n\tActual=0.");
		test(x2).isSize(2);
		assertThrown(()->test(x2).isSize(0)).is("Array did not have the expected size.\n\tExpect=0.\n\tActual=2.");

		assertThrown(()->test(null).isEmpty()).is("Value was null.");
		test(x1).isEmpty();
		assertThrown(()->test(x2).isEmpty()).is("Array was not empty.");

		assertThrown(()->test(null).isNotEmpty()).is("Value was null.");
		assertThrown(()->test(x1).isNotEmpty()).is("Array was empty.");
		test(x2).isNotEmpty();

		test(null).item(0).doesNotExist();
		test(x1).item(0).doesNotExist();
		test(x2).item(0).exists();

		test(x2).contains("foo");
		assertThrown(()->test(x2).contains("z")).is("Array did not contain expected value.\n\tExpect=\"z\".\n\tActual=\"[foo, bar]\".");

		test(x1).doesNotContain("foo");
		assertThrown(()->test(x2).doesNotContain("foo")).is("Array contained unexpected value.\n\tUnexpected=\"foo\".\n\tActual=\"[foo, bar]\".");
		assertThrown(()->test(x2).doesNotContain("bar")).is("Array contained unexpected value.\n\tUnexpected=\"bar\".\n\tActual=\"[foo, bar]\".");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		assertThrown(()->test(null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).is("Foo 1");
		test(null).stdout();
	}
}
