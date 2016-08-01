/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import java.io.*;

import com.ibm.juno.core.plaintext.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testNoParserInput",
	serializers=PlainTextSerializer.class
)
public class TestNoParserInput extends RestServlet {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// @Content annotated InputStream.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testInputStream")
	public String testInputStream(@Content InputStream in) throws Exception {
		return IOUtils.read(in);
	}

	//====================================================================================================
	// @Content annotated Reader.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testReader")
	public String testReader(@Content Reader in) throws Exception {
		return IOUtils.read(in);
	}

	//====================================================================================================
	// @Content annotated PushbackReader.
	// This should always fail since the servlet reader is not a pushback reader.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testPushbackReader")
	public String testPushbackReader(@Content PushbackReader in) throws Exception {
		return IOUtils.read(in);
	}
}
