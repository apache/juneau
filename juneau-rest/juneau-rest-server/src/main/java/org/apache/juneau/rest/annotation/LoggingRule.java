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
 * Represents a single logging rule for how to handle logging of HTTP requests/responses.
 *
 * <ul class='seealso'>
 * 	<li class='jf'>{@link RestContext#REST_callLoggerConfig}
 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_callLoggerConfig}
 * </ul>
 */
public @interface LoggingRule {

	/**
	 * Defines the status codes that match this rule.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li>A single value (e.g. <js>"404"</js>).
	 * 	<li>A closed range of values (e.g. <js>"400-499"</js>).
	 * 	<li>An open range of values (e.g. <js>"400-"</js>, <js>"-299"</js>, <js>">=500"</js>).
	 * 	<li>The value <js>"*"</js> to match any code.  This is the default value.
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public String codes() default "*";

	/**
	 * Specifies whether only debug requests match against this rule.
	 *
	 * <p>
	 * Allows you to tailor logging on debug requests.
	 *
	 * <p>
	 * See the {@link RestResource#debug() @RestResource(debug)} annotation on details of how to enable debugging.
	 *
	 * <p>
	 * The possible values are (case-insensitive):
	 * <ul>
	 * 	<li><js>"true</jk> - Match debug requests only.
	 * 	<li><js>"false"</jk> - Match any requests.
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public String debugOnly() default "false";

	/**
	 * Disables logging entirely for this rule.
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public String disabled() default "false";

	/**
	 * Defines Java exceptions that match this rule.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li>A fully-qualified class name (e.g. <js>"java.lang.StringIndexOutOfBoundsException"</js>).
	 * 	<li>A simple class name (e.g. <js>"StringIndexOutOfBoundsException"</js>).
	 * 	<li>A pattern with metacharacters (e.g. <js>"String*Exception"</js>).
	 * 	<li>Multiple patterns separated by spaces or commas (e.g. <js>"String*Exception, IO*Exception"</js>).
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public String exceptions() default "";

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
	 * If not specified, uses the value specified by the {@link Logging#level() @Logging(level)} annotation value.
	 *
	 * <p>
	 * {@link Level#OFF} can be used to turn off logging.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public String level() default "";

	/**
	 * Identifies the level of detail to log on HTTP requests.
	 *
	 * <p>
	 * The possible values are (case-insensitive):
	 * <ul>
	 * 	<li><js>"short</jk> (default) - Just the HTTP method and URL.
	 * 	<li><js>"medium"</jk> (default) - Also the URL parameters, body size, and request headers.
	 * 	<li><js>"long"</jk> - Also the request body as UTF-8 and spaced-hex text (debug must be enabled).
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public String req() default "short";

	/**
	 * Identifies the level of detail to log on HTTP responses.
	 *
	 * <p>
	 * The possible values are (case-insensitive):
	 * <ul>
	 * 	<li><js>"short</jk> (default) - Just the response code.
	 * 	<li><js>"medium"</jk> (default) - Also the body size, response headers, and execution time.
	 * 	<li><js>"long"</jk> - Also the response body as UTF-8 and spaced-hex text (debug must be enabled).
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public String res() default "short";

	/**
	 * Shortcut for specifying <js>"long"</js> for {@link #req() @LoggingRule(req)} and {@link #res() @LoggingRule(res)}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public String verbose() default "false";
}
