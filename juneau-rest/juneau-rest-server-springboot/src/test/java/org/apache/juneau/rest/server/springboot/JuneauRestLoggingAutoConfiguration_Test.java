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

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Handler;
import java.util.logging.Logger;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.slf4j.*;
import org.slf4j.helpers.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.builder.*;
import org.springframework.boot.test.context.runner.*;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.jul.*;

/**
 * Tests for {@link JuneauRestLoggingAutoConfiguration}.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.SpringbootTest
class JuneauRestLoggingAutoConfiguration_Test extends TestBase {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JuneauRestLoggingAutoConfiguration.class));

	@Test void a01_discoveryComesFromAutoConfigurationImports() {
		try (var ctx = new SpringApplicationBuilder(A01_DiscoveryApp.class)
			.web(WebApplicationType.NONE)
			.properties("spring.main.banner-mode=off", "juneau.rest.logging.propagate-levels=true")
			.run()) {
			assertTrue(ctx.containsBean("juneauRestLogLevelPropagatorInstaller"));
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class A01_DiscoveryApp {}

	@Test void b01_defaultOn_contributesInstallerBean() {
		runner.run(ctx -> assertThat(ctx).hasBean("juneauRestLogLevelPropagatorInstaller"));
	}

	@Test void b02_propertyOff_disablesInstallerBean() {
		runner.withPropertyValues("juneau.rest.logging.propagate-levels=false")
			.run(ctx -> assertThat(ctx).doesNotHaveBean("juneauRestLogLevelPropagatorInstaller"));
	}

	@Test void c01_nonLogbackBackend_noopsSilently() {
		var cfg = new JuneauRestLoggingAutoConfiguration();
		var backend = new ILoggerFactory() {
			@Override
			public org.slf4j.Logger getLogger(String name) {
				return NOPLogger.NOP_LOGGER;
			}
		};
		assertDoesNotThrow(() -> cfg.install(backend, false));
	}

	@Test void d01_installsSingleResetResistantPropagator_andSurvivesReset() {
		var cfg = new JuneauRestLoggingAutoConfiguration();
		var context = new LoggerContext();
		var jul = Logger.getLogger("todo368.phase3.d01");
		var prev = jul.getLevel();
		try {
			jul.setLevel(null);
			cfg.install(context, false);
			cfg.install(context, false);

			var propagators = context.getCopyOfListenerList().stream()
				.filter(LevelChangePropagator.class::isInstance)
				.map(LevelChangePropagator.class::cast)
				.toList();
			assertEquals(1, propagators.size(), "installer must be idempotent via instanceof check");
			assertTrue(propagators.get(0).isResetResistant(), "installed listener must survive LoggerContext.reset()");

			context.getLogger("todo368.phase3.d01").setLevel(Level.DEBUG);
			assertEquals(java.util.logging.Level.FINE, jul.getLevel(), "DEBUG should propagate to JUL FINE");

			context.reset();
			context.getLogger("todo368.phase3.d01").setLevel(Level.TRACE);
			assertEquals(java.util.logging.Level.FINEST, jul.getLevel(),
				"propagation should remain active after reset");
		} finally {
			jul.setLevel(prev);
			context.stop();
		}
	}

	@Test void d02_defaultResetJulFalse_preservesExistingJulState() {
		var cfg = new JuneauRestLoggingAutoConfiguration();
		var context = new LoggerContext();
		var jul = Logger.getLogger("todo368.phase3.d02");
		var prevLevel = jul.getLevel();
		var prevUseParentHandlers = jul.getUseParentHandlers();
		var prevHandlers = jul.getHandlers();
		var marker = new Handler() {
			@Override public void publish(java.util.logging.LogRecord rec) {
				// No-op: this handler only marks its presence to verify it survives cfg.install(...).
			}
			@Override public void flush() {
				// No-op: marker handler; nothing to flush.
			}
			@Override public void close() {
				// No-op: marker handler; nothing to close.
			}
		};
		try {
			for (var h : prevHandlers)
				jul.removeHandler(h);
			jul.addHandler(marker);
			jul.setUseParentHandlers(false);
			jul.setLevel(java.util.logging.Level.WARNING);

			cfg.install(context, false);

			assertEquals(java.util.logging.Level.WARNING, jul.getLevel(),
				"reset-jul defaults to false, so explicit JUL levels stay intact");
			assertThat(jul.getHandlers()).contains(marker);
		} finally {
			jul.removeHandler(marker);
			for (var h : prevHandlers)
				jul.addHandler(h);
			jul.setUseParentHandlers(prevUseParentHandlers);
			jul.setLevel(prevLevel);
			context.stop();
		}
	}

	@Test void d03_resetJulTrue_clearsAndReappliesFromLogback() {
		var cfg = new JuneauRestLoggingAutoConfiguration();
		var context = new LoggerContext();
		var name = "todo368.phase3.d03";
		var jul = Logger.getLogger(name);
		var prev = jul.getLevel();
		try {
			jul.setLevel(java.util.logging.Level.WARNING);
			context.getLogger(name).setLevel(Level.ERROR);

			cfg.install(context, true);

			assertEquals(java.util.logging.Level.SEVERE, jul.getLevel(),
				"reset-jul=true should reapply the explicit Logback level into JUL");
		} finally {
			jul.setLevel(prev);
			context.stop();
		}
	}
}
