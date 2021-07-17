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
import java.time.temporal.*;
import java.util.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class DateAssertion_Test {

	private DateAssertion test(Date value) {
		return assertDate(value).silent();
	}

	@Test
	public void a01_basic() throws Exception {
		Date x1 = new Date(0), x2 = new Date(Long.MAX_VALUE), now = new Date();

		assertThrown(()->test((Date)null).exists()).message().is("Value was null.");
		test(x1).exists();
		test(x1).exists();

		assertThrown(()->test(x1).isNull()).message().is("Value was not null.");

		test(x1).isEqual(x1);
		test(x1).isEqual(x1, ChronoUnit.DAYS);
		assertThrown(()->test(x2).isEqual(x1, ChronoUnit.DAYS)).message().contains("Unexpected value.");

		test(x1).isEqual(x1);
		test(x1).isEqual(x1, ChronoUnit.DAYS);
		assertThrown(()->test(x2).isEqual(x1, ChronoUnit.DAYS)).message().contains("Unexpected value.");

		assertThrown(()->test(x1).isBefore(null)).message().is("Argument 'value' cannot be null.");
		test(x1).isBefore(x2);
		assertThrown(()->test(x2).isBefore(x1)).message().contains("Value was not before expected.");

		assertThrown(()->test(x1).isAfter(null)).message().is("Argument 'value' cannot be null.");
		test(x2).isAfter(x1);
		assertThrown(()->test(x1).isAfter(x2)).message().contains("Value was not after expected.");

		test(x1).isBeforeNow();
		assertThrown(()->test(x2).isBeforeNow()).message().contains("Value was not before expected.");

		test(x2).isAfterNow();
		assertThrown(()->test(x1).isAfterNow()).message().contains("Value was not after expected.");

		assertThrown(()->test(now).isBetween(null,x2)).message().is("Argument 'lower' cannot be null.");
		assertThrown(()->test(now).isBetween(x1,null)).message().is("Argument 'upper' cannot be null.");
		test(now).isBetween(x1, x2);
		assertThrown(()->test(x1).isBetween(now,x2)).message().contains("Value was not after expected.");
		assertThrown(()->test(x2).isBetween(x1,now)).message().contains("Value was not before expected.");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test((Date)null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test((Date)null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
		test((Date)null).stdout();
	}
}
