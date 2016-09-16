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
package org.apache.juneau.server.samples;

import org.apache.juneau.microservice.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

/**
 * Runs all the testcases in this project.
 * Starts a REST service running org.apache.juneau.server.samples.RootResources on port 10000.
 * Stops the REST service after running the tests.
 */
@RunWith(Suite.class)
@SuiteClasses({
	AddressBookResourceTest.class,
	RootResourcesTest.class,
	SampleRemoteableServicesResourceTest.class,
	TestMultiPartFormPostsTest.class
})
public class _TestSuite {
	static RestMicroservice microservice;

	@BeforeClass
	public static void setUp() {
		try {
			microservice = new RestMicroservice(new String[0]);
			microservice.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@AfterClass
	public static void tearDown() {
		try {
			microservice.stop();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
