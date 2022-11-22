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
package org.apache.juneau.rest.logger;

import static org.apache.juneau.rest.logger.CallLoggingDetail.*;
import static org.apache.juneau.Enablement.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static java.util.logging.Level.*;

import javax.servlet.http.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.rest.*;

/**
 * Default implementation of a {@link CallLogger} that only logs REST call errors unless no-log is enabled on the request.
 *
 * <p>
 * Useful for REST tests where you know that a particular call is going to produce an error response and you don't want that
 * response to be logged producing noisy test output.
 *
 * <p>
 * Requests can be tagged as no-log (meaning don't log if there's an error) in any of the following ways:
 * <ul>
 * 	<li>A <js>"No-Trace: true"</js> header.
 * 	<li>A <js>"noTrace=true"</js> query parameter.
 * 	<li>A <js>"NoTrace"</js> request attribute with a string value of <js>"true"</js>.
 * </ul>
 *
 * <h5 class='section'>Configured Settings:</h5>
 * <ul>
 * 	<li>Logs to the {@link RestContext#getLogger() context logger}.
 * 	<li>Only calls with status code &gt;=400 will be logged.
 * 	<li>Logs full request and response entity.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.LoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 */
public class BasicTestCallLogger extends CallLogger {

	/**
	 * Constructor.
	 *
	 * @param beanStore The bean store containing injectable beans for this logger.
	 */
	public BasicTestCallLogger(BeanStore beanStore) {
		super(beanStore);
	}

	@Override
	protected Builder init(BeanStore beanStore) {
		return super.init(beanStore)
			.normalRules(  // Rules when debugging is not enabled.
				CallLoggerRule.create(beanStore)  // Log 500+ errors with status-line and header information.
					.statusFilter(x -> x >= 400)
					.level(SEVERE)
					.requestDetail(HEADER)
					.responseDetail(HEADER)
					.enabled(CONDITIONAL)
					.enabledPredicate(x -> ! isNoTrace(x))  // Only log if it's not a no-trace request.
					.logStackTrace()
					.build(),
				CallLoggerRule.create(beanStore)  // Log 400-500 errors with just status-line information.
					.statusFilter(x -> x >= 400)
					.level(WARNING)
					.requestDetail(STATUS_LINE)
					.responseDetail(STATUS_LINE)
					.enabled(CONDITIONAL)
					.enabledPredicate(x -> ! isNoTrace(x))  // Only log if it's not a no-trace request.
					.logStackTrace()
					.build()
			)
			.debugRules(  // Rules when debugging is enabled.
				CallLoggerRule.create(beanStore)  // Log everything with full details.
					.level(SEVERE)
					.requestDetail(ENTITY)
					.responseDetail(ENTITY)
					.logStackTrace()
					.build()
			)
		;
	}

	private static boolean isNoTrace(HttpServletRequest req) {
		Object o = req.getAttribute("NoTrace");
		if (o != null)
			return "true".equalsIgnoreCase(o.toString());
		String s = req.getHeader("No-Trace");
		if (s != null)
			return "true".equalsIgnoreCase(s);
		return emptyIfNull(req.getQueryString()).contains("noTrace=true");
	}
}
