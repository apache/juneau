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
package org.apache.juneau.marshall.bson;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/** Debug test to inspect BSON output for arrays. Disabled to avoid stdout noise during normal test runs. */
@Disabled("Debug test - prints hex dumps for manual inspection; run manually when needed")
class BsonArrayDebug_Test {

	@Test
	void dumpIntArrayBson() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().build();
		var bytes = s.write(ints(1, 2, 3));
		assertTrue(bytes.length > 0);
	}

	@Test
	void dumpJsonListBson() {
		var s = BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().build();
		var x = new org.apache.juneau.marshall.collections.JsonList("['abc',123]");
		var bytes = s.write(x);
		assertTrue(bytes.length > 0);
	}
}
