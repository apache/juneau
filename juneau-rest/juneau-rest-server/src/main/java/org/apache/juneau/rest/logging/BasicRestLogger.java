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

import static org.apache.juneau.rest.logging.RestLoggingDetail.*;

import java.util.function.*;

import static java.util.logging.Level.*;

import org.apache.juneau.rest.*;

/**
 * Default implementation of a {@link RestLogger} with typically handling.
 *
 * <p>
 * Uses the following builder settings:
 * <p class='bcode w800'>
 * 		RestLogger
 * 			.<jsm>create</jsm>()
 * 			.logger(<jv>context</jv>.getLogger())  <jc>// Use logger registered on REST context.</jc>
 * 			.stackTraceStore(<jv>context</jv>.getStackTraceStore())  <jc>// Use stack trace store registered on REST context.</jc>
 * 			.normalRules(  <jc>// Rules when debugging is not enabled.</jc>
 * 				<jsm>createRule</jsm>()  <jc>// Log 500+ errors with status-line and header information.</jc>
 * 					.statusFilter(x -&gt; x &gt;= 500)
 * 					.level(<jsf>SEVERE</jsf>)
 * 					.requestDetail(<jsf>HEADER</jsf>)
 * 					.responseDetail<jsf>(HEADER</jsf>)
 * 					.build(),
 * 				<jsm>createRule</jsm>()  <jc>// Log 400-500 errors with just status-line information.</jc>
 * 					.statusFilter(x -&gt; x &gt;= 400)
 * 					.level(<jsf>WARNING</jsf>)
 * 					.requestDetail(<jsf>STATUS_LINE</jsf>)
 * 					.responseDetail(<jsf>STATUS_LINE</jsf>)
 * 					.build()
 * 			)
 * 			.debugRules(  <jc>// Rules when debugging is enabled.</jc>
 * 				<jsm>createRule</jsm>()  <jc>// Log everything with full details.</jc>
 * 					.level(<jsf>SEVERE</jsf>)
 * 					.requestDetail(<jsf>ENTITY</jsf>)
 * 					.responseDetail(<jsf>ENTITY</jsf>)
 * 					.build()
 * 			)
 * 		;
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestLoggingAndDebugging}
 * </ul>
 */
public class BasicRestLogger extends RestLogger {

	/**
	 * Returns a builder with the settings used by this logger.
	 */
	public static final Supplier<RestLoggerBuilder> SETTINGS = ()->builder();

	/**
	 * Constructor.
	 *
	 * @param context The context of the resource object.
	 */
	public BasicRestLogger(RestContext context) {
		super(builder().logger(context.getLogger()).stackTraceStore(context.getStackTraceStore()));
	}

	private static RestLoggerBuilder builder() {
		return create()
			.normalRules(  // Rules when debugging is not enabled.
				createRule()  // Log 500+ errors with status-line and header information.
					.statusFilter(x -> x >= 500)
					.level(SEVERE)
					.requestDetail(HEADER)
					.responseDetail(HEADER)
					.build(),
				createRule()  // Log 400-500 errors with just status-line information.
					.statusFilter(x -> x >= 400)
					.level(WARNING)
					.requestDetail(STATUS_LINE)
					.responseDetail(STATUS_LINE)
					.build()
			)
			.debugRules(  // Rules when debugging is enabled.
				createRule()  // Log everything with full details.
					.level(SEVERE)
					.requestDetail(ENTITY)
					.responseDetail(ENTITY)
					.build()
			)
		;
	}
}
