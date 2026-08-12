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
package org.apache.juneau.config.internal;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.config.format.*;
import org.apache.juneau.config.store.*;
import org.junit.jupiter.api.*;

/**
 * Direct coverage of package-private {@link ConfigMap} constructor overloads that have no
 * production caller.
 */
@SuppressWarnings("resource") // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
class ConfigMap_Test {

	@Test void a01_contentsConstructor_noFormat_defaultsToIni() throws Exception {
		var s = MemoryStore.create().build();
		var cm = new ConfigMap(s, "A01.cfg", "[S1]\nk1 = v1\n");
		assertEquals("v1", cm.getEntry("S1", "k1").getValue());
	}

	@Test void a02_contentsConstructor_nullFormat_defaultsToIni() throws Exception {
		var s = MemoryStore.create().build();
		var cm = new ConfigMap(s, "A02.cfg", "[S1]\nk1 = v1\n", null);
		assertEquals("v1", cm.getEntry("S1", "k1").getValue());
	}

	@Test void a03_contentsConstructor_explicitFormat() throws Exception {
		var s = MemoryStore.create().build();
		var cm = new ConfigMap(s, "A03.cfg", "S1:\n  k1: v1\n", YamlConfigFormat.INSTANCE);
		assertEquals("v1", cm.getEntry("S1", "k1").getValue());
	}
}
