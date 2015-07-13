/*
 *  Copyright (c) Martin Gropp, All rights reserved.
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

import groovy.transform.TypeChecked
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FirstParam


@TypeChecked
public class GroovyUtil {
	public static <T, U extends Closeable> T withCloseable(U closeable, @ClosureParams(value=FirstParam.class) Closure<T> action) throws IOException {
		try {
			return action(closeable);
		}
		finally {
			closeable.close();
		}
	}
}
