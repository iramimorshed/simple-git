# Gitlet
Gitlet is a version-control system that has features similar to Git.

## Commands 
#### Initializes a new Gitlet version-control system
java gitlet.Main init 
<br>

#### Adds file copy to the staging area found in Stage.java
java gitlet.Main add [file name]


#### Tracks the saved files in the current commit and staging area. 
java gitlet.Main commit [message]

#### If file is staged for addition, unstage it. If current commit includes file, stage it for removal and remove file from the working directory. 
java gitlet.Main rm [file name]

#### Displays info about each commit starting from current commit and going backwards to the parent commits along commit tree. 
java gitlet.Main log

#### Displays info about all commits made. 
java gitlet.Main global-log

#### Prints ids of commits with given commit message. 
java gitlet.Main find [commit message]

#### Displays currently existing branches, marking current brach with '*'. Displays files staged for addition and removal. 
java gitlet.Main status

#### Takes version of file in head commit (front of current branch) and places into working directory, overwriting file there if there exists one. 
java gitlet.Main checkout -- [file name]

#### Takes version of file in commit with given id and places into working directory, overwriting version of file there if there exists one. 
java gitlet.Main checkout [commit id] -- [file name]

#### Takes all files in commit at head of given branch and places into working directory, overwriting version of files there if they exist. The given branch considered current branch or head branch. Files tracked in current branch but nonexistent in checked-out branch deleted. Staging area is also cleared if checked-out branch is not current branch. 
java gitlet.Main checkout [branch name]

#### Creates new branch with given name and points it at current head node. 
java gitlet.Main branch [branch name]

#### Deletes branch with given name i.e. deletes pointer associated with branch not all commits created under branch. 
java gitlet.Main rm-branch [branch name]

#### Checks out files tracked by commit given by id. Removes tracked files not present in given commit and moves current branch head to commit node. 
java gitlet.Main reset [commit id]

#### Merges files from branch given by name below into current branch. 
java gitlet.Main merge [branch name]
