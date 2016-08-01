/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import java.text.*;

import junit.framework.*;

import org.junit.Assert;

import com.ibm.juno.client.*;
import com.ibm.juno.core.filters.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.serializer.*;

public class TestUtils {

	private static JsonSerializer js2 = new JsonSerializer.Simple()
		.addFilters(IteratorFilter.class, EnumerationFilter.class);

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
