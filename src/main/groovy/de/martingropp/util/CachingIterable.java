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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Take an iterator and (lazily) cache every item that is
 * retrieved.
 * This class does not support concurrent iterations! 
 */
public class CachingIterable<T> implements Iterable<T> {
	private final Iterator<T> iterator;
	// use weak reference?
	private final List<T> cache = new ArrayList<>();
	
	public CachingIterable(Iterator<T> iterator) {
		this.iterator = iterator;
	}
	
	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private boolean fromCache = true;
			private Iterator<T> cacheIterator = cache.iterator();
			
			@Override
			public boolean hasNext() {
				if (!fromCache) {
					return iterator.hasNext();
				}
				
				if (cacheIterator.hasNext()) {
					return true;
				}
				
				fromCache = false;
				return iterator.hasNext();
			}

			@Override
			public T next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				
				if (fromCache) {
					return cacheIterator.next();
				} else {
					T next = iterator.next();
					cache.add(next);
					return next;
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	
	}
}
