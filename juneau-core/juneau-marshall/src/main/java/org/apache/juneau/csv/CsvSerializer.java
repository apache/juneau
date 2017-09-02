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
package org.apache.juneau.csv;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * TODO - Work in progress.  CSV serializer.
 */
public final class CsvSerializer extends WriterSerializer {

	/** Default serializer, all default settings.*/
	public static final CsvSerializer DEFAULT = new CsvSerializer(PropertyStore.create());

	private final CsvSerializerContext ctx;

	/**
	 * Constructor.
	 *
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public CsvSerializer(PropertyStore propertyStore) {
		super(propertyStore, "text/csv");
		this.ctx = createContext(CsvSerializerContext.class);
	}

	@Override /* CoreObject */
	public CsvSerializerBuilder builder() {
		return new CsvSerializerBuilder(propertyStore);
	}

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new CsvSerializerSession(ctx, args);
	}
}
