/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.labels.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testOptionsWithoutNls"
)
public class TestOptionsWithoutNls extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Should get to the options page without errors
	//====================================================================================================
	@RestMethod(name="OPTIONS", path="/testOptions/*")
	public ResourceOptions testOptions(RestRequest req) {
		return new ResourceOptions(this, req);
	}

	//====================================================================================================
	// Missing resource bundle should cause {!!x} string.
	//====================================================================================================
	@RestMethod(name="GET", path="/testMissingResourceBundle")
	public String test(RestRequest req) {
		return req.getMessage("bad", 1, 2, 3);
	}

}
