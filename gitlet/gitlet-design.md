# Gitlet Design Document
author: Arvind Vivekanandan, Har Vey Yuen

## Design Document Guidelines

Please use the following format for your Gitlet design document. Your design
document should be written in markdown, a language that allows you to nicely
format and style a text file. Organize your design document in a way that
will make it easy for you or a course-staff member to read.

## 1. Classes and Data Structures

Include here any class definitions. For each class list the instance
variables and static variables (if any). Include a ***brief description***
of each variable and its purpose in the class. Your explanations in
this section should be as concise as possible. Leave the full
explanation to the following sections. You may cut this section short
if you find your document is too wordy.

### Main.java
This class is the entry point of the program. It implements methods to run all
functions of the Gitlet repository and support each command of the program.

#### Fields

1. GitletRepo repo: A custom Gitlet repository object created to store
   all data necessary in the repo ()

### Commit.java
This class serves as the object structure of a singular commit.

#### Fields

1. String hash: Hash value of the commit
2. String parentHash: Hash value of the parent commit
3. String commitMessage: Message for the commit
4. String timeStamp: Time stamp of the commit (use java.util.Date and java.util.Formatter)
5. TreeMap<String, String> blobHashes: Each key value pair maps the name of each file to the hash of each file in the commit

### GitletRepo.java
This class represents the actual .gitlet repo inside the CWD, containing every commit and their relevant files, as well as branches (pointers).

#### Fields

1. TreeMap<String, String> stagingArea: Each key value pair maps the name of each file to the hash of each file
3. TreeMap<String, String> branches: Each key value pair maps the name of each branch to the hash of the commit that the branch points to
4. String head: Represents the current branch we are on
5. File blobDirectory: Directory for the files representing blobs (names of the files are their hashes)
6. File commitDirectory: Directory for all the commits (names of the commits are their hashes)


## 2. Algorithms

This is where you tell us how your code works. For each class, include
a high-level description of the methods in that class. That is, do not
include a line-by-line breakdown of your code, but something you would
write in a javadoc comment above a method, ***including any edge cases
you are accounting for***. We have read the project spec too, so make
sure you do not repeat or rephrase what is stated there.  This should
be a description of how your code accomplishes what is stated in the
spec.


The length of this section depends on the complexity of the task and
the complexity of your design. However, simple explanations are
preferred. Here are some formatting tips:

* For complex tasks, like determining merge conflicts, we recommend
  that you split the task into parts. Describe your algorithm for each
  part in a separate section. Start with the simplest component and
  build up your design, one piece at a time. For example, your
  algorithms section for Merge Conflicts could have sections for:

  * Checking if a merge is necessary.
  * Determining which files (if any) have a conflict.
  * Representing the conflict in the file.

* Try to clearly mark titles or names of classes with white space or
  some other symbols.

### Main.java

1. void main(String[] args): This is the entry point of the program. It first checks to make sure that the input array is not empty. Then it creates a GitletRepo object. Then it has a switch statement for the various commands.

### GitletRepo.java
1. void init(): If the directory for `.gitlet` already exists, then throw an error. Otherwise, make the `.gitlet` directory, and then make the `blob`, `commit`, `branches`, and `staging` directories within it. Create an initial commit with no parent hashes, a default initial commit message, and the timestamp being the Unix Epoch. Serialize and add this commit to the commit directory. Create a new `master` branch and assign the head to it. 
2. void add(): Add the specified file to the adding stage ArrayList and add it to the adding stage directory. This file should be named its hash.
3. void commit(): Create a new commit object. Set the parent hash to the commit that the head branch points to. Set the message to the message that the user passes in and the timestamp. Set the TreeMap of names of files (blobs) to their hashes. Serialize the commit object and add it to the commit directory. 
4. void checkout(): Change branch or specific commit, grab the contents of the specified file, then overwrite the contents of the file in `CWD`

## 3. Persistence

Describe your strategy for ensuring that you don’t lose the state of your program
across multiple runs. Here are some tips for writing this section:

* This section should be structured as a list of all the times you
  will need to record the state of the program or files. For each
  case, you must prove that your design ensures correct behavior. For
  example, explain how you intend to make sure that after we call
  `java gitlet.Main add wug.txt`,
  on the next execution of
  `java gitlet.Main commit -m “modify wug.txt”`,
  the correct commit will be made.

* A good strategy for reasoning about persistence is to identify which
  pieces of data are needed across multiple calls to Gitlet. Then,
  prove that the data remains consistent for all future calls.

* This section should also include a description of your .gitlet
  directory and any files or subdirectories you intend on including
  there.

### GitletRepo
`.gitlet` folder will have `blobs` and `commits` subdirectories to 
store the hashcodes as the names and the actual contents of each blob and commit.

## 4. Design Diagram

Attach a picture of your design diagram illustrating the structure of your
classes and data structures. The design diagram should make it easy to
visualize the structure and workflow of your program.

