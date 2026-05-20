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
package org.apache.juneau.bean.hal;

import java.util.*;

/**
 * Represents a list of {@link HalResource} objects.
 *
 * <p>
 * Serializes as a top-level JSON array when used as a value inside a HAL {@code _embedded} map. Mirrors the
 * top-level-array pattern used by {@code JsonSchemaArray}.
 *
 * @serial exclude
 */
public class HalResourceArray extends LinkedList<HalResource> {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public HalResourceArray() {}

	/**
	 * Constructor with predefined resources to add to this array.
	 *
	 * @param value The list of resources in this array.
	 */
	public HalResourceArray(HalResource...value) {
		addAll(value);
	}

	/**
	 * Convenience method for adding one or more {@link HalResource} objects to this array.
	 *
	 * @param value The {@link HalResource} objects to add to this array.
	 * @return This object.
	 */
	public HalResourceArray addAll(HalResource...value) {
		Collections.addAll(this, value);
		return this;
	}
}
