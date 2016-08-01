/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.xml.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import com.ibm.juno.core.xml.*;

/**
 * Annotation for specifying various XML options for the XML and RDF/XML serializers.
 * <p>
 * 	Can be applied to Java packages, types, fields, and methods.
 * <p>
 * 	Can be used for the following:
 * <ul>
 * 	<li>Override the element name on the XML representation of a bean or object.
 * 	<li>Override the child element name on the XML representation of collection or array properties.
 * 	<li>Specify the XML namespace on a package, class, or method.
 * 	<li>Override the XML format on a POJO.
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target({TYPE,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Xml {

	/**
	 * Sets the name of the XML element in cases where the XML element has no name (e.g. the root element).
	 * <p>
	 * 	Applies only to {@link ElementType#TYPE}.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<ja>@Xml</ja>(name=<js>"MyBean"</js>)
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public int</jk> f1 = 123;
	 * 	}
	 * 		</p>
	 * 		<p>
	 * 			Without the <ja>@Xml</ja> annotations, serializing this bean as XML would have produced the following...
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	<xt>&lt;object&gt;</xt>
	 * 		<xt>&lt;f1&gt;</xt>123<xt>&lt;/f1&gt;</xt>
	 * 	<xt>&lt;/object&gt;</xt>
	 * 		</p>
	 * 		<p>
	 * 			With the annotations, serializing this bean as XML produces the following...
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	<xt>&lt;MyBean&gt;</xt>
	 * 		<xt>&lt;f1&gt;</xt>123<xt>&lt;/f1&gt;</xt>
	 * 	<xt>&lt;/MyBean&gt;</xt>
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 */
	String name() default "";

	/**
	 * Sets the name of the XML child elements for bean properties of type collection and array.
	 * <p>
	 * 	Applies only to collection and array bean properties.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<jk>public class</jk> MyBean {
	 * 		<ja>@Xml</ja>(childName=<js>"child"</js>}
	 * 		<jk>public</jk> String[] <jf>children</jf> = {<js>"foo"</js>,<js>"bar"</js>};
	 * 	}
	 * 		</p>
	 * 		<p>
	 * 			Without the <ja>@Xml</ja> annotation, serializing this bean as XML would have produced the following...
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	<xt>&lt;object&gt;</xt>
	 * 		<xt>&lt;children&gt;</xt>
	 * 			<xt>&lt;string&gt;</xt>foo<xt>&lt;/string&gt;</xt>
	 * 			<xt>&lt;string&gt;</xt>bar<xt>&lt;/string&gt;</xt>
	 * 		<xt>&lt;/children&gt;</xt>
	 * 	<xt>&lt;/object&gt;</xt>
	 * 		</p>
	 * 		<p>
	 * 			With the annotations, serializing this bean as XML produces the following...
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	<xt>&lt;object&gt;</xt>
	 * 		<xt>&lt;children&gt;</xt>
	 * 			<xt>&lt;child&gt;</xt>foo<xt>&lt;/child&gt;</xt>
	 * 			<xt>&lt;child&gt;</xt>bar<xt>&lt;/child&gt;</xt>
	 * 		<xt>&lt;/children&gt;</xt>
	 * 	<xt>&lt;/object&gt;</xt>
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 */
	String childName() default "";

	/**
	 * Sets the XML prefix of this property or class.
	 * <ul>
	 * 	<li>When applied to a {@link ElementType#TYPE}, namespace is applied to all properties in the class, and all
	 * 		subclasses of the class.
	 * 	<li>When applied to bean properties on {@link ElementType#METHOD} and {@link ElementType#FIELD}, applies
	 * 		to the bean property.
	 * </ul>
	 * <p>
	 * 	Must either be matched to a {@link #namespace()} annotation on the same object, parent object, or a {@link XmlNs} with the same name
	 * 	through the {@link XmlSchema#xmlNs()} annotation on the package.
	 * </p>
	 */
	String prefix() default "";

	/**
	 * Sets the namespace URI of this property or class.
	 * <p>
	 * 	Must be matched with a {@link #prefix()} annotation on this object, a parent object, or a {@link XmlNs} with the same name
	 * 	through the {@link XmlSchema#xmlNs()} annotation on the package.
	 */
	String namespace() default "";

	/**
	 * The {@link XmlFormat} to use for serializing this object type.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// Normally, bean properties would be rendered as child elements of the bean element.</jc>
	 * 		<jc>// Override so that it's rendered as a "f1='123'" attribute on the bean element instead.</jc>
	 * 		<ja>@Xml</ja>(format=XmlFormat.<jsf>ATTR</jsf>}
	 * 		<jk>public int</jk> f1 = 123;
	 *
	 * 		<jc>// Normally, bean URL properties would be rendered as XML attributes on the bean element.</jc>
	 * 		<jc>// Override so that it's rendered as an &lt;href&gt;http://foo&lt;/href&gt; child element instead.</jc>
	 * 		<ja>@BeanProperty</ja>(uri=<jk>true</jk>)
	 * 		<ja>@Xml</ja>(format=XmlFormat.<jsf>ELEMENT</jsf>}
	 * 		<jk>public</jk> URL <jf>href</jf> = <jk>new</jk> URL(<js>"http://foo"</js>);
	 *
	 * 		<jc>// Normally, collection properties would be grouped under a single &lt;children&gt; child element on the bean element.</jc>
	 * 		<jc>// Override so that entries are directly children of the bean element with each entry having an element name of &lt;child&gt;.</jc>
	 * 		<ja>@Xml</ja>(format=XmlFormat.<jsf>COLLAPSED</jsf>, childName=<js>"child"</js>}
	 * 		<jk>public</jk> String[] <jf>children</jf> = <js>"foo"</js>,<js>"bar"</js>};
	 * 	}
	 * 		</p>
	 * 		<p>
	 * 			Without the <ja>@Xml</ja> annotations, serializing this bean as XML would have produced the following...
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	<xt>&lt;object</xt> <xa>href</xa>=<js>'http://foo'</js><xt>&gt;</xt>
	 * 		<xt>&lt;f1&gt;</xt>123<xt>&lt;/f1&gt;</xt>
	 * 		<xt>&lt;children&gt;</xt>
	 * 			<xt>&lt;string&gt;</xt>foo<xt>&lt;/string&gt;</xt>
	 * 			<xt>&lt;string&gt;</xt>bar<xt>&lt;/string&gt;</xt>
	 * 		<xt>&lt;/children&gt;</xt>
	 * 	<xt>&lt;/object&gt;</xt>
	 * 		</p>
	 * 		<p>
	 * 			With the annotations, serializing this bean as XML produces the following...
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	<xt>&lt;object</xt> <xa>f1</xa>=<js>'123'</js><xt>&gt;</xt>
	 * 		<xt>&lt;href&gt;</xt>http://foo<xt>&lt;/href&gt;</xt>
	 * 		<xt>&lt;child&gt;</xt>foo<xt>&lt;/child&gt;</xt>
	 * 		<xt>&lt;child&gt;</xt>bar<xt>&lt;/child&gt;</xt>
	 * 	<xt>&lt;/object&gt;</xt>
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 */
	XmlFormat format() default XmlFormat.NORMAL;

	/**
	 * Associates a content handler with a bean class or bean property.
	 * <p>
	 * 	Refer to {@link XmlContentHandler} for more information.
	 */
	Class<? extends XmlContentHandler<?>> contentHandler() default XmlContentHandler.NULL.class;
}
