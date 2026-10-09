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
package org.apache.juneau.rest.server.terminal;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.junit.jupiter.api.*;

/**
 * The pinned xterm.js WebJar: its version resolves and it ships the UMD bundle and the stylesheet.
 */
class XtermWebJar_Test extends TestBase {

	@Test void a01_pinnedVersionResolves() {
		assertEquals("5.5.0", WebJarResolver.version("org.webjars.npm", "xterm__xterm"));
	}

	@Test void a02_umdBundleAndStylesheetArePresent() {
		var cl = getClass().getClassLoader();
		for (var p : new String[] {"xterm__xterm/5.5.0/lib/xterm.js", "xterm__xterm/5.5.0/css/xterm.css"})
			assertNotNull(cl.getResource(WebJarResolver.WEBJARS_ROOT + p), p);
	}
}
