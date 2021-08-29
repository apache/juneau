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
package org.apache.juneau.rest.logging;

import java.util.function.*;
import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Interface class used for logging HTTP requests.
 *
 * <p>
 * The {@link RestLoggerBuilder#implClass(Class)} method has been provided for easy extension of this class.
 *
 * <p>
 * The following default implementations are also provided:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link BasicRestLogger} - The default logger typically used.
 * 	<li class='jc'>{@link BasicDisabledRestLogger} - A no-op logger if you want to turn off logging entirely.
 * 	<li class='jc'>{@link BasicTestRestLogger} - A logger useful for testcases.
 * 	<li class='jc'>{@link BasicTestCaptureRestLogger} - Useful for capturing log messages for testing logging itself.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='jf'>{@link RestContext#REST_callLogger}
 * 	<li class='jm'>{@link RestContextBuilder#callLoggerDefault(Class)}
 * 	<li class='jm'>{@link RestContextBuilder#callLoggerDefault(RestLogger)}
 * 	<li class='jf'>{@link RestContext#REST_debug}
 * 	<li class='jf'>{@link RestContext#REST_debugOn}
 * 	<li class='ja'>{@link Rest#debug}
 * 	<li class='ja'>{@link RestOp#debug}
 * 	<li class='link'>{@doc RestLoggingAndDebugging}
 * </ul>
 */
public interface RestLogger {

	/** Represents no logger */
	public abstract class Null implements RestLogger {}

	/**
	 * System property name for the default logger name to use for {@link RestLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_LOGGER</c> environment variable.
	 * <p>
	 * If not specified, the default is <js>"global"</js>.
	 */
	public static final String SP_logger = "juneau.restLogger.logger";

	/**
	 * System property name for the default enablement setting for {@link RestLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_ENABLED</c> environment variable.
	 * <p>
	 * The possible values are:
	 * <ul>
	 * 	<li>{@link Enablement#ALWAYS "ALWAYS"} (default) - Logging is enabled.
	 * 	<li>{@link Enablement#NEVER "NEVER"} - Logging is disabled.
	 * 	<li>{@link Enablement#CONDITIONAL "CONDITIONALLY"} - Logging is enabled if it passes the {@link RestLoggerBuilder#enabledTest(Predicate)} test.
	 * </ul>
	 */
	public static final String SP_enabled = "juneau.restLogger.enabled";

	/**
	 * System property name for the default request detail setting for {@link RestLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_REQUESTDETAIL</c> environment variable.
	 * <p>
	 * The possible values are:
	 * <ul>
	 * 	<li>{@link RestLoggingDetail#STATUS_LINE "STATUS_LINE"} (default) - Log only the status line.
	 * 	<li>{@link RestLoggingDetail#HEADER "HEADER"} - Log the status line and headers.
	 * 	<li>{@link RestLoggingDetail#ENTITY "ENTITY"} - Log the status line and headers and body if available.
	 * </ul>
	 */
	public static final String SP_requestDetail = "juneau.restLogger.requestDetail";

	/**
	 * System property name for the default response detail setting for {@link RestLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_RESPONSEDETAIL</c> environment variable.
	 * <p>
	 * The possible values are:
	 * <ul>
	 * 	<li>{@link RestLoggingDetail#STATUS_LINE "STATUS_LINE"} (default) - Log only the status line.
	 * 	<li>{@link RestLoggingDetail#HEADER "HEADER"} - Log the status line and headers.
	 * 	<li>{@link RestLoggingDetail#ENTITY "ENTITY"} - Log the status line and headers and body if available.
	 * </ul>
	 */
	public static final String SP_responseDetail = "juneau.restLogger.responseDetail";

	/**
	 * System property name for the logging level setting for {@link RestLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_LEVEL</c> environment variable.
	 * <p>
	 * The possible values are:
	 * <ul>
	 * 	<li>{@link Level#OFF "OFF"} (default)
	 * 	<li>{@link Level#SEVERE "SEVERE"}
	 * 	<li>{@link Level#WARNING "WARNING"}
	 * 	<li>{@link Level#INFO "INFO"}
	 * 	<li>{@link Level#CONFIG "CONFIG"}
	 * 	<li>{@link Level#FINE "FINE"}
	 * 	<li>{@link Level#FINER "FINER"}
	 * 	<li>{@link Level#FINEST "FINEST"}
	 * </ul>
	 */
	public static final String SP_level = "juneau.restLogger.level";

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static RestLoggerBuilder create() {
		return new RestLoggerBuilder();
	}

	/**
	 * Called at the end of a servlet request to log the request.
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 */
	public void log(HttpServletRequest req, HttpServletResponse res);
}
