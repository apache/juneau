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
package org.apache.juneau.rest.server.widgets;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link AvatarChip} factory / setter coverage and {@link AvatarChip#validate()} edge cases (L9 A same-origin
 * {@code imageUrl}).  Also pins that {@code AvatarChip} is <b>not</b> a {@link Widget} (S1).
 */
class AvatarChip_Test extends TestBase {

	@Test void a01_of_initials() {
		var a = AvatarChip.of("Ada L.").initials("AL").status(Status.ONLINE);
		assertEquals("Ada L.", a.displayName);
		assertEquals(Status.ONLINE, a.status);
		a.validate();
	}

	@Test void a02_of_sameOriginImage() {
		var a = AvatarChip.of("Ada L.").imageUrl("/avatars/ada.png");
		a.validate();
	}

	@Test void a03_nullStatus_isNoRing() {
		var a = AvatarChip.of("Ada").initials("AD");
		assertNull(a.status);
		a.validate();
	}

	@Test void a04_displayNameRequired() {
		var a = AvatarChip.of("  ").initials("X");
		assertThrows(IllegalArgumentException.class, () -> a.validate());
	}

	@Test void a05_neitherInitialsNorImage_rejected() {
		var a = AvatarChip.of("Ada");
		var e = assertThrows(IllegalArgumentException.class, () -> a.validate());
		assertTrue(e.getMessage().contains("initials"), e::getMessage);
	}

	@Test void a06_absoluteOrProtocolRelativeImage_rejected() {
		for (var bad : new String[]{"http://cdn/x.png", "https://gravatar.example/x", "//cdn/x.png", "../x.png", "data:image/png;base64,AAA"}) {
			var a = AvatarChip.of("Ada").imageUrl(bad);
			assertThrows(IllegalArgumentException.class, () -> a.validate(), () -> "expected reject for " + bad);
		}
	}

	@Test void a07_menu_fansOut() {
		var a = AvatarChip.of("Ada").initials("AD").menu(MenuItem.link("dup", "A", "/a"), MenuItem.link("dup", "B", "/b"));
		assertThrows(IllegalArgumentException.class, a::validate);
	}

	@Test void a08_notAWidget() {
		assertFalse(Widget.class.isAssignableFrom(AvatarChip.class));
	}
}
