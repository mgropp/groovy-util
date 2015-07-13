/*
 * Copyright (c) Martin Gropp, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package de.martingropp.util;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrimitiveUtil {
	public static boolean isPrimitiveWrapper(Object object) {
		if (object == null) {
			return false;
		}
		
		Class<?> cls = object.getClass();
		return (
			cls.equals(Boolean.class) ||
			cls.equals(Character.class) ||
			cls.equals(Byte.class) ||
			cls.equals(Short.class) ||
			cls.equals(Integer.class) ||
			cls.equals(Long.class) ||
			cls.equals(Float.class) ||
			cls.equals(Double.class)
		);
	}
	
	public static final Pattern LITERALS_PATTERN =
		Pattern.compile("\\s*(?:\\\"(?<string>(?:\\\\.|[^\"])*)\\\"|(?<number>(0x)?[0-9\\.]+)[LlFfDd]?|(?<boolean>true|false))\\s*(,)?\\s*");

	/**
	 * @param literals
	 *   String/number/boolean literals separated by commas.
	 * @return
	 *   a list of parsed literals (String, Number, Boolean)
	 * @throws ParseException, IllegalArgumentException
	 */
	public static List<?> parseLiterals(String literals) throws ParseException, IllegalArgumentException {
		// TODO: char
		List<Object> result = new ArrayList<>();
		Matcher m = LITERALS_PATTERN.matcher(literals);
		
		int start = 0;
		while (m.find(start)) {
			if (m.group("string") != null) {
				result.add(m.group("string"));
			} else if (m.group("number") != null) {
				result.add(NumberFormat.getInstance().parse(m.group("number")));
			} else if (m.group("boolean") != null) {
				result.add(Boolean.parseBoolean(m.group("boolean")));
			} else {
				throw new AssertionError();
			}
			
			start = m.end();
		}
		
		if (start != literals.length()) {
			throw new IllegalArgumentException("Could not parse the entire literals string!");
		}
		
		return result;
	}
}
