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
public class ArrayAssertion_Test {

	@Test
	public void a01_basic() throws Exception {
		String[] x1={}, x2={"foo"};

		assertThrown(()->assertArray(null).exists()).is("Value was null.");
		assertArray(x1).exists();

		assertArray(null).doesNotExist();
		assertThrown(()->assertArray(x1).doesNotExist()).is("Value was not null.");

		assertThrown(()->assertArray(null).isSize(0)).is("Value was null.");
		assertArray(x1).isSize(0);
		assertThrown(()->assertArray(x1).isSize(1)).is("Array did not have the expected size.  Expected=1, Actual=0.");
		assertArray(x2).isSize(1);
		assertThrown(()->assertArray(x2).isSize(0)).is("Array did not have the expected size.  Expected=0, Actual=1.");

		assertThrown(()->assertArray(null).isEmpty()).is("Value was null.");
		assertArray(x1).isEmpty();
		assertThrown(()->assertArray(x2).isEmpty()).is("Array was not empty.");

		assertThrown(()->assertArray(null).isNotEmpty()).is("Value was null.");
		assertThrown(()->assertArray(x1).isNotEmpty()).is("Array was empty.");
		assertArray(x2).isNotEmpty();

		assertArray(null).item(0).doesNotExist();
		assertArray(x1).item(0).doesNotExist();
		assertArray(x2).item(0).exists();

		assertArray(new String[]{"foo"}).stringItem(0).is("foo");
		assertArray(new Date[]{new Date(0)}).dateItem(0).isBeforeNow();
		assertArray(new Integer[]{1}).integerItem(0).is(1);
		assertArray(new Long[]{1l}).longItem(0).is(1l);
		assertArray(new List[]{AList.of("foo")}).listItem(0).stringItem(0).is("foo");
		assertArray(new List[]{AList.of("foo")}).collectionItem(0).isSize(1);

		assertThrown(()->assertArray(new Integer[]{1}).listItem(0)).is("Object was not expected type.  Expected=java.util.List, Actual=java.lang.Integer.");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->ArrayAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		ArrayAssertion.create(null).stdout().stderr();
	}
}
