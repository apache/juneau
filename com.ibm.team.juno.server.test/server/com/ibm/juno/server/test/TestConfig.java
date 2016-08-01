/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import com.ibm.juno.core.ini.*;
import com.ibm.juno.microservice.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testConfig"
)
@SuppressWarnings("serial")
public class TestConfig extends Resource {

	@RestMethod(name="GET", path="/")
	public ConfigFile test1(RestRequest req) {
		return req.getConfig();
	}

	@RestMethod(name="GET", path="/{key}/{class}")
	public Object test2(RestRequest req, @Attr("key") String key, @Attr("class") Class<?> c) throws Exception {
		System.err.println("o = " + req.getConfig().getObject(c, key));
		return req.getConfig().getObject(c, key);
	}
}
