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
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;

/**
 * Represents a logging rule used by {@link RestLogger}.
 */
public class RestLoggerRule {

	private final Predicate<Integer> statusFilter;
	private final Predicate<HttpServletRequest> requestFilter;
	private final Predicate<HttpServletResponse> responseFilter;
	private final Predicate<Throwable> exceptionFilter;
	private final Level level;
	private final Enablement enabled;
	private final Predicate<HttpServletRequest> enabledTest;
	private final RestLoggingDetail requestDetail, responseDetail;
	private final boolean logStackTrace;

	/**
	 * Constructor.
	 *
	 * @param b Builder
	 */
	RestLoggerRule(RestLoggerRuleBuilder b) {
		this.statusFilter = b.statusFilter;
		this.exceptionFilter = b.exceptionFilter;
		this.requestFilter = b.requestFilter;
		this.responseFilter = b.responseFilter;
		this.level = b.level;
		this.enabled = b.enabled;
		this.enabledTest = b.enabledTest;
		this.requestDetail = b.requestDetail;
		this.responseDetail = b.responseDetail;
		this.logStackTrace = b.logStackTrace;
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static RestLoggerRuleBuilder create() {
		return new RestLoggerRuleBuilder();
	}

	/**
	 * Returns <jk>true</jk> if this rule matches the specified parameters.
	 *
	 * @param req The HTTP request being logged.  Never <jk>null</jk>.
	 * @param res The HTTP response being logged.  Never <jk>null</jk>.
	 * @return <jk>true</jk> if this rule matches the specified parameters.
	 */
	public boolean matches(HttpServletRequest req, HttpServletResponse res) {

		if (requestFilter != null && ! requestFilter.test(req))
			return false;

		if (responseFilter != null && ! responseFilter.test(res))
			return false;

		if (statusFilter != null && ! statusFilter.test(res.getStatus()))
			return false;

		Throwable e = (Throwable)req.getAttribute("Exception");
		if (e != null && exceptionFilter != null && ! exceptionFilter.test(e))
			return false;

		return true;
	}

	/**
	 * Returns the detail level for HTTP requests.
	 *
	 * @return the detail level for HTTP requests, or <jk>null</jk> if it's not set.
	 */
	public RestLoggingDetail getRequestDetail() {
		return requestDetail;
	}

	/**
	 * Returns the detail level for HTTP responses.
	 *
	 * @return the detail level for HTTP responses, or <jk>null</jk> if it's not set.
	 */
	public RestLoggingDetail getResponseDetail() {
		return responseDetail;
	}

	/**
	 * Returns the log level on this rule.
	 *
	 * @return The log level on this rule, or <jk>null</jk> if it's not set.
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * Returns the enablement flag value on this rule.
	 *
	 * @return The enablement flag value on this rule, or <jk>null</jk> if it's not set.
	 */
	public Enablement getEnabled() {
		return enabled;
	}

	/**
	 * Returns the enablement predicate test on this rule.
	 *
	 * @return The enablement predicate test on this rule, or <jk>null</jk> if it's not set.
	 */
	public Predicate<HttpServletRequest> getEnabledTest() {
		return enabledTest;
	}

	/**
	 * Returns <jk>true</jk> if a stack trace should be logged.
	 *
	 * @return <jk>true</jk> if a stack trace should be logged.
	 */
	public boolean isLogStackTrace() {
		return logStackTrace;
	}

	private OMap toMap() {
		return OMap.of()
			.a("codeFilter", statusFilter)
			.a("exceptionFilter", exceptionFilter)
			.a("requestFilter", requestFilter)
			.a("responseFilter", responseFilter)
			.a("level", level)
			.a("requestDetail", requestDetail)
			.a("responseDetail", responseDetail)
			.a("enabled", enabled)
			.a("enabledTest", enabledTest);
	}

	@Override /* Object */
	public String toString() {
		return SimpleJsonSerializer.DEFAULT.toString(toMap());
	}
}
