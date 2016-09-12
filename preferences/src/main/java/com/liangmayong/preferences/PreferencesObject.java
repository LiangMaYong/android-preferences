package com.liangmayong.preferences;

public interface PreferencesObject {

	String toValue();

	void writeToObject(String value);
	
}
