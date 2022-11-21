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
package org.apache.juneau.testutils;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Utility class for creating mocked stream serializers.
 */
public class MockStreamSerializer extends OutputStreamSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	public static class Builder extends OutputStreamSerializer.Builder {
		MockStreamSerializerFunction function = (s,o) -> StringUtils.stringify(o).getBytes();
		Function<SerializerSession,Map<String,String>> headers = (s) -> Collections.emptyMap();

		public Builder function(MockStreamSerializerFunction function) {
			this.function = function;
			return this;
		}

		public Builder headers(Function<SerializerSession,Map<String,String>> headers) {
			this.headers = headers;
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

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final MockStreamSerializerFunction function;

	public MockStreamSerializer(Builder builder) {
		super(builder);
		function = builder.function;
	}

	@Override /* SerializerSession */
	protected void doSerialize(SerializerSession session, SerializerPipe out, Object o) throws IOException, SerializeException {
		out.getOutputStream().write(function.apply((OutputStreamSerializerSession)session, o));
	}
}
