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
package org.apache.juneau.xml.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;

/**
 * Annotation for specifying various XML options for the XML and RDF/XML serializers.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Marshalled classes/methods/fields/packages.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when an {@link #on()} value is specified.
 * </ul>
 *
 * <p>
 * Can be used for the following:
 * <ul>
 * 	<li>Override the child element name on the XML representation of collection or array properties.
 * 	<li>Specify the XML namespace on a package, class, or method.
 * 	<li>Override the XML format on a POJO.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/XmlBasics">XML Basics</a>
 * </ul>
 */
@Documented
@Target({TYPE,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
@Repeatable(XmlAnnotation.Array.class)
@ContextApply(XmlAnnotation.Apply.class)
public @interface Xml {

	/**
	 * Sets the name of the XML child elements for bean properties of type collection and array.
	 *
	 * <p>
	 * Applies only to collection and array bean properties.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 * 		<ja>@Xml</ja>(childName=<js>"child"</js>}
	 * 		<jk>public</jk> String[] <jf>children</jf> = {<js>"foo"</js>,<js>"bar"</js>};
	 * 	}
	 * </p>
	 *
	 * <p>
	 * Without the <ja>@Xml</ja> annotation, serializing this bean as XML would have produced the following...
	 * <p class='bxml'>
	 * 	<xt>&lt;object&gt;</xt>
	 * 		<xt>&lt;children&gt;</xt>
	 * 			<xt>&lt;string&gt;</xt>foo<xt>&lt;/string&gt;</xt>
	 * 			<xt>&lt;string&gt;</xt>bar<xt>&lt;/string&gt;</xt>
	 * 		<xt>&lt;/children&gt;</xt>
	 * 	<xt>&lt;/object&gt;</xt>
	 * </p>
	 *
	 * <p>
	 * With the annotations, serializing this bean as XML produces the following...
	 * <p class='bxml'>
	 * 	<xt>&lt;object&gt;</xt>
	 * 		<xt>&lt;children&gt;</xt>
	 * 			<xt>&lt;child&gt;</xt>foo<xt>&lt;/child&gt;</xt>
	 * 			<xt>&lt;child&gt;</xt>bar<xt>&lt;/child&gt;</xt>
	 * 		<xt>&lt;/children&gt;</xt>
	 * 	<xt>&lt;/object&gt;</xt>
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String childName() default "";

    /**
     * Optional description for the exposed API.
     *
     * @return The annotation value.
     * @since 9.2.0
     */
    String[] description() default {};

    /**
	 * The {@link XmlFormat} to use for serializing this object type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// Normally, bean properties would be rendered as child elements of the bean element.</jc>
	 * 		<jc>// Override so that it's rendered as a "f1='123'" attribute on the bean element instead.</jc>
	 * 		<ja>@Xml</ja>(format=XmlFormat.<jsf>ATTR</jsf>}
	 * 		<jk>public int</jk> <jf>f1</jf> = 123;
	 *
	 * 		<jc>// Normally, bean URL properties would be rendered as XML attributes on the bean element.</jc>
	 * 		<jc>// Override so that it's rendered as an &lt;href&gt;http://foo&lt;/href&gt; child element instead.</jc>
	 * 		<ja>@Beanp</ja>(uri=<jk>true</jk>)
	 * 		<ja>@Xml</ja>(format=XmlFormat.<jsf>ELEMENT</jsf>}
	 * 		<jk>public</jk> URL <jf>href</jf> = <jk>new</jk> URL(<js>"http://foo"</js>);
	 *
	 * 		<jc>// Normally, collection properties would be grouped under a single &lt;children&gt; child element on the bean element.</jc>
	 * 		<jc>// Override so that entries are directly children of the bean element with each entry having an element name of &lt;child&gt;.</jc>
	 * 		<ja>@Xml</ja>(format=XmlFormat.<jsf>COLLAPSED</jsf>, childName=<js>"child"</js>}
	 * 		<jk>public</jk> String[] <jf>children</jf> = <js>"foo"</js>,<js>"bar"</js>};
	 * 	}
	 * </p>
	 *
	 * <p>
	 * Without the <ja>@Xml</ja> annotations, serializing this bean as XML would have produced the following...
	 * <p class='bxml'>
	 * 	<xt>&lt;object</xt> <xa>href</xa>=<js>'http://foo'</js><xt>&gt;</xt>
	 * 		<xt>&lt;f1&gt;</xt>123<xt>&lt;/f1&gt;</xt>
	 * 		<xt>&lt;children&gt;</xt>
	 * 			<xt>&lt;string&gt;</xt>foo<xt>&lt;/string&gt;</xt>
	 * 			<xt>&lt;string&gt;</xt>bar<xt>&lt;/string&gt;</xt>
	 * 		<xt>&lt;/children&gt;</xt>
	 * 	<xt>&lt;/object&gt;</xt>
	 * </p>
	 *
	 * <p>
	 * With the annotations, serializing this bean as XML produces the following...
	 * <p class='bxml'>
	 * 	<xt>&lt;object</xt> <xa>f1</xa>=<js>'123'</js><xt>&gt;</xt>
	 * 		<xt>&lt;href&gt;</xt>http://foo<xt>&lt;/href&gt;</xt>
	 * 		<xt>&lt;child&gt;</xt>foo<xt>&lt;/child&gt;</xt>
	 * 		<xt>&lt;child&gt;</xt>bar<xt>&lt;/child&gt;</xt>
	 * 	<xt>&lt;/object&gt;</xt>
	 * </p>
	 *
	 * @return The annotation value.
	 */
	XmlFormat format() default XmlFormat.DEFAULT;

	/**
	 * Sets the namespace URI of this property or class.
	 *
	 * <p>
	 * Must be matched with a {@link #prefix()} annotation on this object, a parent object, or a {@link XmlNs @XmlNs} with the
	 * same name through the {@link XmlSchema#xmlNs() @XmlSchema(xmlNs)} annotation on the package.
	 *
	 * @return The annotation value.
	 */
	String namespace() default "";

	/**
	 * Dynamically apply this annotation to the specified classes/methods/fields.
	 *
	 * <p>
	 * Used in conjunction with {@link org.apache.juneau.BeanContext.Builder#applyAnnotations(Class...)} to dynamically apply an annotation to an existing class/method/field.
	 * It is ignored when the annotation is applied directly to classes/methods/fields.
	 *
	 * <h5 class='section'>Valid patterns:</h5>
	 * <ul class='spaced-list'>
	 *  <li>Classes:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass"</js>
	 * 				</ul>
	 * 			<li>Fully qualified inner class:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass$Inner1$Inner2"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass"</js>
	 * 				</ul>
	 * 			<li>Simple inner:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2"</js>
	 * 					<li><js>"Inner1$Inner2"</js>
	 * 					<li><js>"Inner2"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>Fully qualified with args:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple with args:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner2.myMethod"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Fields:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner2.myField"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>A comma-delimited list of anything on this list.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] on() default {};

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Identical to {@link #on()} except allows you to specify class objects instead of a strings.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] onClass() default {};

	/**
	 * Sets the XML prefix of this property or class.
	 *
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When applied to a {@link ElementType#TYPE}, namespace is applied to all properties in the class, and all
	 * 		subclasses of the class.
	 * 	<li>
	 * 		When applied to bean properties on {@link ElementType#METHOD} and {@link ElementType#FIELD}, applies
	 * 		to the bean property.
	 * </ul>
	 *
	 * <p>
	 * Must either be matched to a {@link #namespace()} annotation on the same object, parent object, or a
	 * {@link XmlNs @XmlNs} with the same name through the {@link XmlSchema#xmlNs() @XmlSchema(xmlNs)} annotation on the package.
	 *
	 * @return The annotation value.
	 */
	String prefix() default "";
}