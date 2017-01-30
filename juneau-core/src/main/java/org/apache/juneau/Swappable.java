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
package org.apache.juneau;

/**
 * Identifies a class that gets swapped out for another class during serialization.
 * <p>
 * *** This feature has not yet been implemented ***
 * <p>
 * Allows fine-tuned controlling of serialization of classes by allowing you to create a surrogate
 * form of the class that then gets serialized instead of the original class.
 * <p>
 * During serialization, the {@link #swap(String)} method is used to convert this object into a serialized
 * form.
 * <p>
 * Serialized form can be any object that can be serialized by this framework.
 * <p>
 * Parsing back into the original object can be accomplished by specifying a public constructor that takes in
 * a single parameter of type T.
 *
 * @param <T> The class of the serialized form of this class.
 */
public interface Swappable<T> {

	/**
	 * Method to implement that converts this object to a surrogate serialized form.
	 *
	 * @param mediaType The media type string being serialized to (e.g. <js>"application/json"</js>).
	 * @return The surrogate object.
	 */
	public T swap(String mediaType);
}
