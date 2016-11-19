package org.jsync.sync;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import lombok.val;

public class Commiter extends Updater {

	public Commiter() throws NoFilepatternException, IOException, GitAPIException {
		super();
	}

	public Commiter(String localFolder, String branch) throws IOException, NoFilepatternException, GitAPIException {
		super(localFolder, branch);
	}

	public List<DiffEntry> update() throws NoFilepatternException, GitAPIException, IOException {
		ObjectId oldHead = repository.resolve(Constants.HEAD);
		// if we are at initial commit, add a README.MD file
		if (oldHead == null) {
			File file = new File(repository.getDirectory().getParent(), "README.MD");
			file.createNewFile();
			initialCommit();
			try {
				git.checkout().setForce(true).setName(branch).call();
			} catch (Exception e) {
				git.checkout().setCreateBranch(true).setForce(true).setName(branch).call();
			}
			oldHead = repository.resolve(Constants.HEAD);
		}

		val status = git.status().call();
		if (!status.isClean()) {
			git.add().addFilepattern(".").call();
			git.add().setUpdate(true).addFilepattern(".").call();
			git.commit().setMessage(Integer.toString(getRevision() + 1)).call();
		}

		val newHead = repository.resolve(Constants.HEAD);
		val rw = new RevWalk(repository);
		val df = new DiffFormatter(DisabledOutputStream.INSTANCE);
		df.setRepository(repository);
		df.setDiffComparator(RawTextComparator.DEFAULT);
		df.setDetectRenames(true);
		val returnList = df.scan(rw.parseCommit(oldHead).getTree(), rw.parseCommit(newHead).getTree());
		rw.close();
		df.close();
		return returnList;
	}
}
