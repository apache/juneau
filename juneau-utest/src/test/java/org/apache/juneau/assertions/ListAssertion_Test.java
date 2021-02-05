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

	@Test
	public void a01_basic() throws Exception {
		List<String> x1 = AList.create(), x2 = AList.of("a","b");

		assertThrown(()->assertList(null).exists()).is("Value was null.");
		assertList(x1).exists();

		assertList(null).doesNotExist();
		assertThrown(()->assertList(x1).doesNotExist()).is("Value was not null.");

		assertThrown(()->assertList(null).isSize(0)).is("Value was null.");
		assertList(x1).isSize(0);
		assertThrown(()->assertList(x1).isSize(1)).is("Collection did not have the expected size.  Expect=1, Actual=0.");
		assertList(x2).isSize(2);
		assertThrown(()->assertList(x2).isSize(0)).is("Collection did not have the expected size.  Expect=0, Actual=2.");

		assertThrown(()->assertList(null).isEmpty()).is("Value was null.");
		assertList(x1).isEmpty();
		assertThrown(()->assertList(x2).isEmpty()).is("Collection was not empty.");

		assertThrown(()->assertList(null).isNotEmpty()).is("Value was null.");
		assertThrown(()->assertList(x1).isNotEmpty()).is("Collection was empty.");
		assertList(x2).isNotEmpty();

		assertList(null).item(0).doesNotExist();
		assertList(x1).item(0).doesNotExist();
		assertList(x2).item(0).exists();
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->ListAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		ListAssertion.create(null).stdout().stderr();
	}
}
