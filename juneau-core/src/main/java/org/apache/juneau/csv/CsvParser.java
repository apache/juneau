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
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;

/**
 * TODO - Work in progress.  CSV parser.
 */
@Consumes("text/csv")
public class CsvParser extends ReaderParser {

	/** Default parser, all default settings.*/
	public static final CsvParser DEFAULT = new CsvParser(PropertyStore.create());

	
	/**
	 * Constructor.
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public CsvParser(PropertyStore propertyStore) {
		super(propertyStore);
	}

	@Override /* CoreObject */
	public CsvParserBuilder builder() {
		return new CsvParserBuilder(propertyStore);
	}

	@SuppressWarnings("unused")
	private <T> T parseAnything(ParserSession session, ClassMeta<T> eType, ParserReader r, Object outer, BeanPropertyMeta pMeta) throws Exception {
		throw new NoSuchMethodException("Not implemented.");
	}

	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
		CsvParserSession s = (CsvParserSession)session;
		ParserReader r = s.getReader();
		if (r == null)
			return null;
		T o = parseAnything(s, type, r, s.getOuter(), null);
		return o;
	}
}
