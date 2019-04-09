package org.apache.juneau.server.config.repository;

import org.apache.juneau.json.annotation.Json;

@Json
public class ConfigItem {

	public ConfigItem(String value) {
		this.value = value;
	}

	private String value;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
