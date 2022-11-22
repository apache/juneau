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

import org.apache.juneau.jena.annotation.*;

/**
 * Used in conjunction with the {@link Rdf#collectionFormat() @Rdf(collectionFormat)} annotation to fine-tune how
 * classes, beans, and bean properties are serialized, particularly collections.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
public enum RdfCollectionFormat {

	/**
	 * Default formatting (default).
	 *
	 * <p>
	 * Inherit formatting from parent class or parent package.
	 * If no formatting specified at any level, default is {@link #SEQ}.
	 */
	DEFAULT,

	/**
	 * Causes collections and arrays to be rendered as RDF sequences.
	 */
	SEQ,

	/**
	 * Causes collections and arrays to be rendered as RDF bags.
	 */
	BAG,

	/**
	 * Causes collections and arrays to be rendered as RDF lists.
	 */
	LIST,

	/**
	 * Causes collections and arrays to be rendered as multi-valued RDF properties instead of sequences.
	 *
	 * <p>
	 * Note that enabling this setting will cause order of elements in the collection to be lost.
	 */
	MULTI_VALUED;

}