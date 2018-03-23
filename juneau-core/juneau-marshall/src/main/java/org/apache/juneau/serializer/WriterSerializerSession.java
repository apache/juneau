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

import static org.apache.juneau.serializer.WriterSerializer.*;

import java.io.*;

import org.apache.juneau.*;

/**
 * Subclass of {@link SerializerSession} for character-based serializers.
 * 
 * <h5 class='topic'>Description</h5>
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

	private final int maxIndent;
	private final boolean useWhitespace;
	private final char quoteChar;

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
	protected WriterSerializerSession(WriterSerializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
		
		useWhitespace = getProperty(WSERIALIZER_useWhitespace, boolean.class, ctx.useWhitespace);
		maxIndent = getProperty(WSERIALIZER_maxIndent, int.class, ctx.maxIndent);
		quoteChar = getProperty(WSERIALIZER_quoteChar, String.class, ""+ctx.quoteChar).charAt(0);
	}

	/**
	 * Constructor for sessions that don't require context.
	 * 
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected WriterSerializerSession(SerializerSessionArgs args) {
		this(WriterSerializer.DEFAULT, args);
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
	
	@Override /* SerializerSession */
	public final String serializeToString(Object o) throws SerializeException {
		return serialize(o);
	}
	
	/**
	 * Returns the {@link WriterSerializer#WSERIALIZER_useWhitespace} setting value for this session.
	 * 
	 * @return The {@link WriterSerializer#WSERIALIZER_useWhitespace} setting value for this session.
	 */
	protected boolean isUseWhitespace() {
		return useWhitespace;
	}

	/**
	 * Returns the {@link WriterSerializer#WSERIALIZER_maxIndent} setting value for this session.
	 * 
	 * @return The {@link WriterSerializer#WSERIALIZER_maxIndent} setting value for this session.
	 */
	protected int getMaxIndent() {
		return maxIndent;
	}

	/**
	 * Returns the {@link WriterSerializer#WSERIALIZER_quoteChar} setting value for this session.
	 * 
	 * @return The {@link WriterSerializer#WSERIALIZER_quoteChar} setting value for this session.
	 */
	protected char getQuoteChar() {
		return quoteChar;
	}

	@Override /* Session */
	public ObjectMap asMap() {
		return super.asMap()
			.append("WriterSerializerSession", new ObjectMap()
				.append("maxIndent", maxIndent)
				.append("useWhitespace", useWhitespace)
				.append("quoteChar", quoteChar)
			);
	}
}
