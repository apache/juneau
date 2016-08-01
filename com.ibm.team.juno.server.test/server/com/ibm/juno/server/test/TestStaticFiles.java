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

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testStaticFiles",
	staticFiles="{xdocs:'xdocs'}"
)
public class TestStaticFiles extends RestServlet {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Tests the @RestResource(staticFiles) annotation.
	//====================================================================================================
	@RestMethod(name="GET", path="/*")
	public String testXdocs() {
		return null;
	}

}
