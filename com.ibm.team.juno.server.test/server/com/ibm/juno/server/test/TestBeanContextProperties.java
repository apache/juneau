/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import java.io.*;
import java.util.*;

import com.ibm.juno.core.filters.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testBeanContextProperties",
	filters=DateFilter.ISO8601DTZ.class
)
public class TestBeanContextProperties extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Validate that filters defined on class filter to underlying bean context.
	//====================================================================================================
	@RestMethod(name="GET", path="/testClassFilters/{d1}")
	public Reader testClassFilters(@Attr("d1") Date d1, @Param("d2") Date d2, @Header("X-D3") Date d3) throws Exception {
		DateFilter df = DateFilter.ISO8601DTZ.class.newInstance();
		return new StringReader(
			"d1="+df.filter(d1)+",d2="+df.filter(d2)+",d3="+df.filter(d3)+""
		);
	}
}
