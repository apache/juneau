/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.filters;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filter.*;

/**
 * Transforms beans into {@link String Strings} by simply calling the {@link Object#toString()} method.
 * <p>
 * 	Allows you to specify classes that should just be converted to {@code Strings} instead of potentially
 * 	being turned into Maps by the {@link BeanContext} (or worse, throwing {@link BeanRuntimeException BeanRuntimeExceptions}).
 * <p>
 * 	This is usually a one-way filter.
 * 	Beans serialized as strings cannot be reconstituted using a parser unless it is a <a class='doclink' href='../package-summary.html#PojoCategories'>Type 5 POJO</a>.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> The class type of the bean.
 */
public class BeanStringFilter<T> extends PojoFilter<T,String> {

	/**
	 * Converts the specified bean to a {@link String}.
	 */
	@Override /* PojoFilter */
	public String filter(T o) {
		return o.toString();
	}
}
