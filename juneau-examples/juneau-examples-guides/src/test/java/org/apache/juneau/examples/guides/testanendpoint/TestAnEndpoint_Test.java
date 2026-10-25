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
package org.apache.juneau.examples.guides.testanendpoint;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.examples.guides.*;
import org.junit.jupiter.api.*;

/**
 * Backs the "Test an endpoint with MockRestClient" guide ({@code pages/guides/03.TestAnEndpoint.md}).
 *
 * <p>
 * The guide's own code is a test, {@link GreetingResource_Test}, so surefire runs it directly.  This class checks the
 * page's "Run it" output against that test class.
 */
class TestAnEndpoint_Test extends TestBase {

	@Test
	void a01_runOutputMatchesTestCount() {
		var tests = Arrays.stream(GreetingResource_Test.class.getDeclaredMethods())
			.filter(x -> x.isAnnotationPresent(Test.class))
			.count();
		var expected = String.join("\n",
			"[INFO] Results:",
			"[INFO]",
			"[INFO] Tests run: " + tests + ", Failures: 0, Errors: 0, Skipped: 0",
			"[INFO]",
			"[INFO] BUILD SUCCESS");
		assertEquals(expected, GuideRegion.read("TestAnEndpoint/run-output.txt", "mvn-test"));
	}
}
