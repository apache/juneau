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
package org.apache.juneau.msgpack;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Configurable properties on the {@link MsgPackSerializer} class.
 * <p>
 * Context properties are set by calling {@link PropertyStore#setProperty(String, Object)} on the property store
 * passed into the constructor.
 * <p>
 * See {@link PropertyStore} for more information about context properties.
 *
 * <h6 class='topic'>Inherited configurable properties</h6>
 * <ul class='doctree'>
 * 	<li class='jc'><a class="doclink" href="../BeanContext.html#ConfigProperties">BeanContext</a> - Properties associated with handling beans on serializers and parsers.
 * 	<ul>
 * 		<li class='jc'><a class="doclink" href="../serializer/SerializerContext.html#ConfigProperties">SerializerContext</a> - Configurable properties common to all serializers.
 * 	</ul>
 * </ul>
 */
public final class MsgPackSerializerContext extends SerializerContext {

	/**
	 * <b>Configuration property:</b>  Add <js>"_type"</js> properties when needed.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"MsgPackSerializer.addBeanTypeProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred through reflection.
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * For example, when serializing a {@code Map<String,Object>} field, where the bean class cannot be determined from the value type.
	 * <p>
	 * When present, this value overrides the {@link SerializerContext#SERIALIZER_addBeanTypeProperties} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 */
	public static final String MSGPACK_addBeanTypeProperties = "MsgPackSerializer.addBeanTypeProperties";

	final boolean
		addBeanTypeProperties;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link PropertyStore#getContext(Class)}.
	 *
	 * @param ps The property store that created this context.
	 */
	public MsgPackSerializerContext(PropertyStore ps) {
		super(ps);
		addBeanTypeProperties = ps.getProperty(MSGPACK_addBeanTypeProperties, boolean.class, ps.getProperty(SERIALIZER_addBeanTypeProperties, boolean.class, true));
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("MsgPackSerializerContext", new ObjectMap()
				.append("addBeanTypeProperties", addBeanTypeProperties)
			);
	}
}
