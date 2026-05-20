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

import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;

/**
 * Abstract base for a JSON Patch operation per
 * <a href="https://datatracker.ietf.org/doc/html/rfc6902">RFC 6902</a>.
 *
 * <p>
 * Polymorphic dispatch is wired here via
 * {@code @Marshalled(typePropertyName="op", dictionary={...})}: on read, the parser inspects the {@code op}
 * member and instantiates the matching concrete subclass; on write, the serializer emits {@code op} as the
 * discriminator and the operation's own members alongside.
 *
 * <p>
 * <b>Not</b> declared {@code sealed} - keeping it open lets downstream callers add their own ops if needed
 * (e.g. JSON Patch extensions or vendor-specific test operators) by adding the new subclass to a custom
 * dictionary.
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanJsonPatch">juneau-bean-jsonpatch</a>
 * </ul>
 */
@Marshalled(
	typePropertyName = "op",
	dictionary = { AddOp.class, RemoveOp.class, ReplaceOp.class, MoveOp.class, CopyOp.class, TestOp.class }
)
public abstract class JsonPatchOperation {

	private String path;

	/**
	 * Default constructor.
	 */
	protected JsonPatchOperation() {}

	/**
	 * Convenience constructor.
	 *
	 * @param path The JSON Pointer target of the operation.
	 */
	protected JsonPatchOperation(String path) {
		this.path = path;
	}

	/**
	 * Bean property getter:  <property>path</property>.
	 *
	 * <p>
	 * The target JSON Pointer (RFC 6901) of this operation.
	 *
	 * @return The value of the <property>path</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getPath() { return path; }

	/**
	 * Bean property setter:  <property>path</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonPatchOperation setPath(String value) {
		path = value;
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return JsonSerializer.DEFAULT.toString(this);
	}
}
