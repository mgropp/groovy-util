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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Run a process, receive its output through listeners,
 * wait for special output lines.
 */
public class ProcessWatcher {
	public static interface OutputListener {
		void processOutputLine(String line, boolean stderr);
	}
	
	public static interface ExitListener {
		void processTerminated(int exitCode);
	}
	
	//private final String[] command;
	private final ProcessBuilder processBuilder;
	public Process process = null;
	private Writer writer = null;
	
	private List<OutputListener> stdoutListeners = new ArrayList<>();
	private List<OutputListener> stderrListeners = new ArrayList<>();
	private List<ExitListener> exitListeners = new ArrayList<>();
	
	private WatcherThread stdoutWatcher;
	private WatcherThread stderrWatcher;
	
	private static class TriggerLine {
		public Pattern pattern;
		public Semaphore semaphore;
		
		// when the semaphore is released:
		// matched is true if the pattern matched and false if
		// the process exited.
		// -> no synchronization necessary.
		public boolean matched = false;
		
		public TriggerLine(Pattern pattern, Semaphore semaphore) {
			this.pattern = pattern;
			this.semaphore = semaphore;
		}
	}
	
	private List<TriggerLine> triggerLinesStdout = new LinkedList<>();
	private List<TriggerLine> triggerLinesStderr = new LinkedList<>();
	
	private class WatcherThread extends Thread {
		private final BufferedReader reader;
		private final List<OutputListener> listeners;
		private final boolean stderr; 
		private final boolean callExitListeners;
		private final List<TriggerLine> triggerLines;
		
		public WatcherThread(
			BufferedReader reader,
			List<OutputListener> listeners,
			final List<TriggerLine> triggerLines,
			boolean stderr,
			boolean callExitListeners
		) {
			this.reader = reader;
			this.listeners = listeners;
			this.triggerLines = triggerLines;
			this.stderr = stderr;
			this.callExitListeners = callExitListeners;
			
			OutputListener listener = new OutputListener() {
				@Override
				public void processOutputLine(String line, boolean stderr) {
					List<Semaphore> semaphores = new ArrayList<>();
					synchronized (triggerLines) {
						for (TriggerLine triggerLine : triggerLines) {
							if (triggerLine.pattern.matcher(line).matches()) {
								triggerLine.matched = true;
								semaphores.add(triggerLine.semaphore);
							}
						}
					}
					
					for (Semaphore semaphore : semaphores) {
						semaphore.release();
					}
				}
			};
			
			if (stderr) {
				addStderrListener(listener);
			} else {
				addStdoutListener(listener);
			}
		}
		
		@Override
		public void run() {
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					for (OutputListener listener : listeners) {
						listener.processOutputLine(line, stderr);
					}
				}
				
