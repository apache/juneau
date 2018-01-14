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

import org.apache.juneau.*;
import org.apache.juneau.parser.*;

/**
 * Parses a MessagePack stream into a POJO model.
 * 
 * 
 * <h5 class='topic'>Media types</h5>
 * 
 * Handles <code>Content-Type</code> types:  <code><b>octal/msgpack</b></code>
 */
public class MsgPackParser extends InputStreamParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, all default settings.*/
	public static final MsgPackParser DEFAULT = new MsgPackParser(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

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
	public MsgPackParserSession createSession(ParserSessionArgs args) {
		return new MsgPackParserSession(this, args);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("MsgPackParser", new ObjectMap());
	}
}
