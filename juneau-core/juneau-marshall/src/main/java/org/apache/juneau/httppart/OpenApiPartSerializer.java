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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Serializes POJOs to values suitable for transmission as HTTP headers, query/form-data parameters, and path variables.
 *
 * <p>
 * TODO
 *
 * <table class='styled'>
 * 	<tr><th>Type</th><th>Format</th><th>Valid parameter types</th></tr>
 * 	<tr>
 * 		<td><code>string</code><br>(empty)</td>
 * 		<td>
 * 			<code>byte</code>
 * 			<br><code>binary</code>
 * 			<br><code>binary-spaced</br>
 * 		</td>
 * 		<td>
 * 			<ul>
 * 				<li><code><jk>byte</jk>[]</code> (default)
 * 				<li>{@link InputStream}
 * 				<li>{@link Reader} - Read into String and then converted using {@link String#getBytes()}.
 * 				<li>{@link Object} - Converted to String and then converted using {@link String#getBytes()}.
 * 				<li>Any POJO transformable to a <code><jk>byte</jk>[]</code> via the following methods:
 * 					<ul>
 * 						<li><code><jk>public byte</jk>[] toBytes() {...}</code>
 * 						<li><code><jk>public byte</jk>[]</jk> toFoo() {...}</code> (any method name starting with "to")
 * 					</ul>
 * 				<li>Any POJO transformable to a <code><jk>byte</jk>[]</code> via a {@link PojoSwap}.
 * 			</ul>
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>string</code><br>(empty)</td>
 * 		<td>
 * 			<code>date</code>
 * 			<code>date-time</code>
 * 		</td>
 * 		<td>
 * 			<ul>
 * 				<li>{@link Calendar} (default)
 * 				<li>{@link Date}
 * 				<li>Any POJO transformable to a {@link Calendar} via the following methods:
 * 					<ul>
 * 						<li><code><jk>public</jk> Calendar toCalendar() {...}</code>
 * 						<li><code><jk>public</jk> Calendar toFoo() {...}</code> (any method name starting with "to")
 * 					</ul>
 * 				<li>Any POJO transformable to a {@link Calendar} via a {@link PojoSwap}.
 * 			</ul>
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>string</code><br>(empty)</td>
 * 		<td><code>uon</code></td>
 * 		<td>
 * 			<ul>
 * 				<li>Any <a class='doclink' href='../../../../overview-summary.html#juneau-marshall.PojoCategories'>Serializable POJO</a> type.
 * 			</ul>
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>string</code><br>(empty)</td>
 * 		<td>(empty)</td>
 * 		<td>
 * 			<ul>
 * 				<li>{@link String} (default)
 * 				<li>Any POJO transformable to a {@link String} via the following methods:
 * 					<ul>
 * 						<li><code><jk>public</jk> String toString() {...}</code>
 * 					</ul>
 * 				<li>Any POJO transformable to a {@link String} via a {@link PojoSwap}.
 * 			</ul>
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>boolean</code></td>
 * 		<td>
 * 			&nbsp;
 * 		</td>
 * 		<td>
 * 			<ul>
 * 				<li>{@link Boolean} (default)
 * 				<li><jk>boolean</jk>
 * 				<li>{@link String} - Converted to a {@link Boolean}.
 * 				<li>Any POJO transformable to a {@link Boolean} via the following methods:
 * 					<ul>
 * 						<li><code><jk>public</jk> Boolean toBoolean() {...}</code>
 * 						<li><code><jk>public</jk> Boolean toFoo() {...}</code> (any method name starting with "to")
 * 					</ul>
 * 				<li>Any POJO transformable to a {@link Boolean} via a {@link PojoSwap}.
 * 			</ul>
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>array</code></td>
 * 		<td>
 * 			&nbsp;
 * 		</td>
 * 		<td>
 * 			<ul>
 * 				<li>Arrays or Collections of any defaults on this list.
 * 				<li>Any POJO transformable to arrays of the default types (e.g. <code>Integer[]</code>, <code>Boolean[][]</code>, etc...).
 * 					<br>For example:
 * 					<ul>
 * 						<li><code><jk>public</jk> Boolean[][] toFoo() {...}</code> (any method name starting with "to")
 * 					</ul>
 * 				<li>Any POJO transformable to arrays of the default types via a {@link PojoSwap}
 * 			</ul>
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>object</code></td>
 * 		<td>
 * 			&nbsp;
 * 		</td>
 * 		<td>
 * 			<ul>
 * 				<li>Beans with properties of anything on this list.
 * 				<li>Maps with string keys.
 * 				<li>Any POJO transformable to a map via a {@link PojoSwap}
 * 			</ul>
 * 		</td>
 * 	</tr>
 * </table>
 */
public class OpenApiPartSerializer extends UonPartSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "OpenApiPartSerializer.";

	/**
	 * Configuration property:  OpenAPI schema description.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"OpenApiPartSerializer.schema"</js>
	 * 	<li><b>Data type:</b>  <code>HttpPartSchema</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link OpenApiPartSerializerBuilder#schema(HttpPartSchema)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines the OpenAPI schema for this part serializer.
	 */
	public static final String OAPI_schema = PREFIX + "schema.o";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link OpenApiPartSerializer}, all default settings. */
	public static final OpenApiPartSerializer DEFAULT = new OpenApiPartSerializer(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final HttpPartSchema schema;

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public OpenApiPartSerializer(PropertyStore ps) {
		super(
			ps.builder()
				.set(UON_encoding, false)
				.build()
		);
		this.schema = getProperty(OAPI_schema, HttpPartSchema.class, HttpPartSchema.DEFAULT);
	}

	@Override /* Context */
	public OpenApiPartSerializerBuilder builder() {
		return new OpenApiPartSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link UonPartSerializerBuilder} object.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link UonPartSerializerBuilder} object.
	 */
	public static OpenApiPartSerializerBuilder create() {
		return new OpenApiPartSerializerBuilder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Entry point methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override
	public OpenApiPartSerializerSession createSession(SerializerSessionArgs args) {
		return new OpenApiPartSerializerSession(this, args);
	}

	@Override
	public OpenApiPartSerializerSession createSession() {
		return new OpenApiPartSerializerSession(this, SerializerSessionArgs.DEFAULT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  OpenAPI schema description.
	 *
	 * @see #OAPI_schema
	 * @return
	 * 	The default part schema on this serializer, or <jk>null</jk> if none is defined.
	 */
	protected final HttpPartSchema getSchema() {
		return schema;
	}
}
