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

import org.apache.juneau.*;
import org.apache.juneau.utils.*;

/**
 * Subclass of {@link Serializer} for character-based serializers.
 */
public abstract class WriterSerializer extends Serializer {

	/**
	 * Constructor.
	 * 
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <code>media-type</code> specification of
	 * 	<a class="doclink" href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">RFC2616/14.1</a>
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <code>produces</code>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<br><code><jk>super</jk>(propertyStore, <js>"application/json"</js>, <js>"application/json"</js>, <js>"text/json"</js>);</code>
	 * 	<br>...or...
	 * 	<br><code><jk>super</jk>(propertyStore, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);</code>
	 */
	protected WriterSerializer(PropertyStore ps, String produces, String...accept) {
		super(ps, produces, accept);
	}


	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	@Override /* SerializerSession */
	public abstract WriterSerializerSession createSession(SerializerSessionArgs args);


	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
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
	@Override /* Serializer */
	public final String serialize(Object o) throws SerializeException {
		return createSession(createDefaultSessionArgs()).serialize(o);
	}

	/**
	 * Identical to {@link #serialize(Object)} except throws a {@link RuntimeException} instead of a {@link SerializeException}.
	 * 
	 * <p>
	 * This is typically good enough for debugging purposes.
	 * 
	 * @param o The object to serialize.
	 * @return The serialized object.
	 */
	public final String toString(Object o) {
		try {
			return serialize(o);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Wraps the specified object inside a {@link StringObject}.
	 * 
	 * @param o The object to wrap.
	 * @return The wrapped object.
	 */
	public final StringObject toStringObject(Object o) {
		return new StringObject(this, o);
	}

	/**
	 * Convenience method for serializing an object and sending it to STDOUT.
	 * 
	 * @param o The object to serialize.
	 * @return This object (for method chaining).
	 */
	public final WriterSerializer println(Object o) {
		System.out.println(toString(o));  // NOT DEBUG
		return this;
	}
}
