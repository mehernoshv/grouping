# grouping

This is a solution for the exercise provided at https://github.com/MightyHive/hiring-exercises/tree/master/grouping

Prequistes to compile and run the solution are:
1) Java 8 JDK
2) Maven install

To compile and run the MS, pull down the repo:

git clone https://github.com/mehernoshv/grouping.git

This will create a directory grouping. The source code is in "src" directory. (Standard Maven directory structure)

cd grouping

In that directory run:
    ./mvn clean package (for linux, etc), or
    ./mvn.cmd clean package (for windows) 

This will create the packaged jar in target directory. (Please note: I have only tested on linux,)

To run the solution

java -jar target/Grouping-1.0-SNAPSHOT.jar <Input CSV file path> <Output CSV file path> <Matching mode>

	Where <Matching mode> can be either MatchEmail OR MatchPhone OR Both


Example
  java -jar target/Grouping-1.0-SNAPSHOT.jar ./input1.csv ./output1.csv Both

