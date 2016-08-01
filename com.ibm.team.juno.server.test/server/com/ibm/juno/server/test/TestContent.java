/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import static com.ibm.juno.server.RestServletProperties.*;

import java.util.*;

import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testContent",
	properties={
		@Property(name=REST_allowMethodParam, value="*")
	}
)
public class TestContent extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@RestMethod(name="POST", path="/boolean")
	public boolean testBool(@Content boolean b) {
		return b;
	}

	@RestMethod(name="POST", path="/Boolean")
	public Boolean testBoolean(@Content Boolean b) {
		return b;
	}

	@RestMethod(name="POST", path="/int")
	public int testInt(@Content int i) {
		return i;
	}

	@RestMethod(name="POST", path="/Integer")
	public Integer testInteger(@Content Integer i) {
		return i;
	}

	@RestMethod(name="POST", path="/float")
	public float testFloat(@Content float f) {
		return f;
	}

	@RestMethod(name="POST", path="/Float")
	public Float testFloat2(@Content Float f) {
		return f;
	}

	@RestMethod(name="POST", path="/Map")
	public TreeMap<String,String> testMap(@Content TreeMap<String,String> m) {
		return m;
	}

	@RestMethod(name="POST", path="/B")
	public DTOs.B testPojo1(@Content DTOs.B b) {
		return b;
	}

	@RestMethod(name="POST", path="/C")
	public DTOs.C testPojo2(@Content DTOs.C c) {
		return c;
	}
}
