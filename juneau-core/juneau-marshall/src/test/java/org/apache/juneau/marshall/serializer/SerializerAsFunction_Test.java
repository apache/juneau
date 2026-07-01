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
package org.apache.juneau.marshall.serializer;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.msgpack.*;
import org.junit.jupiter.api.*;

class SerializerAsFunction_Test extends TestBase {

	@Test void a01_writerSerializer_asThrowingFunction() {
		ThrowingFunction<Object,String> f = Json5Serializer.DEFAULT;
		assertEquals("123", f.apply(123));
	}

	@Test void a02_outputStreamSerializer_asThrowingFunction() {
		ThrowingFunction<Object,byte[]> f = MsgPackSerializer.DEFAULT;
		assertNotNull(f.apply(123));
		assertTrue(f.apply(123).length > 0);
	}
}
