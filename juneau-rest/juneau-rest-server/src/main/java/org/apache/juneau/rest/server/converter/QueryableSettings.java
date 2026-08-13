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

/**
 * Per-resource protocol-selection settings for the {@link ProtocolQueryable} converter.
 *
 * <p>
 * A resource chooses the active {@link QueryProtocol} for {@link ProtocolQueryable} by registering a
 * {@code QueryableSettings} bean in its bean store (mirrors the {@link IntrospectableSettings} idiom).  When no such
 * bean is present, {@link ProtocolQueryable} resolves the default ({@link NativeQueryProtocol}), so the out-of-the-box
 * behavior is the historical Juneau-native {@code s/v/o/p/l} protocol.
 *
 * <h5 class='section'>Example - selecting the DataTables protocol:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(converters=ProtocolQueryable.<jk>class</jk>)
 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> QueryableSettings queryableSettings() {
 * 			<jk>return</jk> QueryableSettings.<jsm>create</jsm>()
 * 				.protocol(<jk>new</jk> DataTablesQueryProtocol())
 * 				.build();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Hardening knobs:</h5>
 * <p>
 * Beyond protocol selection, {@code QueryableSettings} carries a few safety limits consumed by protocols that accept
 * richer (and potentially adversarial) client input than the native {@code s/v/o/p/l} form &mdash; most notably
 * {@code DataTablesQueryProtocol}, whose per-column search boxes and column/order descriptor arrays are driven directly
 * from the browser:
 * <ul class='spaced-list'>
 * 	<li>{@link #allowRegexSearch() allowRegexSearch} &mdash; whether client-supplied regular-expression search
 * 		({@code search[regex]=true}) is honored.  Defaults to <jk>false</jk> (treated as a literal substring search)
 * 		to close the ReDoS/CPU-burn vector of compiling and running arbitrary caller regexes against every row.
 * 	<li>{@link #maxColumns() maxColumns} &mdash; the maximum number of {@code columns[i]} descriptors parsed
 * 		(default {@value #DEFAULT_MAX_COLUMNS}), bounding the O(columns &times; params) parse cost.
 * 	<li>{@link #maxOrderColumns() maxOrderColumns} &mdash; the maximum number of {@code order[i]} descriptors parsed
 * 		(default {@value #DEFAULT_MAX_ORDER_COLUMNS}).
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ProtocolQueryable}
 * 	<li class='jc'>{@link QueryProtocol}
 * 	<li class='jc'>{@link NativeQueryProtocol}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Converters">Converters</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class QueryableSettings {

	/** The default {@link #maxColumns()} cap. */
	public static final int DEFAULT_MAX_COLUMNS = 64;

	/** The default {@link #maxOrderColumns()} cap. */
	public static final int DEFAULT_MAX_ORDER_COLUMNS = 8;

	/** The default settings ({@link NativeQueryProtocol}) used when no bean is registered. */
	public static final QueryableSettings DEFAULT = create().build();

	private final QueryProtocol protocol;
	private final boolean allowRegexSearch;
	private final int maxColumns;
	private final int maxOrderColumns;

	private QueryableSettings(Builder b) {
		this.protocol = b.protocol;
		this.allowRegexSearch = b.allowRegexSearch;
		this.maxColumns = b.maxColumns;
		this.maxOrderColumns = b.maxOrderColumns;
	}

	/**
	 * Builder creator.
	 *
	 * @return A new builder (defaults to the native protocol).
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Returns the selected query protocol.
	 *
	 * @return The selected protocol.  Never <jk>null</jk> &mdash; returns {@link NativeQueryProtocol#INSTANCE} when none was set.
	 */
	public QueryProtocol protocol() {
		return protocol == null ? NativeQueryProtocol.INSTANCE : protocol;
	}

	/**
	 * Returns whether client-supplied regular-expression search is honored.
	 *
	 * @return <jk>true</jk> if regex search is allowed; defaults to <jk>false</jk>.
	 */
	public boolean allowRegexSearch() {
		return allowRegexSearch;
	}

	/**
	 * Returns the maximum number of column descriptors a protocol should parse.
	 *
	 * @return The column cap ({@value #DEFAULT_MAX_COLUMNS} by default).
	 */
	public int maxColumns() {
		return maxColumns;
	}

	/**
	 * Returns the maximum number of order descriptors a protocol should parse.
	 *
	 * @return The order cap ({@value #DEFAULT_MAX_ORDER_COLUMNS} by default).
	 */
	public int maxOrderColumns() {
		return maxOrderColumns;
	}

	/**
	 * Builder for {@link QueryableSettings}.
	 */
	public static class Builder {
		private QueryProtocol protocol;
		private boolean allowRegexSearch = false;
		private int maxColumns = DEFAULT_MAX_COLUMNS;
		private int maxOrderColumns = DEFAULT_MAX_ORDER_COLUMNS;

		/**
		 * Sets the query protocol {@link ProtocolQueryable} should use for this resource.
		 *
		 * @param value The protocol.  If <jk>null</jk>, the native protocol is used.
		 * @return This object.
		 */
		public Builder protocol(QueryProtocol value) {
			this.protocol = value;
			return this;
		}

		/**
		 * Enables or disables honoring client-supplied regular-expression search.
		 *
		 * <p>
		 * When disabled (the default), a protocol treats a {@code regex=true} search flag as a literal substring
		 * search, closing the ReDoS/CPU-burn vector of running arbitrary caller regexes against every row.
		 *
		 * @param value <jk>true</jk> to allow regex search.
		 * @return This object.
		 */
		public Builder allowRegexSearch(boolean value) {
			this.allowRegexSearch = value;
			return this;
		}

		/**
		 * Sets the maximum number of column descriptors a protocol should parse.
		 *
		 * @param value The cap.  Values &lt; 1 are clamped to 1.
		 * @return This object.
		 */
		public Builder maxColumns(int value) {
			this.maxColumns = Math.max(1, value);
			return this;
		}

		/**
		 * Sets the maximum number of order descriptors a protocol should parse.
		 *
		 * @param value The cap.  Values &lt; 1 are clamped to 1.
		 * @return This object.
		 */
		public Builder maxOrderColumns(int value) {
			this.maxOrderColumns = Math.max(1, value);
			return this;
		}

		/**
		 * Builds the settings.
		 *
		 * @return A new {@link QueryableSettings}.
		 */
		public QueryableSettings build() {
			return new QueryableSettings(this);
		}
	}
}
