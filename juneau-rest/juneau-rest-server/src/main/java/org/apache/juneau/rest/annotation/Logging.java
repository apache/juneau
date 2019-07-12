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
package org.apache.juneau.rest.annotation;

import java.util.logging.*;

import org.apache.juneau.rest.*;

/**
 * Configures the {@link RestCallLogger} used by REST classes and methods.
 *
 * <p>
 * This annotation can be used on the {@link RestResource#logging()} and {@link RestMethod#logging()} annotations
 * to control how and when HTTP requests are logged and at what level of detail.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestResource</ja>(
 * 		logging=<ja>@Logging</ja>(
 * 			level=<js>"INFO"</js>,
 * 			rules={
 * 				<ja>@LoggingRule</ja>(codes=<js>"400-499"</js>, level=<js>"WARNING"</js>, req=<js>"SHORT"</js>, res=<js>"MEDIUM"</js>),
 * 				<ja>@LoggingRule</ja>(codes=<js>">=500"</js>, level=<js>"SEVERE"</js>, req=<js>"LONG"</js>, res=<js>"LONG"</js>)
 * 			}
 * 		}
 * 	)
 * 	<jk>public class</jk> MyResource {
 *
 * 		<ja>@RestMethod</ja>(
 * 			logging=<ja>@Logging</ja>(
 * 				level=<js>"WARNING"</js>,
 * 				rules={
 * 					<ja>@LoggingRule</ja>(codes=<js>"400-499"</js>, level=<js>"SEVERE"</js>, req=<js>"LONG"</js>, res=<js>"LONG"</js>)
 * 				}
 * 			}
 * 		)
 * 		<jk>public</jk> String getFoo() {...}
 * 	}
 * <p>
 *
 * <p>
 * This annotation is inheritable from parent classes and overridable by child classes.  When defined on multiple levels,
 * the annotation values are combined.
 * <br>Rules defined on child classes are matched before rules defined on parent classes.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
 * 	<li class='jf'>{@link RestContext#REST_callLoggerConfig}
 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_callLoggerConfig}
 * </ul>
 */
public @interface Logging {

	/**
	 * Disables logging entirely for this servlet or method.
	 *
	 * <p>
	 * The possible values are (case-insensitive):
	 * <ul>
	 * 	<li><js>"true</jk> - Disable logging.
	 * 	<li><js>"false"</jk> (default) - Don't disable logging.
	 * 	<li><js>"per-request"</jk> - Disable logging if No-Trace is set on the request.
	 * </ul>
	 *
	 * <p>
	 * The No-Trace setting on a request can be set by adding <c class='snippet'>X-NoTrace: true</c> to the request header.
	 * It can also be set programmatically by calling either the {@link RestRequest#setNoTrace(Boolean)} or
	 * {@link RestResponse#setNoTrace(Boolean)} methods.
	 *
	 * <p>
	 * Setting this value to <js>"true"</js> is equivalent to setting the level to <js>"off"</js>.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public String disabled() default "";

	/**
	 * Identifies the logging level at which to log REST calls.
	 *
	 * <p>
	 * See the {@link Level} class for possible values.
	 *
	 * <p>
	 * Values are case-insensitive.
	 *
	 * <p>
	 * The default level is {@link Level#INFO}.
	 *
	 * <p>
	 * {@link Level#OFF} can be used to turn off logging.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public String level() default "";

	/**
	 * Defines the rules to use for logging REST calls.
	 *
	 * <p>
	 * No defines rules results in no logged messages.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public LoggingRule[] rules() default {};

	/**
	 * Specifies the time in milliseconds to cache stack trace hashes.
	 *
	 * <p>
	 * This setting can be used to periodically log stack traces (e.g. every 24 hours) so that stack traces don't get
	 * lost from rolling-over log files.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public String stackTraceHashingTimeout() default "";

	/**
	 * Specifies whether to use stack trace hashing in the log file.
	 *
	 * <p>
	 * This setting can be used to eliminate duplicate stacktraces in your log file by logging them once and then
	 * logging identifying hash IDs.
	 *
	 * <p>
	 * The possible values are (case-insensitive):
	 * <ul>
	 * 	<li><js>"true</jk> - Use stack trace hashing.
	 * 	<li><js>"false"</jk> (default) - Don't use stack trace hashing.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public String useStackTraceHashing() default "";
}
