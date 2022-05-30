package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Arvind Vivekanandan
 */
public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        checkCommandInputted(args);
        GitletRepo repo = new GitletRepo();
        switch (args[0]) {
        case "init":
            validateNumArgs(args, 1);
            repo.init();
            break;
        case "add":
            validateNumArgs(args, 2);
            repo.add(args[1]);
            break;
        case "commit":
            validateNumArgs(args, 2);
            repo.commit(args[1]);
            break;
        case "checkout":
            checkoutCase(repo, args);
            break;
        case "log":
            validateNumArgs(args, 1);
            repo.log();
            break;
        case "global-log":
            validateNumArgs(args, 1);
            repo.globalLog();
            break;
        case "find":
            validateNumArgs(args, 2);
            repo.find(args[1]);
            break;
        case "rm":
            validateNumArgs(args, 2);
            repo.rm(args[1]);
            break;
        case "branch":
            validateNumArgs(args, 2);
            repo.branch(args[1]);
            break;
        case "rm-branch":
            validateNumArgs(args, 2);
            repo.rmBranch(args[1]);
            break;
        case "status":
            validateNumArgs(args, 1);
            repo.status();
            break;
        case "reset":
            validateNumArgs(args, 2);
            repo.reset(args[1]);
            break;
        case "merge":
            validateNumArgs(args, 2);
            repo.merge(args[1]);
            break;
        default:
            extraCreditRemoteCommands(repo, args);
        }
        return;
    }

    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }

    public static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            throw new RuntimeException(
                    String.format("Incorrect operands."));
        }
    }

    public static void checkoutCase(GitletRepo repo, String[] args)
            throws IOException {
        if (args.length == 2) {
            repo.checkoutBranch(args[1]);
        } else if (args.length == 3 && args[1].equals("--")) {
            repo.checkoutFile(args[2]);
        } else if (args.length == 4 && args[2].equals("--")) {
            repo.checkoutFileWithCommitID(args[3], args[1]);
        } else {
            exitWithError("Incorrect operands.");
        }
    }

    public static void checkCommandInputted(String[] args) {
        if (args.length == 0) {
            exitWithError("Please enter a command.");
        }
    }

    public static void extraCreditRemoteCommands(GitletRepo repo,
                                                 String[] args)
            throws IOException {
        switch (args[0]) {
        case "add-remote":
            validateNumArgs(args, 3);
            repo.addRemote(args[1], args[2]);
            break;
        case "rm-remote":
            validateNumArgs(args, 2);
            repo.rmRemote(args[1]);
            break;
        case "push":
            validateNumArgs(args, 3);
            repo.push(args[1], args[2]);
            break;
        case "fetch":
            validateNumArgs(args, 3);
            repo.fetch(args[1], args[2]);
            break;
        case "pull":
            validateNumArgs(args, 3);
            repo.pull(args[1], args[2]);
            break;
        default:
            exitWithError(String.format("No command with that name exists."));
        }
        return;
    }
}
