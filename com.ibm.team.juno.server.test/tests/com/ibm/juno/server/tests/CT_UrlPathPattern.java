/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.json.*;
import com.ibm.juno.server.*;

public class CT_UrlPathPattern {
	@Test
	public void testComparison() throws Exception {
		List<UrlPathPattern> l = new LinkedList<UrlPathPattern>();

		l.add(new UrlPathPattern("/foo"));
		l.add(new UrlPathPattern("/foo/*"));
		l.add(new UrlPathPattern("/foo/bar"));
		l.add(new UrlPathPattern("/foo/bar/*"));
		l.add(new UrlPathPattern("/foo/{id}"));
		l.add(new UrlPathPattern("/foo/{id}/*"));
		l.add(new UrlPathPattern("/foo/{id}/bar"));
		l.add(new UrlPathPattern("/foo/{id}/bar/*"));

		Collections.sort(l);
		assertEquals("['/foo/bar','/foo/bar/*','/foo/{id}/bar','/foo/{id}/bar/*','/foo/{id}','/foo/{id}/*','/foo','/foo/*']", JsonSerializer.DEFAULT_LAX.serialize(l));
	}
}
