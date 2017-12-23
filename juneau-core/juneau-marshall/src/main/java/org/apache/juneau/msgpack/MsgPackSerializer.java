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
 * Serializes POJO models to MessagePack.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Accept</code> types: <code>octal/msgpack</code>
 *
 * <p>
 * Produces <code>Content-Type</code> types: <code>octal/msgpack</code>
 */
public class MsgPackSerializer extends OutputStreamSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "MsgPackSerializer.";

	/**
	 * <b>Configuration property:</b>  Add <js>"_type"</js> properties when needed.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"MsgPackSerializer.addBeanTypeProperties.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * For example, when serializing a {@code Map<String,Object>} field, where the bean class cannot be determined from
	 * the value type.
	 *
	 * <p>
	 * When present, this value overrides the {@link #SERIALIZER_addBeanTypeProperties} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 */
	public static final String MSGPACK_addBeanTypeProperties = PREFIX + "addBeanTypeProperties.b";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final MsgPackSerializer DEFAULT = new MsgPackSerializer(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean
		addBeanTypeProperties;

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public MsgPackSerializer(PropertyStore ps) {
		super(ps, "octal/msgpack");
		this.addBeanTypeProperties = getProperty(MSGPACK_addBeanTypeProperties, boolean.class, getProperty(SERIALIZER_addBeanTypeProperties, boolean.class, true));
	}

	@Override /* Context */
	public MsgPackSerializerBuilder builder() {
		return new MsgPackSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link MsgPackSerializerBuilder} object.
	 * 
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> MsgPackSerializerBuilder()</code>.
	 * 
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies 
	 * the settings of the object called on.
	 * 
	 * @return A new {@link MsgPackSerializerBuilder} object.
	 */
	public static MsgPackSerializerBuilder create() {
		return new MsgPackSerializerBuilder();
	}
	
	@Override /* Serializer */
	public OutputStreamSerializerSession createSession(SerializerSessionArgs args) {
		return new MsgPackSerializerSession(this, args);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("MsgPackSerializer", new ObjectMap()
				.append("addBeanTypeProperties", addBeanTypeProperties)
			);
	}
}
