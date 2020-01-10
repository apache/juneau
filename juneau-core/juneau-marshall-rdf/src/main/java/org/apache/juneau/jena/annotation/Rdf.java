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
package org.apache.juneau.jena.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.jena.*;

/**
 * Annotation for specifying options for RDF serializers.
 *
 * <p>
 * Can be applied to Java packages, types, fields, and methods.
 *
 * <p>
 * Can be used for the following:
 * <ul>
 * 	<li>Override the default behavior of how collections and arrays are serialized.
 * </ul>
 */
@Documented
@Target({PACKAGE,TYPE,FIELD,METHOD})
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
	 */
	boolean beanUri() default false;

	/**
	 * The format for how collections (lists and arrays for example) are serialized in RDF.
	 *
	 * @see RdfCollectionFormat
	 */
	RdfCollectionFormat collectionFormat() default RdfCollectionFormat.DEFAULT;

	/**
	 * Sets the namespace URI of this property or class.
	 *
	 * <p>
	 * Must be matched with a {@link #prefix() @Rdf(prefix)} annotation on this object, a parent object, or a {@link RdfNs @RdfNs} with the
	 * same name through the {@link RdfSchema#rdfNs() @RdfSchema(rdfNs)} annotation on the package.
	 */
	String namespace() default "";

	/**
	 * Dynamically apply this annotation to the specified classes/methods/fields.
	 *
	 * <p>
	 * Used in conjunction with the {@link RdfConfig#applyRdf()}.
	 * It is ignored when the annotation is applied directly to classes/methods/fields.
	 *
	 * <p>
	 * The valid pattern matches are:
	 * <ul>
	 * 	<li>Classes:
	 * 		<ul>
	 * 			<li>Fully qualified: <js>"com.foo.MyClass"</js>
	 * 			<li>Fully qualified inner class: <js>"com.foo.MyClass$Inner1$Inner2"</js>
	 * 			<li>Simple: <js>"MyClass"</js>
	 * 			<li>Simple inner: <js>"MyClass$Inner1$Inner2"</js> or <js>"Inner1$Inner2"</js> or <js>"Inner2"</js>
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>Fully qualified with args: <js>"com.foo.MyClass.myMethod(String,int)"</js> or <js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js> or <js>"com.foo.MyClass.myMethod()"</js>
	 * 			<li>Fully qualified: <js>"com.foo.MyClass.myMethod"</js>
	 * 			<li>Simple with args: <js>"MyClass.myMethod(String,int)"</js> or <js>"MyClass.myMethod(java.lang.String,int)"</js> or <js>"MyClass.myMethod()"</js>
	 * 			<li>Simple: <js>"MyClass.myMethod"</js>
	 * 			<li>Simple inner class: <js>"MyClass$Inner1$Inner2.myMethod"</js> or <js>"Inner1$Inner2.myMethod"</js> or <js>"Inner2.myMethod"</js>
	 * 		</ul>
	 * 	<li>Fields:
	 * 		<ul>
	 * 			<li>Fully qualified: <js>"com.foo.MyClass.myField"</js>
	 * 			<li>Simple: <js>"MyClass.muyField"</js>
	 * 			<li>Simple inner class: <js>"MyClass$Inner1$Inner2.myField"</js> or <js>"Inner1$Inner2.myField"</js> or <js>"Inner2.myField"</js>
	 * 		</ul>
	 * 	<li>A comma-delimited list of anything on this list.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-marshall.DynamicallyAppliedAnnotations}
	 * </ul>
	 */
	String on() default "";

	/**
	 * Sets the XML prefix of this property or class.
	 *
	 * <p>
	 * Must either be matched to a {@link #namespace() @Rdf(namespace)} annotation on the same object, parent object, or a {@link RdfNs @RdfNs}
	 * with the same name through the {@link RdfSchema#rdfNs() @RdfSchema(rdfNs)} annotation on the package.
	 */
	String prefix() default "";
}
