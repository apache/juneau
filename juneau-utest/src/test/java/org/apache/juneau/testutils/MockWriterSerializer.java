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

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Utility class for creating mocked writer serializers.
 */
public class MockWriterSerializer extends WriterSerializer {

	private final BiFunction<SerializerSession,Object,String> serializeFunction;
	private final Function<SerializerSession,Map<String,String>> headerFunction;

	protected MockWriterSerializer(Builder builder) {
		super(builder);
		this.serializeFunction = builder.serializeFunction;
		this.headerFunction = builder.headerFunction;
	}

	public static Builder create() {
		return new Builder();
	}

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new WriterSerializerSession(this, args) {
			@Override /* SerializerSession */
			protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
				out.getWriter().write(serializeFunction.apply(this,o));
			}
			@Override /* SerializerSession */
			public Map<String,String> getResponseHeaders() {
				return headerFunction.apply(this);
			}
		};
	}

	public static class Builder extends WriterSerializerBuilder {
		BiFunction<SerializerSession,Object,String> serializeFunction = (s,o) -> StringUtils.stringify(o);
		Function<SerializerSession,Map<String,String>> headerFunction = (s) -> Collections.emptyMap();

		public Builder serialize(BiFunction<SerializerSession,Object,String> function) {
			this.serializeFunction = function;
			return this;
		}

		public Builder headers(Function<SerializerSession,Map<String,String>> function) {
			this.headerFunction = function;
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
	}

	@Override
	public Builder copy() {
		throw new NoSuchMethodError("Not implemented.");
	}
}
