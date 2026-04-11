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
package org.apache.juneau.ng.rest.client;

import java.util.*;
import java.util.function.*;

/**
 * Determines the {@link java.lang.System.Logger.Level} for a {@link RestLogEntry}.
 *
 * <p>
 * Used by {@link BasicRestLogger} (and available to custom {@link RestLogger} implementations via
 * {@link RestLogEntry#getLevel()}) to decide how to emit each log line.
 *
 * <p>
 * The built-in {@link #DEFAULT} resolver applies these ordered rules:
 * <ul>
 * 	<li>{@link java.lang.System.Logger.Level#ERROR} — transport error or HTTP status ≥ 500
 * 	<li>{@link java.lang.System.Logger.Level#WARNING} — HTTP status ≥ 400, or response contains a {@code Thrown} header
 * 	<li>{@link java.lang.System.Logger.Level#INFO} — all other calls
 * </ul>
 *
 * <p>
 * Custom resolvers can be built using the fluent {@link #rules()} builder:
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	RestLogLevelResolver <jv>resolver</jv> = RestLogLevelResolver.<jsm>rules</jsm>()
 * 		.rule(System.Logger.Level.<jsf>ERROR</jsf>, <jv>e</jv> -&gt; <jv>e</jv>.getStatusCode() &gt;= 500)
 * 		.rule(System.Logger.Level.<jsf>WARNING</jsf>, <jv>e</jv> -&gt; <jv>e</jv>.getStatusCode() &gt;= 400)
 * 		.defaultLevel(System.Logger.Level.<jsf>INFO</jsf>)
 * 		.build();
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@FunctionalInterface
public interface RestLogLevelResolver {

	/**
	 * Computes the log level for the given entry.
	 *
	 * @param entry The log entry. Never <jk>null</jk>.
	 * @return The computed level. Never <jk>null</jk>.
	 */
	System.Logger.Level resolve(RestLogEntry entry);

	/**
	 * The default level resolver.
	 *
	 * <ul>
	 * 	<li>{@link java.lang.System.Logger.Level#ERROR} — transport error ({@code getError() != null}) or HTTP status ≥ 500
	 * 	<li>{@link java.lang.System.Logger.Level#WARNING} — HTTP status ≥ 400, or response has a {@code Thrown} header
	 * 	  (a Juneau server-side header indicating an exception was thrown)
	 * 	<li>{@link java.lang.System.Logger.Level#INFO} — all other calls
	 * </ul>
	 */
	RestLogLevelResolver DEFAULT = rules()
		.rule(System.Logger.Level.ERROR, e -> e.getError() != null || e.getStatusCode() >= 500)
		.rule(System.Logger.Level.WARNING, e -> e.getStatusCode() >= 400 || e.hasResponseHeader("Thrown"))
		.defaultLevel(System.Logger.Level.INFO)
		.build();

	/**
	 * Returns a new {@link RuleBuilder} for constructing ordered predicate-based level resolvers.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	static RuleBuilder rules() {
		return new RuleBuilder();
	}

	/**
	 * Fluent builder for ordered predicate-based {@link RestLogLevelResolver} instances.
	 *
	 * <p>
	 * Rules are evaluated in registration order; the first matching rule wins. If no rule matches,
	 * the {@link #defaultLevel(java.lang.System.Logger.Level)} is used (default: {@link java.lang.System.Logger.Level#INFO}).
	 *
	 * @since 9.2.1
	 */
	class RuleBuilder {

		private final List<Rule> rules = new ArrayList<>();
		private System.Logger.Level defaultLevel = System.Logger.Level.INFO;

		private RuleBuilder() {}

		/**
		 * Adds a rule that maps to the given level when the predicate matches.
		 *
		 * @param level The level to emit when the predicate is true. Must not be <jk>null</jk>.
		 * @param when The predicate tested against each log entry. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public RuleBuilder rule(System.Logger.Level level, Predicate<RestLogEntry> when) {
			rules.add(new Rule(level, when));
			return this;
		}

		/**
		 * Sets the fallback level used when no rule matches.
		 *
		 * @param value The default level. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public RuleBuilder defaultLevel(System.Logger.Level value) {
			defaultLevel = value;
			return this;
		}

		/**
		 * Builds and returns the {@link RestLogLevelResolver}.
		 *
		 * @return A new instance. Never <jk>null</jk>.
		 */
		public RestLogLevelResolver build() {
			var snapshot = List.copyOf(rules);
			var fallback = defaultLevel;
			return entry -> {
				for (var rule : snapshot)
					if (rule.when().test(entry))
						return rule.level();
				return fallback;
			};
		}

		private record Rule(System.Logger.Level level, Predicate<RestLogEntry> when) {}
	}
}
