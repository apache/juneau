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
package org.apache.juneau.bean.jsonpatch;

import org.apache.juneau.marshall.*;

/**
 * The JSON Patch {@code "add"} operation per
 * <a href="https://datatracker.ietf.org/doc/html/rfc6902#section-4.1">RFC 6902 &#167; 4.1</a>.
 *
 * <p>
 * Adds {@link #getValue() value} at {@link #getPath() path}.
 */
@Marshalled(typeName = "add")
public class AddOp extends JsonPatchOperation {

	private Object value;

	/**
	 * Default constructor.
	 */
	public AddOp() {}

	/**
	 * Convenience constructor.
	 *
	 * @param path The JSON Pointer target.  Can be <jk>null</jk> to leave the property unset.
	 * @param value The value to add.  Can be <jk>null</jk> to leave the property unset.
	 */
	public AddOp(String path, Object value) {
		super(path);
		this.value = value;
	}

	/**
	 * Bean property getter:  <property>value</property>.
	 *
	 * @return The value of the <property>value</property> property, or <jk>null</jk> if it is not set.
	 */
	public Object getValue() { return value; }

	/**
	 * Bean property setter:  <property>value</property>.
	 *
	 * @param value The new value for this property.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public AddOp setValue(Object value) {
		this.value = value;
		return this;
	}
}
