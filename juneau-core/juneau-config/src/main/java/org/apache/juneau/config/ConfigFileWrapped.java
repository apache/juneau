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
package org.apache.juneau.config;

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.config.listener.*;
import org.apache.juneau.config.vars.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Wraps an instance of {@link ConfigFileImpl} in an interface that will automatically replace {@link VarResolver}
 * variables.
 * 
 * <p>
 * The {@link ConfigFile#getResolving(VarResolver)} returns an instance of this class.
 * 
 * <p>
 * This class overrides the {@link #getString(String, String)} to resolve string variables.
 * <br>All other method calls are passed through to the inner config file.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-config'>Overview &gt; juneau-config</a>
 * </ul>
 */
public final class ConfigFileWrapped extends ConfigFile {

	private final ConfigFileImpl cf;
	private final VarResolverSession vs;

	ConfigFileWrapped(ConfigFileImpl cf, VarResolver vr) {
		this.cf = cf;
		this.vs = vr.builder()
			.vars(ConfigFileVar.class)
			.contextObject(ConfigFileVar.SESSION_config, cf)
			.build()
			.createSession();
	}

	ConfigFileWrapped(ConfigFileImpl cf, VarResolverSession vs) {
		this.cf = cf;
		this.vs = vs;
	}

	@Override /* ConfigFile */
	public void clear() {
		cf.clear();
	}

	@Override /* ConfigFile */
	public boolean containsKey(Object key) {
		return cf.containsKey(key);
	}

	@Override /* ConfigFile */
	public boolean containsValue(Object value) {
		return cf.containsValue(value);
	}

	@Override /* ConfigFile */
	public Set<java.util.Map.Entry<String,Section>> entrySet() {
		return cf.entrySet();
	}

	@Override /* ConfigFile */
	public Section get(Object key) {
		return cf.get(key);
	}

	@Override /* ConfigFile */
	public boolean isEmpty() {
		return cf.isEmpty();
	}

	@Override /* ConfigFile */
	public Set<String> keySet() {
		return cf.keySet();
	}

	@Override /* ConfigFile */
	public Section put(String key, Section value) {
		return cf.put(key, value);
	}

	@Override /* ConfigFile */
	public void putAll(Map<? extends String,? extends Section> map) {
		cf.putAll(map);
	}

	@Override /* ConfigFile */
	public Section remove(Object key) {
		return cf.remove(key);
	}

	@Override /* ConfigFile */
	public int size() {
		return cf.size();
	}

	@Override /* ConfigFile */
	public Collection<Section> values() {
		return cf.values();
	}

	@Override /* ConfigFile */
	public ConfigFile loadIfModified() throws IOException {
		cf.loadIfModified();
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile load() throws IOException {
		cf.load();
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile load(Reader r) throws IOException {
		cf.load(r);
		return this;
	}


	@Override /* ConfigFile */
	public boolean isEncoded(String key) {
		return cf.isEncoded(key);
	}

	@Override /* ConfigFile */
	public ConfigFile addLines(String section, String... lines) {
		cf.addLines(section, lines);
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile addHeaderComments(String section, String... headerComments) {
		cf.addHeaderComments(section, headerComments);
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile clearHeaderComments(String section) {
		cf.clearHeaderComments(section);
		return this;
	}

	@Override /* ConfigFile */
	public Section getSection(String name) {
		return cf.getSection(name);
	}

	@Override /* ConfigFile */
	public Section getSection(String name, boolean create) {
		return cf.getSection(name, create);
	}

	@Override /* ConfigFile */
	public ConfigFile addSection(String name) {
		cf.addSection(name);
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile setSection(String name, Map<String,String> contents) {
		cf.setSection(name, contents);
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile removeSection(String name) {
		cf.removeSection(name);
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile save() throws IOException {
		cf.save();
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile serializeTo(Writer out, ConfigFileFormat format) throws IOException {
		cf.serializeTo(out, format);
		return this;
	}

	@Override /* ConfigFile */
	public String toString() {
		return cf.toString();
	}

	@Override /* ConfigFile */
	public ConfigFile getResolving(VarResolver varResolver) {
		assertFieldNotNull(varResolver, "vr");
		return new ConfigFileWrapped(cf, varResolver);
	}

	@Override /* ConfigFile */
	public ConfigFile getResolving(VarResolverSession varSession) {
		assertFieldNotNull(varSession, "vs");
		return new ConfigFileWrapped(cf, varSession);
	}

	@Override /* ConfigFile */
	public ConfigFile getResolving() {
		return new ConfigFileWrapped(cf, VarResolver.DEFAULT);
	}

	@Override /* ConfigFile */
	public ConfigFile addListener(ConfigListener listener) {
		cf.addListener(listener);
		return this;
	}

	@Override /* ConfigFile */
	public Writable toWritable() {
		return cf.toWritable();
	}

	@Override /* ConfigFile */
	public ConfigFile merge(ConfigFile newCf) {
		cf.merge(newCf);
		return this;
	}

	@Override /* ConfigFile */
	protected BeanSession getBeanSession() {
		return cf.getBeanSession();
	}

	@Override /* ConfigFile */
	public String get(String sectionName, String sectionKey) {
		return vs.resolve(cf.get(sectionName, sectionKey));
	}

	@Override /* ConfigFile */
	public String put(String sectionName, String sectionKey, String value, boolean encoded) {
		return cf.put(sectionName, sectionKey, value, encoded);
	}

	@Override /* ConfigFile */
	public String put(String sectionName, String sectionKey, Object value, Serializer serializer, boolean encoded,
			boolean newline) throws SerializeException {
		return cf.put(sectionName, sectionKey, value, serializer, encoded, newline);
	}

	@Override /* ConfigFile */
	public String remove(String sectionName, String sectionKey) {
		return cf.remove(sectionName, sectionKey);
	}

	@Override /* ConfigFile */
	public Set<String> getSectionKeys(String sectionName) {
		return cf.getSectionKeys(sectionName);
	}

	@Override /* ConfigFile */
	protected void readLock() {
		cf.readLock();
	}

	@Override /* ConfigFile */
	protected void readUnlock() {
		cf.readUnlock();
	}

	@Override /* ConfigFile */
	protected String serialize(Object o, Serializer s, boolean newline) throws SerializeException {
		return cf.serialize(o, s, newline);
	}

	@Override /* ConfigFile */
	protected <T> T parse(String s, Parser parser, Type type, Type... args) throws ParseException {
		return cf.parse(s, parser, type, args);
	}
}
