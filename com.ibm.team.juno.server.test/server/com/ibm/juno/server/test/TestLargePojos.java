/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;


import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.jena.*;
import com.ibm.juno.server.tests.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testLargePojos"
)
public class TestLargePojos extends RestServletJenaDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test how long it takes to serialize/parse various content types.
	//====================================================================================================
	@RestMethod(name="GET", path="/")
	public LargePojo testGet() {
		return LargePojo.create();
	}

	@RestMethod(name="PUT", path="/")
	@SuppressWarnings("unused")
	public String testPut(@Content LargePojo in) {
		return "ok";
	}
}
