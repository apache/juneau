/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.samples;

import java.io.*;
import java.util.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filters.*;
import com.ibm.juno.microservice.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * Sample REST resource for loading URL-Encoded form posts into POJOs.
 */
@RestResource(
	path="/urlEncodedForm",
	messages="nls/UrlEncodedFormResource"
)
public class UrlEncodedFormResource extends Resource {
	private static final long serialVersionUID = 1L;

	/** GET request handler */
	@RestMethod(name="GET", path="/")
	public ReaderResource doGet(RestRequest req) throws IOException {
		return req.getReaderResource("UrlEncodedForm.html", true);
	}

	/** POST request handler */
	@RestMethod(name="POST", path="/")
	public Object doPost(@Content FormInputBean input) throws Exception {
		// Just mirror back the request
		return input;
	}

	public static class FormInputBean {
		public String aString;
		public int aNumber;
		@BeanProperty(filter=CalendarFilter.ISO8601DT.class)
		public Calendar aDate;
	}
}
