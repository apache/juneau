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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Tests the {@code ActionResult.ok()} readability aliases (WORK-J0563): they must be indistinguishable from the
 * {@code success(...)} factory they alias.
 */
class ActionResult_Ok_Test extends TestBase {

	@Test void a01_ok_isSuccessWithNoRow() {
		assertBean(ActionResult.ok(), "contractVersion,outcome,row", "1,success,<null>");
	}

	@Test void a02_ok_wireEqualsSuccessNull() {
		assertEquals(Json.of(ActionResult.success(null)), Json.of(ActionResult.ok()));
	}

	@Test void a03_okRow_carriesRowLikeSuccess() {
		var row = Map.of("id", "INC-1");
		assertBean(ActionResult.ok(row), "outcome,row", "success,{id=INC-1}");
		assertEquals(Json.of(ActionResult.success(row)), Json.of(ActionResult.ok(row)));
	}

	@Test void a04_ok_remainsChainable() {
		assertBean(ActionResult.ok().message("Deleted"), "outcome,message", "success,Deleted");
	}
}
