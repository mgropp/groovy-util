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
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Do something after something has been idle for some time.
 * 
 * @author mgropp
 */
public class IdleManager implements Runnable {
	private final ScheduledExecutorService executor;
	
	private long idleSince = -1L;
	
	/** idle duration at last check */
	private long lastIdleCheck = -1L;
	
	private List<Pair<Long,Object>> idleTasks = [];
	private List<Pair<Long,Object>> onceTasks = [];
	private ScheduledFuture future = null;
	
	public IdleManager(ScheduledExecutorService executor) {
		this.executor = executor;
	}

	/** Execute actions if necessary */
	public synchronized void run() {
		try {
			long currentIdleTime = System.nanoTime() - idleSince;
			//logger.debug("Checking... Idle for ${(long)(currentIdleTime/1000000L)}ms.");
			
			long nextTask = -1L;
			for (Pair<Long,Object> task : idleTasks) {
				long taskIdleTime = task.first;
				Object action = task.second;
				
				if (taskIdleTime > lastIdleCheck) {
					//logger.debug("Found task to run: " + task);
					if (currentIdleTime >= taskIdleTime) {
						if (action instanceof Callable) {
							executor.submit(action);
						} else {
							executor.submit({ -> action.call() });
						}
					} else {
						if (nextTask < 0 || taskIdleTime < nextTask) {
							nextTask = taskIdleTime;
						}
					}
				}
			}
			
			lastIdleCheck = currentIdleTime;
			
			if (nextTask > 0L) {
				// shouldn't make a significant difference, but
				// you never know...
				currentIdleTime = System.nanoTime() - idleSince;
				long waitTime = nextTask - currentIdleTime;
				if (waitTime <= 0L) {
					waitTime = 0L;
				}
				//logger.debug("Currently idle for ${(long)(currentIdleTime/1000000L)}ms, next task due for ${(long)(nextTask/1000000L)}ms. Waiting for ${(long)(waitTime/1000000L)}ms...");
				future = executor.schedule(
					this,
					waitTime,
					TimeUnit.NANOSECONDS
				);
			} else {
				future = null;
			}
		}
		catch (Exception e) {
			//logger.error(e);
			System.err.println(e);
			throw e;
		} 
	}
	
	
	public void pause() {
		if (future != null) {
			future.cancel(false);
			future = null;
		}
		idleSince = -1L;
		lastIdleCheck = -1L;
	}
	
	public void start() {
		if (idleSince > 0L) {
			throw new IllegalStateException("already started");
		}
		
		long nextTask = -1L;
		for (Pair<Long,Object> task : idleTasks) {
			if (nextTask < 0L || task.first < nextTask) {
				nextTask = task.first;
			}
		}
		
		idleSince = System.nanoTime();
		
		if (nextTask >= 0L) {
			future = executor.schedule(
				this,
				nextTask,
				TimeUnit.NANOSECONDS
			);
		}
	}
	
	public boolean isActive() {
		return (idleSince > 0L);
	}
	
	/**
	 * Call this method to indicate activity (= not idle).
	 */
	public void ping() {
		if (!isActive()) {
			return;
		}
		
		if (future != null) {
			future.cancel(false);
			future = null;
		}
		idleSince = -1L;
		lastIdleCheck = -1L;
		
		// remove all once tasks
		for (Pair<Long,Object> task : onceTasks) {
			idleTasks.remove(task);
		}
		onceTasks.clear();
		
		start();
	}
	
	public void addOnce(long idleTimeMillis, Object action, boolean startCountingNow=false) {
		doAdd(idleTimeMillis, action, true, startCountingNow);
	}
	
	public void add(long idleTimeMillis, Object action) {
		doAdd(idleTimeMillis, action, false, false);
	}
	
	/**
	 * Add a new idle task.
	 * 
	 * @param idleTimeMillis
	 *   run action after idleTimeMilllis milliseconds of inactivity
	 *   (i.e. no ping calls)
	 * @param action
	 *   the action to be run after the timeout
	 * @param once
	 *   keep the task only till the next ping
	 *   ("if the user doesn't say anything for $time, do $action")
	 * @param startCountingNow
	 *   If the IdleManager is already running, interpret the task idle time
	 *   as starting now and not at start()/the last ping().
	 *   Currently Only valid for once=true (since we're changing the task idle time).
	 */
	private synchronized void doAdd(long idleTimeMillis, Object action, boolean once, boolean startCountingNow) {
		if (startCountingNow && !once) {
			throw new IllegalArgumentException("You cannot set startCountingNow=true and once=false.");
		}
		
		long taskIdleTime = 1000000L* idleTimeMillis;
		
		// already started?
		if (idleSince < 0L) {
			Pair<Long,Object> task = new Pair<>(taskIdleTime, action);
			//logger.debug("Adding task: ${task} (executor: ${executor})");
			idleTasks.add(task);
			if (once) {
				onceTasks.add(task);
			}
		
		} else {
			long currentIdleTime = System.nanoTime() - idleSince;
			long timeUntilTask;
			if (startCountingNow) {
				timeUntilTask = taskIdleTime;
				taskIdleTime += currentIdleTime;
			} else {
				timeUntilTask = taskIdleTime - currentIdleTime
			}
			
			Pair<Long,Object> task = new Pair<>(taskIdleTime, action);
			//logger.debug(String.format(
			//	"Adding task to running idle manager (%sadjusting start time; executor: %s): %s",
			//	startCountingNow ? "" : "not ",
			//	executor,
			//	task
			//));
			idleTasks.add(task);
			if (once) {
				onceTasks.add(task);
			}
			
			// could the task still be executed in the current cycle?
			if (timeUntilTask >= 0) {
				if (future == null) {
					// there is no other task waiting
					// => schedule!
					future = executor.schedule(
						this,
						timeUntilTask,
						TimeUnit.NANOSECONDS
					);
				} else {
					// there is another task
					long nextTaskIdleTime = currentIdleTime + future.getDelay(TimeUnit.NANOSECONDS);
					if (nextTaskIdleTime > taskIdleTime) {
						// the new task should be next!
						future.cancel(false);
						future = executor.schedule(
							this,
							timeUntilTask,
							TimeUnit.NANOSECONDS
						);
					}
				}
			}
		}
	}
	
	public void clear() {
		pause();
		idleTasks.clear();
	}
}
