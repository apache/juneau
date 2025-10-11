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
package org.apache.juneau.transforms;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;

class LocaleSwap_StringSwapTest extends StringSwapTest_Base {

	private static LocaleSwap SWAP = new LocaleSwap();

	private static <T> StringSwap_Tester<T> tester(int index, String label, T object, StringSwap<T> swap, String expected) {
		return StringSwap_Tester.create(index, label, object, swap, expected, BeanContext.DEFAULT_SESSION).build();
	}

	private static final StringSwap_Tester<?>[] TESTERS = {
		tester(1, "Language only", Locale.ENGLISH, SWAP, "en"),
		tester(2, "Language and country", Locale.JAPAN, SWAP, "ja-JP"),
		tester(3, "null", null, SWAP, null)
	};

	static StringSwap_Tester<?>[] testers() {
		return TESTERS;
	}
}