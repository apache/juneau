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

import java.net.*;
import java.util.*;

import org.apache.juneau.microservice.*;

/**
 * Utility class for starting up the tests microservice.
 * @author james.bognar
 */
public class TestMicroservice {
	static RestMicroservice microservice;
	static URI microserviceURI;

	/**
	 * Starts the microservice.
	 * @return <jk>true</jk> if the service started, <jk>false</jk> if it's already started.
	 * If this returns <jk>false</jk> then don't call stopMicroservice()!.
	 */
	public static boolean startMicroservice() {
		if (microservice != null)
			return false;
		try {
			Locale.setDefault(Locale.US);
			microservice = new RestMicroservice()
				.setConfig("juneau-rest-test.cfg", false)
				.setManifestContents(
					"Test-Entry: test-value"
				);
			microserviceURI = microservice.start().getURI();
			return true;
		} catch (Throwable e) {
			System.err.println(e); // NOT DEBUG
			return false;
		}
	}

	/**
	 * Returns the URI of the microservice.
	 * @return The URI of the microservice.
	 */
	public static URI getURI() {
		if (microservice == null)
			startMicroservice();
		return microserviceURI;
	}

	/**
	 * Stops the microservice.
	 */
	public static void stopMicroservice() {
		try {
			microservice.stop();
			microservice = null;
		} catch (Exception e) {
			System.err.println(e); // NOT DEBUG
		}
	}
}
