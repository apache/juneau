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
package org.apache.juneau.rest.server.management;

/**
 * Exposure settings for the {@code /loggers} endpoint.
 *
 * <p>
 * The {@code GET} (read) endpoints are always exposed; the {@code PUT}/{@code POST} <b>set-level</b> endpoints
 * <b>mutate runtime logging</b> and are therefore <b>deny-by-default</b> on the same sensitive-by-default
 * posture as the diagnostic dumps.  A consumer opts in <i>explicitly</i> by registering a
 * {@code LoggersSettings} bean (built with {@link Builder#enableWrite()}) in the resource's bean store.  When
 * no such bean is present, {@link LoggersManager} resolves the default (writes disabled) and the set-level
 * endpoints respond {@code 403 Forbidden}.
 *
 * <h5 class='topic'>Backend selection (explicit)</h5>
 *
 * <p>
 * The {@code /loggers} endpoint drives a {@link LogBackend}.  By default that is
 * {@link JulLogBackend java.util.logging}.  To drive a different backend (e.g. Logback or Log4j2 via the
 * {@code juneau-rest-server-management-logging} add-on), declare it <i>explicitly</i> with
 * {@link Builder#backend(LogBackend)} &mdash; the endpoint never classpath-scans for, and then drives, a backend
 * the operator did not choose.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link LoggersManager}
 * 	<li class='jc'>{@link LogBackend}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ManagementSurface">Management Surface</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class LoggersSettings {

	/** The default (read-only, JUL-backed) settings used when no bean is registered. */
	public static final LoggersSettings DEFAULT = create().build();

	private final boolean writeEnabled;
	private final LogBackend backend;

	private LoggersSettings(Builder b) {
		this.writeEnabled = b.writeEnabled;
		this.backend = b.backend == null ? JulLogBackend.INSTANCE : b.backend;
	}

	/**
	 * Builder creator.
	 *
	 * @return A new builder (writes disabled).
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * @return <jk>true</jk> if the {@code PUT}/{@code POST} set-level endpoints are enabled.
	 */
	public boolean isWriteEnabled() {
		return writeEnabled;
	}

	/**
	 * @return The explicitly-declared {@link LogBackend} the {@code /loggers} endpoint drives, or
	 * 	{@link JulLogBackend#INSTANCE} when none was declared.  Never <jk>null</jk>.
	 */
	public LogBackend getBackend() {
		return backend;
	}

	/**
	 * Builder for {@link LoggersSettings}.
	 */
	public static class Builder {
		private boolean writeEnabled;
		private LogBackend backend;

		/**
		 * Enables the {@code PUT}/{@code POST} set-level endpoints.
		 *
		 * @return This object.
		 */
		public Builder enableWrite() {
			writeEnabled = true;
			return this;
		}

		/**
		 * Explicitly declares the {@link LogBackend} the {@code /loggers} endpoint drives.
		 *
		 * <p>
		 * When not set, the endpoint drives {@link JulLogBackend java.util.logging}.  Pass a Logback / Log4j2
		 * backend (from {@code juneau-rest-server-management-logging}) or a custom implementation to drive a
		 * different backend &mdash; this is the explicit opt-in; the endpoint does not auto-detect.
		 *
		 * @param value The backend.  <jk>null</jk> resets to the JUL default.
		 * @return This object.
		 */
		public Builder backend(LogBackend value) {
			backend = value;
			return this;
		}

		/**
		 * Builds the settings.
		 *
		 * @return A new {@link LoggersSettings}.
		 */
		public LoggersSettings build() {
			return new LoggersSettings(this);
		}
	}
}
