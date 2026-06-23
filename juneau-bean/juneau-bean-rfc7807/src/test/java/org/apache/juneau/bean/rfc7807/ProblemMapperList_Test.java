/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.rfc7807;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ProblemMapperList_Test extends TestBase {

	/** A minimal no-op mapper for testing — maps nothing. */
	private static ProblemMapper<?> noOp() {
		return new ProblemMapper<Throwable>() {
			@Override public Class<Throwable> getExceptionType() { return Throwable.class; }
			@Override public Problem map(Throwable e) { return null; }
		};
	}

	//------------------------------------------------------------------------------------------------------------------
	// a — default constructor
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_defaultConstructor_isEmpty() {
		var list = new ProblemMapperList();
		assertTrue(list.isEmpty());
		assertTrue(list.asList().isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b — of(ProblemMapper<?>...)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_of_noArgs_isEmpty() {
		var list = ProblemMapperList.of();
		assertTrue(list.isEmpty());
	}

	@Test void b02_of_nullArray_isEmpty() {
		var list = ProblemMapperList.of((ProblemMapper<?>[]) null);
		assertTrue(list.isEmpty());
	}

	@Test void b03_of_nullElementsSkipped() {
		var m = noOp();
		var list = ProblemMapperList.of(null, m, null);
		assertEquals(1, list.asList().size());
		assertSame(m, list.asList().get(0));
	}

	@Test void b04_of_multipleMappers_preservesOrder() {
		var m1 = noOp();
		var m2 = noOp();
		var list = ProblemMapperList.of(m1, m2);
		assertEquals(2, list.asList().size());
		assertSame(m1, list.asList().get(0));
		assertSame(m2, list.asList().get(1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c — append(ProblemMapper<?>)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_appendSingle_nullIgnored() {
		var list = new ProblemMapperList();
		list.append((ProblemMapper<?>) null);
		assertTrue(list.isEmpty());
	}

	@Test void c02_appendSingle_addsMapper() {
		var m = noOp();
		var list = new ProblemMapperList().append(m);
		assertEquals(1, list.asList().size());
		assertSame(m, list.asList().get(0));
	}

	@Test void c03_appendSingle_returnsSelf() {
		var list = new ProblemMapperList();
		assertSame(list, list.append(noOp()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d — append(Collection<?>)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_appendCollection_nullCollectionIgnored() {
		var list = new ProblemMapperList();
		list.append((Collection<ProblemMapper<?>>) null);
		assertTrue(list.isEmpty());
	}

	@Test void d02_appendCollection_nullElementsSkipped() {
		var m = noOp();
		var list = new ProblemMapperList();
		list.append(Arrays.asList(null, m, null));
		assertEquals(1, list.asList().size());
		assertSame(m, list.asList().get(0));
	}

	@Test void d03_appendCollection_addsAllNonNull() {
		var m1 = noOp();
		var m2 = noOp();
		var list = new ProblemMapperList();
		list.append(List.of(m1, m2));
		assertEquals(2, list.asList().size());
	}

	@Test void d04_appendCollection_returnsSelf() {
		var list = new ProblemMapperList();
		assertSame(list, list.append(List.of(noOp())));
	}

	//------------------------------------------------------------------------------------------------------------------
	// e — asList() unmodifiable
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_asList_unmodifiable() {
		var list = ProblemMapperList.of(noOp());
		var view = list.asList();
		assertThrows(UnsupportedOperationException.class, () -> view.remove(0));
	}
}
