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

import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * An implementation of {@link HttpPartSerializer} that simply serializes everything using {@link Object#toString()}.
 *
 * <p>
 * More precisely, uses the {@link ClassUtils#toString(Object)} method to stringify objects.
 */
public class SimplePartSerializer implements HttpPartSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link SimplePartSerializer}, all default settings. */
	public static final SimplePartSerializer DEFAULT = new SimplePartSerializer();

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	@Override
	public SimplePartSerializerSession createSession(SerializerSessionArgs args) {
		return new SimplePartSerializerSession();
	}

	/**
	 * Convenience method for creating a new serializer session with default session args.
	 *
	 * @return A new session object.
	 */
	public SimplePartSerializerSession createSession() {
		return new SimplePartSerializerSession();
	}
}
