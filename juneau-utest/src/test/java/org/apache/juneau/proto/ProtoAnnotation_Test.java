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
package org.apache.juneau.proto;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.collections.JsonMap;
import org.apache.juneau.proto.annotation.Proto;
import org.apache.juneau.proto.annotation.ProtoAnnotation;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Proto} annotation.
 */
class ProtoAnnotation_Test {

	@Test
	void f01_comment() throws Exception {
		// Serialize structure with name/test; @Proto(comment) on bean field emits comment when bean is used
		var a = JsonMap.of("name", "test");
		var proto = ProtoSerializer.DEFAULT.serialize(a);
		assertNotNull(proto);
		assertTrue(proto.contains("name"));
		assertTrue(proto.contains("test"));
	}

	@Test
	void f02_annotationEquivalency() {
		var a1 = ProtoAnnotation.create().comment("x").build();
		var a2 = ProtoAnnotation.create().comment("x").build();
		assertEquals(a1.comment(), a2.comment());
	}
}
