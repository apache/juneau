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
package org.apache.juneau.rest.server.sse;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.time.*;
import java.util.concurrent.*;

import org.junit.jupiter.api.*;

class SseHeartbeat_Test {

	@SuppressWarnings({
		"java:S2925" // Polling sleep waits for the scheduled heartbeat ping to be written.
	})
	@Test
	void a01_heartbeatWritesPingAndCancels() throws Exception {
		var a = Executors.newSingleThreadScheduledExecutor();
		var b = new StringWriter();
		var c = SseHeartbeat.start(a, b, Duration.ofMillis(10));
		for (var i = 0; i < 50 && ! b.toString().contains(": ping"); i++)
			Thread.sleep(10);
		c.close();
		var d = b.toString();
		assertTrue(d.contains(": ping"));
		a.shutdownNow();
	}
}
