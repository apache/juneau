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

import java.util.*;

import org.apache.juneau.collections.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ListAssertion_Test {

	private <E> ListAssertion<E> test(List<E> value) {
		return assertList(value).silent();
	}

	@Test
	public void a01_basic() throws Exception {
		List<String> x1 = AList.create(), x2 = AList.of("a","b");

		assertThrown(()->test(null).exists()).message().is("Value was null.");
		test(x1).exists();

		test(null).isNull();
		assertThrown(()->test(x1).isNull()).message().is("Value was not null.");

		assertThrown(()->test(null).isSize(0)).message().is("Value was null.");
		test(x1).isSize(0);
		assertThrown(()->test(x1).isSize(1)).message().is("Collection did not have the expected size.\n\tExpect=1.\n\tActual=0.");
		test(x2).isSize(2);
		assertThrown(()->test(x2).isSize(0)).message().is("Collection did not have the expected size.\n\tExpect=0.\n\tActual=2.");

		assertThrown(()->test(null).isEmpty()).message().is("Value was null.");
		test(x1).isEmpty();
		assertThrown(()->test(x2).isEmpty()).message().is("Collection was not empty.");

		assertThrown(()->test(null).isNotEmpty()).message().is("Value was null.");
		assertThrown(()->test(x1).isNotEmpty()).message().is("Collection was empty.");
		test(x2).isNotEmpty();

		test(null).item(0).isNull();
		test(x1).item(0).isNull();
		test(x2).item(0).exists();
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test(null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test(null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
		test(null).stdout();
	}
}
