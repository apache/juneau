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
package org.apache.juneau.http;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class RedactedQueryParams_Test extends TestBase {

	@Test void a01_masksSensitiveValues_caseInsensitive() {
		assertEquals("/a?user=bob&Token=[REDACTED]&PASSWORD=[REDACTED]", RedactedQueryParams.redactUri("/a?user=bob&Token=abc&PASSWORD=p%40ss"));
	}

	@Test void a02_allDefaultNamesMasked() {
		for (var n : RedactedQueryParams.DEFAULT)
			assertEquals("/a?" + n + "=[REDACTED]", RedactedQueryParams.redactUri("/a?" + n + "=v"), n);
	}

	@Test void a03_percentEncodedNameAndFragment() {
		assertEquals("/a?api%5Fkey=[REDACTED]#frag", RedactedQueryParams.redactUri("/a?api%5Fkey=zzz#frag"));
	}

	@Test void a04_noQueryOrNull() {
		assertEquals("/a", RedactedQueryParams.redactUri("/a"));
		assertNull(RedactedQueryParams.redactUri(null));
	}

	@Test void a05_nonSensitiveAndValuelessKept() {
		assertEquals("/a?x=1&flag&y=", RedactedQueryParams.redactUri("/a?x=1&flag&y="));
	}

	@Test void a06_customNames() {
		assertEquals("/a?mine=[REDACTED]&token=abc", RedactedQueryParams.redactUri("/a?mine=1&token=abc", List.of("mine")));
	}
}
