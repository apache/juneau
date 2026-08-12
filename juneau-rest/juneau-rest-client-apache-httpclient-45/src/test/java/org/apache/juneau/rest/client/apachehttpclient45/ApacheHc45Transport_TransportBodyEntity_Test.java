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
package org.apache.juneau.rest.client.apachehttpclient45;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.http.entity.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

/**
 * Direct unit test for {@link ApacheHc45Transport.TransportBodyEntity#getContent()} &mdash; a defensive
 * guard against the entity being read the "pull" way, which Apache HttpClient's wire-level serializer
 * never does (it always calls {@link AbstractHttpEntity#writeTo} instead), so this can only be exercised
 * by calling it directly. In the same package as {@link ApacheHc45Transport} since the nested class is
 * package-private.
 */
class ApacheHc45Transport_TransportBodyEntity_Test {

	@Test
	void a01_getContent_alwaysThrowsUnsupportedOperationException() {
		var entity = new ApacheHc45Transport.TransportBodyEntity(TransportBody.of(StringBody.of("x", "text/plain")));
		var ex = assertThrows(UnsupportedOperationException.class, entity::getContent);
		assertTrue(ex.getMessage().contains("writeTo"), ex.getMessage());
	}
}
