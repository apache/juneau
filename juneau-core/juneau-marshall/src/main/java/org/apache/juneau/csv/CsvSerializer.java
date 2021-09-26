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

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.serializer.*;

/**
 * TODO - Work in progress.  CSV serializer.
 */
@ConfigurableContext
public final class CsvSerializer extends WriterSerializer implements CsvMetaProvider,CsvCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final CsvSerializer DEFAULT = new CsvSerializer(create());

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Map<ClassMeta<?>,CsvClassMeta> csvClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,CsvBeanPropertyMeta> csvBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected CsvSerializer(CsvSerializerBuilder builder) {
		super(builder);
	}

	@Override /* Context */
	public CsvSerializerBuilder copy() {
		return new CsvSerializerBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link CsvSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> CsvSerializerBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link CsvSerializerBuilder} object.
	 */
	public static CsvSerializerBuilder create() {
		return new CsvSerializerBuilder();
	}

	@Override /* Context */
	public CsvSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public CsvSerializerSession createSession(SerializerSessionArgs args) {
		return new CsvSerializerSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* CsvMetaProvider */
	public CsvClassMeta getCsvClassMeta(ClassMeta<?> cm) {
		CsvClassMeta m = csvClassMetas.get(cm);
		if (m == null) {
			m = new CsvClassMeta(cm, this);
			csvClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* CsvMetaProvider */
	public CsvBeanPropertyMeta getCsvBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return CsvBeanPropertyMeta.DEFAULT;
		CsvBeanPropertyMeta m = csvBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new CsvBeanPropertyMeta(bpm.getDelegateFor(), this);
			csvBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"CsvSerializer",
				OMap
					.create()
					.filtered()
			);
	}
}
