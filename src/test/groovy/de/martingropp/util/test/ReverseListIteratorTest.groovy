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

package de.martingropp.util.test;

import spock.lang.Specification
import de.martingropp.util.ReverseListIterator;

public class ReverseListIteratorTest extends Specification {
	def testReverseListIterator() {
		when:
			ReverseListIterator iterator = new ReverseListIterator(list.listIterator());
			LinkedList test = new LinkedList();
			for (item in iterator) {
				test.addFirst(item);
			}
		then:
			test == list
		where:
			list << [ [], [ 0 ], 0..1, 0..10 ];
	}
}
