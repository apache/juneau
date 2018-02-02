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

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.annotation.*;

/**
 * Parses URL-encoded text into POJO models.
 * 
 * <h5 class='topic'>Media types</h5>
 * 
 * Handles <code>Content-Type</code> types:  <code><b>application/x-www-form-urlencoded</b></code>
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
public class UrlEncodingParser extends UonParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "UrlEncodingParser.";

	/**
	 * Configuration property:  Parser bean property collections/arrays as separate key/value pairs
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"UrlEncodingParser.expandedParams.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link UrlEncodingParserBuilder#expandedParams(boolean)}
	 * 			<li class='jm'>{@link UrlEncodingParserBuilder#expandedParams()}
	 * 			<li class='ja'>{@link UrlEncoding#expandedParams()}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * This is the parser-side equivalent of the {@link #URLENC_expandedParams} setting.
	 * 
	 * <p>
	 * If <jk>false</jk>, serializing the array <code>[1,2,3]</code> results in <code>?key=$a(1,2,3)</code>.
	 * <br>If <jk>true</jk>, serializing the same array results in <code>?key=1&amp;key=2&amp;key=3</code>.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If parsing multi-part parameters, it's highly recommended to use Collections or Lists
	 * 		as bean property types instead of arrays since arrays have to be recreated from scratch every time a value
	 * 		is added to it.
	 * </ul>
	 */
	public static final String URLENC_expandedParams = PREFIX + "expandedParams.b";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link UrlEncodingParser}. */
	public static final UrlEncodingParser DEFAULT = new UrlEncodingParser(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean
		expandedParams;

	/**
	 * Constructor.
	 * 
	 * @param ps The property store containing all the settings for this object.
	 */
	public UrlEncodingParser(PropertyStore ps) {
		super(
			ps.builder()
				.set(UON_decoding, true)
				.build(), 
			"application/x-www-form-urlencoded"
		);
		expandedParams = getProperty(URLENC_expandedParams, boolean.class, false);
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

	
	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public UrlEncodingParserSession createSession(ParserSessionArgs args) {
		return new UrlEncodingParserSession(this, args);
	}
	
	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("UrlEncodingParser", new ObjectMap()
				.append("expandedParams", expandedParams)
			);
	}
}
