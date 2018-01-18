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
package org.apache.juneau.rest;


import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Class that handles the basic lifecycle of an HTTP REST call.
 * 
 * <h5 class='topic'>Additional Information</h5>
 * <ul>
 * 	<li class='jf'>{@link RestContext#REST_callHandler}
 * </ul>
 */
public interface RestCallHandler {

	/**
	 * Represents no RestCallHandler.
	 * 
	 * <p>
	 * Used on annotation to indicate that the value should be inherited from the parent class, and
	 * ultimately {@link RestCallHandlerDefault} if not specified at any level.
	 */
	public interface Null extends RestCallHandler {}

	/**
	 * Creates a {@link RestRequest} object based on the specified incoming {@link HttpServletRequest} object.
	 * 
	 * @param req The request object from the {@link #service(HttpServletRequest, HttpServletResponse)} method.
	 * @return The wrapped request object.
	 * @throws ServletException If any errors occur trying to interpret the request.
	 */
	public RestRequest createRequest(HttpServletRequest req) throws ServletException;

	/**
	 * Creates a {@link RestResponse} object based on the specified incoming {@link HttpServletResponse} object
	 * and the request returned by {@link #createRequest(HttpServletRequest)}.
	 * 
	 * @param req The request object returned by {@link #createRequest(HttpServletRequest)}.
	 * @param res The response object from the {@link #service(HttpServletRequest, HttpServletResponse)} method.
	 * @return The wrapped response object.
	 * @throws ServletException If any errors occur trying to interpret the request or response.
	 */
	public RestResponse createResponse(RestRequest req, HttpServletResponse res) throws ServletException;

	/**
	 * The main service method.
	 * 
	 * @param r1 The incoming HTTP servlet request object.
	 * @param r2 The incoming HTTP servlet response object.
	 * @throws ServletException
	 * @throws IOException
	 */
	public void service(HttpServletRequest r1, HttpServletResponse r2) throws ServletException, IOException;

	/**
	 * The main method for serializing POJOs passed in through the {@link RestResponse#setOutput(Object)} method or
	 * returned by the Java method.
	 * 
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @param output The output to serialize in the response.
	 * @throws IOException
	 * @throws RestException
	 */
	public void handleResponse(RestRequest req, RestResponse res, Object output) throws IOException, RestException ;

	/**
	 * Handle the case where a matching method was not found.
	 * 
	 * @param rc The HTTP response code.
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @throws Exception
	 */
	public void handleNotFound(int rc, RestRequest req, RestResponse res) throws Exception;

	/**
	 * Method for handling response errors.
	 * 
	 * @param req The servlet request.
	 * @param res The servlet response.
	 * @param e The exception that occurred.
	 * @throws IOException Can be thrown if a problem occurred trying to write to the output stream.
	 */
	public void handleError(HttpServletRequest req, HttpServletResponse res, RestException e) throws IOException;

	/**
	 * Method for rendering response errors.
	 * 
	 * @param req The servlet request.
	 * @param res The servlet response.
	 * @param e The exception that occurred.
	 * @throws IOException Can be thrown if a problem occurred trying to write to the output stream.
	 */
	public void renderError(HttpServletRequest req, HttpServletResponse res, RestException e) throws IOException;

	/**
	 * Returns the session objects for the specified request.
	 * 
	 * @param req The REST request.
	 * @return The session objects for that request.
	 */
	public Map<String,Object> getSessionObjects(RestRequest req);
}
