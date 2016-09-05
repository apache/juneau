// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.client;

import static java.lang.String.*;

import java.io.*;
import java.net.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.util.*;
import org.apache.juneau.internal.*;

/**
 * Exception representing a <code>400+</code> HTTP response code against a remote resource.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class RestCallException extends IOException {

	private static final long serialVersionUID = 1L;

	private int responseCode;
	private String response, responseStatusMessage;
	HttpResponseException e;
	private HttpResponse httpResponse;


	/**
	 * Constructor.
	 *
	 * @param msg The exception message.
	 */
	public RestCallException(String msg) {
		super(msg);
	}

	/**
	 * Constructor.
	 *
	 * @param e The inner cause of the exception.
	 */
	public RestCallException(Exception e) {
		super(e.getLocalizedMessage(), e);
		if (e instanceof FileNotFoundException) {
			responseCode = 404;
		} else if (e.getMessage() != null) {
			Pattern p = Pattern.compile("[^\\d](\\d{3})[^\\d]");
			Matcher m = p.matcher(e.getMessage());
			if (m.find())
				responseCode = Integer.parseInt(m.group(1));
		}
		setStackTrace(e.getStackTrace());
	}

	/**
	 * Create an exception with a simple message and the status code and body of the specified response.
	 *
	 * @param msg The exception message.
	 * @param response The HTTP response object.
	 * @throws ParseException
	 * @throws IOException
	 */
	public RestCallException(String msg, HttpResponse response) throws ParseException, IOException {
		super(format("%s%nstatus='%s'%nResponse: %n%s%n", msg, response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity(), IOUtils.UTF8)));
	}

	/**
	 * Constructor.
	 *
	 * @param responseCode The response code.
	 * @param responseMsg The response message.
	 * @param method The HTTP method (for message purposes).
	 * @param url The HTTP URL (for message purposes).
	 * @param response The reponse from the server.
	 */
	public RestCallException(int responseCode, String responseMsg, String method, URI url, String response) {
		super(format("HTTP method '%s' call to '%s' caused response code '%s,%s'.%nResponse: %n%s%n", method, url, responseCode, responseMsg, response));
		this.responseCode = responseCode;
		this.responseStatusMessage = responseMsg;
		this.response = response;
	}

	/**
	 * Sets the HTTP response object that caused this exception.
	 *
	 * @param httpResponse The HTTP respose object.
	 * @return This object (for method chaining).
	 */
	protected RestCallException setHttpResponse(HttpResponse httpResponse) {
		this.httpResponse = httpResponse;
		return this;
	}

	/**
	 * Returns the HTTP response object that caused this exception.
	 *
	 * @return The HTTP response object that caused this exception, or <jk>null</jk> if no response was created yet when the exception was thrown.
	 */
	public HttpResponse getHttpResponse() {
		return this.httpResponse;
	}

	/**
	 * Returns the HTTP response status code.
	 *
	 * @return The response status code.  If a connection could not be made at all, returns <code>0</code>.
	 */
	public int getResponseCode() {
		return responseCode;
	}

	/**
	 * Returns the HTTP response message body text.
	 *
	 * @return The response message body text.
	 */
	public String getResponseMessage() {
		return response;
	}

	/**
	 * Returns the response status message as a plain string.
	 *
	 * @return The response status message.
	 */
	public String getResponseStatusMessage() {
		return responseStatusMessage;
	}

	/**
	 * Sets the inner cause for this exception.
	 * @param cause The inner cause.
	 * @return This object (for method chaining).
	 */
	@Override /* Throwable */
	public synchronized RestCallException initCause(Throwable cause) {
		super.initCause(cause);
		return this;
	}
}
