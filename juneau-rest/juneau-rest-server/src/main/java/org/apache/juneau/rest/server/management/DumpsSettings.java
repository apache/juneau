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
 * Deny-by-default exposure settings for the diagnostic dump endpoints ({@code /threaddump}, {@code /heapdump}).
 *
 * <p>
 * Both dumps are <b>off by default</b> on security grounds &mdash; a thread dump can leak sensitive state and a
 * heap dump can be large and contain secrets.  A consumer opts in <i>explicitly</i> by registering a
 * {@code DumpsSettings} bean (built with {@link Builder#enableThreadDump()} / {@link Builder#enableHeapDump()})
 * in the resource's bean store.  When no such bean is present, {@link DumpsManager} resolves the default
 * (both disabled) and the endpoints respond {@code 403 Forbidden}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link DumpsManager}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ManagementSurface">Management Surface</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class DumpsSettings {

	/** The default (deny-all) settings used when no bean is registered. */
	public static final DumpsSettings DEFAULT = create().build();

	private final boolean threadDumpEnabled;
	private final boolean heapDumpEnabled;

	private DumpsSettings(Builder b) {
		this.threadDumpEnabled = b.threadDumpEnabled;
		this.heapDumpEnabled = b.heapDumpEnabled;
	}

	/**
	 * Builder creator.
	 *
	 * @return A new builder (both dumps disabled).
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * @return <jk>true</jk> if the {@code /threaddump} endpoint is enabled.
	 */
	public boolean isThreadDumpEnabled() {
		return threadDumpEnabled;
	}

	/**
	 * @return <jk>true</jk> if the {@code /heapdump} endpoint is enabled.
	 */
	public boolean isHeapDumpEnabled() {
		return heapDumpEnabled;
	}

	/**
	 * Builder for {@link DumpsSettings}.
	 */
	public static class Builder {
		private boolean threadDumpEnabled;
		private boolean heapDumpEnabled;

		/**
		 * Enables the {@code /threaddump} endpoint.
		 *
		 * @return This object.
		 */
		public Builder enableThreadDump() {
			threadDumpEnabled = true;
			return this;
		}

		/**
		 * Enables the {@code /heapdump} endpoint.
		 *
		 * @return This object.
		 */
		public Builder enableHeapDump() {
			heapDumpEnabled = true;
			return this;
		}

		/**
		 * Builds the settings.
		 *
		 * @return A new {@link DumpsSettings}.
		 */
		public DumpsSettings build() {
			return new DumpsSettings(this);
		}
	}
}
