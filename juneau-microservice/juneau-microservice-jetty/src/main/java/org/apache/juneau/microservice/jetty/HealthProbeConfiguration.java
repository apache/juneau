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

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.health.*;

import jakarta.servlet.*;

/**
 * Opt-in probe configuration that auto-mounts {@link HealthServlet}.
 *
 * @since 10.0.0
 */
@Configuration
public class HealthProbeConfiguration {

	/**
	 * Default probe settings bean.
	 *
	 * @return Default settings.
	 */
	@Bean
	@ConditionalOnMissingBean(HealthProbeSettings.class)
	public HealthProbeSettings healthProbeSettings() {
		return HealthProbeSettings.create().build();
	}

	/**
	 * Probe servlet bean discovered by {@link JettyServerComponent}.
	 *
	 * @return Probe servlet.
	 */
	@Bean(name="healthProbeServlet")
	@ConditionalOnMissingBean(name="healthProbeServlet")
	public Servlet healthProbeServlet() {
		return new HealthServlet();
	}
}
