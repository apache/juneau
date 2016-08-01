/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import java.io.*;

import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testRestClient"
)
public class TestRestClient extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Echo response
	//====================================================================================================
	@RestMethod(name="POST", path="/")
	public Reader test1(RestRequest req) throws Exception {
		return new StringReader(req.getInputAsString());
	}
}
