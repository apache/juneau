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
package org.apache.juneau.httppart;

import static org.apache.juneau.internal.ExceptionUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * An implementation of {@link HttpPartSerializer} that simply serializes everything using {@link Object#toString()}.
 *
 * <p>
 * More precisely, uses the {@link ClassUtils#toString(Object)} method to stringify objects.
 */
public class SimplePartSerializer extends BaseHttpPartSerializer {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link SimplePartSerializer}, all default settings. */
	public static final SimplePartSerializer DEFAULT = create().build();

	/**
	 * Static creator.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder extends BaseHttpPartSerializer.Builder {

		Builder() {
			super();
		}

		Builder(Builder builder) {
			super(builder);
		}

		@Override
		public SimplePartSerializer build() {
			return new SimplePartSerializer(this);
		}

		@Override
		public BeanContextableBuilder copy() {
			return new Builder(this);
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor
	 *
	 * @param builder The builder for this object.
	 */
	protected SimplePartSerializer(Builder builder) {
		super(builder);
	}

	@Override
	public SimplePartSerializerSession createPartSession(SerializerSessionArgs args) {
		return new SimplePartSerializerSession();
	}

	@Override
	public Builder copy() {
		throw unsupportedOperationException("Not implemented.");
	}
}
