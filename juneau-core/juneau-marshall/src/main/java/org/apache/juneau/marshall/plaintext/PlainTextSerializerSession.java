/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall.plaintext;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;

import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session object that lives for the duration of a single use of {@link PlainTextSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 */
@SuppressWarnings({
	"resource", // Resource management handled externally
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115" // Constants use UPPER_snakeCase naming convention
})
public class PlainTextSerializerSession extends WriterSerializerSession implements RecordWritable {

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializerSession.Builder<Builder> {

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(PlainTextSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
		}

		@Override
		public PlainTextSerializerSession build() {
			return new PlainTextSerializerSession(this);
		}

	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(PlainTextSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected PlainTextSerializerSession(Builder builder) {
		super(builder);
	}

	@Override /* RecordWritable */
	public RecordWriter writeRecords(Object output) throws IOException {
		return RecordAdapter.writer(this, output);
	}

	@Override /* RecordWritable */
	public boolean isRecordStreaming() {
		return false;
	}

	@Override /* Overridden from SerializerSession */
	protected void doWrite(SerializerPipe out, Object o) throws IOException, SerializeException {
		out.getWriter().write(o == null ? "null" : convertToType(o, String.class));
	}
}