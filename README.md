# PageRank

## Introduction
PageRank is a way of measuring the importance of website pages. The Pagerank algorithm looks at links from page A to page B, if a link exists, this is interpreted as a "vote" (page B is voting for page A). 

### How we can use PageRank?
The Web is a graph, a set of nodes corresponds to the various pages and the hyperlinks are the arcs.
We start from a random page than:
* Pick at random an outogoing link with probability λ
* Jump at random into a page with probability 1-λ

The PageRank of page x Pr(x) is the probability of being on page x at a random moment in time.

### Algorithm
PageRank can be computed either iteratively or algebraically. In the iterative method we start initializing the PageRank of every node:

![](http://latex.codecogs.com/gif.latex?PR%28x%29%3D%5Cfrac%7B1%7D%7BN%7D%5Cquad%20%5Cforall%20x%20%5Cquad%20N%3Anumber%5C%2Cof%5C%2Cnodes%5C%2Cin%5C%2Cthe%5C%2Cgraph)

At each step we compute the PageRank as:

![](http://latex.codecogs.com/gif.latex?PR%28x%29%3D%5Cfrac%7B1-%5Clambda%7D%7BN%7D&plus;%5Clambda%20%5Csum_%7By%20%5Crightarrow%20x%7D%5Cfrac%7BPr%28y%29%7D%7Bout%28y%29%7D%5Cquad%5Cforall%20x)

Pr(x) correspond to the probability to be ant node x at the current moment. I can get there in 2 ways, on the previous step i flip a coin and :

* if i get tail (1-λ) so i perform a random hop and i go to x with  ![](http://latex.codecogs.com/gif.latex?Pr%3D%5Cfrac%7B1%7D%7BN%7D)
* if i get head λ the only way to go to x is to be in a node y which points to x and select that arc that connects y to x with ![](http://latex.codecogs.com/gif.latex?Pr%3D%5Cfrac%7B1%7D%7Bnumber%5C%2Cof%5C%2Coutgoing%5C%2Clinks%7D%5Cquad)
&nbsp; y is not the only node that points to x so i have to sum up over all the nodes that points to x.

We procede in a new iteration if &nbsp; ![](http://latex.codecogs.com/gif.latex?%5Cleft%20%7CPR%28t&plus;1%29-PR%28t%29%20%5Cright%20%7C%3C%20%5Cepsilon)

## Computing PageRank using MapReduce
Given a graph, we are going to implement PageRank algorithm using Hadoop.

## Input
We will use as graph this file [links-simple-sorted.7z](http://haselgrove.id.au/wikipedia/20160305/links-simple-sorted.7z). This file is derived from the 5 March 2016 version of the english language wikipedia data dump. They contain all links between proper Wikipedia pages. The format of the lines is:
```
from1: to11 to12 to13 ...
from2: to21 to22 to23 ...
    ...
```
##  Algorithm
We can subdivide the entire work in 3 phases: parsing, calculating and ordering.

### Parsing
The parser is a map job that take a line of the input text and emit as key the node number and as value the initial pagerank of that node and the list of adjacent nodes.
I get the number of nodes counting the numbers of line in the input file and than i pass this value to the parser (and also to the reducer).
```
Input:
1: 2 3

Output:
1	0.25	2 3
``` 
### Calculating
The map phase take as input the output file of the parser. 
Each line is formatted in this way

```
x 	PR(x)	y1 y2 ... 
```
For each line of the file, the mapper:

* First of all it emits a pair key,value in which the key (of type Text) contains the node id and the value (of type Text) contains the PR(x) and the list of adjacencies:
```
< x,PR(x)	y1 y2>	
...
```
This will be used by the reducer to reconstruct the graph.

* Subsequently the mapper emits for every node y1,y2,... of the adjacency list a pair key,value in which the key is the node id and the value is the 

```
<y1,PR(x)/out(x)>	
<y2,PR(x)/out(x)> 
...
```

### Ordering
