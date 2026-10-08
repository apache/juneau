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
package org.apache.juneau.rest.server.views;

import java.util.*;

import org.apache.juneau.commons.bean.*;

/**
 * The response shape of an {@code aggregate}-mode bulk action ({@link RowAction.BulkMode#AGGREGATE}) &mdash;
 * one outcome per id submitted in the matching {@link BulkRequest}.
 *
 * <h5 class='section'>Own, independently-versioned contract</h5>
 * <p>
 * {@link #CONTRACT_VERSION} is this type's own wire contract, never aliased to {@code VIEW}, {@code ACTION_RESULT},
 * or {@link BulkMutateDef#CONTRACT_VERSION} &mdash; each of those versions what it defines (a view's shape, a
 * single-row action's outcome, a bulk-mutate action list) and this one versions only this response shape.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(path=<js>"/changes/abort"</js>)
 * 	<jk>public</jk> BulkResult abortChanges(<ja>@Content</ja> BulkRequest <jv>request</jv>) {
 * 		BulkResult.Builder <jv>result</jv> = BulkResult.<jsm>create</jsm>();
 * 		<jk>for</jk> (String <jv>id</jv> : <jv>request</jv>.ids) {
 * 			Change <jv>change</jv> = <jv>changeStore</jv>.find(<jv>id</jv>);
 * 			<jk>if</jk> (<jv>change</jv> == <jk>null</jk>) {
 * 				<jv>result</jv>.notFound(<jv>id</jv>);
 * 			} <jk>else if</jk> (!<js>"PENDING"</js>.equals(<jv>change</jv>.state)) {
 * 				<jv>result</jv>.failed(<jk>new</jk> BulkResult.Failure(<jv>id</jv>, <js>"Change is already merged."</js>));
 * 			} <jk>else</jk> {
 * 				<jv>changeStore</jv>.abort(<jv>id</jv>);
 * 				<jv>result</jv>.succeeded(<jv>id</jv>);
 * 			}
 * 		}
 * 		<jk>return</jk> <jv>result</jv>.build();
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
@BeanType(properties="contractVersion,succeeded,notFound,failed")
public final class BulkResult {

	/** This type's own wire contract version. */
	public static final String CONTRACT_VERSION = "1";

	/**
	 * One failed-outcome entry: the id that was submitted, and why it failed.
	 *
	 * @param id The submitted id.
	 * @param message Why it failed.
	 * @since 10.0.0
	 */
	public record Failure(String id, String message) {}

	/** The frozen contract-version discriminator (always {@value #CONTRACT_VERSION}). */
	public final String contractVersion;

	/** The ids that succeeded. */
	public final List<String> succeeded;

	/** The ids that were submitted but no longer exist. */
	public final List<String> notFound;

	/** The ids that failed, with a reason each. */
	public final List<Failure> failed;

	private BulkResult(Builder b) {
		contractVersion = CONTRACT_VERSION;
		succeeded = List.copyOf(b.succeeded);
		notFound = List.copyOf(b.notFound);
		failed = List.copyOf(b.failed);
	}

	/**
	 * Starts a new {@link Builder}.
	 *
	 * @return A new, empty builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * This result's contract version.
	 *
	 * @return {@link #CONTRACT_VERSION}.
	 */
	public String contractVersion() {
		return contractVersion;
	}

	/**
	 * The ids that succeeded.
	 *
	 * @return An immutable list. Never <jk>null</jk>.
	 */
	public List<String> succeeded() {
		return succeeded;
	}

	/**
	 * The ids that were submitted but no longer exist (e.g. deleted between selection and submit).
	 *
	 * @return An immutable list. Never <jk>null</jk>.
	 */
	public List<String> notFound() {
		return notFound;
	}

	/**
	 * The ids that failed, with a reason each.
	 *
	 * @return An immutable list. Never <jk>null</jk>.
	 */
	public List<Failure> failed() {
		return failed;
	}

	/**
	 * A mutable builder for {@link BulkResult}.
	 *
	 * @since 10.0.0
	 */
	public static final class Builder {

		private final List<String> succeeded = new ArrayList<>();
		private final List<String> notFound = new ArrayList<>();
		private final List<Failure> failed = new ArrayList<>();

		private Builder() {}

		/**
		 * Appends ids to the succeeded list.
		 *
		 * @param ids The ids. May be called more than once; each call appends.
		 * @return This builder.
		 */
		public Builder succeeded(String...ids) {
			succeeded.addAll(Arrays.asList(ids));
			return this;
		}

		/**
		 * Appends ids to the not-found list.
		 *
		 * @param ids The ids. May be called more than once; each call appends.
		 * @return This builder.
		 */
		public Builder notFound(String...ids) {
			notFound.addAll(Arrays.asList(ids));
			return this;
		}

		/**
		 * Appends failures to the failed list.
		 *
		 * @param failures The failures. May be called more than once; each call appends.
		 * @return This builder.
		 */
		public Builder failed(Failure...failures) {
			failed.addAll(Arrays.asList(failures));
			return this;
		}

		/**
		 * Builds the immutable {@link BulkResult}.
		 *
		 * @return A new {@link BulkResult}.
		 */
		public BulkResult build() {
			return new BulkResult(this);
		}
	}
}
