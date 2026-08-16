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
package org.apache.juneau.rest.server.springboot;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.builder.*;

/**
 * Verifies startup remains healthy when Logback is excluded from the runtime classpath.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.SpringbootTest
@EnabledIfSystemProperty(named="logback.excluded", matches="true")
class JuneauRestLoggingAutoConfiguration_NoLogbackClasspath_Test extends TestBase {

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class A01_NoLogbackApp {}

	@Test void a01_contextStartsWithoutLogbackAndSkipsInstaller() {
		try (var ctx = new SpringApplicationBuilder(A01_NoLogbackApp.class)
			.web(WebApplicationType.NONE)
			.properties("spring.main.banner-mode=off", "juneau.rest.logging.propagate-levels=true")
			.run()) {
			assertFalse(ctx.containsBean("juneauRestLogLevelPropagatorInstaller"));
		}
	}
}
