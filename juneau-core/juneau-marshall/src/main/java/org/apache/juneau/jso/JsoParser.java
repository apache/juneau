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
package org.apache.juneau.jso;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;

/**
 * Parses POJOs from HTTP responses as Java {@link ObjectInputStream ObjectInputStreams}.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Consumes <c>Content-Type</c> types:  <bc>application/x-java-serialized-object</bc>
 */
@ConfigurableContext
public final class JsoParser extends InputStreamParser implements JsoMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, all default settings.*/
	public static final JsoParser DEFAULT = new JsoParser(create());

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Map<ClassMeta<?>,JsoClassMeta> jsoClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,JsoBeanPropertyMeta> jsoBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected JsoParser(JsoParserBuilder builder) {
		super(builder);
	}

	@Override /* Context */
	public JsoParserBuilder copy() {
		return new JsoParserBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link JsoParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> JsoParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link JsoParserBuilder} object.
	 */
	public static JsoParserBuilder create() {
		return new JsoParserBuilder();
	}

	@Override /* Parser */
	public JsoParserSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Parser */
	public JsoParserSession createSession(ParserSessionArgs args) {
		return new JsoParserSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* JsoMetaProvider */
	public JsoClassMeta getJsoClassMeta(ClassMeta<?> cm) {
		JsoClassMeta m = jsoClassMetas.get(cm);
		if (m == null) {
			m = new JsoClassMeta(cm, this);
			jsoClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* JsoMetaProvider */
	public JsoBeanPropertyMeta getJsoBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return JsoBeanPropertyMeta.DEFAULT;
		JsoBeanPropertyMeta m = jsoBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new JsoBeanPropertyMeta(bpm.getDelegateFor(), this);
			jsoBeanPropertyMetas.put(bpm, m);
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
				"JsoParser",
				OMap
					.create()
					.filtered()
			);
	}
}
