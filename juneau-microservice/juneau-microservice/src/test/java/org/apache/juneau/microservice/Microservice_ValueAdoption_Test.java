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
package org.apache.juneau.microservice;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.commons.settings.*;
import org.apache.juneau.testing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Acceptance tests for the {@code @Value}-driven {@code juneau.workingDir} default on
 * {@link Microservice.Builder}.
 *
 * <p>
 * 3-test triad per migrated field — system property set, unset (default = null), and
 * {@code Settings.setGlobal} override.
 */
@JettyMicroserviceTest
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class Microservice_ValueAdoption_Test {

	private static final String SP = "juneau.workingDir";

	@AfterEach
	void cleanup() {
		Settings.get().unsetGlobal(SP);
		System.clearProperty(SP);
	}

	@Test
	void a01_workingDir_set() {
		System.setProperty(SP, "/tmp/todo92-set");
		assertEquals(new File("/tmp/todo92-set"), Microservice.create().workingDir);
	}

	@Test
	void a02_workingDir_unset() {
		assertNull(Microservice.create().workingDir);
	}

	@Test
	void a03_workingDir_setGlobal() {
		Settings.get().setGlobal(SP, "/tmp/todo92-global");
		assertEquals(new File("/tmp/todo92-global"), Microservice.create().workingDir);
	}
}
