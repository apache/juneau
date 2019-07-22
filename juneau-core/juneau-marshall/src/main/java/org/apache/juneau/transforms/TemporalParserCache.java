// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.transforms;

import java.time.temporal.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Cache of reusable <c>TemporalParser</c> objects.
 */
class TemporalParserCache {

	private static final Map<Class<? extends Temporal>,TemporalParser<? extends Temporal>> TO_TEMPORAL_CACHE = new ConcurrentHashMap<>();

	/**
	 * Retrieves a temporal parser for the specified {@link Temporal} class.
	 *
	 * @param <T> The temporal class.
	 * @param c The temporal class.
	 * @return The parser for the specified class.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends Temporal> TemporalParser<T> getTemporalParser(Class<T> c) {
		TemporalParser tt = TO_TEMPORAL_CACHE.get(c);
		if (tt == null) {
			tt = new TemporalParser(c);
			TO_TEMPORAL_CACHE.put(c, tt);
		}
		return tt;
	}
}
