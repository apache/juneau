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

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJOs to HTTP responses as Java Serialized Object {@link ObjectOutputStream ObjectOutputStreams}.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Accept</code> types: <code>application/x-java-serialized-object</code>
 *
 * <p>
 * Produces <code>Content-Type</code> types: <code>application/x-java-serialized-object</code>
 */
public class JsoSerializer extends OutputStreamSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final JsoSerializer DEFAULT = new JsoSerializer(PropertyStore.create());


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final SerializerContext ctx;

	/**
	 * Constructor.
	 *
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public JsoSerializer(PropertyStore propertyStore) {
		super(propertyStore, "application/x-java-serialized-object");
		this.ctx = createContext(SerializerContext.class);
	}

	@Override /* CoreObject */
	public JsoSerializerBuilder builder() {
		return new JsoSerializerBuilder(propertyStore);
	}

	@Override /* Serializer */
	public OutputStreamSerializerSession createSession(SerializerSessionArgs args) {
		return new JsoSerializerSession(ctx, args);
	}
}
