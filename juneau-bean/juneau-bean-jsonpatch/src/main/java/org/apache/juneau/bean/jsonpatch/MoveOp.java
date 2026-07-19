/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.jsonpatch;

import org.apache.juneau.marshall.*;

/**
 * The JSON Patch {@code "move"} operation per
 * <a href="https://datatracker.ietf.org/doc/html/rfc6902#section-4.4">RFC 6902 &#167; 4.4</a>.
 *
 * <p>
 * Moves the value at {@link #getFrom() from} to {@link #getPath() path}.
 */
@Marshalled(typeName = "move")
public class MoveOp extends JsonPatchOperation {

	private String from;

	/**
	 * Default constructor.
	 */
	public MoveOp() {}

	/**
	 * Convenience constructor.
	 *
	 * @param path The JSON Pointer destination.  Can be <jk>null</jk>.
	 * @param from The JSON Pointer source.  Can be <jk>null</jk>.
	 */
	public MoveOp(String path, String from) {
		super(path);
		this.from = from;
	}

	/**
	 * Bean property getter:  <property>from</property>.
	 *
	 * @return The value of the <property>from</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getFrom() { return from; }

	/**
	 * Bean property setter:  <property>from</property>.
	 *
	 * @param value The new value for this property.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public MoveOp setFrom(String value) {
		from = value;
		return this;
	}
}
