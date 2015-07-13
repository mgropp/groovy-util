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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

public abstract class PreprocessingReader extends Reader {
	protected final LinkedList<BufferedReader> readers = new LinkedList<>();
	protected final LinkedList<String> buffer = new LinkedList<>(); 
	
	protected int line = 0;
	
	public PreprocessingReader() {
	}
	
	public PreprocessingReader(BufferedReader reader) {
		this.readers.push(reader);
	}
	
	/**
	 * @param line
	 * @param buffer
	 * @return
	 *   false iff read should return 0
	 * @throws IOException
	 */
	protected abstract boolean preprocess(String line, LinkedList<String> buffer) throws IOException;
	
	@Override
	public void close() throws IOException {
		for (BufferedReader r : readers) {
			r.close();
		}
		readers.clear();
		buffer.clear();
	}
	
	protected void popReader() throws IOException {
		readers.pop().close();
	}
	
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		while (buffer.isEmpty() && !readers.isEmpty()) {
			BufferedReader reader = readers.getFirst();
			String line = reader.readLine();
			if (line == null) {
				popReader();
				if (readers.isEmpty()) {
					return -1;
				}
				continue;
			}
			
			line = line + '\n';
			
			if (!preprocess(line, buffer)) {
				return 0;
			}
		}
		
		String bufferedString = buffer.pop();
		char[] bufferedArray = bufferedString.toCharArray();
		
		if (bufferedString.length() <= len) {
			System.arraycopy(
				bufferedArray,
				0,
				cbuf,
				off,
				bufferedArray.length
			);
			
			line++;
			return bufferedArray.length;
			
		} else {
			System.arraycopy(
				bufferedArray,
				0,
				cbuf,
				off,
				len
			);
			
			buffer.push(bufferedString.substring(len));
			return len;
		}
	}
	
	public int getLine() {
		return line;
	}
}
