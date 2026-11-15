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
package org.apache.juneau.commons.conversion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests that the class-name guard applies to String-to-Class conversion.
 */
class BasicConverter_ClassNameGuard_Test extends TestBase {

	@Test void a01_noGuardAllowsAll() {
		assertEquals(Date.class, BasicConverter.INSTANCE.to("java.util.Date", Class.class));
		assertNull(BasicConverter.INSTANCE.getClassNameGuard());
	}

	@Test void a02_guardBlocksDisallowedName() {
		var c = new BasicConverter("java.lang.String"::equals);
		assertEquals(String.class, c.to("java.lang.String", Class.class));
		assertThrows(RuntimeException.class, () -> c.to("java.util.Date", Class.class));
	}

	@Test void a03_guardScopedToInstance() {
		var c = new BasicConverter("java.lang.String"::equals);
		assertThrows(RuntimeException.class, () -> c.to("java.util.Date", Class.class));
		assertEquals(Date.class, BasicConverter.INSTANCE.to("java.util.Date", Class.class));
	}

	@Test void a04_configurableConverterGuard() {
		var c = new ConfigurableConverter("java.lang.String"::equals);
		assertNotNull(c.getClassNameGuard());
		assertEquals(String.class, c.to("java.lang.String", Class.class));
		assertThrows(RuntimeException.class, () -> c.to("java.util.Date", Class.class));
		assertEquals(Date.class, new ConfigurableConverter().to("java.util.Date", Class.class));
	}
}
