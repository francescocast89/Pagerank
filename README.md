# Pagerank in MapReduce
A Hadoop MapReduce implementation of the PageRank algorithm. Starting from an input file in which each line represents a node of the graph and its adjacent nodes
```
node1 to11 to12 to13 ...
...
```
Vertex identiﬁers and adjacencies are spaced by a tab delimiter, instead neighbour nodes in the adjacency list are separate by a space.
The output file will contain two components per line, the first represent the PageRank value and the second is the identifier of the node which have that PageRank. 
```
PR(1) node1
...
```
Sample input ﬁles can be found in the folder test.

# Pre-requisites
Hadoop and Maven installed and properly configured.

# Compiling and Running
To build and compile the application Pagerank.java inside the PageRank folder we follow this steps:
```
mvn clean package
hadoop namenode -format
start-all.sh
```
At this point, it is possible to add the input file in hdfs:
```
hadoop fs -put test/simple-graph.txt simple-graph.txt
```
Than the Pagerank package can be executed typing:
```
hadoop jar target/Pagerank-1.0-SNAPSHOT.jar it.cnr.isti.pad.Pagerank simple-graph.txt pagerank_output
```
The results will be available in the output directory called ```pagerank_output```
