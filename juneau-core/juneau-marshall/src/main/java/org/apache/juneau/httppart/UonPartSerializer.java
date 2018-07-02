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
package org.apache.juneau.httppart;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.uon.*;

/**
 * Serializes POJOs to values suitable for transmission as HTTP headers, query/form-data parameters, and path variables.
 *
 * <p>
 * This serializer uses UON notation for all parts by default.  This allows for arbitrary POJOs to be losslessly
 * serialized as any of the specified HTTP types.
 */
public class UonPartSerializer extends UonSerializer implements HttpPartSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link UonPartSerializer}, all default settings. */
	public static final UonPartSerializer DEFAULT = new UonPartSerializer(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public UonPartSerializer(PropertyStore ps) {
		super(
			ps.builder()
				.set(UON_encoding, false)
				.build()
		);
	}

	@Override /* Context */
	public UonPartSerializerBuilder builder() {
		return new UonPartSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link UonPartSerializerBuilder} object.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link UonPartSerializerBuilder} object.
	 */
	public static UonPartSerializerBuilder create() {
		return new UonPartSerializerBuilder();
	}

	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	/**
	 * Convenience method for calling the parse method without a schema object.
	 *
	 * @param type The category of value being serialized.
	 * @param value The value being serialized.
	 * @return The serialized value.
	 */
	public String serialize(HttpPartType type, Object value) {
		return serialize(type, null, value);
	}

	@Override /* PartSerializer */
	public String serialize(HttpPartType type, HttpPartSchema schema, Object value) {
		try {
			// Shortcut for simple types.
			ClassMeta<?> cm = getClassMetaForObject(value);
			if (cm != null) {
				if (cm.isNumber() || cm.isBoolean())
					return ClassUtils.toString(value);
				if (cm.isString()) {
					String s = ClassUtils.toString(value);
					if (s.isEmpty() || ! UonUtils.needsQuotes(s))
						return s;
				}
			}
			StringWriter w = new StringWriter();
			UonSerializerSession s = new UonSerializerSession(this, false, createDefaultSessionArgs());
			s.serialize(value, w);
			return w.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
