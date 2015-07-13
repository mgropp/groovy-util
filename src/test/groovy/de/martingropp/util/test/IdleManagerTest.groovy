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

package de.martingropp.util.test;

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

import org.junit.Assert
import org.junit.Test

import de.martingropp.util.IdleManager;

public class IdleManagerTest {
	@Test
	public void testIdleNotRun() {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		IdleManager idleManager = new IdleManager(executor);
		
		boolean idleActionExecuted = false;
		idleManager.add(1000, { idleActionExecuted = true });
		idleManager.start();
		
		for (int i = 0; i < 10; i++) {
			Thread.sleep(500);
			idleManager.ping();
		}
		
		Assert.assertFalse(idleActionExecuted);
		
		executor.shutdownNow();
	}
	
	@Test
	public void testIdleRunDirectly() {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		IdleManager idleManager = new IdleManager(executor);
		
		boolean idleActionExecuted = false;
		idleManager.add(1000, { idleActionExecuted = true });
		idleManager.start();
		
		Thread.sleep(1500);
		
		Assert.assertTrue(idleActionExecuted);
		
		executor.shutdownNow();
	}
	
	@Test
	public void testIdleRunAfterReset() {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		IdleManager idleManager = new IdleManager(executor);
		
		boolean idleActionExecuted = false;
		idleManager.add(1000, { idleActionExecuted = true });
		idleManager.start();
		
		for (int i = 0; i < 10; i++) {
			Thread.sleep(500);
			idleManager.ping();
		}
		
		Thread.sleep(1500);
		
		Assert.assertTrue(idleActionExecuted);
		
		executor.shutdownNow();
	}
	
	@Test
	public void testIdleRunOnlyOnce() {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		IdleManager idleManager = new IdleManager(executor);
		
		int idleActionExecuted = 0;
		idleManager.add(500, { idleActionExecuted++ });
		idleManager.start();
		
		Thread.sleep(3000);
		
		Assert.assertTrue(idleActionExecuted == 1);
		
		executor.shutdownNow();
	}
	
	@Test
	public void testIdleRunTwo() {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		IdleManager idleManager = new IdleManager(executor);
		
		int firstIdleActionExecuted = 0;
		int secondIdleActionExecuted = 0;
		long firstTime = -1L;
		long secondTime = -1L;
		idleManager.add(500, { firstTime = System.nanoTime(); firstIdleActionExecuted++ });
		idleManager.add(1000, { secondTime = System.nanoTime(); secondIdleActionExecuted++ });
		
		long startTime = System.nanoTime();
		idleManager.start();
		
		Thread.sleep(2000);
		
		Assert.assertTrue(firstIdleActionExecuted == 1);
		Assert.assertTrue(secondIdleActionExecuted == 1);
		
		Assert.assertTrue(firstTime - startTime > 500000000L);
		Assert.assertTrue(secondTime - startTime > 1000000000L);
		
		Assert.assertTrue(firstTime - startTime < 700000000L);
		Assert.assertTrue(secondTime - startTime < 1200000000L);
		
		executor.shutdownNow();
	}

	@Test
	public void testIdleOnce() {
		for (once in [ true, false ]) {
			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
			IdleManager idleManager = new IdleManager(executor);
			
			int idleActionExecuted = 0;
			if (once) {
				idleManager.addOnce(300, { idleActionExecuted++; }, false);
			} else {
				idleManager.add(300, { idleActionExecuted++; });
			}
			idleManager.start();
			
			Thread.sleep(1000);
			idleManager.ping();
			
			Thread.sleep(1000);
			
			Assert.assertTrue(idleActionExecuted == 1 + (once ? 0 : 1));
			
			executor.shutdownNow();
		}
	}
		
	@Test
	public void testIdleOnceNotRun() {
		for (once in [ true, false ]) {
			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
			IdleManager idleManager = new IdleManager(executor);
			
			boolean idleActionExecuted = false;
			if (once) {
				idleManager.addOnce(1000, { idleActionExecuted = true });
			} else {
				idleManager.add(1000, { idleActionExecuted = true });
			}
			idleManager.start();
			idleManager.ping();
			
			Thread.sleep(1500);
			
			Assert.assertTrue(idleActionExecuted != once);
			
			executor.shutdownNow();
		}
	}
	
	@Test
	public void testAddToActiveFirst() {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		IdleManager idleManager = new IdleManager(executor);
		
		long a = -1;
		long b = -1;
		idleManager.add(1500, { if (a >= 0) { b = System.nanoTime() } });
		long start = System.nanoTime();
		idleManager.start();
		idleManager.add(800, { a = System.nanoTime() });
		idleManager.ping();
		
		Thread.sleep(2000);
		
		Assert.assertTrue(a >= 0);
		Assert.assertTrue(b >= 0);
		
		Assert.assertTrue(a - start >= 800000000L);
		Assert.assertTrue(b - start >= 1500000000L);
		
		Assert.assertTrue(a - start < 1200000000L);
		Assert.assertTrue(b - start < 2000000000L);
		
		executor.shutdownNow();
	}
	
	@Test
	public void testAddToActiveLast() {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		IdleManager idleManager = new IdleManager(executor);
		
		long a = -1;
		long b = -1;
		idleManager.add(800, { a = System.nanoTime() });
		long start = System.nanoTime();
		idleManager.start();
		idleManager.add(1500, { if (a >= 0) { b = System.nanoTime() } });
		idleManager.ping();
		
		Thread.sleep(2000);
		
		Assert.assertTrue(a >= 0);
		Assert.assertTrue(b >= 0);
		
		Assert.assertTrue(a - start >= 800000000L);
		Assert.assertTrue(b - start >= 1500000000L);
		
		Assert.assertTrue(a - start < 1200000000L);
		Assert.assertTrue(b - start < 2000000000L);
		
		executor.shutdownNow();
	}
}
