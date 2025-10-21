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
package org.apache.juneau.objecttools;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests the PojoPaginator class.
 */
class ObjectPaginator_Test extends TestBase {

	ObjectPaginator op = ObjectPaginator.create();
	BeanSession bs = BeanContext.DEFAULT_SESSION;

	//-----------------------------------------------------------------------------------------------------------------
	// Null input
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_nullInput() {
		assertNull(op.run(bs, null, null));
	}

	@Test void a02_nonCollectionInput() {
		assertEquals("foo", op.run(bs, "foo", PageArgs.create(1, 3)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Arrays
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_arrays_basic() {
		var in = new int[]{1,2,3};
		assertList(op.run(in, 0, 3), 1,2,3);
		assertList(op.run(in, 1, 3), 2,3);
		assertList(op.run(in, 1, 1), 2);
		assertList(op.run(in, 4, 1));
		assertList(op.run(in, 0, 0));

		var in2 = a("1","2","3");
		assertList(op.run(in2, 1, 1), "2");

		var in3 = new boolean[]{false,true,false};
		assertList(op.run(in3, 1, 1), true);

		var in4 = new byte[]{1,2,3};
		assertList(op.run(in4, 1, 1), (byte)2);

		var in5 = new char[]{'1','2','3'};
		assertList(op.run(in5, 1, 1), '2');

		var in6 = new double[]{1,2,3};
		assertList(op.run(in6, 1, 1), (double)2);

		var in7 = new float[]{1,2,3};
		assertList(op.run(in7, 1, 1), (float)2);

		var in8 = new long[]{1,2,3};
		assertList(op.run(in8, 1, 1), (long)2);

		var in9 = new short[]{1,2,3};
		assertList(op.run(in9, 1, 1), (short)2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Collections
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_collections_basic() {
		var in = list(1,2,3);
		assertList(op.run(in, 0, 3), 1,2,3);
		assertList(op.run(in, 1, 3), 2,3);
		assertList(op.run(in, 1, 1), 2);
		assertList(op.run(in, 4, 1));
		assertList(op.run(in, 0, 0));

		var in2 = set(1,2,3);
		assertList(op.run(in2, 0, 3), 1,2,3);
		assertList(op.run(in2, 1, 3), 2,3);
		assertList(op.run(in2, 1, 1), 2);
		assertList(op.run(in2, 4, 1));
		assertList(op.run(in2, 0, 0));
	}
}