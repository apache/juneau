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

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.uon.*;

/**
 * @deprecated Use {@link UonParser}
 */
@Deprecated
@SuppressWarnings({ "unchecked" })
public class UonPartParser extends UonParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link UonPartParser}. */
	public static final UonPartParser DEFAULT = new UonPartParser(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public UonPartParser(PropertyStore ps) {
		super(
			ps.builder()
				.build(),
			"application/x-www-form-urlencoded"
		);
	}

	@Override /* Context */
	public UonPartParserBuilder builder() {
		return new UonPartParserBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link UonPartParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> UonPartParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link UonPartParserBuilder} object.
	 */
	public static UonPartParserBuilder create() {
		return new UonPartParserBuilder();
	}

	@Override
	public <T> T parse(HttpPartType partType, String in, ClassMeta<T> type) throws ParseException {
		if (in == null)
			return null;
		if (type.isString() && in.length() > 0) {
			// Shortcut - If we're returning a string and the value doesn't start with "'" or is "null", then
			// just return the string since it's a plain value.
			// This allows us to bypass the creation of a UonParserSession object.
			char x = firstNonWhitespaceChar(in);
			if (x != '\'' && x != 'n' && in.indexOf('~') == -1)
				return (T)in;
			if (x == 'n' && "null".equals(in))
				return null;
		}
		UonParserSession session = createParameterSession();
		try (ParserPipe pipe = session.createPipe(in)) {
			try (UonReader r = session.getUonReader(pipe, false)) {
				return session.parseAnything(type, r, null, true, null);
			}
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}
}
