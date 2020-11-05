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

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Builder class for the {@link LoggingRule} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class LoggingRuleBuilder extends AnnotationBuilder {

	/** Default value */
	public static final LoggingRule DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static LoggingRuleBuilder create() {
		return new LoggingRuleBuilder();
	}

	private static class Impl extends AnnotationImpl implements LoggingRule {

		private final String codes, debugOnly, disabled, exceptions, level, req, res, verbose;

		Impl(LoggingRuleBuilder b) {
			super(b);
			this.codes = b.codes;
			this.debugOnly = b.debugOnly;
			this.disabled = b.disabled;
			this.exceptions = b.exceptions;
			this.level = b.level;
			this.req = b.req;
			this.res = b.res;
			this.verbose = b.verbose;
			postConstruct();
		}

		@Override /* LoggingRule */
		public String codes() {
			return codes;
		}

		@Override /* LoggingRule */
		public String debugOnly() {
			return debugOnly;
		}

		@Override /* LoggingRule */
		public String disabled() {
			return disabled;
		}

		@Override /* LoggingRule */
		public String exceptions() {
			return exceptions;
		}

		@Override /* LoggingRule */
		public String level() {
			return level;
		}

		@Override /* LoggingRule */
		public String req() {
			return req;
		}

		@Override /* LoggingRule */
		public String res() {
			return res;
		}

		@Override /* LoggingRule */
		public String verbose() {
			return verbose;
		}
	}


	String codes="*", debugOnly="false", disabled="false", exceptions="", level="", req="short", res="short", verbose="false";

	/**
	 * Constructor.
	 */
	public LoggingRuleBuilder() {
		super(LoggingRule.class);
	}

	/**
	 * Instantiates a new {@link LoggingRule @LoggingRule} object initialized with this builder.
	 *
	 * @return A new {@link LoggingRule @LoggingRule} object.
	 */
	public LoggingRule build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link LoggingRule#codes()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public LoggingRuleBuilder codes(String value) {
		this.codes = value;
		return this;
	}

	/**
	 * Sets the {@link LoggingRule#debugOnly()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public LoggingRuleBuilder debugOnly(String value) {
		this.debugOnly = value;
		return this;
	}

	/**
	 * Sets the {@link LoggingRule#disabled()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public LoggingRuleBuilder disabled(String value) {
		this.disabled = value;
		return this;
	}

	/**
	 * Sets the {@link LoggingRule#exceptions()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public LoggingRuleBuilder exceptions(String value) {
		this.exceptions = value;
		return this;
	}

	/**
	 * Sets the {@link LoggingRule#level()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public LoggingRuleBuilder level(String value) {
		this.level = value;
		return this;
	}

	/**
	 * Sets the {@link LoggingRule#req()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public LoggingRuleBuilder req(String value) {
		this.req = value;
		return this;
	}

	/**
	 * Sets the {@link LoggingRule#res()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public LoggingRuleBuilder res(String value) {
		this.res = value;
		return this;
	}

	/**
	 * Sets the {@link LoggingRule#verbose()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public LoggingRuleBuilder verbose(String value) {
		this.verbose = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
