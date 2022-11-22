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
package org.apache.juneau.jena;

import org.apache.juneau.*;
import org.apache.juneau.xml.*;

/**
 * Interface for providing access to {@link RdfClassMeta}, {@link RdfBeanMeta}, and {@link RdfBeanPropertyMeta} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
public interface RdfMetaProvider extends XmlMetaProvider {

	/**
	 * Returns the language-specific metadata on the specified class.
	 *
	 * @param cm The class to return the metadata on.
	 * @return The metadata.
	 */
	RdfClassMeta getRdfClassMeta(ClassMeta<?> cm);

	/**
	 * Returns the language-specific metadata on the specified bean.
	 *
	 * @param bm The bean to return the metadata on.
	 * @return The metadata.
	 */
	RdfBeanMeta getRdfBeanMeta(BeanMeta<?> bm);

	/**
	 * Returns the language-specific metadata on the specified bean property.
	 *
	 * @param bpm The bean property to return the metadata on.
	 * @return The metadata.
	 */
	RdfBeanPropertyMeta getRdfBeanPropertyMeta(BeanPropertyMeta bpm);
}
