/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import com.ibm.juno.core.json.*;
import com.ibm.juno.core.plaintext.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testUrlContent",
	serializers={PlainTextSerializer.class},
	parsers={JsonParser.class}
)
public class TestUrlContent extends RestServlet {
	private static final long serialVersionUID = 1L;

	@RestMethod(name="GET", path="/testString")
	public String testString(@Content String content) {
		return String.format("class=%s, value=%s", content.getClass().getName(), content.toString());
	}

	@RestMethod(name="GET", path="/testEnum")
	public String testEnum(@Content TestEnum content) {
		return String.format("class=%s, value=%s", content.getClass().getName(), content.toString());
	}

	public static enum TestEnum {
		X1
	}

	@RestMethod(name="GET", path="/testBean")
	public String testBean(@Content TestBean content) throws Exception {
		return String.format("class=%s, value=%s", content.getClass().getName(), JsonSerializer.DEFAULT_LAX.serialize(content));
	}

	public static class TestBean {
		public int f1;
		public String f2;
	}

	@RestMethod(name="GET", path="/testInt")
	public String testString(@Content Integer content) {
		return String.format("class=%s, value=%s", content.getClass().getName(), content.toString());
	}
}
