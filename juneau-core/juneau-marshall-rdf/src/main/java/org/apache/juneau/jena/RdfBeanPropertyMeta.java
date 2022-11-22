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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.xml.*;

/**
 * Metadata on bean properties specific to the RDF serializers and parsers pulled from the {@link Rdf @Rdf} annotation
 * on the bean property.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
public class RdfBeanPropertyMeta extends ExtendedBeanPropertyMeta {

	/**
	 * Default instance.
	 */
	public static final RdfBeanPropertyMeta DEFAULT = new RdfBeanPropertyMeta();


	private RdfCollectionFormat collectionFormat = RdfCollectionFormat.DEFAULT;
	private Namespace namespace = null;
	private boolean isBeanUri;

	/**
	 * Constructor.
	 *
	 * @param bpm The metadata of the bean property of this additional metadata.
	 * @param mp RDF metadata provider (for finding information about other artifacts).
	 */
	public RdfBeanPropertyMeta(BeanPropertyMeta bpm, RdfMetaProvider mp) {
		super(bpm);

		List<Rdf> rdfs = bpm.getAllAnnotationsParentFirst(Rdf.class);
		List<RdfSchema> schemas = bpm.getAllAnnotationsParentFirst(RdfSchema.class);

		rdfs.forEach(x -> {
			if (x.collectionFormat() != RdfCollectionFormat.DEFAULT)
				collectionFormat = x.collectionFormat();
			if (x.beanUri())
				isBeanUri = true;
		});

		namespace = RdfUtils.findNamespace(rdfs, schemas);
	}

	private RdfBeanPropertyMeta() {
		super(null);
	}

	/**
	 * Returns the RDF collection format of this property from the {@link Rdf#collectionFormat} annotation on this bean
	 * property.
	 *
	 * @return The RDF collection format, or {@link RdfCollectionFormat#DEFAULT} if annotation not specified.
	 */
	protected RdfCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Returns the RDF namespace associated with this bean property.
	 *
	 * <p>
	 * Namespace is determined in the following order of {@link Rdf#prefix() @Rdf(prefix)} annotations:
	 * <ol>
	 * 	<li>Bean property field.
	 * 	<li>Bean getter.
	 * 	<li>Bean setter.
	 * 	<li>Bean class.
	 * 	<li>Bean package.
	 * 	<li>Bean superclasses.
	 * 	<li>Bean superclass packages.
	 * 	<li>Bean interfaces.
	 * 	<li>Bean interface packages.
	 * </ol>
	 *
	 * @return The namespace associated with this bean property, or <jk>null</jk> if no namespace is associated with it.
	 */
	public Namespace getNamespace() {
		return namespace;
	}

	/**
	 * Returns <jk>true</jk> if this bean property is marked with {@link Rdf#beanUri() @Rdf(beanUri)} as <jk>true</jk>.
	 *
	 * @return <jk>true</jk> if this bean property annotation is <jk>true</jk>.
	 */
	public boolean isBeanUri() {
		return isBeanUri;
	}
}
