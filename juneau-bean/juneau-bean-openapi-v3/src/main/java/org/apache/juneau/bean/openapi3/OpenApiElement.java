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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.json.*;

/**
 * Root class for all Swagger beans.
 */
public abstract class OpenApiElement {

	private boolean strict;
	private Map<String,Object> extra;

	OpenApiElement() {}

	OpenApiElement(OpenApiElement copyFrom) {
		this.strict = copyFrom.strict;
		this.extra = CollectionUtils.copyOf(copyFrom.extra);
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
		var m = new JsonMap();
		for (var s : keySet())
			m.put(s, get(s, Object.class));
		return m;
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
	 * Generic property getter.
	 *
	 * <p>
	 * Can be used to retrieve non-standard Swagger fields such as <js>"$ref"</js>.
	 *
	 * @param property The property name to retrieve.  Must not be <jk>null</jk>.
	 * @return The property value, or <jk>null</jk> if the property does not exist or is not set.
	 */
	@Beanp("*")
	public Object get(String property) {
		assertArgNotNull("property", property);
		return opt(extra).map(x -> x.get(property)).orElse(null);
	}

	/**
	 * Generic property getter.
	 *
	 * <p>
	 * Can be used to retrieve non-standard Swagger fields such as <js>"$ref"</js>.
	 *
	 * @param property The property name to retrieve.
	 * @param type The datatype to cast the value to.
	 * @param <T> The datatype to cast the value to.
	 * @return The property value, or <jk>null</jk> if the property does not exist or is not set.
	 */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return toType(get(property), type);
	}

	/**
	 * Returns all the keys on this element.
	 *
	 * @return
	 * 	All the keys on this element.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Set<String> keySet() {
		return extraKeys();
	}

	/**
	 * Generic property setter.
	 *
	 * <p>
	 * Can be used to set non-standard Swagger fields such as <js>"$ref"</js>.
	 *
	 * @param property The property name to set.  Must not be <jk>null</jk>.
	 * @param value The new value for the property.
	 * @return This object
	 * @throws RuntimeException if strict mode is enabled.
	 */
	@Beanp("*")
	public OpenApiElement set(String property, Object value) {
		assertArgNotNull("property", property);
		if (strict)
			throw new RuntimeException("Cannot set property '" + property + "' in strict mode.");
		if (extra == null)
			extra = CollectionUtils.map();
		extra.put(property, value);
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return JsonSerializer.DEFAULT_SORTED.toString(this);
	}

	/**
	 * Returns <jk>true</jk> if contents should be validated per the Swagger spec.
	 *
	 * @return <jk>true</jk> if contents should be validated per the Swagger spec.
	 */
	protected boolean isStrict() { return strict; }

	/**
	 * Sets strict mode on this bean.
	 *
	 * @return This object
	 */
	protected OpenApiElement strict() {
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
	 * @return This object
	 */
	protected OpenApiElement strict(Object value) {
		strict = value != null && toBoolean(value);
		return this;
	}
}