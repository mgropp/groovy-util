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

public final class Triple<A,B,C> implements Serializable {
	private static final long serialVersionUID = 5421099056893167128L;
	
	public final A first;
	public final B second;
	public final C third;

	public Triple(A first, B second, C third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	public static <A,B,C> Triple<A,B,C> of(A first, B second, C third) {
		return new Triple<A,B,C>(first, second, third);
	}

	@Override
	public int hashCode() {
		int hash = 17;
		hash = hash * 31 + ((first != null) ? first.hashCode() : 0);
		hash = hash * 31 + ((second != null) ? second.hashCode() : 0);
		hash = hash * 31 + ((third != null) ? third.hashCode() : 0);
		
		return hash;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Triple)) {
			return false;
		}
		
		Triple<?,?,?> otherTriple = (Triple<?,?,?>)other;
		return
			(
				this.first == otherTriple.first
				||
				(
					this.first != null &&
					otherTriple.first != null &&
					this.first.equals(otherTriple.first)
				)
			)
			&&
			(
				this.second == otherTriple.second
				||
				(
					this.second != null &&
					otherTriple.second != null &&
					this.second.equals(otherTriple.second)
				)
			)
			&&
			(
				this.third == otherTriple.third
				||
				(
					this.third != null &&
					otherTriple.third != null &&
					this.third.equals(otherTriple.third)
				)
			);
	}

	@Override
	public String toString() {
		return "<" + first + ", " + second + ", " + third + ">";
	}
}