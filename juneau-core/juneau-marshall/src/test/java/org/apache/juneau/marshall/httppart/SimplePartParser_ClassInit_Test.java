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
package org.apache.juneau.marshall.httppart;

import static org.apache.juneau.commons.httppart.HttpPartType.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that parsing a string into a {@link Class} resolves the class without running its static initializer.
 */
class SimplePartParser_ClassInit_Test extends TestBase {

	static final AtomicBoolean INITIALIZED = new AtomicBoolean();

	public static class Probe {
		static {
			INITIALIZED.set(true);
		}
	}

	public static class Bean {
		public Class<?> type;
	}

	@BeforeEach void setUp() {
		INITIALIZED.set(false);
	}

	@Test void a01_simplePartParser_doesNotInitializeClass() throws Exception {
		var session = SimplePartParser.DEFAULT.getPartSession();
		var c = session.read(QUERY, null, Probe.class.getName(), (ClassMeta<Class<?>>)(ClassMeta<?>)MarshallingContext.DEFAULT.getClassMeta(Class.class));
		assertEquals(Probe.class, c);
		assertFalse(INITIALIZED.get(), "Static initializer ran while parsing a Class value");
	}

	@Test void a02_classMeta_newInstanceFromString_doesNotInitializeClass() throws Exception {
		var cm = MarshallingContext.DEFAULT.getClassMeta(Class.class);
		assertEquals(Probe.class, cm.newInstanceFromString(null, Probe.class.getName()));
		assertFalse(INITIALIZED.get(), "Static initializer ran while parsing a Class value");
	}

	@Test void a03_jsonBeanProperty_doesNotInitializeClass() throws Exception {
		var b = Json5.to("{type:'" + Probe.class.getName() + "'}", Bean.class);
		assertEquals(Probe.class, b.type);
		assertFalse(INITIALIZED.get(), "Static initializer ran while parsing a Class value");
	}
}
