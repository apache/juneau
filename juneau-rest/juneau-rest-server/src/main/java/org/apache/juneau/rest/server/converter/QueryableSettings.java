/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.converter;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;

import org.apache.juneau.commons.beanquery.*;

/**
 * Per-resource limits and defaults for the {@link Queryable} converter.
 *
 * <p>
 * A resource tunes {@link Queryable}'s BeanQuery engine by registering a {@code QueryableSettings} bean in its bean
 * store (mirrors the {@link IntrospectableSettings} idiom).  When no such bean is present, {@link Queryable} uses
 * {@link #DEFAULT}, which sets nothing and so leaves every setting at the context builder's own default.
 *
 * <p>
 * The defaults live in {@link org.apache.juneau.commons.beanquery.BeanQueryContext.Builder BeanQueryContext.Builder}
 * only &mdash; this class never duplicates them.  Each setting
 * is tracked as set or unset; {@link #applyTo(BeanQueryContext.Builder) applyTo} copies only the ones that were set,
 * so an unset setting stays at the builder default (currently: {@code allowRegex} true, {@code regexTimeout} 50ms,
 * {@code countPolicy} {@link CountPolicy#IF_REQUESTED IF_REQUESTED}, {@code defaultLimit} 100, {@code maxLimit} 1000,
 * and caps of 64 search clauses, 8 sort keys, 4096 search characters and 16 expression levels).
 *
 * <p>
 * Every setting here is a trusted, server-side cap or default; none of them is ever set from request data.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(converters=Queryable.<jk>class</jk>)
 * 	<jk>public class</jk> PeopleResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> QueryableSettings queryableSettings() {
 * 			<jc>// allowRegex defaults to true; call allowRegex(false) here to opt this resource out.</jc>
 * 			<jk>return</jk> QueryableSettings.<jsm>create</jsm>().defaultLimit(20).maxLimit(500).build();
 * 		}
 *
 * 		<ja>@RestGet</ja>(<js>"/people"</js>)
 * 		<jk>public</jk> List&lt;Person&gt; getPeople() {
 * 			<jk>return</jk> <jf>people</jf>;
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Queryable}
 * 	<li class='jc'>{@link org.apache.juneau.commons.beanquery.BeanQueryContext.Builder BeanQueryContext.Builder}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Converters">Converters</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class QueryableSettings {

	/** The default settings used when no bean is registered: nothing set, so every builder default applies. */
	public static final QueryableSettings DEFAULT = create().build();

	private final Boolean allowRegex;
	private final Duration regexTimeout;
	private final CountPolicy countPolicy;
	private final Integer defaultLimit;
	private final boolean maxLimitSet;
	private final Integer maxLimit;
	private final Integer maxSearchClauses;
	private final Integer maxSortKeys;
	private final Integer maxSearchLength;
	private final Integer maxExpressionDepth;

	private QueryableSettings(Builder b) {
		this.allowRegex = b.allowRegex;
		this.regexTimeout = b.regexTimeout;
		this.countPolicy = b.countPolicy;
		this.defaultLimit = b.defaultLimit;
		this.maxLimitSet = b.maxLimitSet;
		this.maxLimit = b.maxLimit;
		this.maxSearchClauses = b.maxSearchClauses;
		this.maxSortKeys = b.maxSortKeys;
		this.maxSearchLength = b.maxSearchLength;
		this.maxExpressionDepth = b.maxExpressionDepth;
	}

	/**
	 * Builder creator.
	 *
	 * @return A new builder with nothing set.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Creates a builder pre-populated with these settings.
	 *
	 * @return A new builder.
	 */
	public Builder copy() {
		var b = new Builder();
		b.allowRegex = allowRegex;
		b.regexTimeout = regexTimeout;
		b.countPolicy = countPolicy;
		b.defaultLimit = defaultLimit;
		b.maxLimitSet = maxLimitSet;
		b.maxLimit = maxLimit;
		b.maxSearchClauses = maxSearchClauses;
		b.maxSortKeys = maxSortKeys;
		b.maxSearchLength = maxSearchLength;
		b.maxExpressionDepth = maxExpressionDepth;
		return b;
	}

	/**
	 * Copies every setting that was set onto a context builder.
	 *
	 * <p>
	 * Settings that were not set are left untouched, so the builder's own defaults apply.
	 *
	 * @param <B> The context builder type.
	 * @param builder The builder to copy onto.  Must not be <jk>null</jk>.
	 * @return The same builder, for chaining.
	 * @throws IllegalArgumentException If a value is rejected by the builder (for example a non-positive limit).
	 */
	public <B extends BeanQueryContext.Builder<?,B>> B applyTo(B builder) {
		reqnn("builder", builder);
		// maxLimit goes before defaultLimit so that narrowing maxLimit can auto-narrow an unset defaultLimit, while an
		// explicit defaultLimit is never silently changed.
		if (maxLimitSet)
			builder.maxLimit(maxLimit);
		if (defaultLimit != null)
			builder.defaultLimit(defaultLimit);
		if (allowRegex != null)
			builder.allowRegex(allowRegex);
		if (regexTimeout != null)
			builder.regexTimeout(regexTimeout);
		if (countPolicy != null)
			builder.countPolicy(countPolicy);
		if (maxSearchClauses != null)
			builder.maxSearchClauses(maxSearchClauses);
		if (maxSortKeys != null)
			builder.maxSortKeys(maxSortKeys);
		if (maxSearchLength != null)
			builder.maxSearchLength(maxSearchLength);
		if (maxExpressionDepth != null)
			builder.maxExpressionDepth(maxExpressionDepth);
		return builder;
	}

	/**
	 * Builder for {@link QueryableSettings}.
	 *
	 * <p>
	 * Each setter records a value to be copied by {@link QueryableSettings#applyTo(BeanQueryContext.Builder)}; the
	 * values are validated by the context builder when applied.
	 */
	public static class Builder {
		Boolean allowRegex;
		Duration regexTimeout;
		CountPolicy countPolicy;
		Integer defaultLimit;
		boolean maxLimitSet;
		Integer maxLimit;
		Integer maxSearchClauses;
		Integer maxSortKeys;
		Integer maxSearchLength;
		Integer maxExpressionDepth;

		Builder() {}

		/**
		 * Enables or disables the {@code $regex} search operator.
		 *
		 * @param value <jk>true</jk> to allow {@code $regex}.  Builder default is <jk>true</jk>; call
		 * 	{@code allowRegex(false)} to opt out.
		 * @return This object.
		 */
		public Builder allowRegex(boolean value) {
			this.allowRegex = value;
			return this;
		}

		/**
		 * Sets the time budget for all {@code $regex} matching in one query.
		 *
		 * @param value The budget.  Must be positive.
		 * @return This object.
		 */
		public Builder regexTimeout(Duration value) {
			this.regexTimeout = reqnn("value", value);
			return this;
		}

		/**
		 * Sets which counts are computed.
		 *
		 * @param value The policy.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder countPolicy(CountPolicy value) {
			this.countPolicy = reqnn("value", value);
			return this;
		}

		/**
		 * Sets the page size used when the request specifies no limit.
		 *
		 * @param value The default limit.  Must be positive.
		 * @return This object.
		 */
		public Builder defaultLimit(int value) {
			this.defaultLimit = value;
			return this;
		}

		/**
		 * Sets the largest page size a request may ask for; larger limits are clamped to it.
		 *
		 * @param value The cap, or <jk>null</jk> for no cap.  Must be positive if set.
		 * @return This object.
		 */
		public Builder maxLimit(Integer value) {
			this.maxLimitSet = true;
			this.maxLimit = value;
			return this;
		}

		/**
		 * Sets the largest number of search clauses.
		 *
		 * @param value The cap.  Must be positive.
		 * @return This object.
		 */
		public Builder maxSearchClauses(int value) {
			this.maxSearchClauses = value;
			return this;
		}

		/**
		 * Sets the largest number of sort keys.
		 *
		 * @param value The cap.  Must be positive.
		 * @return This object.
		 */
		public Builder maxSortKeys(int value) {
			this.maxSortKeys = value;
			return this;
		}

		/**
		 * Sets the longest search, sort, view or opts string, in characters.
		 *
		 * @param value The cap.  Must be positive.
		 * @return This object.
		 */
		public Builder maxSearchLength(int value) {
			this.maxSearchLength = value;
			return this;
		}

		/**
		 * Sets the deepest allowed expression nesting.
		 *
		 * @param value The cap.  Must be positive.
		 * @return This object.
		 */
		public Builder maxExpressionDepth(int value) {
			this.maxExpressionDepth = value;
			return this;
		}

		/**
		 * Builds the (immutable) settings.
		 *
		 * @return A new {@link QueryableSettings}.
		 */
		public QueryableSettings build() {
			return new QueryableSettings(this);
		}
	}
}
