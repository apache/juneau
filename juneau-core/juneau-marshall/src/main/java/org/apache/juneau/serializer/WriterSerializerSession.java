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
package org.apache.juneau.serializer;

import java.io.*;

/**
 * Subclass of {@link SerializerSession} for character-based serializers.
 *
 * <h5 class='section'>Description:</h5>
 *
 * This class is typically the parent class of all character-based serializers.
 * <br>It has 1 abstract method to implement...
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link #doSerialize(SerializerPipe, Object)}
 * </ul>
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public abstract class WriterSerializerSession extends SerializerSession {

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
	protected WriterSerializerSession(Serializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
	}

	/**
	 * Constructor for sessions that don't require context.
	 *
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected WriterSerializerSession(SerializerSessionArgs args) {
		super(args);
	}

	@Override /* SerializerSession */
	public final boolean isWriterSerializer() {
		return true;
	}

	/**
	 * Convenience method for serializing an object to a <code>String</code>.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@Override /* SerializerSession */
	public final String serialize(Object o) throws SerializeException {
		StringWriter w = new StringWriter();
		serialize(o, w);
		return w.toString();
	}
}
