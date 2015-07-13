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

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

@TypeChecked
public class ThreadUtils {
	public static final ExecutorService singleThreadExecutor;
	static {
		singleThreadExecutor = Executors.newSingleThreadExecutor();
		singleThreadExecutor.submit({
			Thread.currentThread().setName("lsv-thread-utils");
		} as Callable);
	}
	
	public static <T> T invokeAndWait(Callable<T> task) throws ExecutionException {
		Future<T> future = singleThreadExecutor.submit(task);
		while (true) {
			try {
				return future.get();
			}
			catch (InterruptedException e) {
				continue;
			}
		}
	}
	
	/**
	 * Behaves like SwingUtilities.invokeLater, but
	 * allows Exceptions (which are printed to stderr
	 * by default) and runs on a different thread.
	 *
	 * @param task
	 * @param exceptionHandler
	 * @return
	 */
	public static <T> Future<T> invokeLater(final Callable<T> task, Closure exceptionHandler=null) {
		return singleThreadExecutor.submit(new Callable() {
			@Override
			def call() {
				try {
					return task.call();
				}
				catch (Exception e) {
					if (exceptionHandler != null) {
						return exceptionHandler(e);
					}
					
					System.err.println("Exception in Callable submitted via ThreadUtils.invokeLater!");
					e.printStackTrace();
					return null;
				}
			}
		});
	}
	
	public static <T> Future<T> invokeLater(Closure task, Closure exceptionHandler=null) {
		return singleThreadExecutor.submit(new Callable() {
			@Override
			def call() {
				try {
					return task();
				}
				catch (Exception e) {
					if (exceptionHandler != null) {
						return exceptionHandler(e);
					}
					
					System.err.println("Exception in Callable submitted via ThreadUtils.invokeLater!");
					e.printStackTrace();
					return null;
				}
			}
		});
	}
}
