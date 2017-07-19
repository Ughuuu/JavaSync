package org.jsync.sync;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import lombok.val;

public class Puller extends Updater {
	private final String remote;

	public Puller() throws NoFilepatternException, IOException, GitAPIException {
		this("assets", "master", "http://github.com/Ughuuu/JavaSync.git");
	}

	public Puller(String localFolder, String branch, String remote)
			throws IOException, NoFilepatternException, GitAPIException {
		// super(localFolder, branch);
		this.remote = remote;
		val head = repository.resolve(Constants.HEAD);
		try {
			Git result = Git.cloneRepository().setURI(remote).setDirectory(new File(localFolder)).call();
			result.close();
		} catch (Exception e) {
		}
	}

	@Override
	public List<DiffEntry> update() throws NoFilepatternException, GitAPIException, IOException {
		StoredConfig config = git.getRepository().getConfig();
		config.setString("remote", "origin", "url", remote);
		// config.setString("remote", "origin", "fetch",
		// "+refs/heads/*:refs/remotes/*");
		config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/*");
		config.save();

		val oldHead = repository.resolve(Constants.HEAD);
		try {
			git.pull().setRemote("origin").setRemoteBranchName("master").setRebase(false)
					.setStrategy(MergeStrategy.RECURSIVE).call();
			/*
			 * FetchResult fetchResult = git.fetch().setRemote("origin").call();
			 * //TrackingRefUpdate refUpdate =
			 * fetchResult.getTrackingRefUpdate("refs/heads/master"); //Result result =
			 * refUpdate.getResult();
			 */
			Status status = git.status().call();
			if (!status.isClean()) {
				git.merge().include(repository.getRef("master")).setFastForward(FastForwardMode.FF_ONLY)
						.setStrategy(MergeStrategy.RESOLVE).setCommit(true).call();
				// return;
			}
		} catch (Exception e) {
			e.printStackTrace();
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
