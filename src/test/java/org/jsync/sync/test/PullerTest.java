package org.jsync.sync.test;

import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.FileWriter;

import org.jsync.sync.Commiter;
import org.jsync.sync.Puller;
import org.junit.Test;

import lombok.val;

public class PullerTest {

	private static final String localFolder = "test";
	private static final String localBranch = "test";
	private static final String remote = "https://github.com/Ughuuu/JavaSync.git";

	@Test
	public void testWorkspace() throws Exception {
		// first delete the folder if it already exists
		assertSame(true, deleteDirectory(new File(localFolder)));
		// check if initial revision is 0
		val updater = new Puller(localFolder, localBranch, remote);
		assertSame(0, updater.getRevision());

		updater.update();
		assertSame(1, updater.getRevision());
	}

	public static boolean deleteDirectory(File directory) {
		if (directory.exists()) {
			File[] files = directory.listFiles();
			if (null != files) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						deleteDirectory(files[i]);
					} else {
						files[i].delete();
					}
				}
			}
		} else {
			return true;
		}
		return (directory.delete());
	}
}
