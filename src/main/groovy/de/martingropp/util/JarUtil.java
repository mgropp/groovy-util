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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Copy resources from the classpath (e.g. a jar file) to a
 * normal file system if necessary.
 * 
 * @author mgropp
 */
public class JarUtil {
	private static File createTempDir() throws IOException {
		final Path path = Files.createTempDirectory(JarUtil.class.getSimpleName());
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					Files.walkFileTree(
						path,
						new SimpleFileVisitor<Path>() {
							@Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
								if(attributes.isRegularFile()){
									Files.delete(file);
								}
								
								return FileVisitResult.CONTINUE;
							}
							
							@Override
							public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
								Files.delete(dir);
								return FileVisitResult.CONTINUE;
							}
						}
					);
				}
				catch (IOException e) {
				}
			}
		});
		
		return path.toFile();
	}
	
	public static List<File> ensureUrisAreFiles(List<URI> uris, File targetDir) throws IOException {
		List<File> files = new ArrayList<>();
		
		for (URI uri : uris) {
			if ("file".equals(uri.getScheme())) {
				files.add(new File(uri.getPath()));
				continue;
			}
			
			if (targetDir == null) {
				targetDir = createTempDir();
			}
			
			files.add(ensureUriIsFile(uri, targetDir));
		}
		
		return files;
	}
	
	public static List<File> ensureUrisAreFiles(List<URI> uris) throws IOException {
		return ensureUrisAreFiles(uris, null);
	}
	
	public static File ensureUriIsFile(URI uri) throws IOException {
		if ("file".equals(uri.getScheme())) {
			return new File(uri.getPath());
		}
		
		return ensureUriIsFile(uri, createTempDir());
	}
	
	private static String extractResourcePath(String path) {
		int index = path.lastIndexOf('!');
		if (index >= 0 && index < path.length()-1) {
			path = path.substring(index + 1);
		}
		
		return path;
	}
	
	public static File ensureUriIsFile(URI uri, File targetDir) throws IOException {
		if ("file".equals(uri.getScheme())) {
			return new File(uri.getPath());
		}
		
		File file = new File(targetDir, extractResourcePath(uri.getSchemeSpecificPart()));
		file.getParentFile().mkdirs();
				
		//logger.info("Copying " + lexUri + "to file " + file);
		
		InputStream stream = uri.toURL().openStream();
		try {
			Files.copy(
				stream,
				file.toPath(),
				StandardCopyOption.REPLACE_EXISTING
			);
		}
		finally {
			stream.close();
		}
		
		return file;
	}
}
