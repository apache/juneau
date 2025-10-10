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

package org.apache.juneau.utest.utils;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ArrayUtils.toList;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.common.utils.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Utility class for creating mocked writer serializers.
 */
public class FakeWriterSerializer extends WriterSerializer implements HttpPartSerializer {

	//-----------------------------------------------------------------------------------------------------------------
	// Predefined types
	//-----------------------------------------------------------------------------------------------------------------

	public static final X X = new X(create());

	public static class X extends FakeWriterSerializer {
		public X(Builder builder) {
			super(builder
				.function((s,o) -> out(o))
				.partFunction((s,t,o) -> out(o))
			);
		}

		private static String out(Object value) {
			if (value instanceof List<?> x)
				value = Utils.join(x, '|');
			if (value instanceof Collection<?> x)
				value = Utils.join(x, '|');
			if (isArray(value))
				value = Utils.join(toList(value, Object.class), "|");
			return "x" + value + "x";
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	public static class Builder extends WriterSerializer.Builder {
		Function2<WriterSerializerSession,Object,String> function = (s,o) -> s(o);
		Function3<HttpPartType,HttpPartSchema,Object,String> partFunction = (t,s,o) -> s(o);
		Function<WriterSerializerSession,Map<String,String>> headers = s -> Collections.emptyMap();

		public Builder function(Function2<WriterSerializerSession,Object,String> value) {
			function = value;
			return this;
		}

		public Builder partFunction(Function3<HttpPartType,HttpPartSchema,Object,String> value) {
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
			return this;
		}
	}

	private final Function2<WriterSerializerSession,Object,String> function;
	private final Function3<HttpPartType,HttpPartSchema,Object,String> partFunction;
	private final Function<WriterSerializerSession,Map<String,String>> headers;

	public FakeWriterSerializer(Builder builder) {
		super(builder);
		this.function = builder.function;
		this.partFunction = builder.partFunction;
		this.headers = builder.headers;
	}

	@Override /* SerializerSession */
	protected void doSerialize(SerializerSession session, SerializerPipe out, Object o) throws IOException, SerializeException {
		out.getWriter().write(function.apply((WriterSerializerSession)session,o));
	}

	@Override /* SerializerSession */
	public Map<String,String> getResponseHeaders(SerializerSession session) {
		return headers.apply((WriterSerializerSession)session);
	}

	@Override
	public HttpPartSerializerSession getPartSession() {
		return partFunction::apply;
	}
}