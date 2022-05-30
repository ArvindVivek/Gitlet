package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

public class GitletRepo implements Serializable {
    /** Current working directory of repo. */
    static final File CWD = new File(".");
    /** .gitlet folder in directory. */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");
    /** Blobs folder in .gitlet folder. */
    static final File BLOBS_FOLDER = Utils.join(GITLET_FOLDER, "blobs");
    /** Commits folder in .gitlet folder. */
    static final File COMMIT_FOLDER = Utils.join(GITLET_FOLDER, "commits");
    /** Serialized file of this class, containing all information. */
    static final File GITLET_REPO_FILE
            = Utils.join(GITLET_FOLDER, "gitletRepo");
    /** Branches with branchname and commit hash. */
    private TreeMap<String, String> branches;
    /** Current head of repo. */
    private String head = "master";
    /** Check if init was called. */
    private boolean initialized = false;
    /** Staging area of files to add and remove. */
    private TreeMap<String, String> stagingArea;
    /** Map of tree mappings to remote name and directory. */
    private TreeMap<String, String> remotes;

    public GitletRepo() {
        stagingArea = new TreeMap<>();
        branches = new TreeMap<>();
        remotes = new TreeMap<>();
        if (GITLET_REPO_FILE.exists()) {
            this.head = Utils
                    .readObject(GITLET_REPO_FILE, GitletRepo.class).head;
            this.stagingArea
                    = Utils.readObject(GITLET_REPO_FILE, GitletRepo.class)
                    .stagingArea;
            this.branches = Utils.readObject(GITLET_REPO_FILE, GitletRepo.class)
                    .branches;
            remotes = Utils.readObject(GITLET_REPO_FILE, GitletRepo.class)
                    .remotes;
            initialized = true;
        }
    }

    public void init() throws IOException {
        if (GITLET_FOLDER.exists()) {
            System.out.println("A Gitlet version-control "
                    + "system already exists in the current directory.");
            System.exit(0);
        } else {
            GITLET_FOLDER.mkdir();
            BLOBS_FOLDER.mkdir();
            COMMIT_FOLDER.mkdir();

            TreeMap<String, String> blobs = new TreeMap<>();
            Commit initialCommit
                    = new Commit(null, "initial commit", blobs);
            File commit = Utils.join(COMMIT_FOLDER, initialCommit.getHash());
            commit.createNewFile();
            Utils.writeObject(commit, initialCommit);
            this.branches.put("master", initialCommit.getHash());

            GITLET_REPO_FILE.createNewFile();
            Utils.writeObject(GITLET_REPO_FILE, this);
        }
    }

    public void add(String fileName) throws IOException {
        checkInitialized();
        File addFile = Utils.join(CWD, fileName);
        if (!addFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        } else {
            String bHash = Utils.sha1(Utils.readContents(addFile));
            String prevHash = currentCommit().getBlobs().get(fileName);
            if (prevHash != null && bHash.equals(prevHash)) {
                if (stagingArea.containsKey(fileName)
                        && stagingArea.get(fileName).equals("")) {
                    stagingArea.remove(fileName);
                }
            } else {
                if (stagingArea.containsKey(fileName)
                        && stagingArea.get(fileName).equals("")) {
                    stagingArea.remove(fileName);
                }
                File blobFile = Utils.join(BLOBS_FOLDER, bHash);
                blobFile.createNewFile();
                Utils.writeContents(blobFile, Utils.readContents(addFile));
                stagingArea.put(fileName, bHash);
            }
            Utils.writeObject(GITLET_REPO_FILE, this);
        }
    }

