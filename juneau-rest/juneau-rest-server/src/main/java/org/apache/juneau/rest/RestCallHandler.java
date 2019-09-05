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
 * <ul class='seealso'>
 * 	<li class='jf'>{@link RestContext#REST_callHandler}
 * </ul>
 */
public interface RestCallHandler {

	/**
	 * Represents no RestCallHandler.
	 *
	 * <p>
	 * Used on annotation to indicate that the value should be inherited from the parent class, and
	 * ultimately {@link BasicRestCallHandler} if not specified at any level.
	 */
	public interface Null extends RestCallHandler {}

	/**
	 * The main service method.
	 *
	 * @param r1 The incoming HTTP servlet request object.
	 * @param r2 The incoming HTTP servlet response object.
	 * @throws ServletException Error occurred.
	 * @throws IOException Thrown by underlying stream.
	 */
	public void service(HttpServletRequest r1, HttpServletResponse r2) throws ServletException, IOException;

	/**
	 * Wraps an incoming servlet request/response pair into a single {@link RestCall} object.
	 *
	 * @param req The rest request.
	 * @param res The rest response.
	 * @return The wrapped request/response pair.
	 */
	public RestCall createCall(HttpServletRequest req, HttpServletResponse res);

	/**
	 * Creates a {@link RestRequest} object based on the specified incoming {@link HttpServletRequest} object.
	 *
	 * @param call The current REST call.
	 * @return The wrapped request object.
	 * @throws ServletException If any errors occur trying to interpret the request.
	 */
	public RestRequest createRequest(RestCall call) throws ServletException;

	/**
	 * Creates a {@link RestResponse} object based on the specified incoming {@link HttpServletResponse} object
	 * and the request returned by {@link #createRequest(RestCall)}.
	 *
	 * @param call The current REST call.
	 * @return The wrapped response object.
	 * @throws ServletException If any errors occur trying to interpret the request or response.
	 */
	public RestResponse createResponse(RestCall call) throws ServletException;

	/**
	 * The main method for serializing POJOs passed in through the {@link RestResponse#setOutput(Object)} method or
	 * returned by the Java method.
	 *
	 * @param call The current REST call.
	 * @throws Exception Can be thrown if error occurred while handling response.
	 */
	public void handleResponse(RestCall call) throws Exception;

	/**
	 * Handle the case where a matching method was not found.
	 *
	 * @param call The current REST call.
	 * @throws Exception Can be thrown if error occurred while handling response.
	 */
	public void handleNotFound(RestCall call) throws Exception;

	/**
	 * Method for handling response errors.
	 *
	 * @param call The current REST call.
	 * @param e The exception that occurred.
	 * @throws Exception Can be thrown if error occurred while handling response.
	 */
	public void handleError(RestCall call, Throwable e) throws Exception;

	/**
	 * Method for converting thrown exceptions into other types before they are handled.
	 *
	 * @param t The thrown object.
	 * @return The converted thrown object.
	 */
	public Throwable convertThrowable(Throwable t);

	/**
	 * Returns the session objects for the specified request.
	 *
	 * @param req The REST request.
	 * @param res The REST response.
	 * @return The session objects for that request.
	 */
	public Map<String,Object> getSessionObjects(RestRequest req, RestResponse res);
}
