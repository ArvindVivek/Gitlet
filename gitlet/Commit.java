package gitlet;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Commit implements Serializable {
    /** Hash value of this commit. */
    private String hash;
    /** Hash value of parent 1 commit. */
    private String parentHash;
    /** Hash value of parent 2 commit if merge commit. */
    private String parentHashMerge;
    /** If this is a merge commit. */
    private boolean mergeCommit;
    /** Commit message from user. */
    private String commitMessage;
    /** Time stamp of this commit when created. */
    private String timeStamp;
    /** Values of files tracked and their 'blob' value or file
     * contents at time of commit. */
    private TreeMap<String, String> blobs;

    public Commit(String pHash, String cMessage, TreeMap<String,
            String> blobSet) {
        this.parentHash = pHash;
        this.commitMessage = cMessage;
        this.blobs = blobSet;
        Date date = new Date();
        this.timeStamp =
                new SimpleDateFormat("E MMM d HH:mm:ss YYYY Z")
                        .format(date);
        this.hash = Utils.sha1(Utils.serialize(this));
        this.mergeCommit = false;
        this.parentHashMerge = null;
    }

    public void setMergeCommit(String pHashMerge) {
        this.parentHashMerge = pHashMerge;
        this.mergeCommit = true;
    }

    public boolean getMergeCommit() {
        return this.mergeCommit;
    }

    public String getHash() {
        return Utils.sha1(Utils.serialize(this));
    }

    public String getParentHash() {
        return this.parentHash;
    }

    public String getParentHashMerge() {
        return this.parentHashMerge;
    }

    public String getCommitMessage() {
        return this.commitMessage;
    }

    public String getTimeStamp() {
        return this.timeStamp;
    }

    public TreeMap<String, String> getBlobs() {
        return this.blobs;
    }

    public String getLogFormat() {
        String headerLine;
        String commitLine;
        String dateLine;
        String messageLine;
        if (mergeCommit) {
            headerLine = "===\n";
            commitLine = "commit " + getHash() + "\n"
                    + "Merge: " + getParentHash().substring(0, 7)
                    + " " + getParentHashMerge().substring(0, 7) + "\n";
            dateLine = "Date: " + getTimeStamp() + "\n";
            messageLine = getCommitMessage() + "\n";
        } else {
            headerLine = "===\n";
            commitLine = "commit " + getHash() + "\n";
            dateLine = "Date: " + getTimeStamp() + "\n";
            messageLine = getCommitMessage() + "\n";
        }
        return headerLine + commitLine + dateLine + messageLine;
    }
}
