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
package org.apache.juneau.bson;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** Debug test to inspect BSON output for arrays. Disabled to avoid stdout noise during normal test runs. */
@Disabled("Debug test - prints hex dumps for manual inspection; run manually when needed")
class BsonArrayDebug_Test {

	@Test
	void dumpIntArrayBson() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().build();
		var bytes = s.serialize(ints(1, 2, 3));
		// Print hex to see structure
		var sb = new StringBuilder();
		for (var i = 0; i < Math.min(bytes.length, 80); i++) {
			sb.append(String.format("%02x ", bytes[i] & 0xFF));
			if ((i + 1) % 16 == 0) sb.append("\n");
		}
		System.out.println("First 80 bytes (hex):");
		System.out.println(sb);
		assertTrue(bytes.length > 0);
	}

	@Test
	void dumpJsonListBson() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().build();
		var x = new org.apache.juneau.collections.JsonList("['abc',123]");
		var bytes = s.serialize(x);
		var sb = new StringBuilder();
		for (var i = 0; i < Math.min(bytes.length, 100); i++) {
			sb.append(String.format("%02x ", bytes[i] & 0xFF));
			if ((i + 1) % 16 == 0) sb.append("\n");
		}
		System.out.println("JsonList BSON (hex):");
		System.out.println(sb);
		assertTrue(bytes.length > 0);
	}
}
