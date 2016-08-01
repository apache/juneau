/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.net.*;

import com.ibm.juno.core.serializer.*;

/**
 * Used to identify a class or bean property as a URI.
 * <p>
 * 	By default, instances of {@link URL} and {@link URI} are considered URIs during serialization, and are
 * 		handled differently depending on the serializer (e.g. <code>HtmlSerializer</code> creates a hyperlink,
 * 		<code>RdfXmlSerializer</code> creates an <code>rdf:resource</code> object, etc...).
 * <p>
 * 	This annotation allows you to identify other classes that return URIs via <code>toString()</code> as URI objects.
 * <p>
 * 	Relative URIs are automatically prepended with {@link SerializerProperties#SERIALIZER_absolutePathUriBase} and {@link SerializerProperties#SERIALIZER_relativeUriBase}
 * 		during serialization just like relative <code>URIs</code>.
 * <p>
 * 	This annotation can be applied to classes, interfaces, or bean property methods for fields.
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 *
 * 	<jc>// Applied to a class whose toString() method returns a URI.</jc>
 * 	<ja>@URI</ja>
 * 	<jk>public class</jk> MyURI {
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String toString() {
 * 			<jk>return</jk> <js>"http://localhost:9080/foo/bar"</js>;
 * 		}
 * 	}
 *
 * 	<jc>// Applied to bean properties</jc>
 * 	<jk>public class</jk> MyBean {
 *
 * 		<ja>@URI</ja>
 * 		<jk>public</jk> String <jf>beanUri</jf>;
 *
 * 		<ja>@URI</ja>
 * 		<jk>public</jk> String getParentUri() {
 * 			...
 * 		}
 * 	}
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target({TYPE,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface URI {}