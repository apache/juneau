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
package org.apache.juneau.microservice.tomcat;

/**
 * Programmatic settings for the embedded Tomcat server contributed by {@link TomcatConfiguration}.
 *
 * <p>
 * Users who need to override the defaults pulled from <c>Config</c> / <c>ManifestFile</c> should
 * contribute a <c>@Bean TomcatSettings</c> from their own <c>@Configuration</c> class.  Any value
 * left {@code null} on this settings bean falls back to the corresponding <c>Config</c> / <c>ManifestFile</c>
 * entry, then to a hard-coded default.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Configuration</ja>
 * 	<jk>public class</jk> MyAppConfig {
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> TomcatSettings tomcatSettings() {
 * 			<jk>return</jk> TomcatSettings.<jsm>create</jsm>()
 * 				.ports(8080, 0)
 * 				.build();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceTomcatBasics">juneau-microservice-tomcat Basics</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class TomcatSettings {

	/**
	 * Builder for {@link TomcatSettings}.
	 */
	public static class Builder {

		int[] ports;
		String baseDir;

		Builder() {}

		/**
		 * Builds a new {@link TomcatSettings} from this builder.
		 *
		 * @return A new settings instance.
		 */
		public TomcatSettings build() {
			return new TomcatSettings(this);
		}

		/**
		 * Specifies the ports to try when binding the Tomcat server.
		 *
		 * <p>
		 * The first available port is used.  A value of <c>0</c> indicates "try a random port".  When set, this
		 * value takes precedence over the <c>Tomcat/port</c> config-file entry and the <c>Tomcat-Port</c>
		 * manifest entry.
		 *
		 * @param value The candidate ports, in priority order.  Can be <jk>null</jk> to defer to config / manifest.
		 * @return This object.
		 */
		public Builder ports(int...value) {
			ports = value;
			return this;
		}

		/**
		 * Specifies the Catalina base directory used by the embedded Tomcat server for its work files.
		 *
		 * <p>
		 * When set, the {@link TomcatServerFactory} uses this directory as both the Catalina base and the root
		 * context doc-base, and the directory is left in place after the server stops.  When not set, the
		 * {@link TomcatServerComponent} creates a fresh temp directory on start-up and deletes it on stop.
		 *
		 * @param value The absolute or relative path to the base directory.  Can be <jk>null</jk> to use an
		 * 	auto-created temp directory.
		 * @return This object.
		 */
		public Builder baseDir(String value) {
			baseDir = value;
			return this;
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final int[] ports;
	private final String baseDir;

	TomcatSettings(Builder b) {
		ports = b.ports;
		baseDir = b.baseDir;
	}

	/**
	 * Returns the candidate ports to try when binding the Tomcat server, in priority order.
	 *
	 * @return The ports, or <jk>null</jk> if not set.
	 */
	public int[] getPorts() {
		return ports == null ? null : ports.clone();
	}

	/**
	 * Returns the Catalina base directory to use when building the Tomcat server.
	 *
	 * @return The base directory path, or <jk>null</jk> if not set (an auto-created temp directory is used).
	 */
	public String getBaseDir() {
		return baseDir;
	}
}
