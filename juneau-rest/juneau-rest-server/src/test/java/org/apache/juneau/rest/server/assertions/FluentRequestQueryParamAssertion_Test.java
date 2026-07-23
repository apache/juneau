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
package org.apache.juneau.rest.server.assertions;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.httppart.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link FluentRequestQueryParamAssertion} tolerates a null {@code value} (TODO-286 #4 null-tolerance fix).
 *
 * <p>
 * A null {@link RequestQueryParam} used to NPE in the constructor ({@code value.asString()}) and in every transform
 * method ({@code value.asBoolean()}, etc.).
 */
class FluentRequestQueryParamAssertion_Test extends TestBase {

	@Test void a01_nullValue_chainedConstructor_doesNotThrow() {
		var a = new FluentRequestQueryParamAssertion<>(null, (RequestQueryParam)null, null);
		assertDoesNotThrow(a::isNull);
	}

	@Test void a02_nullValue_directConstructor_doesNotThrow() {
		var a = new FluentRequestQueryParamAssertion<>((RequestQueryParam)null, null);
		assertDoesNotThrow(a::isNull);
	}

	@Test void a03_nullValue_transformMethodsDoNotThrowAndYieldNull() {
		var a = new FluentRequestQueryParamAssertion<>((RequestQueryParam)null, null);
		assertDoesNotThrow(() -> a.asBoolean().isNull());
		assertDoesNotThrow(() -> a.asInteger().isNull());
		assertDoesNotThrow(() -> a.asLong().isNull());
		assertDoesNotThrow(() -> a.asZonedDateTime().isNull());
		assertDoesNotThrow(() -> a.as(String.class).isNull());
	}
}
