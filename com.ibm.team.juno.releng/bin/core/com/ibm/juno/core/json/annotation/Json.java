/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.json.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Annotation for specifying various JSON options for the JSON serializers and parsers.
 * <p>
 * 	Can be applied to Java types.
 * <p>
 * 	Can be used for the following:
 * <ul>
 * 	<li>Wrap bean instances inside wrapper object (e.g. <code>{'wrapperAttr':bean}</code>).
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Json {

	/**
	 * Wraps beans in a JSON object with the specified attribute name.
	 * <p>
	 * 	Applies only to {@link ElementType#TYPE}.
	 * <p>
	 * 	This annotation can be applied to beans as well as other objects serialized to other types (e.g. strings).
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	<ja>@Json</ja>(wrapperAttr=<js>"myWrapper"</js>)
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public int</jk> f1 = 123;
	 * 	}
	 * </p>
	 * <p>
	 * 	Without the <ja>@Xml</ja> annotations, serializing this bean as JSON would have produced the following...
	 * </p>
	 * <p class='bcode'>
	 * 	{
	 * 		f1: 123
	 * 	}
	 * </p>
	 * <p>
	 * 	With the annotations, serializing this bean as XML produces the following...
	 * </p>
	 * <p class='bcode'>
	 * 	{
	 * 		myWrapper: {
	 * 			f1: 123
	 * 		}
	 * 	}
	 * </p>
	 * 	</dd>
	 * </dl>
	 */
	String wrapperAttr() default "";
}
