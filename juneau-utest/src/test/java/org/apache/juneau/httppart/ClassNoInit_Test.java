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
package org.apache.juneau.httppart;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that resolving a {@link Class} from a string does not run the named class's static initializer.
 */
class ClassNoInit_Test extends TestBase {

	// Kept separate from the probes so that reading a flag never initializes a probe class.
	static class Flags {
		static volatile boolean p1, p2;
	}

	public static class Probe1 {
		static {
			Flags.p1 = true;
		}
	}

	public static class Probe2 {
		static {
			Flags.p2 = true;
		}
	}

	@Test void a01_simplePartParser() throws Exception {
		var cm = BeanContext.DEFAULT.getClassMeta(Class.class);
		var c = SimplePartParser.DEFAULT.getPartSession().parse(HttpPartType.QUERY, null, Probe1.class.getName(), cm);
		assertEquals(Probe1.class, c);
		assertFalse(Flags.p1);
	}

	@Test void a02_newInstanceFromString() throws Exception {
		var cm = BeanContext.DEFAULT.getClassMeta(Class.class);
		var c = cm.newInstanceFromString(null, Probe2.class.getName());
		assertEquals(Probe2.class, c);
		assertFalse(Flags.p2);
	}
}