				try {
					process.waitFor();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				finally {
					List<Semaphore> semaphores = new ArrayList<>();
					synchronized (triggerLines) {
						for (TriggerLine triggerLine: triggerLines) {
							if (triggerLine.pattern.matcher(line).matches()) {
								triggerLine.matched = false;
							}
						}
					}
					
					for (Semaphore semaphore : semaphores) {
						semaphore.release();
					}
					
					int exitCode = process.exitValue();
					if (callExitListeners) {
						for (ExitListener listener : exitListeners) {
							listener.processTerminated(exitCode);
						}
					}
				}
			}
			catch (IOException e) {
			}
		}
	}
	
	public ProcessWatcher(String... command) {
		//this.command = command;
		processBuilder = new ProcessBuilder(command);
	}
	
	public void start() throws IOException {
		process = processBuilder.start();
		
		stdoutWatcher = new WatcherThread(
			new BufferedReader(new InputStreamReader(process.getInputStream())),
			stdoutListeners,
			triggerLinesStdout,
			false,
			false
		);
		stdoutWatcher.start();
		
		stderrWatcher = new WatcherThread(
			new BufferedReader(new InputStreamReader(process.getErrorStream())),
			stderrListeners,
			triggerLinesStderr,
			true,
			true
		);
		stderrWatcher.start();
		
		writer = new OutputStreamWriter(process.getOutputStream());
		
		final Thread shutdownHook = new Thread() {
			@Override
			public void run() {
				//System.err.println("Destroying process: " + process + " (" + Arrays.asList(command) + ")");
				try {
					writer.close();
				}
				catch (IOException e) {
				}
				process.destroy();
				do {
					try {
						//System.err.println(process + ": " + process.waitFor());
						process.waitFor();
					}
					catch (InterruptedException e) {
						continue;
					}
				} while (false);
			}
		};
		
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		
		addExitListener(
			new ExitListener() {
				@Override
				public void processTerminated(int exitCode) {
					try {
						Runtime.getRuntime().removeShutdownHook(shutdownHook);
					}
					catch (IllegalStateException e) {
						// ignore ("Shutdown in progress")
					}
				}
			}
		);
	}
	
	public void addStdoutListener(OutputListener listener) {
		stdoutListeners.add(listener);
	}
	
	public void addStderrListener(OutputListener listener) {
		stderrListeners.add(listener);
	}
	
	public void addExitListener(ExitListener listener) {
		exitListeners.add(listener);
	}
	
	public void addUniversalListener(OutputListener listener) {
		stdoutListeners.add(listener);
		stderrListeners.add(listener);
	}
	
	public void send(String text) throws IOException {
		writer.write(text);
		writer.flush();
	}
	
	public void sendLine(String text) throws IOException {
		send(text + '\n');
	}
	
	public void terminate() {
		if (process == null) {
			throw new IllegalStateException("Process not running!");
		}
		process.destroy();
	}
	
	public int closeStdinAndWaitForProcess() throws InterruptedException {
		final Semaphore semaphore = new Semaphore(0);
		addExitListener(new ExitListener() {
			@Override
			public void processTerminated(int exitCode) {
				semaphore.release();
			}			
		});
		
		try {
			writer.close();
		}
		catch (IOException e) {
			// throw new RuntimeException(e);
		}

		return process.waitFor();
	}
	
	public void closeStdinAndTerminate(long waitTime, TimeUnit timeUnit) {
		final Semaphore semaphore = new Semaphore(0);
		addExitListener(new ExitListener() {
			@Override
			public void processTerminated(int exitCode) {
				semaphore.release();
			}			
		});
		
		try {
			writer.close();
		}
		catch (IOException e) {
			// throw new RuntimeException(e);
		}

		try {
			if (!semaphore.tryAcquire(waitTime, timeUnit)) {
				process.destroy();
			}
		}
		catch (InterruptedException e) {
			// shorter wait time
			process.destroy();
		}
	}
	
	private void waitForLine(Pattern pattern, List<TriggerLine> triggerLines) throws InterruptedException {
		Semaphore semaphore = new Semaphore(0);
		TriggerLine triggerLine = new TriggerLine(pattern, semaphore);
		synchronized (triggerLines) {
			if (!isActive()) {
				throw new InterruptedException("No match, process exited.");
			}
			
			triggerLines.add(triggerLine);
		}
		
		try {
			semaphore.acquire();
		}
		finally {
			triggerLines.remove(triggerLine);
		}
		
		if (!triggerLine.matched) {
			throw new InterruptedException("No match, process exited.");
		}
	}
	
	public void waitForLineStdout(Pattern pattern) throws InterruptedException {
		waitForLine(pattern, triggerLinesStdout);
	}
	
	public void waitForLineStderr(Pattern pattern) throws InterruptedException {
		waitForLine(pattern, triggerLinesStderr);
	}
	
	public void waitForLineStdout(String line) throws InterruptedException {
		waitForLine(
			Pattern.compile(String.format("^%s$", Pattern.quote(line))),
			triggerLinesStdout
		);
	}
	
	public void waitForLineStderr(String line) throws InterruptedException {
		waitForLine(
			Pattern.compile(String.format("^%s$", Pattern.quote(line))),
			triggerLinesStderr
		);
	}
	
	/**
	 * Call waitFor on the Process object.
	 * 
	 * @return the exit code of the process
	 * @throws InterruptedException
	 */
	public int waitForProcess() throws InterruptedException {
		return process.waitFor();
	}
	
	/**
	 * Wait for the watcher threads.
	 *  
	 * @throws InterruptedException
	 */
	public void join() throws InterruptedException {
		if (stdoutWatcher != null) {
			stdoutWatcher.join();
		}
		
		if (stderrWatcher != null) {
			stderrWatcher.join();
		}
	}
	
	/**
	 * Check if the process is still running.
	 * 
	 * @return true iff the process is still active
	 */
	public boolean isActive() {
		// There just has to be a better way?!
		if (process == null) {
			return false;
		}
		
		try {
			process.exitValue();
			return false;
		}
		catch (IllegalStateException e) {
			return true;
		}
		catch (IllegalThreadStateException e) {
			return true;
		}
	}
	
	public static String removeEscapeSequences(String text) {
		return text.replaceAll("(\u001b\\[|\u009b)[^@-_a-z]*[@-_a-z]|\u001b[@-_]", "");
	}
	
	/**
	 * Return a command array to run a program that
	 * needs a terminal.
	 * The returned command will also take care of
	 * cleaning up any child processes left by
	 * <tt>command</tt>.
	 */
	public static String[] withPty(String command) {
		boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
		if (windows) {
			return new String[]{ command };
		}
		
		return new String[]{
			"/usr/bin/setsid",
			"/bin/bash", "-c",
			String.format(
				"trap \"/usr/bin/pkill -s 0\" EXIT\n" +
				"/usr/bin/script -q -c \"%s\" /dev/null",
				command
			)
		};
	}
	
	public static void main(String[] args) throws IOException {
		ProcessWatcher p = new ProcessWatcher("/bin/bash", "-i");
		
		p.addStdoutListener(new OutputListener() {
			@Override
			public void processOutputLine(String line, boolean stderr) {
				System.out.println(line);
			}
		});
		p.addStderrListener(new OutputListener() {
			@Override
			public void processOutputLine(String line, boolean stderr) {
				System.err.println(line);
			}
		});
		p.addExitListener(new ExitListener() {
			@Override
			public void processTerminated(int exitCode) {
				System.err.println();
				System.err.println("Process exited with code " + exitCode + ".");
				System.exit(0);
			}
		});
		p.start();
		
		try (Scanner scanner = new Scanner(System.in)) {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				System.err.println("Read line: »" + line + "«");
				p.sendLine(line);
			}
		}
	}
}
