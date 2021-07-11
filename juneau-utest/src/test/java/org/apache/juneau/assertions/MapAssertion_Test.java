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

		assertThrown(()->test(null).exists()).is("Value was null.");
		test(x1).exists();

		assertCollection(null).doesNotExist();
		assertThrown(()->test(x1).doesNotExist()).is("Value was not null.");

		assertThrown(()->test(null).isSize(0)).is("Value was null.");
		test(x1).isSize(0);
		assertThrown(()->test(x1).isSize(1)).is("Map did not have the expected size.  Expect=1, Actual=0.");
		test(x2).isSize(2);
		assertThrown(()->test(x2).isSize(0)).is("Map did not have the expected size.  Expect=0, Actual=2.");

		assertThrown(()->test(null).isEmpty()).is("Value was null.");
		test(x1).isEmpty();
		assertThrown(()->test(x2).isEmpty()).is("Map was not empty.");

		assertThrown(()->test(null).isNotEmpty()).is("Value was null.");
		assertThrown(()->test(x1).isNotEmpty()).is("Map was empty.");
		test(x2).isNotEmpty();

		test(x2).value("a").asInteger().is(1);
		test(x2).value("z").asInteger().isNull();
		test((Map<String,Object>)null).value("a").asInteger().isNull();

		test(x2).containsKey("a");
		assertThrown(()->test(x2).containsKey("x")).is("Map did not contain expected key.\n\tContents: {a:1,b:2}\n\tExpected key: x");
		assertThrown(()->test((Map<?,?>)null).containsKey("x")).is("Value was null.");

		test(x2).doesNotContainKey("x");
		assertThrown(()->test(x2).doesNotContainKey("a")).is("Map contained unexpected key.\n\tContents: {a:1,b:2}\n\tUnexpected key: a");
		assertThrown(()->test((Map<?,?>)null).containsKey("x")).is("Value was null.");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		test(null).stdout();
	}
}
