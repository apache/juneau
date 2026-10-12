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
package org.apache.juneau.rest.server.widgets;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link Op} wire-token and operator-class contract.
 */
class Op_Test extends TestBase {

	@Test void a01_wireTokens() {
		assertList(Arrays.stream(Op.values()).map(Op::wire).toList(), "eq", "ne", "present", "absent", "in", "contains");
	}

	@Test void a02_requiresValue() {
		assertTrue(Op.EQ.requiresValue());
		assertTrue(Op.NE.requiresValue());
		assertTrue(Op.IN.requiresValue());
		assertTrue(Op.CONTAINS.requiresValue());
		assertFalse(Op.PRESENT.requiresValue());
		assertFalse(Op.ABSENT.requiresValue());
	}

	@Test void a03_isCollectionOp() {
		assertTrue(Op.IN.isCollectionOp());
		assertTrue(Op.CONTAINS.isCollectionOp());
		assertFalse(Op.EQ.isCollectionOp());
		assertFalse(Op.NE.isCollectionOp());
		assertFalse(Op.PRESENT.isCollectionOp());
		assertFalse(Op.ABSENT.isCollectionOp());
	}
}
