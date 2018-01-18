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

import static org.apache.juneau.internal.BeanPropertyUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;

/**
 * Root class for all Swagger beans.
 * 
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#juneau-dto.Swagger'>Overview > juneau-dto > Swagger</a>
 * </ul>
 */
public abstract class SwaggerElement {

	private boolean strict;
	private Map<String,Object> extra;

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
	 * @return This object (for method chaining).
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
	 * @return This object (for method chaining).
	 */
	protected SwaggerElement strict(Object value) {
		strict = value == null ? false : toBoolean(value);
		return this;
	}

	/**
	 * The map used to store 'extra' properties on the swagger element.
	 * 
	 * <p>
	 * For example, the <js>"$ref"</js> field is not a part of the Swagger doc, but is often
	 * found since it is a part of the JSON-Schema spec. 
	 * <br>This map allows you to store such properties.
	 * 
	 * <p>
	 * This map is lazy-created once this method is called.
	 * 
	 * @return 
	 * 	The extra properties map.
	 * 	<br>It's an instance of {@link LinkedHashMap}.
	 */
	@BeanProperty("*")
	public Map<String,Object> getExtraProperties() {
		if (extra == null)
			extra = new LinkedHashMap<>();
		return extra;
	}
	
	/**
	 * Generic property getter.
	 * 
	 * <p>
	 * Can be used to retrieve non-standard Swagger fields such as <js>"$ref"</js>.
	 * 
	 * @param property The property name to retrieve.
	 * @param type The datatype to cast the value to.
	 * @return The property value, or <jk>null</jk> if the property does not exist or is not set.
	 */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "strict": return toType(isStrict(), type);
			default: return toType(getExtraProperties().get(property), type);
		}
	};
	
	/**
	 * Generic property setter.
	 * 
	 * <p>
	 * Can be used to set non-standard Swagger fields such as <js>"$ref"</js>.
	 * 
	 * @param property The property name to set.
	 * @param value The new value for the property.
	 * @return This object (for method chaining).
	 */
	public SwaggerElement set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "strict": return strict(value);
			default: 
				getExtraProperties().put(property, value);
				return this;
		}
	}
	
	@Override /* Object */
	public String toString() {
		return JsonSerializer.DEFAULT.toString(this);
	}
}
