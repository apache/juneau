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
package org.apache.juneau.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Single deny-list of vendor-specific terms that must never appear in shipped output or sources of this module.
 */
final class DualHatGuard {

	/** Lower-case terms; matched case-insensitively. */
	private static final List<String> DENIED = List.of("slds-", "salesforce", "lightning");

	private DualHatGuard() {}

	/** Fails if the text contains any denied term. */
	static void assertClean(String text) {
		var lower = text.toLowerCase();
		for (var term : DENIED)
			assertFalse(lower.contains(term), () -> "denied term '" + term + "' found in:\n" + text);
	}
}
