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
package org.apache.juneau.urlencoding;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.uon.*;

/**
 * Parses URL-encoded text into POJO models.
 * {@review}
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Content-Type</c> types:  <bc>application/x-www-form-urlencoded</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Parses URL-Encoded text (e.g. <js>"foo=bar&amp;baz=bing"</js>) into POJOs.
 *
 * <p>
 * Expects parameter values to be in UON notation.
 *
 * <p>
 * This parser uses a state machine, which makes it very fast and efficient.
 */
@ConfigurableContext
public class UrlEncodingParser extends UonParser implements UrlEncodingMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link UrlEncodingParser}. */
	public static final UrlEncodingParser DEFAULT = new UrlEncodingParser(create());

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean expandedParams;

	private final Map<ClassMeta<?>,UrlEncodingClassMeta> urlEncodingClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,UrlEncodingBeanPropertyMeta> urlEncodingBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected UrlEncodingParser(UrlEncodingParserBuilder builder) {
		super(builder);
		expandedParams = builder.expandedParams;
	}

	@Override /* Context */
	public UrlEncodingParserBuilder copy() {
		return new UrlEncodingParserBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link UrlEncodingParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> UrlEncodingParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link UrlEncodingParserBuilder} object.
	 */
	public static UrlEncodingParserBuilder create() {
		return new UrlEncodingParserBuilder();
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Entry point methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Parser */
	public UrlEncodingParserSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Parser */
	public UrlEncodingParserSession createSession(ParserSessionArgs args) {
		return new UrlEncodingParserSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* UrlEncodingMetaProvider */
	public UrlEncodingClassMeta getUrlEncodingClassMeta(ClassMeta<?> cm) {
		UrlEncodingClassMeta m = urlEncodingClassMetas.get(cm);
		if (m == null) {
			m = new UrlEncodingClassMeta(cm, this);
			urlEncodingClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* UrlEncodingMetaProvider */
	public UrlEncodingBeanPropertyMeta getUrlEncodingBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return UrlEncodingBeanPropertyMeta.DEFAULT;
		UrlEncodingBeanPropertyMeta m = urlEncodingBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new UrlEncodingBeanPropertyMeta(bpm.getDelegateFor(), this);
			urlEncodingBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Parser bean property collections/arrays as separate key/value pairs.
	 *
	 * @see UrlEncodingParserBuilder#expandedParams()
	 * @return
	 * <jk>false</jk> if serializing the array <c>[1,2,3]</c> results in <c>?key=$a(1,2,3)</c>.
	 * <br><jk>true</jk> if serializing the same array results in <c>?key=1&amp;key=2&amp;key=3</c>.
	 */
	protected final boolean isExpandedParams() {
		return expandedParams;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"UrlEncodingParser",
				OMap
					.create()
					.filtered()
					.a("expandedParams", expandedParams)
			);
	}
}
