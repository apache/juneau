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
package org.apache.juneau.rest.client.remote;

import java.util.*;

import static org.apache.juneau.httppart.HttpPartType.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;

/**
 * Represents the metadata about an annotated argument of a method on a REST proxy class.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#jrc.Proxies">REST Proxies</a>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 */
public final class RemoteOperationArg {

	private final int index;
	private final HttpPartType partType;
	private final Optional<HttpPartSerializer> serializer;
	private final HttpPartSchema schema;

	RemoteOperationArg(int index, HttpPartType partType, HttpPartSchema schema) {
		this.index = index;
		this.partType = partType;
		this.serializer = BeanCreator.of(HttpPartSerializer.class).type(schema.getSerializer()).execute();
		this.schema = schema;
	}

	/**
	 * Returns the name of the HTTP part.
	 *
	 * @return The name of the HTTP part.
	 */
	public String getName() {
		return schema.getName();
	}

	/**
	 * Returns whether the <c>skipIfEmpty</c> flag was found in the schema.
	 *
	 * @return <jk>true</jk> if the <c>skipIfEmpty</c> flag was found in the schema.
	 */
	public boolean isSkipIfEmpty() {
		return schema.isSkipIfEmpty();
	}

	/**
	 * Returns the method argument index.
	 *
	 * @return The method argument index.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Returns the HTTP part type.
	 *
	 * @return The HTTP part type.  Never <jk>null</jk>.
	 */
	public HttpPartType getPartType() {
		return partType;
	}

	/**
	 * Returns the HTTP part serializer to use for serializing this part.
	 *
	 * @return The HTTP part serializer, or the default if not specified.
	 */
	public Optional<HttpPartSerializer> getSerializer() {
		return serializer;
	}

	/**
	 * Returns the HTTP part schema information about this part.
	 *
	 * @return The HTTP part schema information, or <jk>null</jk> if not found.
	 */
	public HttpPartSchema getSchema() {
		return schema;
	}

	static RemoteOperationArg create(ParamInfo mpi) {
		int i = mpi.getIndex();
		if (mpi.hasAnnotation(Header.class)) {
			return new RemoteOperationArg(i, HEADER, HttpPartSchema.create(Header.class, mpi));
		} else if (mpi.hasAnnotation(Query.class)) {
			return new RemoteOperationArg(i, QUERY, HttpPartSchema.create(Query.class, mpi));
		} else if (mpi.hasAnnotation(FormData.class)) {
			return new RemoteOperationArg(i, FORMDATA, HttpPartSchema.create(FormData.class, mpi));
		} else if (mpi.hasAnnotation(Path.class)) {
			return new RemoteOperationArg(i, PATH, HttpPartSchema.create(Path.class, mpi));
		} else if (mpi.hasAnnotation(Content.class)) {
			return new RemoteOperationArg(i, BODY, HttpPartSchema.create(Content.class, mpi));
		}
		return null;
	}
}
