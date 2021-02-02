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
package org.apache.juneau.jso;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJOs to HTTP responses as Java Serialized Object {@link ObjectOutputStream ObjectOutputStreams}.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>application/x-java-serialized-object</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/x-java-serialized-object</bc>
 */
@ConfigurableContext
public class JsoSerializer extends OutputStreamSerializer implements JsoMetaProvider, JsoCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "JsoSerializer";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final JsoSerializer DEFAULT = new JsoSerializer(PropertyStore.DEFAULT);

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Map<ClassMeta<?>,JsoClassMeta> jsoClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,JsoBeanPropertyMeta> jsoBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public JsoSerializer(PropertyStore ps) {
		super(ps, "application/x-java-serialized-object", null);
	}

	@Override /* Context */
	public JsoSerializerBuilder builder() {
		return new JsoSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link JsoSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> JsoSerializerBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link JsoSerializerBuilder} object.
	 */
	public static JsoSerializerBuilder create() {
		return new JsoSerializerBuilder();
	}

	@Override /* Context */
	public JsoSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public JsoSerializerSession createSession(SerializerSessionArgs args) {
		return new JsoSerializerSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* JsoMetaProvider */
	public JsoClassMeta getJsoClassMeta(ClassMeta<?> cm) {
		JsoClassMeta m = jsoClassMetas.get(cm);
		if (m == null) {
			m = new JsoClassMeta(cm, this);
			jsoClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* JsoMetaProvider */
	public JsoBeanPropertyMeta getJsoBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return JsoBeanPropertyMeta.DEFAULT;
		JsoBeanPropertyMeta m = jsoBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new JsoBeanPropertyMeta(bpm.getDelegateFor(), this);
			jsoBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"JsoSerializer",
				OMap
					.create()
					.filtered()
			);
	}
}
