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
package org.apache.juneau.rest.server.springboot;

import org.slf4j.*;
import org.springframework.beans.factory.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.jul.*;

/**
 * Spring Boot logging bridge auto-configuration.
 *
 * <p>
 * When enabled (default), and only when Logback is the active SLF4J backend, installs one JUL
 * {@code LevelChangePropagator} listener so Spring logging-level changes propagate to JUL.
 *
 * <p>
 * This is additive by default ({@code juneau.rest.logging.propagate-levels.reset-jul=false}) and
 * silently no-ops when the backend is not Logback.
 *
 * @since 10.0.0
 */
@AutoConfiguration
@ConditionalOnClass(name="ch.qos.logback.classic.jul.LevelChangePropagator")
@ConditionalOnProperty(name="juneau.rest.logging.propagate-levels", havingValue="true", matchIfMissing=true)
public class JuneauRestLoggingAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name="juneauRestLogLevelPropagatorInstaller")
	public Runnable juneauRestLogLevelPropagatorInstaller(Environment env) {
		var resetJul = env.getProperty("juneau.rest.logging.propagate-levels.reset-jul", Boolean.class, false);
		return () -> install(LoggerFactory.getILoggerFactory(), resetJul);
	}

	@Bean
	@ConditionalOnBean(name="juneauRestLogLevelPropagatorInstaller")
	public SmartInitializingSingleton juneauRestLogLevelPropagatorBootstrap(Runnable juneauRestLogLevelPropagatorInstaller) {
		return juneauRestLogLevelPropagatorInstaller::run;
	}

	void install(ILoggerFactory loggerFactory, boolean resetJul) {
		if (! (loggerFactory instanceof LoggerContext context))
			return;
		if (hasPropagator(context))
			return;

		var propagator = new JuneauResetResistantLevelChangePropagator();
		propagator.setContext(context);
		propagator.setResetJUL(resetJul);
		propagator.start();
		context.addListener(propagator);
	}

	private static boolean hasPropagator(LoggerContext context) {
		for (var listener : context.getCopyOfListenerList())
			if (listener instanceof LevelChangePropagator)
				return true;
		return false;
	}
}
