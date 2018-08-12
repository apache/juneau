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
package org.apache.juneau.htmlschema;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.serializer.*;

/**
 * Context object that lives for the duration of a single serialization of {@link HtmlSchemaDocSerializer} and its subclasses.
 *
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public class HtmlSchemaSerializerSession extends HtmlSerializerSession {

	private final JsonSchemaSerializerSession js;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param jsctx
	 * 	The JSON-Schema serializer used to convert a POJO into JSON-Schema metadata.
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime arguments.
	 */
	protected HtmlSchemaSerializerSession(JsonSchemaSerializer jsctx, HtmlSchemaSerializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
		this.js = jsctx.createSession(args);
	}

	@Override /* SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws Exception {
		ObjectMap schema = js.getSchema(toClassMeta(o));
		super.doSerialize(out, schema);
	}

	private ClassMeta<?> toClassMeta(Object o) {
		if (o instanceof Type)
			return getClassMeta((Type)o);
		return getClassMetaForObject(o);
	}
}
