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

package org.apache.juneau.releng.nexus;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Resolves {@code {username, password}} for a named {@code <server>} id out of {@code ~/.m2/settings.xml}.
 * A minimal standalone XML parse rather than pulling in the full Maven settings-builder dependency. Does
 * not attempt {@code settings-security.xml} master-password decryption; an encrypted password (wrapped in
 * {@code {...}}) is surfaced as a clear failure rather than silently mis-resolved.
 */
public final class MavenSettingsCredentials {

	/** Resolved credentials for a {@code <server>} entry. */
	public record Credentials(String username, String password) {
	}

	private MavenSettingsCredentials() {
	}

	/** Resolve from the default {@code ~/.m2/settings.xml}. */
	public static Credentials resolve(String serverId) {
		return resolve(serverId, Path.of(System.getProperty("user.home"), ".m2", "settings.xml"));
	}

	/** Resolve from an explicit settings.xml path (test seam). */
	public static Credentials resolve(String serverId, Path settingsXml) {
		if (!Files.isRegularFile(settingsXml))
			throw isex("Maven settings file not found: %s", settingsXml);
		try {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			var doc = factory.newDocumentBuilder().parse(settingsXml.toFile());
			var servers = doc.getElementsByTagName("server");
			for (int i = 0; i < servers.getLength(); i++) {
				var server = (Element) servers.item(i);
				if (!serverId.equals(childText(server, "id")))
					continue;
				var username = childText(server, "username");
				var password = childText(server, "password");
				if (password != null && password.startsWith("{") && password.endsWith("}"))
					throw isex("Encrypted <password> for server '%s' is not supported; "
							+ "decrypt via settings-security.xml master password is not implemented", serverId);
				if (username == null || password == null)
					throw isex("Server '%s' is missing username/password in %s", serverId, settingsXml);
				return new Credentials(username, password);
			}
			throw isex("No <server id=\"%s\"> entry found in %s", serverId, settingsXml);
		} catch (IOException | ParserConfigurationException | SAXException e) {
			throw isex(e, "Cannot parse Maven settings file: %s", settingsXml);
		}
	}

	private static String childText(Element parent, String tag) {
		NodeList nodes = parent.getElementsByTagName(tag);
		if (nodes.getLength() == 0)
			return null;
		var text = nodes.item(0).getTextContent();
		return text == null ? null : text.trim();
	}
}
