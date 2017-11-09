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

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transforms.*;
import org.junit.Assert;
import org.junit.ComparisonFailure;

import junit.framework.*;

public class TestUtils {

	private static JsonSerializer js2 = JsonSerializer.create()
		.simple()
		.pojoSwaps(IteratorSwap.class, EnumerationSwap.class)
		.build();

	/**
	 * Assert that the object equals the specified string after running it through JsonSerializer.DEFAULT_LAX.toString().
	 */
	public static void assertObjectEquals(String s, Object o) {
		assertObjectEquals(s, o, js2);
	}

	/**
	 * Assert that the object is an instance of the specified class.
	 */
	public static void assertClass(Class<?> c, Object o) {
		Assert.assertEquals(c, o == null ? null : o.getClass());
	}

	/**
	 * Assert that the object equals the specified string after running it through ws.toString().
	 */
	public static void assertObjectEquals(String s, Object o, WriterSerializer ws) {
		if ("xxx".equals(s))
			System.err.println("Actual=" + ws.toString(o));
		Assert.assertEquals(s, ws.toString(o));
	}

	public static void checkErrorResponse(boolean debug, RestCallException e, int status, String...contains) throws AssertionFailedError {
		String r = e.getResponseMessage();
		if (debug) {
			System.err.println(r); // NOT DEBUG
			e.printStackTrace();
		}
		if (status != e.getResponseCode()) {
			dumpResponse(r, "Response status code was not correct.  Expected: ''{0}''.  Actual: ''{1}''", status, e.getResponseCode());
			throw new AssertionFailedError(format("Response status code was not correct.  Expected: ''{0}''.  Actual: ''{1}''", status, e.getResponseCode()));
		}
		for (String s : contains) {
			if (r == null || ! r.contains(s)) {
				if (! debug)
					dumpResponse(r, "Response did not have the following expected text: ''{0}''", s);
				throw new AssertionFailedError(format("Response did not have the following expected text: ''{0}''", s));
			}
		}
	}

	private static void dumpResponse(String r, String msg, Object...args) {
		System.err.println("*** Failure ****************************************************************************************"); // NOT DEBUG
		System.err.println(format(msg, args));
		System.err.println("*** Response-Start *********************************************************************************"); // NOT DEBUG
		System.err.println(r); // NOT DEBUG
		System.err.println("*** Response-End ***********************************************************************************"); // NOT DEBUG
	}

	public static void assertEqualsAfterSort(String expected, String actual, String msg, Object...args) {
		String[] e = expected.trim().split("\n"), a = actual.trim().split("\n");

		if (e.length != a.length)
			throw new ComparisonFailure(format(msg, args), expected, actual);

		Arrays.sort(e);
		Arrays.sort(a);

		for (int i = 0; i < e.length; i++)
			if (! e[i].equals(a[i]))
				throw new ComparisonFailure(format(msg, args), expected, actual);
	}
}
