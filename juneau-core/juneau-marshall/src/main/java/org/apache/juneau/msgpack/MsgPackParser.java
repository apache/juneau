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
package org.apache.juneau.msgpack;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;

/**
 * Parses a MessagePack stream into a POJO model.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Content-Type</c> types:  <bc>octal/msgpack</bc>
 */
@ConfigurableContext
public class MsgPackParser extends InputStreamParser implements MsgPackMetaProvider, MsgPackCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "MsgPackParser";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, all default settings.*/
	public static final MsgPackParser DEFAULT = new MsgPackParser(PropertyStore.DEFAULT);

	/** Default parser, all default settings, string input encoded as spaced-hex.*/
	public static final MsgPackParser DEFAULT_SPACED_HEX = new SpacedHex(PropertyStore.DEFAULT);

	/** Default parser, all default settings, string input encoded as BASE64.*/
	public static final MsgPackParser DEFAULT_BASE64 = new Base64(PropertyStore.DEFAULT);

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, string input encoded as spaced-hex. */
	public static class SpacedHex extends MsgPackParser {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public SpacedHex(PropertyStore ps) {
			super(
				ps.builder().set(ISPARSER_binaryFormat, BinaryFormat.SPACED_HEX).build()
			);
		}
	}

	/** Default parser, string input encoded as BASE64. */
	public static class Base64 extends MsgPackParser {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Base64(PropertyStore ps) {
			super(
				ps.builder().set(ISPARSER_binaryFormat, BinaryFormat.BASE64).build()
			);
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Map<ClassMeta<?>,MsgPackClassMeta> msgPackClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,MsgPackBeanPropertyMeta> msgPackBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public MsgPackParser(PropertyStore ps) {
		super(ps, "octal/msgpack");
	}

	@Override /* Context */
	public MsgPackParserBuilder builder() {
		return new MsgPackParserBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link MsgPackParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> MsgPackParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link MsgPackParserBuilder} object.
	 */
	public static MsgPackParserBuilder create() {
		return new MsgPackParserBuilder();
	}

	@Override /* Parser */
	public MsgPackParserSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Parser */
	public MsgPackParserSession createSession(ParserSessionArgs args) {
		return new MsgPackParserSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* MsgPackMetaProvider */
	public MsgPackClassMeta getMsgPackClassMeta(ClassMeta<?> cm) {
		MsgPackClassMeta m = msgPackClassMetas.get(cm);
		if (m == null) {
			m = new MsgPackClassMeta(cm, this);
			msgPackClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* MsgPackMetaProvider */
	public MsgPackBeanPropertyMeta getMsgPackBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return MsgPackBeanPropertyMeta.DEFAULT;
		MsgPackBeanPropertyMeta m = msgPackBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new MsgPackBeanPropertyMeta(bpm.getDelegateFor(), this);
			msgPackBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public ObjectMap toMap() {
		return super.toMap()
			.append("MsgPackParser", new DefaultFilteringObjectMap());
	}
}
