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

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final CsvSerializer DEFAULT = new CsvSerializer(PropertyStore2.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public CsvSerializer(PropertyStore2 ps) {
		super(ps, "text/csv");
	}

	@Override /* Context */
	public CsvSerializerBuilder builder() {
		return new CsvSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link CsvSerializerBuilder} object.
	 * 
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> CsvSerializerBuilder()</code>.
	 * 
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies 
	 * the settings of the object called on.
	 * 
	 * @return A new {@link CsvSerializerBuilder} object.
	 */
	public static CsvSerializerBuilder create() {
		return new CsvSerializerBuilder();
	}

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new CsvSerializerSession(this, args);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("CsvSerializer", new ObjectMap());
	}
}
