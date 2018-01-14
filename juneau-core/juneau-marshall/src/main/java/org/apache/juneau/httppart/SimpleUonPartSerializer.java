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

import org.apache.juneau.*;
import org.apache.juneau.uon.*;

/**
 * An extension of {@link UonPartSerializer} with plain-text string handling.
 * 
 * <p>
 * Uses UON notation for beans and maps (serialized as UON objects), and plain text for everything else.
 * <br>Collections/arrays are also serialized as comma-delimited lists.
 * 
 * <p>
 * The downside to this class vs. {@link UonPartSerializer} is that you may lose type information on the parse side.
 * For example, it's not possible to distinguish between the boolean <jk>false</jk> and the string <js>"false"</js>.
 * The same is true of numbers.  Also, whitespace in strings or strings containing single quotes may get lost if using
 * the {@link UonPartParser} to process them.
 */
public class SimpleUonPartSerializer extends UonPartSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link SimpleUonPartSerializer}, all default settings. */
	public static final SimpleUonPartSerializer DEFAULT = new SimpleUonPartSerializer(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 * 
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public SimpleUonPartSerializer(PropertyStore ps) {
		super(
			ps.builder()
				.set(UON_paramFormat, ParamFormat.PLAINTEXT)
				.build() 
		);
	}
	
	@Override /* Context */
	public SimpleUonPartSerializerBuilder builder() {
		return new SimpleUonPartSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link SimpleUonPartSerializerBuilder} object.
	 * 
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies 
	 * the settings of the object called on.
	 * 
	 * @return A new {@link UonPartSerializerBuilder} object.
	 */
	public static SimpleUonPartSerializerBuilder create() {
		return new SimpleUonPartSerializerBuilder();
	}
}
