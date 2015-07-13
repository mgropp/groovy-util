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

import groovy.transform.TypeChecked

import java.text.MessageFormat
import java.util.Map.Entry
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A (not very elegant) way of bringing regular expressions to lists.
 * It matches list elements using closures and maps list elements to characters
 * that are processed using Java's String regex engine.
 * (A list item will be represented by a character group, e.g. '[abc]'.)
 * 
 * Regular expressions are written referring to the closures instead of
 * characters as in traditional string regexes.
 * 
 * Example:
 * We have a list of strings and want to find all occurrences of a string
 * starting with 'A' followed by at least one string of length 4.
 * 
 * ListRegex.findAll(
 *   "{0}{1}+",
 *   [ { it.startsWith("A") }, { it.length() == 4 } ],
 *   list
 * )
 * 
 * For more examples see the main method.
 * 
 * @author mgropp
 */
@TypeChecked
public class ListRegex {
	private static final String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz_";
	
	/**
	 * Find the first match of a regular expression in a list.
	 * 
	 * Example:
	 * find("{0}.{1}+", [ { it == 2 }, { it % 2 == 0 } ], [ 1, 2, 3, 4, 5, 6 ]) 
	 * 
	 * @param pattern
	 *   The regular expression.
	 *   Use {0}, {1}, ... to refer to the closures to match list items.
	 *   (Uses MessageFormat.format internally.)
	 *   DON'T USE PLAIN CHARACTERS! (We're not matching strings here!)
	 * @param closures
	 *   Closures to match list items (returning boolean values).
	 * @param list
	 *   The list to run the regex on
	 * @param groups
	 *   (out) accepts contents of capturing groups, may be null
	 * @return
	 */
	public static <T> Collection<T> find(
		String pattern,
		List<Closure> closures,
		List<T> list,
		List<? extends Collection<T>> groups
	) {
		List<String> closurePatterns = new ArrayList<>();
		String prepared = prepare(closures, list, closurePatterns);
		
		Pattern compiledPattern = Pattern.compile(
			MessageFormat.format(
				pattern,
				closurePatterns.toArray()
			)
		);
		
		Matcher matcher = compiledPattern.matcher(prepared);
		
		if (matcher.find()) {
			if (groups != null) {
				groups.clear();
				for (int i = 0; i <= matcher.groupCount(); i++) {
					if (matcher.start(i) < 0 || matcher.end(i) < 0) {
						groups.add(null);
						continue;
					}
					groups.add(list.subList(matcher.start(i), matcher.end(i)));
				}
				
				// We can't handle named groups because Java's regex API does not
				// give us the positions of named groups.
			}
			
			return list.subList(matcher.start(), matcher.end());
		}
		
		return null;
	}
	
	public static <T> Collection<T> find(String pattern, List<Closure> closures, List<T> list) {
		return find(pattern, closures, list, null);
	}

	/**
	 * Find all matches of a regular expression in a list.
	 *
	 * Example:
	 * find("{0}.{1}+", [ { it == 2 }, { it % 2 == 0 } ], [ 1, 2, 3, 4, 5, 6 ])
	 *
	 * @param pattern
	 *   The regular expression.
	 *   Use {0}, {1}, ... to refer to the closures to match list items.
	 *   (Uses MessageFormat.format internally.)
	 *   DON'T USE PLAIN CHARACTERS! (We're not matching strings here!)
	 * @param closures
	 *   Closures to match list items (returning boolean values).
	 * @param list
	 *   The list to run the regex on
	 * @param groups
	 *   (out) accepts contents of capturing groups, may be null
	 * @return
	 */

	public static <T> Collection<? extends Collection<T>> findAll(
		String pattern,
		List<Closure> closures,
		List<T> list,
		List<? extends List<? extends Collection<T>>> groups
	) {
		List<String> closurePatterns = new ArrayList<>();
		String prepared = prepare(closures, list, closurePatterns);
		
		Pattern compiledPattern = Pattern.compile(
			MessageFormat.format(
				pattern,
				closurePatterns.toArray()
			)
		);
		
		Matcher matcher = compiledPattern.matcher(prepared);
		
		List<? extends Collection<T>> result = new ArrayList<>();
		if (groups != null) {
			groups.clear();
		}
		
		while (matcher.find()) {
			result.add(list.subList(matcher.start(), matcher.end()));
			
			if (groups != null) {
				List<? extends Collection<T>> matchGroup = new ArrayList<>(matcher.groupCount()+1);
				for (int i = 0; i <= matcher.groupCount(); i++) {
					if (matcher.start(i) < 0 || matcher.end(i) < 0) {
						matchGroup.add(null);
						continue;
					}
					matchGroup.add(list.subList(matcher.start(i), matcher.end(i)));
				}

				groups.add(matchGroup);
			}
		}
		
		return result;
	}
	
	public static <T> Collection<? extends Collection<T>> findAll(
		String pattern,
		List<Closure> closures,
		List<T> list
	) {
		return findAll(pattern, closures, list, null);
	}
	
