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
package org.apache.juneau.jena.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.jena.*;

/**
 * Annotation for specifying options for RDF serializers.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Marshalled classes/methods/fields/packages.
 * </ul>
 *
 * <p>
 * Can be used for the following:
 * <ul>
 * 	<li>Override the default behavior of how collections and arrays are serialized.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}

 * </ul>
 */
@Documented
@Target({ PACKAGE, TYPE, FIELD, METHOD })
@Retention(RUNTIME)
@Inherited
public @interface Rdf {

	/**
	 * Marks a bean property as a resource URI identifier for the bean.
	 *
	 * <p>
	 * Has the following effects on the following serializers:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		{@link RdfSerializer} - Will be rendered as the value of the <js>"rdf:about"</js> attribute
	 * 		for the bean.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	boolean beanUri() default false;

	/**
	 * The format for how collections (lists and arrays for example) are serialized in RDF.
	 *
	 * @see RdfCollectionFormat
	 * @return The annotation value.
	 */
	RdfCollectionFormat collectionFormat() default RdfCollectionFormat.DEFAULT;

	/**
	 * Sets the namespace URI of this property or class.
	 *
	 * <p>
	 * Must be matched with a {@link #prefix() @Rdf(prefix)} annotation on this object, a parent object, or a {@link RdfNs @RdfNs} with the
	 * same name through the {@link RdfSchema#rdfNs() @RdfSchema(rdfNs)} annotation on the package.
	 *
	 * @return The annotation value.
	 */
	String namespace() default "";

	/**
	 * Sets the XML prefix of this property or class.
	 *
	 * <p>
	 * Must either be matched to a {@link #namespace() @Rdf(namespace)} annotation on the same object, parent object, or a {@link RdfNs @RdfNs}
	 * with the same name through the {@link RdfSchema#rdfNs() @RdfSchema(rdfNs)} annotation on the package.
	 *
	 * @return The annotation value.
	 */
	String prefix() default "";
}
