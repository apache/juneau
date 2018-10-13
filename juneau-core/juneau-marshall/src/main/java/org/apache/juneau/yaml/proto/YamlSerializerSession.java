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
package org.apache.juneau.yaml.proto;

import static org.apache.juneau.yaml.proto.YamlSerializer.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * @deprecated Never implemented.
 */
@Deprecated
public class YamlSerializerSession extends WriterSerializerSession {

	private final boolean
		addBeanTypeProperties;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and
	 * 	serializer contexts.
	 */
	protected YamlSerializerSession(YamlSerializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
		addBeanTypeProperties = getProperty(YAML_addBeanTypeProperties, boolean.class, ctx.addBeanTypeProperties);
	}

	@Override /* Session */
	public ObjectMap asMap() {
		return super.asMap()
			.append("YamlSerializerSession", new ObjectMap()
				.append("addBeanTypeProperties", addBeanTypeProperties)
			);
	}

	@Override /* SerializerSesssion */
	protected void doSerialize(SerializerPipe out, Object o) throws Exception {
		serializeAnything(getYamlWriter(out), o, getExpectedRootType(o), "root", null);
	}

	/*
	 * Workhorse method.
	 * Determines the type of object, and then calls the appropriate type-specific serialization method.
	 */
	SerializerWriter serializeAnything(YamlWriter out, Object o, ClassMeta<?> eType,	String attrName, BeanPropertyMeta pMeta) throws Exception {
		return out;
	}

	/**
	 * Returns the {@link #YAML_addBeanTypeProperties} setting value for this session.
	 *
	 * @return The {@link #YAML_addBeanTypeProperties} setting value for this session.
	 */
	protected final boolean isAddBeanTypeProperties() {
		return false;
	}

	/**
	 * Converts the specified output target object to an {@link YamlWriter}.
	 *
	 * @param out The output target object.
	 * @return The output target object wrapped in an {@link YamlWriter}.
	 * @throws Exception
	 */
	protected final YamlWriter getYamlWriter(SerializerPipe out) throws Exception {
		Object output = out.getRawOutput();
		if (output instanceof YamlWriter)
			return (YamlWriter)output;
		YamlWriter w = new YamlWriter(out.getWriter(), getQuoteChar(), isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
	}
}
