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
import de.martingropp.util.CachingIterable;

class CachingIterableTest extends Specification {
	def testCachingIterableSingle() {
		setup:
			List l = 1..10;
			CachingIterable cit = new CachingIterable(l.iterator());
		when:
			List l2 = new ArrayList<>();
			for (item in cit) {
				l2.add(item)
			}
		then:
			l == l2;
	}
	
	def testCachingIterableIncomplete() {
		setup:
			List l = 1..10;
			CachingIterable cit = new CachingIterable(l.iterator());
		when:
			int i = 0;
			if (last > 0) {
				for (item in cit) {
					i++;
					if (i >= last) {
						break;
					}
				}
			}
			
			List l2 = new ArrayList<>();
			for (item in cit) {
				l2.add(item);
			}
		then:
			l == l2;
		where:
			last << (0..10);
	}
}
