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
import static org.junit.jupiter.api.Assumptions.*;

import java.lang.management.*;

import org.apache.juneau.*;
import org.apache.juneau.config.store.*;
import org.junit.jupiter.api.*;

/**
 * Pins the empty-imports guard in {@link ConfigMap#getEntry(String, String)}.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class ConfigMap_ImportsGuard_Test extends TestBase {

	@Test void g01a_noImports_missLookup_allocatesUnder32BytesPerOp() throws Exception {
		var mx = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
		assumeTrue(mx.isThreadAllocatedMemorySupported());
		mx.setThreadAllocatedMemoryEnabled(true);
		var cm = new ConfigMap(MemoryStore.create().build(), "G01.cfg", "[S1]\nk1 = v1\n");
		for (var i = 0; i < 30_000; i++)
			cm.getEntry("S1", "absent");  // warmup
		var id = Thread.currentThread().getId();
		var n = 10_000;
		var hits = 0;
		var t0 = mx.getThreadAllocatedBytes(id);
		for (var i = 0; i < n; i++)
			if (cm.getEntry("S1", "absent") != null)  // no assertion inside the timed loop: assertion helpers allocate
				hits++;
		var perOp = (mx.getThreadAllocatedBytes(id) - t0) / n;
		assertEquals(0, hits, "every lookup is a miss");
		assertTrue(perOp < 32, "allocated " + perOp + " B per miss; the empty-imports stream must be skipped");
	}

	@Test void g01b_import_backedLookup_stillResolves() throws Exception {
		var ms = MemoryStore.create().build();
		ms.write("G01A", "", "[S1]\nk = imported\n");
		ms.write("G01B", "", "<G01A>");
		var cm = new ConfigMap(ms, "G01B");
		assertEquals("imported", cm.getEntry("S1", "k").getValue());
	}
}
