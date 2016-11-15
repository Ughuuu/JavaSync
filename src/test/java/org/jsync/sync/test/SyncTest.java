package org.jsync.sync.test;
import org.junit.Test;

import lombok.val;

import static org.junit.Assert.*;

import org.jsync.sync.Sync;
import org.jsync.sync.Updater;

/**
 * Tests the functionality of Sync class.
 * @author Dragos
 *
 */
public class SyncTest {

    @Test 
    public void testSimpleLoad() throws Exception {
		String className = "org.jsync.sync.test.Syncee";
		val loadClass = new Sync<Object>(className);
    	
    	assertNotNull("The class has not been loaded", loadClass.get());
    	    	
		assertSame("this was false", loadClass.get().getClass().getMethod("getResult").invoke(loadClass.get()));
    	
    	assertSame(loadClass.get().getClass().getName(), className);
    }
}
