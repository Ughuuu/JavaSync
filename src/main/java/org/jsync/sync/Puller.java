package org.jsync.sync;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import lombok.val;

public class Puller extends Updater {
	private final String remote;

	public Puller() throws NoFilepatternException, IOException, GitAPIException {
		this("res", "master", "https://github.com/Ughuuu/JavaSync.git");
	}

	public Puller(String localFolder, String branch, String remote)
			throws IOException, NoFilepatternException, GitAPIException {
		super(localFolder, branch);
		this.remote = remote;
	}

	@Override
	public List<DiffEntry> update() throws NoFilepatternException, GitAPIException, IOException {
		val oldHead = repository.resolve(Constants.HEAD);
		git.pull().setRemote(remote).call();
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
