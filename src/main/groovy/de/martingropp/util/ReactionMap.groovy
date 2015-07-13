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

package de.martingropp.util

import groovy.transform.TypeChecked

/**
 * Will trigger a reaction when both add and
 * trigger have been called for a certain id. 
 * 
 * @author mgropp
 */
@TypeChecked
public class ReactionMap {
	private Map<Object,Closure> closures = [:];
	private Map<Object,Object> arguments = [:];
	
	public synchronized void add(Object id, Closure closure) {
		if (arguments.containsKey(id)) {
			final argument = arguments[id];
			arguments.remove(id);
			
			closure(argument);
		
		} else {
			closures[id] = closure;
		}
	}
	
	public synchronized void trigger(Object id, Object argument=null) {
		if (closures.containsKey(id)) {
			final closure = closures[id];
			closures.remove(id);
			
			closure(argument);
		
		} else {
			arguments[id] = argument;
		}
	}
	
	@Override
	public String toString() {
		return "Closures: ${closures}\nArguments: ${arguments}"
	}
}
