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
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;

/**
 * Session object that lives for the duration of a single use of {@link UonPartSerializer}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public class UonPartSerializerSession extends UonSerializerSession implements HttpPartSerializerSession {

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected UonPartSerializerSession(UonPartSerializer ctx, SerializerSessionArgs args) {
		super(ctx, false, args);
	}

	@Override /* PartSerializer */
	public String serialize(HttpPartType type, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
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
			super.serialize(value, w);
			return w.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
