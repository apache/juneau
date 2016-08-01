/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testCallback"
)
public class TestCallbackStrings extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test GET
	//====================================================================================================
	@RestMethod(name="GET", path="/")
	public ObjectMap test1(RestRequest req) throws Exception {
		return new ObjectMap().append("method","GET").append("headers", getFooHeaders(req)).append("content", req.getInputAsString());
	}

	//====================================================================================================
	// Test PUT
	//====================================================================================================
	@RestMethod(name="PUT", path="/")
	public ObjectMap testCharsetOnResponse(RestRequest req) throws Exception {
		return new ObjectMap().append("method","PUT").append("headers", getFooHeaders(req)).append("content", req.getInputAsString());
	}

	private Map<String,Object> getFooHeaders(RestRequest req) {
		Map<String,Object> m = new TreeMap<String,Object>();
		for (Map.Entry<String,Object> e : req.getHeaders().entrySet())
			if (e.getKey().startsWith("Foo-"))
				m.put(e.getKey(), e.getValue());
		return m;
	}
}
