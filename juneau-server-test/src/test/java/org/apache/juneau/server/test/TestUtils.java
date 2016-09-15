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
package org.apache.juneau.server.test;

import java.text.*;

import org.apache.juneau.client.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transforms.*;
import org.junit.Assert;

import junit.framework.*;

public class TestUtils {

	private static JsonSerializer js2 = new JsonSerializer.Simple()
		.addPojoSwaps(IteratorSwap.class, EnumerationSwap.class);

	/**
	 * Assert that the object equals the specified string after running it through JsonSerializer.DEFAULT_LAX.toString().
	 */
	public static void assertObjectEquals(String s, Object o) {
		assertObjectEquals(s, o, js2);
	}

	/**
	 * Assert that the object equals the specified string after running it through ws.toString().
	 */
	public static void assertObjectEquals(String s, Object o, WriterSerializer ws) {
		Assert.assertEquals(s, ws.toString(o));
	}

	public static void checkErrorResponse(boolean debug, RestCallException e, int status, String...contains) throws AssertionFailedError {
		String r = e.getResponseMessage();
		if (debug) {
			System.err.println(r);
			e.printStackTrace();
		}
		if (status != e.getResponseCode())
			throw new AssertionFailedError(MessageFormat.format("Response status code was not correct.  Expected: ''{0}''.  Actual: ''{1}''", status, e.getResponseCode()));
		for (String s : contains) {
			if (r == null || ! r.contains(s)) {
				if (! debug)
					System.err.println(r);
				throw new AssertionFailedError(MessageFormat.format("Response did not have the following expected text: ''{0}''", s));
			}
		}
	}
}
