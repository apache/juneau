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
public class MapAssertion_Test {

	private <K,V> MapAssertion<K,V> test(Map<K,V> value) {
		return assertMap(value).silent();
	}

	@Test
	public void a01_basic() throws Exception {
		Map<String,Integer> x1 = AMap.create(), x2 = AMap.of("a",1,"b",2);

		assertThrown(()->test(null).exists()).message().is("Value was null.");
		test(x1).exists();

		assertCollection(null).isNull();
		assertThrown(()->test(x1).isNull()).message().is("Value was not null.");

		assertThrown(()->test(null).isSize(0)).message().is("Value was null.");
		test(x1).isSize(0);
		assertThrown(()->test(x1).isSize(1)).message().is("Map did not have the expected size.\n\tExpect=1.\n\tActual=0.");
		test(x2).isSize(2);
		assertThrown(()->test(x2).isSize(0)).message().is("Map did not have the expected size.\n\tExpect=0.\n\tActual=2.");

		assertThrown(()->test(null).isEmpty()).message().is("Value was null.");
		test(x1).isEmpty();
		assertThrown(()->test(x2).isEmpty()).message().is("Map was not empty.");

		assertThrown(()->test(null).isNotEmpty()).message().is("Value was null.");
		assertThrown(()->test(x1).isNotEmpty()).message().is("Map was empty.");
		test(x2).isNotEmpty();

		test(x2).value("a").asInteger().is(1);
		test(x2).value("z").asInteger().isNull();
		test((Map<String,Object>)null).value("a").asInteger().isNull();

		test(x2).containsKey("a");
		assertThrown(()->test(x2).containsKey("x")).message().is("Map did not contain expected key.\n\tExpected key='x'.\n\tValue='{a=1, b=2}'.");
		assertThrown(()->test((Map<?,?>)null).containsKey("x")).message().is("Value was null.");

		test(x2).doesNotContainKey("x");
		assertThrown(()->test(x2).doesNotContainKey("a")).message().is("Map contained unexpected key.\n\tUnexpected key='a'.\n\tValue='{a=1, b=2}'.");
		assertThrown(()->test((Map<?,?>)null).containsKey("x")).message().is("Value was null.");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test(null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test(null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
		test(null).stdout();
	}
}
