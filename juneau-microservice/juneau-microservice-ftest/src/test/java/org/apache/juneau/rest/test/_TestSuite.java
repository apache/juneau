// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.test;

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

/**
 * Runs all the testcases in this project.
 * Starts a REST service running org.apache.juneau.rest.testutils.Root on port 10001.
 * Stops the REST service after running the tests.
 */
@RunWith(Suite.class)
@SuiteClasses({
	ConfigTest.class
})
public class _TestSuite {

	@BeforeClass
	public static void setUp() {
		double version = Double.parseDouble(System.getProperty("java.specification.version"));
		Assume.assumeFalse("Java version " + version + " detected.  Tests will be skipped.", version < 1.8);
		TestMicroservice.startMicroservice();
	}

	@AfterClass
	public static void tearDown() {
		TestMicroservice.stopMicroservice();
	}
}
