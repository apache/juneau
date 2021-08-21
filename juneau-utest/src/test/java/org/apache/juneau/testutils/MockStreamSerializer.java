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
import java.util.function.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Utility class for creating mocked stream serializers.
 */
public class MockStreamSerializer extends OutputStreamSerializer {

	private final Function<Object,String> serializeFunction;

	public MockStreamSerializer(OutputStreamSerializerBuilder builder, String produces, Function<Object,String> serializeFunction) {
		super(builder.produces(produces));
		this.serializeFunction = serializeFunction;
	}

	protected MockStreamSerializer(OutputStreamSerializerBuilder builder) {
		super(builder.produces("text/plain"));
		this.serializeFunction = (o) -> StringUtils.stringify(o);
	}

	public static Builder create() {
		return new Builder();
	}

	@Override /* Serializer */
	public OutputStreamSerializerSession createSession(SerializerSessionArgs args) {
		return new OutputStreamSerializerSession(this, args) {
			@Override /* SerializerSession */
			protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
				out.getWriter().write(serializeFunction.apply(o));
			}
		};
	}

	public static class Builder extends OutputStreamSerializerBuilder {}

	@Override
	public Builder copy() {
		throw new NoSuchMethodError("Not implemented.");
	}
}
