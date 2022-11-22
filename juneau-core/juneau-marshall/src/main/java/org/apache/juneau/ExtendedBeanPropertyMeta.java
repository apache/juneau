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
 * Defines extended language-specific metadata associated with a bean property.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class ExtendedBeanPropertyMeta extends ExtendedMeta {

	private final BeanPropertyMeta bpm;

	/**
	 * Constructor.
	 *
	 * @param bpm The metadata of the bean property we're extending.
	 * @throws BeanRuntimeException If any error occurred trying to construct the metadata.
	 */
	public ExtendedBeanPropertyMeta(BeanPropertyMeta bpm) throws BeanRuntimeException {
		this.bpm = bpm;
	}

	/**
	 * Returns the bean property metadata that was passed into the constructor.
	 *
	 * @return The bean property metadata that was passed into the constructor.
	 */
	protected BeanPropertyMeta getBeanPropertyMeta() {
		return bpm;
	}
}
