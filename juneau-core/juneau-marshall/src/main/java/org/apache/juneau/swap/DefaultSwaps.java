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
package org.apache.juneau.swap;

import static org.apache.juneau.commons.utils.Utils.*;

import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.swaps.*;

/**
 * Maintain the list of default swaps used by all serializers and parsers.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SwapBasics">Swap Basics</a>
 * </ul>
 */
public class DefaultSwaps {

	/**
	 * Prevents instantiation.
	 */
	private DefaultSwaps() {}

	private static final Map<Class<?>,ObjectSwap<?,?>> SWAPS = new ConcurrentHashMap<>();
	static {
		SWAPS.put(Locale.class, new LocaleSwap());
		SWAPS.put(Class.class, new ClassSwap());
		SWAPS.put(StackTraceElement.class, new StackTraceElementSwap());
		SWAPS.put(TimeZone.class, new TimeZoneSwap());
		SWAPS.put(ZoneId.class, new ZoneIdSwap());
		SWAPS.put(MatchResult.class, new MatchResultSwap());
		SWAPS.put(URL.class, new UrlSwap());
	}

	/**
	 * Find the default ObjectSwap for the specified class.
	 *
	 * @param ci The class to find the swap for.
	 * @return The matched swap, or <jk>null</jk> if it couldn't be found.
	 */
	@SuppressWarnings({
		"java:S1452"  // Wildcard required - ObjectSwap<?,?> for heterogeneous default swap types
	})
	public static ObjectSwap<?,?> find(ClassInfo ci) {
		var ci2 = ci.getAllParents().stream().filter(x -> nn(SWAPS.get(x.inner()))).findFirst().orElse(null);
		return ci2 == null ? null : SWAPS.get(ci2.inner());
	}
}