	/**
	 * Try to match an entire list with a  regular expression;
	 *
	 * Example:
	 * find("{0}.{1}+", [ { it == 2 }, { it % 2 == 0 } ], [ 1, 2, 3, 4, 5, 6 ])
	 *
	 * @param pattern
	 *   The regular expression.
	 *   Use {0}, {1}, ... to refer to the closures to match list items.
	 *   (Uses MessageFormat.format internally.)
	 *   DON'T USE PLAIN CHARACTERS! (We're not matching strings here!)
	 * @param closures
	 *   Closures to match list items (returning boolean values).
	 * @param list
	 *   The list to run the regex on
	 * @param groups
	 *   (out) accepts contents of capturing groups, may be null
	 * @return
	 */

	public static <T> boolean matches(
		String pattern,
		List<Closure> closures,
		List<T> list,
		List<? extends Collection<T>> groups
	) {
		List<String> closurePatterns = new ArrayList<>();
		String prepared = prepare(closures, list, closurePatterns);
		
		Pattern regexPattern = Pattern.compile(
			MessageFormat.format(
				pattern,
				closurePatterns.toArray()
			)
		);
		
		Matcher matcher = regexPattern.matcher(prepared);
		
		if (matcher.matches()) {
			if (groups != null) {
				groups.clear();
				for (int i = 0; i <= matcher.groupCount(); i++) {
					if (matcher.start(i) < 0 || matcher.end(i) < 0) {
						groups.add(null);
						continue;
					}
					groups.add(i, list.subList(matcher.start(i), matcher.end(i)));
				}
				
				// We can't handle named groups because Java's regex API does not
				// give us the positions of named groups.
			}
			
			return true;
		}
		
		return false;
	}
	
	public static <T> boolean matches(
		String pattern,
		List<Closure> closures,
		List<T> list
	) {
		return matches(pattern, closures, list, null);
	}
	
	private static <T> String prepare(List<Closure> closures, List<T> list, List<String> closurePatterns) {
		String[] matching = new String[list.size()];
		
		// Go through all list items and check which closures match.
		int ci = 0;
		for (Closure closure : closures) {
			char letter = letters.charAt(ci);
			
			int ti = 0;
			for (T t : list) {
				if (closure(t)) {
					if (matching[ti] == null) {
						matching[ti] = Character.toString(letter);
					}  else {
						matching[ti] += letter;
					}
				}
				
				ti++;
			}
			
			ci++;
		}
		
		// Now assign a character for each unique combination
		// of closure matches
		// Example:
		// closures 1 and 3 match => a
		// closure 1 matches      => b
		// closure 2 matches      => c
		// closure 3 matches      => d
		// ...
		Map<String,Character> map = new HashMap<>();
		int mi = 0;
		StringBuilder msb = new StringBuilder();
		for (String m : matching) {
			if (m == null) {
				msb.append(' ');
			} else if (!map.containsKey(m)) {
				if (mi >= letters.length()) {
					throw new RuntimeException("We're out of characters! Sorry, the expression is too complex for our simple approach.");
				}
			
				map.put(m, letters[mi].charAt(0));
				msb.append(letters[mi]);
				mi++;
			} else {
				msb.append(map.get(m));
			}
		}
		
		// Find character groups for the closures that match
		// when they have to in the converted input.
		// In the example above:
		// closure 1: [ab]
		// closure 2: [c]
		// closure 3: [ad]
		closurePatterns.clear();
		for (int i = 0; i < closures.size(); i++) { 
			char letter = letters.charAt(i);
			
			StringBuilder sb = new StringBuilder("[");
			
			for (Entry<String,Character> entry : map.entrySet()) { 
				if (entry.getKey().indexOf((int)letter) >= 0) {
					sb.append(entry.getValue());
				}
			}
			
			sb.append(']');
			
			if (sb.length() > 2) {
				closurePatterns.add(sb.toString());
			} else {
				closurePatterns.add("");
			}
		}
		
		return msb.toString();
	}
	
	public static void main(String[] args) {
		List<Integer> list = [ 0, 1, 2, 3, 4 ];
		List<Closure> closures = [
			{ it == 0 || it == 2 || it == 3 },
			{ it == 0 || it == 2 },
			{ it == 1 },
			{ it == 4 }
		];
		
		
		System.out.println();
		System.out.println(find("{0}.{0}", closures, list));
		System.out.println(find("{0}.{3}", closures, list));
		System.out.println(find("{1}{0}{3}", closures, list));
		System.out.println(find("{0}+{3}", closures, list));
		System.out.println(find("{2}+({0}|{3})+", closures, list));
		
		System.out.println();
		System.out.println(findAll("{0}", closures, list));
		System.out.println(findAll("{1}", closures, list));
		System.out.println(findAll("{2}", closures, list));
		System.out.println(findAll("{3}", closures, list));
		System.out.println(findAll("{0}.", closures, list));
		
		System.out.println();
		System.out.println(matches("{1}{2}{0}*{3}", closures, list));
		System.out.println(matches("{1}{2}{0}", closures, list));

		System.out.println();
		List<? extends Collection<Integer>> groups = new ArrayList<>();
		System.out.println(find("({1}{2})(({0})({0}))", closures, list, groups));
		System.out.println("  " + groups);
		
		System.out.println();
		List<List<? extends Collection<Integer>>> allGroups = new ArrayList<>();
		System.out.println(findAll("({0}).", closures, list, allGroups));
		System.out.println("  " + allGroups);
		
		System.out.println();
		System.out.println(matches("({1}{2})({0}*{3})", closures, list, groups));
		System.out.println("  " + groups);
	}
}