    public void commit(String commitMessage) throws IOException {
        checkInitialized();
        if (stagingArea.size() == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        if (commitMessage == null || commitMessage.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        Commit currentCommit = currentCommit();
        String parentHash = currentCommit.getHash();
        @SuppressWarnings("unchecked")
        TreeMap<String, String> blobs
                = (TreeMap<String, String>) currentCommit.getBlobs().clone();
        for (String key: stagingArea.keySet()) {
            if (stagingArea.get(key).equals("")) {
                blobs.remove(key);
            } else {
                blobs.put(key, stagingArea.get(key));
            }
        }

        Commit newCommit = new Commit(parentHash, commitMessage, blobs);
        File commitFile = Utils.join(COMMIT_FOLDER, newCommit.getHash());
        commitFile.createNewFile();
        Utils.writeObject(commitFile, newCommit);
        this.branches.put(head, newCommit.getHash());

        stagingArea.clear();
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void mergeCommit(String commitMessage,
                            String parentHashMerge) throws IOException {
        checkInitialized();
        if (stagingArea.size() == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        if (commitMessage == null || commitMessage.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        Commit currentCommit = currentCommit();
        String parentHash = currentCommit.getHash();
        @SuppressWarnings("unchecked")
        TreeMap<String, String> blobs
                = (TreeMap<String, String>) currentCommit.getBlobs().clone();
        for (String key: stagingArea.keySet()) {
            if (stagingArea.get(key).equals("")) {
                blobs.remove(key);
            } else {
                blobs.put(key, stagingArea.get(key));
            }
        }

        Commit newCommit = new Commit(parentHash, commitMessage, blobs);
        newCommit.setMergeCommit(parentHashMerge);
        File commitFile = Utils.join(COMMIT_FOLDER, newCommit.getHash());
        commitFile.createNewFile();
        Utils.writeObject(commitFile, newCommit);
        this.branches.put(head, newCommit.getHash());

        stagingArea.clear();
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void checkoutFile(String filename) {
        checkInitialized();
        Commit commit = currentCommit();
        TreeMap<String, String> blobs = commit.getBlobs();
        if (!blobs.containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        File file = Utils.join(CWD, filename);
        File blobFile = Utils.join(BLOBS_FOLDER, blobs.get(filename));
        Utils.writeContents(file, Utils.readContents(blobFile));
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void checkoutFileWithCommitID(String filename, String commitID) {
        checkInitialized();
        String[] allCommitFiles = COMMIT_FOLDER.list();
        String commitName = null;
        for (String commitFileName: allCommitFiles) {
            if (commitFileName.contains(commitID)) {
                commitName = commitFileName;
                break;
            }
        }
        if (commitName == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        File commitFile = Utils.join(COMMIT_FOLDER, commitName);
        Commit commit = Utils.readObject(commitFile, Commit.class);
        TreeMap<String, String> blobs = commit.getBlobs();
        if (!blobs.containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        File file = Utils.join(CWD, filename);
        File blobFile = Utils.join(BLOBS_FOLDER, blobs.get(filename));
        Utils.writeContents(file, Utils.readContents(blobFile));
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void checkoutBranch(String branchName) throws IOException {
        checkInitialized();
        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        if (branchName.equals(head)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        Commit currCommit = currentCommit();
        File checkoutCommitFile = Utils.join(COMMIT_FOLDER,
                branches.get(branchName));
        Commit checkoutCommit
                = Utils.readObject(checkoutCommitFile, Commit.class);
        ArrayList<File> files = new ArrayList<>();
        File[] allFiles = CWD.listFiles();
        for (File file: allFiles) {
            if (!file.isDirectory()) {
                files.add(file);
            }
        }
        String[] fileNames = new String[files.size()];
        int counter = 0;
        for (File file: files) {
            fileNames[counter] = file.getName();
            counter++;
        }
        for (String filename: fileNames) {
            if (checkoutCommit.getBlobs().containsKey(filename)
                    && !currCommit.getBlobs().containsKey(filename)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
            if (!checkoutCommit.getBlobs().containsKey(filename)
                    && currCommit.getBlobs().containsKey(filename)) {
                Utils.restrictedDelete(Utils.join(CWD, filename));
            }
        }

        for (String filename: checkoutCommit.getBlobs().keySet()) {
            File fileOverwrite = Utils.join(CWD, filename);
            if (!fileOverwrite.exists()) {
                fileOverwrite.createNewFile();
            }
            String bHash = checkoutCommit.getBlobs().get(filename);
            File bFile = Utils.join(BLOBS_FOLDER, bHash);
            Utils.writeContents(fileOverwrite, Utils.readContents(bFile));
        }

        stagingArea.clear();
        head = branchName;
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void log() {
        checkInitialized();
        Commit currentCommit = currentCommit();
        while (true) {
            System.out.println(currentCommit.getLogFormat());
            if (currentCommit.getParentHash() == null) {
                break;
            }
            File nextCommit = Utils.join(COMMIT_FOLDER,
                    currentCommit.getParentHash());
            currentCommit = Utils.readObject(nextCommit, Commit.class);
        }
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void globalLog() {
        checkInitialized();
        String[] commitIDs = COMMIT_FOLDER.list();
        for (String commitID: commitIDs) {
            File commitFile = Utils.join(COMMIT_FOLDER, commitID);
            Commit commit = Utils.readObject(commitFile, Commit.class);
            System.out.println(commit.getLogFormat());
        }
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void find(String message) {
        checkInitialized();
        String[] commitIDs = COMMIT_FOLDER.list();
        int counter = 0;
        for (String commitID: commitIDs) {
            File commitFile = Utils.join(COMMIT_FOLDER, commitID);
            Commit commit = Utils.readObject(commitFile, Commit.class);
            if (message.equals(commit.getCommitMessage())) {
                System.out.println(commitID);
                counter++;
            }
        }
        if (counter == 0) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void rm(String filename) {
        checkInitialized();
        boolean staged = false;
        boolean tracked = false;
        if (stagingArea.containsKey(filename)
                && !stagingArea.get(filename).equals("")) {
            stagingArea.remove(filename);
            staged = true;
        }
        Commit currCommit = currentCommit();
        if (currCommit.getBlobs().containsKey(filename)) {
            stagingArea.put(filename, "");
            if (Utils.join(CWD, filename).exists()) {
                Utils.restrictedDelete(Utils.join(CWD, filename));
            }
            tracked = true;
        }
        if (!staged && !tracked) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void branch(String branchName) {
        checkInitialized();
        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        } else {
            branches.put(branchName, branches.get(head));
            Utils.writeObject(GITLET_REPO_FILE, this);
        }
    }

    public void rmBranch(String branchName) {
        checkInitialized();
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (head.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        branches.remove(branchName);
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void statusPrint(ArrayList<String> branchesStatus,
                            ArrayList<String> stagedFilesStatus,
                            ArrayList<String> removedFilesStatus,
                            ArrayList<String> modifiedStatus,
                            ArrayList<String> untrackedStatus) {
        Collections.sort(branchesStatus);
        int indexOfHead = branchesStatus.indexOf(head);
        branchesStatus.set(indexOfHead, "*" + branchesStatus.get(indexOfHead));
        Collections.sort(stagedFilesStatus);
        Collections.sort(removedFilesStatus);
        Collections.sort(modifiedStatus);
        Collections.sort(untrackedStatus);

        System.out.println("=== Branches ===");
        for (String str: branchesStatus) {
            System.out.println(str);
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String str: stagedFilesStatus) {
            System.out.println(str);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String str: removedFilesStatus) {
            System.out.println(str);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String str: modifiedStatus) {
            System.out.println(str);
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String str: untrackedStatus) {
            System.out.println(str);
        }

        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void status() {
        checkInitialized();
        ArrayList<String> branchesStatus = new ArrayList<>();
        ArrayList<String> stagedFilesStatus = new ArrayList<>();
        ArrayList<String> removedFilesStatus = new ArrayList<>();
        ArrayList<String> modifiedStatus = new ArrayList<>();
        ArrayList<String> untrackedStatus = new ArrayList<>();
        for (String bName: branches.keySet()) {
            branchesStatus.add(bName);
        }
        for (String stagingKey: stagingArea.keySet()) {
            if (stagingArea.get(stagingKey).equals("")) {
                removedFilesStatus.add(stagingKey);
            } else {
                stagedFilesStatus.add(stagingKey);
                if (!Utils.join(CWD, stagingKey).exists()) {
                    continue;
                } else {
                    String fileHash = Utils.sha1
                            (Utils.readContents(Utils.join(CWD, stagingKey)));
                    if (!stagingArea.get(stagingKey).equals(fileHash)) {
                        modifiedStatus.add(stagingKey + " (modified)");
                    }
                }
            }
        }

        for (String fileName: currentCommit().getBlobs().keySet()) {
            if (Utils.join(CWD, fileName).exists()) {
                String fileHash = Utils.sha1
                        (Utils.readContents(Utils.join(CWD, fileName)));
                if (!currentCommit().getBlobs().get(fileName).equals
                        (fileHash)) {
                    modifiedStatus.add(fileName + " (modified)");
                }
            } else {
                if (currentCommit().getBlobs()
                        .containsKey(fileName)
                        && (!stagingArea.containsKey(fileName)
                        || !stagingArea.get(fileName).equals(""))) {
                    modifiedStatus.add(fileName + " (deleted)");
                }
            }
        }

        for (String fileName: CWD.list()) {
            if (!stagingArea.containsKey(fileName)
                    && !currentCommit().getBlobs().containsKey(fileName)
                    && Utils.join(CWD, fileName).isFile()) {
                untrackedStatus.add(fileName);
            }
        }

        statusPrint(branchesStatus, stagedFilesStatus,
                removedFilesStatus, modifiedStatus, untrackedStatus);
    }

    public void reset(String commitID) throws IOException {
        checkInitialized();
        String[] allCommitFiles = COMMIT_FOLDER.list();
        String commitName = null;
        for (String commitFileName: allCommitFiles) {
            if (commitFileName.contains(commitID)) {
                commitName = commitFileName;
                break;
            }
        }
        if (commitName == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        Commit currCommit = currentCommit();
        File checkoutCommitFile = Utils.join(COMMIT_FOLDER, commitName);
        Commit checkoutCommit = Utils.readObject(checkoutCommitFile,
                Commit.class);

        ArrayList<File> files = new ArrayList<>();
        File[] allFiles = CWD.listFiles();
        for (File file: allFiles) {
            if (!file.isDirectory()) {
                files.add(file);
            }
        }
        String[] fileNames = new String[files.size()];
        int counter = 0;
        for (File file: files) {
            fileNames[counter] = file.getName();
            counter++;
        }
        for (String filename: fileNames) {
            if (checkoutCommit.getBlobs().containsKey(filename)
                    && !currCommit.getBlobs().containsKey(filename)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
            if (!checkoutCommit.getBlobs().containsKey(filename)
                    && currCommit.getBlobs().containsKey(filename)) {
                Utils.restrictedDelete(Utils.join(CWD, filename));
            }
        }

        for (String filename: checkoutCommit.getBlobs().keySet()) {
            File fileOverwrite = Utils.join(CWD, filename);
            if (!fileOverwrite.exists()) {
                fileOverwrite.createNewFile();
            }
            String bHash = checkoutCommit.getBlobs().get(filename);
            File bFile = Utils.join(BLOBS_FOLDER, bHash);
            Utils.writeContents(fileOverwrite, Utils.readContents(bFile));
        }

        stagingArea.clear();
        branches.put(head, checkoutCommit.getHash());
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void merge(String branchName) throws IOException {
        checkInitialized();
        checkMergeErrors(branchName);

        ArrayList<File> files = new ArrayList<>();
        File[] allFiles = CWD.listFiles();
        for (File file: allFiles) {
            if (!file.isDirectory()) {
                files.add(file);
            }
        }
        String[] fileNames = new String[files.size()];
        int counter = 0;
        for (File file: files) {
            fileNames[counter] = file.getName();
            counter++;
        }

        Commit currCommit = currentCommit();
        File givenCommitFile =
                Utils.join(COMMIT_FOLDER, branches.get(branchName));
        Commit givenCommit = Utils.readObject(givenCommitFile, Commit.class);
        for (String filename: fileNames) {
            if (givenCommit.getBlobs().containsKey(filename)
                    && !currCommit.getBlobs().containsKey(filename)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        String splitPoint = findSplitPoint(currCommit, givenCommit);
        File splitPointFile = Utils.join(COMMIT_FOLDER, splitPoint);
        Commit splitPointCommit = Utils.readObject
                (splitPointFile, Commit.class);

        if (splitPoint.equals(givenCommit.getHash())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return;
        }
        if (splitPoint.equals(currCommit.getHash())) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        boolean isConflict = mergeAll(givenCommit, currCommit,
                splitPointCommit);

        mergeCommit("Merged " + branchName + " into " + head
                + ".", givenCommit.getHash());
        if (isConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public boolean mergeAll(Commit givenCommit, Commit currCommit,
                            Commit splitPointCommit) throws IOException {
        boolean isConflict = false;
        ArrayList<String> gcFiles =
                new ArrayList<>(givenCommit.getBlobs().keySet());
        TreeMap<String, String> gcBlobs = givenCommit.getBlobs();
        ArrayList<String> currFiles =
                new ArrayList<>(currCommit.getBlobs().keySet());
        TreeMap<String, String> currBlobs = currCommit.getBlobs();
        ArrayList<String> splitFiles =
                new ArrayList<>(splitPointCommit.getBlobs().keySet());
        TreeMap<String, String> splitBlobs = splitPointCommit.getBlobs();

        for (String fileName: gcFiles) {
            if (!splitBlobs.containsKey(fileName) || !splitBlobs.get(fileName)
                    .equals(gcBlobs.get(fileName))) {
                checkoutFileWithCommitID(fileName, givenCommit.getHash());
                add(fileName);
            }
        }

        for (String fileName: splitFiles) {
            if (currBlobs.containsKey(fileName)
                    && splitBlobs.get(fileName).equals(currBlobs.get(fileName))
                    && !gcBlobs.containsKey(fileName)) {
                rm(fileName);
            }
        }

        for (String fileName: currFiles) {
            if (splitBlobs.containsKey(fileName)
                    && gcBlobs.containsKey(fileName)) {
                if ((!splitBlobs.get(fileName).equals(currBlobs.get(fileName))
                        && !splitBlobs.get(fileName)
                        .equals(gcBlobs.get(fileName)))
                         && !gcBlobs.get(fileName).equals
                        (currBlobs.get(fileName))) {
                    isConflict = true;
                    conflictMergeContents(fileName, givenCommit, currCommit);
                }
            } else if (splitBlobs.containsKey(fileName)
                    && !gcBlobs.containsKey(fileName)) {
                if (!splitBlobs.get(fileName).equals(currBlobs.get(fileName))) {
                    isConflict = true;
                    conflictMergeContents(fileName, givenCommit, currCommit);
                }
            } else if (!splitBlobs.containsKey(fileName)
                    && gcBlobs.containsKey(fileName)) {
                isConflict = true;
                conflictMergeContents(fileName, givenCommit, currCommit);
            }
        }

        return isConflict;
    }

    public void checkMergeErrors(String branchName) {
        if (stagingArea.size() != 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branchName.equals(head)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
    }

    public String findSplitPoint(Commit currCommit, Commit givenCommit) {
        String[] allCommits = COMMIT_FOLDER.list();
        int min = Integer.MAX_VALUE;
        String splitPoint = "";
        for (String commit: allCommits) {
            if (containsCommit(commit, currCommit)
                    & containsCommit(commit, givenCommit)) {
                int currDist = findSplitPointHelper(commit, currCommit);
                if (currDist < min) {
                    min = currDist;
                    splitPoint = commit;
                }
            }
        }

        return splitPoint;
    }

    public boolean containsCommit(String commitToFind, Commit whichCommit) {
        if (commitToFind.equals(whichCommit.getHash())) {
            return true;
        }
        if (whichCommit.getParentHash() == null) {
            return false;
        }
        File p1File = Utils.join(COMMIT_FOLDER,
                whichCommit.getParentHash());
        Commit p1 = Utils.readObject(p1File, Commit.class);
        if (whichCommit.getMergeCommit()) {
            File p2File = Utils.join(COMMIT_FOLDER,
                    whichCommit.getParentHashMerge());
            Commit p2 = Utils.readObject(p2File, Commit.class);
            return (containsCommit(commitToFind, p1)
                    || containsCommit(commitToFind, p2));
        }
        return containsCommit(commitToFind, p1);
    }

    public int findSplitPointHelper(String commitToFind, Commit whichCommit) {
        if (commitToFind.equals(whichCommit.getHash())) {
            return 0;
        }
        if (whichCommit.getParentHash() == null) {
            return Integer.MAX_VALUE - 100;
        }
        File p1File = Utils.join(COMMIT_FOLDER,
                whichCommit.getParentHash());
        Commit p1 = Utils.readObject(p1File, Commit.class);
        if (whichCommit.getMergeCommit()) {
            File p2File = Utils.join(COMMIT_FOLDER,
                    whichCommit.getParentHashMerge());
            Commit p2 = Utils.readObject(p2File, Commit.class);
            return 1 + Math.min(findSplitPointHelper(commitToFind, p1),
                    findSplitPointHelper(commitToFind, p2));
        }
        return 1 + findSplitPointHelper(commitToFind, p1);
    }

    public void conflictMergeContents(String fileName, Commit givenCommit,
                                        Commit currCommit) throws IOException {
        TreeMap<String, String> gcBlobs = givenCommit.getBlobs();
        TreeMap<String, String> currBlobs = currCommit.getBlobs();
        File file = Utils.join(CWD, fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        String currContents = "";
        if (currBlobs.containsKey(fileName)) {
            currContents = Utils.readContentsAsString(Utils.join(BLOBS_FOLDER,
                    currBlobs.get(fileName)));
        }

        String givenContents = "";
        if (gcBlobs.containsKey(fileName)) {
            givenContents = Utils.readContentsAsString(Utils.join(BLOBS_FOLDER,
                    gcBlobs.get(fileName)));
        }
        String result = "<<<<<<< HEAD\n" + currContents + "=======\n"
                + givenContents + ">>>>>>>\n";
        Utils.writeContents(file, result.getBytes(StandardCharsets.UTF_8));
        stagingArea.put(fileName, Utils.sha1
                (result.getBytes(StandardCharsets.UTF_8)));
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void addRemote(String remoteName, String dirName) {
        checkInitialized();
        if (remotes.containsKey(remoteName)) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        }
        remotes.put(remoteName, dirName);
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void rmRemote(String remoteName) {
        checkInitialized();
        if (!remotes.containsKey(remoteName)) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        }
        remotes.remove(remoteName);
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void push(String remoteName, String branchName)
            throws IOException {
        checkInitialized();
        String remotePath = remotes.get(remoteName);
        File remoteGitlet  = new File(remotePath);
        if (!remoteGitlet.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        File remoteGitletRepoFile
                = Utils.join(remoteGitlet, "gitletRepo");
        GitletRepo remoteRepo = Utils.readObject(remoteGitletRepoFile,
                GitletRepo.class);
        if (!remoteRepo.branches.containsKey(branchName)) {
            remoteRepo.branch(branchName);
        }
        Commit localHeadCommit = currentCommit();
        String remoteHeadCommitHash = remoteRepo.branches.get(remoteRepo.head);
        if (!containsCommit(remoteHeadCommitHash, localHeadCommit)) {
            System.out.println("Please pull down remote "
                    + "changes before pushing");
            System.exit(0);
        }
        pushHelper(remoteRepo, remoteHeadCommitHash, remoteName);
        remoteRepo.branches.put(remoteRepo.head, currentCommit().getHash());
        Utils.writeObject(remoteGitletRepoFile, remoteRepo);
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void pushHelper(GitletRepo remoteRepo,
                           String remoteHeadHash, String remoteName)
            throws IOException {
        Commit curr = currentCommit();
        String remotePath = remotes.get(remoteName);
        File remoteGitlet  = Utils.join(CWD, remotePath);
        File remoteCommitFolder
                = Utils.join(remoteGitlet, "commits");
        File remoteBlobsFolder
                = Utils.join(remoteGitlet, "blobs");
        while (true) {
            File commitFile = Utils.join(remoteCommitFolder,
                    curr.getHash());
            if (!commitFile.exists()) {
                commitFile.createNewFile();
            }
            Utils.writeObject(commitFile, curr);
            remoteRepo.branches.put(head, curr.getHash());
            for (String blobKey: curr.getBlobs().keySet()) {
                File blobFile = Utils.join(BLOBS_FOLDER,
                        curr.getBlobs().get(blobKey));
                File newBlobFile = Utils.join(remoteBlobsFolder,
                        curr.getBlobs().get(blobKey));
                if (!newBlobFile.exists()) {
                    newBlobFile.createNewFile();
                }
                Utils.writeContents(blobFile, Utils.readContents(blobFile));
            }
            if (curr.getParentHash() == null
                    || curr.getParentHash().equals(remoteHeadHash)) {
                break;
            }
            File nextCommit = Utils.join(COMMIT_FOLDER,
                    curr.getParentHash());
            curr = Utils.readObject(nextCommit, Commit.class);
        }
    }

    public void fetch(String remoteName,
                      String branchName) throws IOException {
        checkInitialized();
        String remotePath = remotes.get(remoteName);
        File remoteGitlet  = Utils.join(CWD, remotePath);
        if (!remoteGitlet.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        File remoteGitletRepoFile
                = Utils.join(remoteGitlet, "gitletRepo");
        GitletRepo remoteRepo = Utils.readObject(remoteGitletRepoFile,
                GitletRepo.class);
        File remoteCommitFolder
                = Utils.join(remoteGitlet, "commits");
        File remoteBlobsFolder
                = Utils.join(remoteGitlet, "blobs");
        if (!remoteRepo.branches.containsKey(branchName)) {
            System.out.println("That remote does not have that branch.");
            System.exit(0);
        }
        String newBranchName = remoteName + File.separator + branchName;
        Commit remoteCurr = Utils.readObject(Utils.join(remoteCommitFolder,
                remoteRepo.branches.get(remoteRepo.head)), Commit.class);
        while (true) {
            File newCommitFile = Utils.join(COMMIT_FOLDER,
                    remoteCurr.getHash());
            if (!newCommitFile.exists()) {
                newCommitFile.createNewFile();
            }
            Utils.writeObject(newCommitFile, remoteCurr);
            for (String blobKey: remoteCurr.getBlobs().keySet()) {
                File blobFile = Utils.join(remoteBlobsFolder,
                        remoteCurr.getBlobs().get(blobKey));
                File newBlobFile = Utils.join(BLOBS_FOLDER,
                        remoteCurr.getBlobs().get(blobKey));
                if (!newBlobFile.exists()) {
                    newBlobFile.createNewFile();
                }
                Utils.writeContents(newBlobFile, Utils.readContents(blobFile));
            }
            if (remoteCurr.getParentHash() == null) {
                break;
            }
            remoteCurr = Utils.readObject(Utils.join(remoteCommitFolder,
                    remoteCurr.getParentHash()), Commit.class);
        }
        if (!this.branches.containsKey(newBranchName)) {
            branch(newBranchName);
        }
        branches.put(newBranchName, remoteRepo.branches.get(remoteRepo.head));
        Utils.writeObject(remoteGitletRepoFile, remoteRepo);
        Utils.writeObject(GITLET_REPO_FILE, this);
    }

    public void pull(String remoteName, String branchName)
            throws IOException {
        checkInitialized();
        fetch(remoteName, branchName);
        merge(remoteName + File.separator + branchName);
    }

    public Commit currentCommit() {
        String currentBranchHash = this.branches.get(head);
        File commitFile = Utils.join(COMMIT_FOLDER, currentBranchHash);
        Commit commit = Utils.readObject(commitFile, Commit.class);
        return commit;
    }

    public void checkInitialized() {
        if (!initialized) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
