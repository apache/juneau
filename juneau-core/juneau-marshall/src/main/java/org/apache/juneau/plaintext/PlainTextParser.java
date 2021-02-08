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
package org.apache.juneau.plaintext;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;

/**
 * Parsers HTTP plain text request bodies into Group 5 POJOs.
 *
 * <p>
 * See {@doc PojoCategories}.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/plain</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/plain</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Essentially just converts plain text to POJOs via static <c>fromString()</c> or <c>valueOf()</c>, or
 * through constructors that take a single string argument.
 *
 * <p>
 * Also parses objects using a transform if the object class has an {@link PojoSwap PojoSwap&lt;?,String&gt;} transform
 * defined on it.
 */
@ConfigurableContext
public class PlainTextParser extends ReaderParser implements PlainTextMetaProvider, PlainTextCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "PlainTextParser";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, all default settings.*/
	public static final PlainTextParser DEFAULT = new PlainTextParser(ContextProperties.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Map<ClassMeta<?>,PlainTextClassMeta> plainTextClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,PlainTextBeanPropertyMeta> plainTextBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param cp The property store containing all the settings for this object.
	 */
	public PlainTextParser(ContextProperties cp) {
		this(cp, "text/plain");
	}

	/**
	 * Constructor.
	 *
	 * @param cp The property store containing all the settings for this object.
	 * @param consumes The media types that this parser consumes.
	 * 	<p>
	 * 	Can contain meta-characters per the <c>media-type</c> specification of {@doc ExtRFC2616.section14.1}
	 */
	public PlainTextParser(ContextProperties cp, String...consumes) {
		super(cp, consumes);
	}

	@Override /* Context */
	public PlainTextParserBuilder builder() {
		return new PlainTextParserBuilder(getContextProperties());
	}

	/**
	 * Instantiates a new clean-slate {@link PlainTextParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> PlainTextParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link PlainTextParserBuilder} object.
	 */
	public static PlainTextParserBuilder create() {
		return new PlainTextParserBuilder();
	}

	@Override /* Parser */
	public PlainTextParserSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Parser */
	public PlainTextParserSession createSession(ParserSessionArgs args) {
		return new PlainTextParserSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* PlainTextMetaProvider */
	public PlainTextClassMeta getPlainTextClassMeta(ClassMeta<?> cm) {
		PlainTextClassMeta m = plainTextClassMetas.get(cm);
		if (m == null) {
			m = new PlainTextClassMeta(cm, this);
			plainTextClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* PlainTextMetaProvider */
	public PlainTextBeanPropertyMeta getPlainTextBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return PlainTextBeanPropertyMeta.DEFAULT;
		PlainTextBeanPropertyMeta m = plainTextBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new PlainTextBeanPropertyMeta(bpm.getDelegateFor(), this);
			plainTextBeanPropertyMetas.put(bpm, m);
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
				"PlainTextParser",
				OMap
					.create()
					.filtered()
		);
	}
}
