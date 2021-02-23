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

	@Test
	public void a01_basic() throws Exception {
		Map<String,Integer> x1 = AMap.create(), x2 = AMap.of("a",1,"b",2);

		assertThrown(()->assertMap(null).exists()).is("Value was null.");
		assertMap(x1).exists();

		assertCollection(null).doesNotExist();
		assertThrown(()->assertMap(x1).doesNotExist()).is("Value was not null.");

		assertThrown(()->assertMap(null).isSize(0)).is("Value was null.");
		assertMap(x1).isSize(0);
		assertThrown(()->assertMap(x1).isSize(1)).is("Map did not have the expected size.  Expect=1, Actual=0.");
		assertMap(x2).isSize(2);
		assertThrown(()->assertMap(x2).isSize(0)).is("Map did not have the expected size.  Expect=0, Actual=2.");

		assertThrown(()->assertMap(null).isEmpty()).is("Value was null.");
		assertMap(x1).isEmpty();
		assertThrown(()->assertMap(x2).isEmpty()).is("Map was not empty.");

		assertThrown(()->assertMap(null).isNotEmpty()).is("Value was null.");
		assertThrown(()->assertMap(x1).isNotEmpty()).is("Map was empty.");
		assertMap(x2).isNotEmpty();

		assertMap(x2).value("a").asInteger().is(1);
		assertMap(x2).value("z").asInteger().isNull();
		assertMap((Map<?,?>)null).value("a").asInteger().isNull();

		assertMap(x2).containsKey("a");
		assertThrown(()->assertMap(x2).containsKey("x")).is("Map did not contain expected key.\n\tContents: {a:1,b:2}\n\tExpected key: x");
		assertThrown(()->assertMap((Map<?,?>)null).containsKey("x")).is("Value was null.");

		assertMap(x2).doesNotContainKey("x");
		assertThrown(()->assertMap(x2).doesNotContainKey("a")).is("Map contained unexpected key.\n\tContents: {a:1,b:2}\n\tUnexpected key: a");
		assertThrown(()->assertMap((Map<?,?>)null).containsKey("x")).is("Value was null.");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->MapAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		MapAssertion.create(null).stdout().silent();
	}
}
