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

package org.apache.juneau.releng;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.rest.server.console.Theme;
import org.junit.jupiter.api.Test;

class ReleaseManagerThemeTest {

	@Test
	void a01_derivesFromLightRedAndKeepsDangerTagTriad() {
		var tokens = ReleaseManagerTheme.INSTANCE.getTokens();
		assertEquals("release-manager", ReleaseManagerTheme.INSTANCE.getName());
		assertEquals(Theme.LIGHT_RED.getTokens().get("--jc-btn-primary"), tokens.get("--jc-btn-primary"));
		assertEquals(Theme.LIGHT_RED.getTokens().get("--jc-chrome-bg"), tokens.get("--jc-chrome-bg"));
		assertNotEquals("#BF2600", tokens.get("--jc-chrome-bg"));
		assertNotEquals("#1589EE", tokens.get("--jc-accent"));
		assertEquals("#fdeceb", tokens.get("--jc-tag-red-bg"));
		assertEquals("#c23934", tokens.get("--jc-tag-red-text"));
		assertEquals("#f3c6c2", tokens.get("--jc-tag-red-border"));
		assertNotEquals(Theme.OPEN.getTokens().get("--jc-tag-red-text"), tokens.get("--jc-tag-red-text"));
	}
}
