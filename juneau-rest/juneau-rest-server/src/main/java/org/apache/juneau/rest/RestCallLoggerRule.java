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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.RestCallLoggingDetail.*;

import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.pojotools.*;

/**
 * Represents a logging rule used for determine how detailed requests should be logged at.
 */
public class RestCallLoggerRule {

	private final Matcher codeMatcher;
	private final Matcher exceptionMatcher;
	private final boolean debugOnly;
	private final Level level;
	private final Enablement disabled;
	private final RestCallLoggingDetail req, res;

	/**
	 * Constructor.
	 *
	 * @param b Builder
	 */
	RestCallLoggerRule(Builder b) {
		this.codeMatcher = isEmpty(b.codes) || "*".equals(b.codes) ? null : NumberMatcherFactory.DEFAULT.create(b.codes);
		this.exceptionMatcher = isEmpty(b.exceptions) ? null : StringMatcherFactory.DEFAULT.create(b.exceptions);
		boolean verbose = b.verbose == null ? false : b.verbose;
		this.disabled = b.disabled;
		this.debugOnly = b.debugOnly == null ? false : b.debugOnly;
		this.level = b.level;
		this.req = verbose ? LONG : b.req != null ? b.req : SHORT;
		this.res = verbose ? LONG : b.res != null ? b.res : SHORT;
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder class for this object.
	 */
	@Bean(fluentSetters=true)
	public static class Builder {
		String codes, exceptions;
		Boolean verbose, debugOnly;
		Enablement disabled;
		Level level;
		RestCallLoggingDetail req, res;

		/**
		 * The code ranges that this logging rule applies to.
		 *
		 * <p>
		 * See {@link NumberMatcherFactory} for format of values.
		 *
		 * <p>
		 * <js>"*"</js> can be used to represent all values.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Can be <jk>null</jk> or an empty string.
		 * @return This object (for method chaining).
		 */
		public Builder codes(String value) {
			this.codes = value;
			String c = emptyIfNull(trim(codes));
			if (c.endsWith("-"))
				codes += "999";
			else if (c.startsWith("-"))
				codes = "0" + codes;
			return this;
		}

		/**
		 * The exception naming pattern that this rule applies to.
		 *
		 * <p>
		 * See {@link StringMatcherFactory} for format of values.
		 *
		 * <p>
		 * The pattern can be against either the fully-qualified or simple class name of the exception.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Can be <jk>null</jk> or an empty string.
		 * @return This object (for method chaining).
		 */
		public Builder exceptions(String value) {
			this.exceptions = value;
			return this;
		}

		/**
		 * Shortcut for specifying {@link RestCallLoggingDetail#LONG} for {@link #req(RestCallLoggingDetail)} and {@link #res(RestCallLoggingDetail)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object (for method chaining).
		 */
		public Builder verbose(Boolean value) {
			this.verbose = value;
			return this;
		}

		/**
		 * Shortcut for calling <c>verbose(<jk>true</jk>);</c>
		 *
		 * @return This object (for method chaining).
		 */
		public Builder verbose() {
			return this.verbose(true);
		}

		/**
		 * Shortcut for specifying {@link Level#OFF} for {@link #level(Level)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object (for method chaining).
		 */
		public Builder disabled(Enablement value) {
			this.disabled = value;
			return this;
		}

		/**
		 * Shortcut for calling <c>disabled(<jk>true</jk>);</c>
		 *
		 * @return This object (for method chaining).
		 */
		public Builder disabled() {
			return this.disabled(Enablement.TRUE);
		}

		/**
		 * This match only applies when debug is enabled on the request.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder debugOnly(Boolean value) {
			this.debugOnly = value;
			return this;
		}

		/**
		 * Shortcut for calling <c>debugOnly(<jk>true</jk>);</c>
		 *
		 * @return This object (for method chaining).
		 */
		public Builder debugOnly() {
			return this.debugOnly(true);
		}

		/**
		 * The level of detail to log on a request.
		 *
		 * <p>
		 * The default value is {@link RestCallLoggingDetail#SHORT}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Can be <jk>null</jk>
		 * @return This object (for method chaining).
		 */
		public Builder req(RestCallLoggingDetail value) {
			this.req = value;
			return this;
		}

		/**
		 * The level of detail to log on a response.
		 *
		 * <p>
		 * The default value is {@link RestCallLoggingDetail#SHORT}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Can be <jk>null</jk>
		 * @return This object (for method chaining).
		 */
		public Builder res(RestCallLoggingDetail value) {
			this.res = value;
			return this;
		}

		/**
		 * The logging level to use for logging the request/response.
		 *
		 * <p>
		 * The default value is {@link Level#INFO}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Can be <jk>null</jk>
		 * @return This object (for method chaining).
		 */
		public Builder level(Level value) {
			this.level = value;
			return this;
		}

		/**
		 * Instantiates a new {@link RestCallLoggerRule} object using the settings in this builder.
		 *
		 * @return A new {@link RestCallLoggerRule} object.
		 */
		public RestCallLoggerRule build() {
			return new RestCallLoggerRule(this);
		}
	}

	/**
	 * Returns <jk>true</jk> if this rule matches the specified parameters.
	 *
	 * @param statusCode The HTTP response status code.
	 * @param debug Whether debug is enabled on the request.
	 * @param e Exception thrown while handling the request.
	 * @return <jk>true</jk> if this rule matches the specified parameters.
	 */
	public boolean matches(int statusCode, boolean debug, Throwable e) {
		if (debugOnly && ! debug)
			return false;
		if (exceptionMatcher != null) {
			if (e == null)
				return false;
			Class<?> c = e.getClass();
			if (! (exceptionMatcher.matches(null, c.getName()) || exceptionMatcher.matches(null, c.getSimpleName())))
				return false;
		}
		if (codeMatcher == null || codeMatcher.matches(null, statusCode))
			return true;
		return false;
	}

	/**
	 * Returns the detail level for HTTP requests.
	 *
	 * @return the detail level for HTTP requests.
	 */
	public RestCallLoggingDetail getReqDetail() {
		return req;
	}

	/**
	 * Returns the detail level for HTTP responses.
	 *
	 * @return the detail level for HTTP responses.
	 */
	public RestCallLoggingDetail getResDetail() {
		return res;
	}

	/**
	 * Returns the log level.
	 *
	 * @return the log level.
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * Returns the disabled flag.
	 *
	 * @return the disabled flag.
	 */
	public Enablement getDisabled() {
		return disabled;
	}

	private OMap toMap() {
		return new DefaultFilteringOMap()
			.append("codes", codeMatcher)
			.append("exceptions", exceptionMatcher)
			.append("debugOnly", debugOnly)
			.append("level", level)
			.append("req", req == SHORT ? null : req)
			.append("res", res == SHORT ? null : res);
	}

	@Override /* Object */
	public String toString() {
		return SimpleJsonSerializer.DEFAULT.toString(toMap());
	}
}
