package org.jsync.sync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestJGit {
	private String remotePath, localPath;
	Git git;

	@Before
	public void init() throws Exception {
		// prepare a new folder
		remotePath = "https://github.com/github/testrepo.git";
		localPath = "gitrepo";
	}

	@Test
	public void testClone() throws IOException, GitAPIException {
		Git.cloneRepository().setURI(remotePath).setDirectory(new File(localPath)).call();
	}

	@Test
	public void testAddCommitPush() throws IOException, GitAPIException {
		Git git = Git.init()
				.setDirectory(new File(localPath))
				.call();

		File myfile = new File(localPath + "/myfile");
		myfile.createNewFile();
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Added myfile").call();
		git.push().call();
	}

	@Test
	public void testTrackMaster() throws IOException, JGitInternalException, GitAPIException {
		Git git = Git.init()
				.setDirectory(new File(localPath))
				.call();
		
		git.branchCreate().setName("master").setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
				.setStartPoint("origin/master").setForce(true).call();
	}

	@Test
	public void testPull() throws IOException, GitAPIException {
		Git git = Git.init()
				.setDirectory(new File(localPath))
				.call();
		git.pull().call();
	}

	static public boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}
}