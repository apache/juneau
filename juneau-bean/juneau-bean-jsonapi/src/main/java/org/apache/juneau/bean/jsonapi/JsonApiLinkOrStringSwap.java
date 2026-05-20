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
package org.apache.juneau.bean.jsonapi;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;

/**
 * Per-property {@link ObjectSwap} applied to JSON:API {@code links} maps so that each map entry round-trips
 * as either a JSON string URL or a {@link JsonApiLink} JSON object per the spec.
 *
 * <p>
 * The swap operates on the whole {@code Map<String,Object>} bean property and walks the entries:
 * <ul class='spaced-list'>
 *   <li>On <b>serialize</b> ({@link #swap}): identity - {@code String} values and {@link JsonApiLink} values
 *     serialize directly as JSON strings and JSON objects respectively.
 *   <li>On <b>parse</b> ({@link #unswap}): each value that came back as a parsed map (i.e. originally a JSON
 *     object) is converted to a {@link JsonApiLink}; all other values pass through unchanged.
 *   <li>{@code null} input ⇒ {@code null} output.
 * </ul>
 */
public class JsonApiLinkOrStringSwap extends ObjectSwap<Object,Object> {

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
			var linkCm = session.getClassMeta(JsonApiLink.class);
			for (var e : raw.entrySet()) {
				var key = String.valueOf(e.getKey());
				var v = e.getValue();
				if (v instanceof Map<?,?>) {
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
