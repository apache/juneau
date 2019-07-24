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
package org.apache.juneau.transform;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.reflect.*;

/**
 * Maintain the list of default PojoSwaps used by all serializers and parsers.
 */
public class DefaultTransforms {

	private static final Map<Class<?>,PojoSwap<?,?>> POJO_SWAPS = new ConcurrentHashMap<>();
	static {
//		POJO_SWAPS.put(byte[].class, new ByteArrayBase64Swap());
//		POJO_SWAPS.put(Enumeration.class, new EnumerationSwap());
//		POJO_SWAPS.put(InputStream.class, new InputStreamBase64Swap());
//		POJO_SWAPS.put(Iterator.class, new IteratorSwap());
//		POJO_SWAPS.put(Locale.class, new LocaleSwap());
//		POJO_SWAPS.put(Reader.class, new ReaderSwap());
//		POJO_SWAPS.put(Calendar.class, new TemporalCalendarSwap.IsoInstant());
//		POJO_SWAPS.put(Date.class, new TemporalCalendarSwap.IsoInstant());
//		POJO_SWAPS.put(Temporal.class, new TemporalCalendarSwap.IsoInstant());
//		POJO_SWAPS.put(TimeZone.class, new TimeZoneSwap());
//		POJO_SWAPS.put(XMLGregorianCalendar.class, new XMLGregorianCalendarSwap());
	}

	/**
	 * Find the default PojoSwap for the specified class.
	 *
	 * @param c The class to find the swap for.
	 * @return The matched swap, or <jk>null</jk> if it couldn't be found.
	 */
	public static PojoSwap<?,?> findDefaultSwap(Class<?> c) {
		ClassInfo ci = ClassInfo.of(c);
		for (ClassInfo ci2 : ci.getAllParents()) {
			PojoSwap<?,?> ps = POJO_SWAPS.get(ci2.inner());
			if (ps != null)
				return ps;
		}
		return null;
	}
}
