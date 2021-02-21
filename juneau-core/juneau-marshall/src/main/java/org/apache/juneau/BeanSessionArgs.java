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
package org.apache.juneau;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;

/**
 * Runtime arguments common to all bean, serializer, and parser sessions.
 */
public class BeanSessionArgs extends SessionArgs {

	/**
	 * Default empty session arguments.
	 */
	public static final BeanSessionArgs DEFAULT = new BeanSessionArgs();

	HttpPartSchema schema;

	/**
	 * Constructor
	 */
	public BeanSessionArgs() {}

	/**
	 * Static creator method.
	 *
	 * @return A new {@link BeanSessionArgs} object.
	 */
	public static BeanSessionArgs create() {
		return new BeanSessionArgs();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * HTTP-part schema.
	 *
	 * <p>
	 * Used for schema-based serializers and parsers to define additional formatting.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanSessionArgs schema(HttpPartSchema value) {
		this.schema = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - SessionArgs */
	public BeanSessionArgs debug(Boolean value) {
		super.debug(value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public BeanSessionArgs locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public BeanSessionArgs mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public BeanSessionArgs properties(Map<String,Object> value) {
		super.properties(value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public BeanSessionArgs property(String key, Object value) {
		super.property(key, value);
		return this;
	}

	@Override /* GENERATED - SessionArgs */
	public BeanSessionArgs timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	// </FluentSetters>

	@Override /* SessionArgs */
	public OMap toMap() {
		return super.toMap()
			.a(
				"BeanSessionArgs",
				OMap
					.create()
					.filtered()
					.a("schema", schema)
			);
	}
}
