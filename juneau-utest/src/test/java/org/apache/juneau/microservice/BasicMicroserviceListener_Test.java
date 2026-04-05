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
package org.apache.juneau.microservice;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BasicMicroserviceListener}.
 */
class BasicMicroserviceListener_Test extends TestBase {

	@Test void a01_instantiate() {
		var x = new BasicMicroserviceListener();
		assertNotNull(x);
	}

	@Test void a02_onConfigChange_noOp() {
		var x = new BasicMicroserviceListener();
		// Should not throw - no-op implementation
		assertDoesNotThrow(() -> x.onConfigChange(null, null));
	}

	@Test void a03_onStart_noOp() {
		var x = new BasicMicroserviceListener();
		// Should not throw - no-op implementation
		assertDoesNotThrow(() -> x.onStart(null));
	}

	@Test void a04_onStop_noOp() {
		var x = new BasicMicroserviceListener();
		// Should not throw - no-op implementation
		assertDoesNotThrow(() -> x.onStop(null));
	}
}
