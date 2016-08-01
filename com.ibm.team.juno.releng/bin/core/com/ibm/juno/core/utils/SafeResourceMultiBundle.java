/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.util.*;

/**
 * A collection of {@link SafeResourceBundle} objects.
 * Allows servlets to define resource bundles for different classes in the class hierarchy.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class SafeResourceMultiBundle extends SafeResourceBundle {

	private List<SafeResourceBundle> bundles = new ArrayList<SafeResourceBundle>();

	/**
	 * Constructor.
	 *
	 * @param bundles The resource bundles to wrap.  Can be <jk>null</jk> to indicate a missing bundle.
	 */
	public SafeResourceMultiBundle(Collection<SafeResourceBundle> bundles) {
		this.bundles.addAll(bundles);
 	}

	@Override /* SafeResourceBundle */
	public String findFirstString(String...keys) {
		for (String key : keys)
			for (SafeResourceBundle srb : bundles)
				if (srb.containsKey(key))
					return srb.getString(key);
		return null;
	}

	@Override /* ResourceBundle */
	public boolean containsKey(String key) {
		for (SafeResourceBundle srb : bundles)
			if (srb.containsKey(key))
				return true;
		return false;
	}

	@Override /* SafeResourceBundle */
	public String getString(String key, Object...args) {
		for (SafeResourceBundle srb : bundles)
			if (srb.containsKey(key))
				return srb.getString(key, args);
		return "{!" + key + "}";
	}

	@Override /* ResourceBundle */
	@SuppressWarnings("unchecked")
	public Set<String> keySet() {
		MultiSet<String> s = new MultiSet<String>();
		for (SafeResourceBundle rb : bundles)
			s.append(rb.keySet());
		return s;
	}

	@Override /* ResourceBundle */
	@SuppressWarnings("unchecked")
	public Enumeration<String> getKeys() {
		MultiSet<String> s = new MultiSet<String>();
		for (SafeResourceBundle rb : bundles)
			s.append(rb.keySet());
		return s.enumerator();
	}

	@Override /* ResourceBundle */
	protected Object handleGetObject(String key) {
		for (SafeResourceBundle srb : bundles)
			if (srb.containsKey(key))
				return srb.handleGetObject(key);
		return "{!"+key+"}";
	}
}