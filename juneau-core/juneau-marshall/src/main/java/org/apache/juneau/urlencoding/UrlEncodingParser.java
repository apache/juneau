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
public class UrlEncodingParser extends UonParser implements UrlEncodingMetaProvider, UrlEncodingCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "UrlEncodingParser";

	/**
	 * Configuration property:  Parser bean property collections/arrays as separate key/value pairs.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.urlencoding.UrlEncodingParser#URLENC_expandedParams URLENC_expandedParams}
	 * 	<li><b>Name:</b>  <js>"UrlEncodingParser.expandedParams.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>UrlEncodingParser.expandedParams</c>
	 * 	<li><b>Environment variable:</b>  <c>URLENCODINGPARSER_EXPANDEDPARAMS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.urlencoding.annotation.UrlEncodingConfig#expandedParams()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.urlencoding.UrlEncodingParserBuilder#expandedParams(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.urlencoding.UrlEncodingParserBuilder#expandedParams()}
	 * 			<li class='ja'>{@link org.apache.juneau.urlencoding.annotation.UrlEncoding#expandedParams()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * This is the parser-side equivalent of the {@link #URLENC_expandedParams} setting.
	 *
	 * <p>
	 * If <jk>false</jk>, serializing the array <c>[1,2,3]</c> results in <c>?key=$a(1,2,3)</c>.
	 * <br>If <jk>true</jk>, serializing the same array results in <c>?key=1&amp;key=2&amp;key=3</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public</jk> String[] f1;
	 * 		<jk>public</jk> List&lt;String&gt; f2;
	 * 	}
	 *
	 * 	UrlEncodingParser p1 = UrlEncodingParser.<jsf>DEFAULT</jsf>;
	 * 	UrlEncodingParser p2 = UrlEncodingParser.<jsm>create</jsm>().expandedParams().build();
	 *
	 * 	A a1 = p1.parse(<js>"f1=@(a,b)&amp;f2=@(c,d)"</js>, A.<jk>class</jk>);
	 *
	 * 	A a2 = p2.parse(<js>"f1=a&amp;f1=b&amp;f2=c&amp;f2=d"</js>, A.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * This option only applies to beans.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If parsing multi-part parameters, it's highly recommended to use Collections or Lists
	 * 		as bean property types instead of arrays since arrays have to be recreated from scratch every time a value
	 * 		is added to it.
	 * </ul>
	 */
	public static final String URLENC_expandedParams = PREFIX + ".expandedParams.b";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link UrlEncodingParser}. */
	public static final UrlEncodingParser DEFAULT = new UrlEncodingParser(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final boolean expandedParams;
	private final Map<ClassMeta<?>,UrlEncodingClassMeta> urlEncodingClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,UrlEncodingBeanPropertyMeta> urlEncodingBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public UrlEncodingParser(PropertyStore ps) {
		super(
			ps.builder()
				.setDefault(UON_decoding, true)
				.build(),
			"application/x-www-form-urlencoded"
		);
		expandedParams = getBooleanProperty(URLENC_expandedParams, false);
	}

	@Override /* Context */
	public UrlEncodingParserBuilder builder() {
		return new UrlEncodingParserBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link UrlEncodingParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> UrlEncodingParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
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
	 * Configuration property:  Parser bean property collections/arrays as separate key/value pairs.
	 *
	 * @see #URLENC_expandedParams
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
			.a("UrlEncodingParser", new DefaultFilteringOMap()
				.a("expandedParams", expandedParams)
			);
	}
}
