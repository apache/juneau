//***************************************************************************************************************************
//* Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
//* distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
//* to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
//* with the License.  You may obtain a copy of the License at                                                              *
//*                                                                                                                         *
//*  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
//*                                                                                                                         *
//* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
//* specific language governing permissions and limitations under the License.                                              *
//***************************************************************************************************************************

package org.apache.juneau.testutils;

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Utility class for creating mocked writer serializers.
 */
public class MockWriterSerializer extends WriterSerializer implements HttpPartSerializer {

	//-----------------------------------------------------------------------------------------------------------------
	// Predefined types
	//-----------------------------------------------------------------------------------------------------------------

	public static final X X = new X(create());

	public static class X extends MockWriterSerializer {
		protected X(Builder builder) {
			super(builder
				.function((s,o) -> out(o))
				.partFunction((s,t,o) -> out(o))
			);
		}

		private static String out(Object value) {
			if (value instanceof List)
				value = join((List<?>)value, '|');
			if (value instanceof Collection)
				value = join((Collection<?>)value, '|');
			if (isArray(value))
				value = join(toList(value, Object.class), "|");
			return "x" + value + "x";
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final MockWriterSerializerFunction function;
	private final MockWriterSerializerPartFunction partFunction;
	private final Function<WriterSerializerSession,Map<String,String>> headers;


	protected MockWriterSerializer(Builder builder) {
		super(builder);
		this.function = builder.function;
		this.partFunction = builder.partFunction;
		this.headers = builder.headers;
	}

	public static Builder create() {
		return new Builder();
	}

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new WriterSerializerSession(this, args) {
			@Override /* SerializerSession */
			protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
				out.getWriter().write(function.apply(this,o));
			}
			@Override /* SerializerSession */
			public Map<String,String> getResponseHeaders() {
				return headers.apply(this);
			}
		};
	}

	@Override
	public HttpPartSerializerSession createPartSession(SerializerSessionArgs args) {
		return new HttpPartSerializerSession() {
			@Override
			public String serialize(HttpPartType type, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
				return partFunction.apply(type, schema, value);
			}
		};
	}

	public static class Builder extends WriterSerializerBuilder {
		MockWriterSerializerFunction function = (s,o) -> StringUtils.stringify(o);
		MockWriterSerializerPartFunction partFunction = (t,s,o) -> StringUtils.stringify(o);
		Function<WriterSerializerSession,Map<String,String>> headers = (s) -> Collections.emptyMap();

		public Builder() {
			super();
		}

		public Builder(Builder copyFrom) {
			super(copyFrom);
		}

		public Builder function(MockWriterSerializerFunction value) {
			function = value;
			return this;
		}

		public Builder partFunction(MockWriterSerializerPartFunction value) {
			partFunction = value;
			return this;
		}

		public Builder headers(Function<WriterSerializerSession,Map<String,String>> value) {
			headers = value;
			return this;
		}

		@Override
		public Builder produces(String value) {
			super.produces(value);
			return this;
		}

		@Override
		public Builder accept(String value) {
			super.accept(value);
			return this;
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}
	}

	@Override
	public Builder copy() {
		throw new NoSuchMethodError("Not implemented.");
	}
}
