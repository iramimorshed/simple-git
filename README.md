# Gitlet
Gitlet is a version-control system that has features similar to Git.

## Commands 
#### 1. Initializes a new Gitlet version-control system
java gitlet.Main init 
<br>

#### 2. Adds file copy to the staging area found in Stage.java
java gitlet.Main add [file name]


#### 3. Tracks the saved files in the current commit and staging area. 
java gitlet.Main commit [message]

#### 4. If file is staged for addition, unstage it. If current commit includes file, stage it for removal and remove file from the working directory. 
java gitlet.Main rm [file name]

#### 5. Displays info about each commit starting from current commit and going backwards to the parent commits along commit tree. 
java gitlet.Main log

#### 6. Displays info about all commits made. 
java gitlet.Main global-log

#### 7. Prints ids of commits with given commit message. 
java gitlet.Main find [commit message]

#### 8. Displays currently existing branches, marking current brach with '*'. Displays files staged for addition and removal. 
java gitlet.Main status

#### 9. Takes version of file in head commit (front of current branch) and places into working directory, overwriting file there if there exists one. 
java gitlet.Main checkout -- [file name]

#### 10. Takes version of file in commit with given id and places into working directory, overwriting version of file there if there exists one. 
java gitlet.Main checkout [commit id] -- [file name]

#### 11. Takes all files in commit at head of given branch and places into working directory, overwriting version of files there if they exist. The given branch considered current branch or head branch. Files tracked in current branch but nonexistent in checked-out branch deleted. Staging area is also cleared if checked-out branch is not current branch. 
java gitlet.Main checkout [branch name]

#### 12. Creates new branch with given name and points it at current head node. 
java gitlet.Main branch [branch name]

#### 13. Deletes branch with given name i.e. deletes pointer associated with branch not all commits created under branch. 
java gitlet.Main rm-branch [branch name]

#### 14. Checks out files tracked by commit given by id. Removes tracked files not present in given commit and moves current branch head to commit node. 
java gitlet.Main reset [commit id]

#### 15. Merges files from branch given by name below into current branch. 
java gitlet.Main merge [branch name]
