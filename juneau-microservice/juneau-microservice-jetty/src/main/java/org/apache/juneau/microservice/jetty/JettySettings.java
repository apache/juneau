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
package org.apache.juneau.microservice.jetty;

import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;

/**
 * Programmatic settings for the Jetty server contributed by {@link JettyConfiguration}.
 *
 * <p>
 * Users who need to override the defaults pulled from <c>Config</c> / <c>ManifestFile</c> should
 * contribute a <c>@Bean JettySettings</c> from their own <c>@Configuration</c> class.  Any value
 * left {@code null} on this settings bean falls back to the corresponding <c>Config</c> / <c>ManifestFile</c>
 * entry, then to a hard-coded default.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Configuration</ja>
 * 	<jk>public class</jk> MyAppConfig {
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> JettySettings jettySettings() {
 * 			<jk>return</jk> JettySettings.<jsm>create</jsm>()
 * 				.ports(8080, 0)
 * 				.build();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceJetty">juneau-microservice-jetty Basics</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class JettySettings {

	/**
	 * Builder for {@link JettySettings}.
	 */
	public static class Builder {

		int[] ports;
		String jettyXml;
		Boolean jettyXmlResolveVars;
		Duration stopTimeout;
		Duration shutdownSettleDelay;

		Builder() {}

		/**
		 * Builds a new {@link JettySettings} from this builder.
		 *
		 * @return A new settings instance.
		 */
		public JettySettings build() {
			return new JettySettings(this);
		}

		/**
		 * Specifies the ports to try when binding the Jetty server.
		 *
		 * <p>
		 * The first available port is used.  A value of <c>0</c> indicates "try a random port".  When set, this
		 * value takes precedence over the <c>Jetty/port</c> config-file entry and the <c>Jetty-Port</c>
		 * manifest entry.
		 *
		 * @param value The candidate ports, in priority order.  Can be <jk>null</jk> to defer to config / manifest.
		 * @return This object.
		 */
		public Builder ports(int...value) {
			ports = cp(value);
			return this;
		}

		/**
		 * Specifies the contents or location of the <c>jetty.xml</c> file used to build the Jetty server.
		 *
		 * <p>
		 * When set, this value takes precedence over the <c>Jetty/config</c> config-file entry and the
		 * <c>Jetty-Config</c> manifest entry.  When not set, the component falls back to those values and
		 * finally to a <c>jetty.xml</c> file located on the file system or classpath.
		 *
		 * @param value The contents or location of the file.  Can be any of:
		 * 	<ul>
		 * 		<li>{@link String} - Relative path to a file on the file system or classpath, or the raw XML content if it begins with <c>&lt;</c>.
		 * 		<li>{@link File} - File on the file system.
		 * 		<li>{@link Path} - Path on the file system.
		 * 		<li>{@link InputStream} - Raw contents as a <c>UTF-8</c> encoded stream.
		 * 		<li>{@link Reader} - Raw contents.
		 * 	</ul>
		 * @param resolveVars If <jk>true</jk>, SVL variables in the file will automatically be resolved.
		 * @return This object.
		 * @throws IOException Thrown by underlying stream.
		 */
		public Builder jettyXml(Object value, boolean resolveVars) throws IOException {
			if (value == null)
				jettyXml = null;
			else if (value instanceof String value2)
				jettyXml = value2.startsWith("<") ? value2 : read(new File(value2));
			else if (value instanceof File value2)
				jettyXml = read(value2);
			else if (value instanceof Path value2)
				jettyXml = read(value2);
			else if (value instanceof InputStream value2)
				jettyXml = read(value2);
			else if (value instanceof Reader value2)
				jettyXml = read(value2);
			else
				throw rex("Invalid object type passed to jettyXml(Object): %s", cn(value));
			jettyXmlResolveVars = resolveVars;
			return this;
		}

		/**
		 * Specifies the bounded graceful-shutdown drain timeout for the Jetty server.
		 *
		 * <p>
		 * On stop, the server stops accepting new connections and waits up to this duration for in-flight requests
		 * to complete before the connector closes (Jetty's <c>stopTimeout</c>).  When set, this value takes
		 * precedence over the <c>Jetty/stopTimeout</c> config-file entry (in milliseconds).  When neither is set,
		 * a default of {@code 30s} is applied.
		 *
		 * @param value The drain timeout.  Can be <jk>null</jk> to defer to config, then to the default.
		 * @return This object.
		 */
		public Builder stopTimeout(Duration value) {
			stopTimeout = value;
			return this;
		}

		/**
		 * Specifies the settle delay applied between flipping the readiness probe out of service and stopping the
		 * connector.
		 *
		 * <p>
		 * On stop, the readiness probe ({@code /readyz}) flips to {@code 503} <i>before</i> the connector closes so a
		 * load balancer / Kubernetes stops routing new traffic.  This brief delay gives the load balancer time to
		 * observe the {@code 503} before in-flight requests are drained.  When set, this value takes precedence over
		 * the <c>Jetty/shutdownSettleDelay</c> config-file entry (in milliseconds).  When neither is set, no settle
		 * delay is applied (defaults to {@code 0}) &mdash; the recommended Kubernetes pattern is a {@code preStop}
		 * hook sleep instead.
		 *
		 * @param value The settle delay.  Can be <jk>null</jk> to defer to config, then to the default.
		 * @return This object.
		 */
		public Builder shutdownSettleDelay(Duration value) {
			shutdownSettleDelay = value;
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
	private final String jettyXml;
	private final Boolean jettyXmlResolveVars;
	private final Duration stopTimeout;
	private final Duration shutdownSettleDelay;

	JettySettings(Builder b) {
		ports = b.ports;
		jettyXml = b.jettyXml;
		jettyXmlResolveVars = b.jettyXmlResolveVars;
		stopTimeout = b.stopTimeout;
		shutdownSettleDelay = b.shutdownSettleDelay;
	}

	/**
	 * Returns the candidate ports to try when binding the Jetty server, in priority order.
	 *
	 * @return The ports, or <jk>null</jk> if not set.
	 */
	public int[] getPorts() {
		return cp(ports);
	}

	/**
	 * Returns the raw <c>jetty.xml</c> contents to use when building the server.
	 *
	 * @return The XML contents, or <jk>null</jk> if not set.
	 */
	public String getJettyXml() {
		return jettyXml;
	}

	/**
	 * Returns whether SVL variables in the <c>jetty.xml</c> file should be resolved.
	 *
	 * @return The resolve-vars flag, or <jk>null</jk> if not set.
	 */
	public Boolean getJettyXmlResolveVars() {
		return jettyXmlResolveVars;
	}

	/**
	 * Returns the bounded graceful-shutdown drain timeout for the Jetty server.
	 *
	 * @return The drain timeout, or <jk>null</jk> if not set.
	 */
	public Duration getStopTimeout() {
		return stopTimeout;
	}

	/**
	 * Returns the settle delay applied between flipping the readiness probe out of service and stopping the connector.
	 *
	 * @return The settle delay, or <jk>null</jk> if not set.
	 */
	public Duration getShutdownSettleDelay() {
		return shutdownSettleDelay;
	}
}
