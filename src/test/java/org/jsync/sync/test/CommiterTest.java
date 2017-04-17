package org.jsync.sync.test;

import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.FileWriter;

import org.jsync.sync.Commiter;
import org.junit.Test;

import lombok.val;

public class CommiterTest {

	private static final String localFolder = "test";
	private static final String localBranch = "test";

	@Test
	public void testWorkspace() throws Exception {
		// first delete the folder if it already exists
		assertSame(true, deleteDirectory(new File(localFolder)));
		// check if initial revision is 0
		val updater = new Commiter(localFolder, localBranch);
		assertSame(0, updater.getRevision());
		assertSame(0, updater.getFiles().size());

		// make a null update
		updater.update();
		assertSame(0, updater.getRevision());
		assertSame(0, updater.getFiles().size());

		// make update with 2 files
		val file1 = new File(localFolder, "README2.MD");
		val file2 = new File(localFolder, "README1.MD");
		file1.createNewFile();
		file2.createNewFile();
		val update = updater.update();
		assertSame(2, updater.getFiles().size());
		assertSame(1, updater.getRevision());
		assertSame(2, update.size());
		assertSame(true, update.stream().anyMatch(t -> t.getNewPath().contains("README1.MD")));
		assertSame(true, update.stream().anyMatch(t -> t.getNewPath().contains("README2.MD")));

		// make null update
		val update2 = updater.update();
		assertSame(1, updater.getRevision());
		assertSame(0, update2.size());

		// make update by deleting a file
		file1.delete();
		val update3 = updater.update();
		assertSame(2, updater.getRevision());
		assertSame(1, update3.size());
		assertSame(true, update3.stream().anyMatch(t -> t.getOldPath().contains("README2.MD")));

		// make null update
		val update4 = updater.update();
		assertSame(2, updater.getRevision());
		assertSame(0, update4.size());

		// make update by renaming a file
		file2.renameTo(new File(localFolder, "README_RENAME.MD"));
		val update5 = updater.update();
		assertSame(3, updater.getRevision());
		assertSame(1, update5.size());
		assertSame(true, update5.stream().anyMatch(t -> t.getNewPath().contains("README_RENAME.MD")));

		// make null update
		val update7 = updater.update();
		assertSame(3, updater.getRevision());
		assertSame(0, update7.size());

		// make update by writing
		val writer = new FileWriter(new File(localFolder, "README_RENAME.MD"));
		writer.write("hello World");
		writer.close();
		val update8 = updater.update();
		assertSame(1, update8.size());
		assertSame(4, updater.getRevision());
		assertSame(true, update8.stream().anyMatch(t -> t.getNewPath().contains("README_RENAME.MD")));

		// make null update
		val update9 = updater.update();
		assertSame(4, updater.getRevision());
		assertSame(0, update9.size());
		assertSame(0, update9.size());
		assertSame(1, updater.getFiles().size());
		assertSame(true, updater.getFiles().stream().anyMatch(t -> t.contains("README_RENAME.MD")));
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
