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

import javax.servlet.http.*;

/**
 * Interface class used for logging HTTP requests to the log file.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jf'>{@link RestContext#REST_callLogger}
 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndErrorHandling}
 * </ul>
 */
public interface RestCallLogger {

	/**
	 * Represents no RestLogger.
	 *
	 * <p>
	 * Used on annotation to indicate that the value should be inherited from the parent class, and
	 * ultimately {@link BasicRestCallLogger} if not specified at any level.
	 */
	public interface Null extends RestCallLogger {}

	/**
	 * Called at the end of a servlet request to log the request.
	 *
	 * @param config The logging configuration.
	 * @param req The servlet request.
	 * @param res The servlet response.
	 */
	public void log(RestCallLoggerConfig config, HttpServletRequest req, HttpServletResponse res);
}
