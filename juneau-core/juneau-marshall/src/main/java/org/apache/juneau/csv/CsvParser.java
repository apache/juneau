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
import org.apache.juneau.parser.*;

/**
 * TODO - Work in progress.  CSV parser.
 */
@ConfigurableContext
public class CsvParser extends ReaderParser implements CsvMetaProvider, CsvCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, all default settings.*/
	public static final CsvParser DEFAULT = new CsvParser(create());

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
	protected CsvParser(CsvParserBuilder builder) {
		super(builder);
	}

	@Override /* Context */
	public CsvParserBuilder copy() {
		return new CsvParserBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link CsvParserBuilder} object.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link CsvParserBuilder} object.
	 */
	public static CsvParserBuilder create() {
		return new CsvParserBuilder();
	}

	@Override /* Parser */
	public CsvParserSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Parser */
	public CsvParserSession createSession(ParserSessionArgs args) {
		return new CsvParserSession(this, args);
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
				"CsvParser",
				OMap
					.create()
					.filtered()
			);
	}
}
