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
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Serializes POJOs to plain text using just the <c>toString()</c> method on the serialized object.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/plain</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/plain</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Essentially converts POJOs to plain text using the <c>toString()</c> method.
 *
 * <p>
 * Also serializes objects using a transform if the object class has an {@link PojoSwap PojoSwap&lt;?,String&gt;}
 * transform defined on it.
 */
@ConfigurableContext
public class PlainTextSerializer extends WriterSerializer implements PlainTextMetaProvider, PlainTextCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "PlainTextSerializer";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final PlainTextSerializer DEFAULT = new PlainTextSerializer(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Map<ClassMeta<?>,PlainTextClassMeta> plainTextClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,PlainTextBeanPropertyMeta> plainTextBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public PlainTextSerializer(PropertyStore ps) {
		this(ps, "text/plain", (String)null);
	}

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <c>media-type</c> specification of {@doc RFC2616.section14.1}
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <c>produces</c>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"application/json,text/json"</js>);
	 * 	</p>
	 * 	<br>...or...
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);
	 * 	</p>
	 * <p>
	 * The accept value can also contain q-values.
	 */
	public PlainTextSerializer(PropertyStore ps, String produces, String accept) {
		super(ps, produces, accept);
	}


	@Override /* Context */
	public PlainTextSerializerBuilder builder() {
		return new PlainTextSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link PlainTextSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> PlainTextSerializerBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link PlainTextSerializerBuilder} object.
	 */
	public static PlainTextSerializerBuilder create() {
		return new PlainTextSerializerBuilder();
	}

	@Override /* Context */
	public  PlainTextSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public PlainTextSerializerSession createSession(SerializerSessionArgs args) {
		return new PlainTextSerializerSession(this, args);
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
	public ObjectMap toMap() {
		return super.toMap()
			.append("PlainTextSerializer", new DefaultFilteringObjectMap()
			);
	}
}
