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
package org.apache.juneau.dto.swagger;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;

/**
 * Root class for all Swagger beans.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Overview &gt; juneau-rest-server &gt; Swagger</a>
 * </ul>
 */
@FluentSetters
public abstract class SwaggerElement {

	private boolean strict;
	private Map<String,Object> extra;

	SwaggerElement() {}

	SwaggerElement(SwaggerElement copyFrom) {
		this.strict = copyFrom.strict;
		this.extra = copyOf(copyFrom.extra);
	}

	/**
	 * Returns <jk>true</jk> if contents should be validated per the Swagger spec.
	 *
	 * @return <jk>true</jk> if contents should be validated per the Swagger spec.
	 */
	protected boolean isStrict() {
		return strict;
	}

	/**
	 * Sets strict mode on this bean.
	 *
	 * @return This object.
	 */
	protected SwaggerElement strict() {
		strict = true;
		return this;
	}

	/**
	 * Sets strict mode on this bean.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> (interpreted as <jk>false</jk>).
	 * @return This object.
	 */
	protected SwaggerElement strict(Object value) {
		strict = value == null ? false : toBoolean(value);
		return this;
	}

	/**
	 * Generic property getter.
	 *
	 * <p>
	 * Can be used to retrieve non-standard Swagger fields such as <js>"$ref"</js>.
	 *
	 * @param <T> The datatype to cast the value to.
	 * @param property The property name to retrieve.
	 * @param type The datatype to cast the value to.
	 * @return The property value, or <jk>null</jk> if the property does not exist or is not set.
	 */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "strict": return toType(isStrict(), type);
			default: return toType(get(property), type);
		}
	};

	/**
	 * Generic property getter.
	 *
	 * <p>
	 * Can be used to retrieve non-standard Swagger fields such as <js>"$ref"</js>.
	 *
	 * @param property The property name to retrieve.
	 * @return The property value, or <jk>null</jk> if the property does not exist or is not set.
	 */
	@Beanp("*")
	public Object get(String property) {
		if (property == null || extra == null)
			return null;
		return extra.get(property);
	};

	/**
	 * Generic property setter.
	 *
	 * <p>
	 * Can be used to set non-standard Swagger fields such as <js>"$ref"</js>.
	 *
	 * @param property The property name to set.
	 * @param value The new value for the property.
	 * @return This object.
	 */
	@Beanp("*")
	public SwaggerElement set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "strict": return strict(value);
			default:
				if (extra == null)
					extra = map();
				extra.put(property, value);
				return this;
		}
	}

	/**
	 * Generic property keyset.
	 *
	 * @return
	 * 	All the non-standard keys on this element.
	 * 	<br>Never <jk>null</jk>.
	 */
	@Beanp("*")
	public Set<String> extraKeys() {
		return extra == null ? Collections.emptySet() : extra.keySet();
	}

	/**
	 * Returns all the keys on this element.
	 *
	 * @return
	 * 	All the keys on this element.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(strict, "strict")
			.build();
		s.addAll(extraKeys());
		return s;
	}

	/**
	 * Returns a copy of this swagger element as a modifiable map.
	 *
	 * <p>
	 * Each call produces a new map.
	 *
	 * @return A map containing all the values in this swagger element.
	 */
	public JsonMap asMap() {
		JsonMap m = new JsonMap();
		keySet().forEach(x -> m.put(x, get(x, Object.class)));
		return m;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* Object */
	public String toString() {
		return JsonSerializer.DEFAULT.toString(this);
	}
}
