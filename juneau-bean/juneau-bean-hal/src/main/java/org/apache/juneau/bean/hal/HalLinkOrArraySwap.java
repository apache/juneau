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
package org.apache.juneau.bean.hal;

import java.util.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.*;

/**
 * Per-property {@link ObjectSwap} applied to HAL {@code _links} maps so that each map entry round-trips
 * as either a single {@link HalLink} (JSON object) or a {@link HalLinkArray} (JSON array) per the spec.
 *
 * <p>
 * The swap operates on the whole {@code Map<String,Object>} bean property and walks the entries:
 * <ul class='spaced-list'>
 *   <li>On <b>serialize</b> ({@link #swap}): identity - values that are {@link HalLink} or
 *     {@link HalLinkArray} already serialize as the correct JSON shape.
 *   <li>On <b>parse</b> ({@link #unswap}): each value that came back as a parsed list is converted to
 *     {@link HalLinkArray}; each value that came back as a parsed map is converted to {@link HalLink}.
 *   <li>{@code null} input ⇒ {@code null} output.
 * </ul>
 *
 * <p>
 * Mirrors {@code BooleanOrSchemaSwap} in {@code juneau-bean-jsonschema} (the entry-disambiguation idea), with
 * iteration over map entries instead of operating on a single value.
 */
public class HalLinkOrArraySwap extends ObjectSwap<Object,Object> {

	@Override /* Overridden from ObjectSwap */
	public Object swap(MarshallingSession session, Object o) throws SerializeException {
		return o;
	}

	@Override /* Overridden from ObjectSwap */
	public Object unswap(MarshallingSession session, Object o, ClassMeta<?> hint) throws ParseException {
		if (o == null)
			return null;
		if (o instanceof Map<?,?> raw) {
			var out = new LinkedHashMap<String,Object>();
			var linkCm = session.getClassMeta(HalLink.class);
			var arrCm = session.getClassMeta(HalLinkArray.class);
			for (var e : raw.entrySet()) {
				var key = String.valueOf(e.getKey());
				var v = e.getValue();
				if (v instanceof Collection<?>) {
					out.put(key, session.convertToType(v, arrCm));
				} else if (v instanceof Map<?,?>) {
					out.put(key, session.convertToType(v, linkCm));
				} else {
					out.put(key, v);
				}
			}
			return out;
		}
		return o;
	}
}
