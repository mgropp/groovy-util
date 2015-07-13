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

import java.io.Serializable;

public final class Pair<A,B> implements Serializable {
	private static final long serialVersionUID = 5421099056893167128L;
	
	public A first;
	public B second;

	public Pair(A first, B second) {
		this.first = first;
		this.second = second;
	}
	
	public static <A,B> Pair<A,B> of(A first, B second) {
		return new Pair<A,B>(first, second);
	}

	@Override
	public int hashCode() {
		int hashFirst = (first != null) ? first.hashCode() : 0;
		int hashSecond = (second != null) ? second.hashCode() : 0;

		return (hashFirst + hashSecond) * hashSecond + hashFirst;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Pair)) {
			return false;
		}
		
		Pair<?,?> otherPair = (Pair<?,?>)other;
		return
			(
				this.first == otherPair.first
				||
				(
					this.first != null &&
					otherPair.first != null &&
					this.first.equals(otherPair.first)
				)
			)
			&&
			(
				this.second == otherPair.second
				||
				(
					this.second != null &&
					otherPair.second != null &&
					this.second.equals(otherPair.second)
				)
			);
	}

	@Override
	public String toString() {
		return "<" + first + ", " + second + ">";
	}
}