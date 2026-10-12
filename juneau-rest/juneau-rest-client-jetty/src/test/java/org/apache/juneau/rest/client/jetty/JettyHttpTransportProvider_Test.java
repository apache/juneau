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
package org.apache.juneau.rest.client.jetty;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link JettyHttpTransportProvider}.
 */
@SuppressWarnings({
	"resource" // Transport instances are closed explicitly or are never started.
})
class JettyHttpTransportProvider_Test {

	/** Defines the provider class itself in this loader (keeping its code source) and hides the Jetty client classes from it. */
	private static final class NoJettyClassLoader extends ClassLoader {
		NoJettyClassLoader() {
			super(JettyHttpTransportProvider_Test.class.getClassLoader());
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (name.startsWith("org.eclipse.jetty.client."))
				throw new ClassNotFoundException(name);
			if (! name.equals(JettyHttpTransportProvider.class.getName()))
				return super.loadClass(name, resolve);
			synchronized (getClassLoadingLock(name)) {
				var loaded = findLoadedClass(name);
				if (loaded == null) {
					try (var in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
						var bytes = in.readAllBytes();
						loaded = defineClass(name, bytes, 0, bytes.length, JettyHttpTransportProvider.class.getProtectionDomain());
					} catch (IOException e) {
						throw new ClassNotFoundException(name, e);
					}
				}
				return loaded;
			}
		}
	}

	@Test
	void a01_priority() {
		assertEquals(75, new JettyHttpTransportProvider().getPriority());
	}

	@Test
	void a02_isAvailable_jettyOnClasspath() {
		assertTrue(new JettyHttpTransportProvider().isAvailable());
	}

	@Test
	void a03_isAvailable_jettyAbsent() throws Exception {
		var provider = (HttpTransportProvider)new NoJettyClassLoader().loadClass(JettyHttpTransportProvider.class.getName()).getDeclaredConstructor().newInstance();
		assertFalse(provider.isAvailable());
	}

	@Test
	void a04_create_returnsJettyTransport() throws Exception {
		try (var transport = new JettyHttpTransportProvider().create()) {
			assertInstanceOf(JettyHttpTransport.class, transport);
			assertTrue(transport.supportsUrlPolicy());
		}
	}
}
