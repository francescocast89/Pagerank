# PageRank

## Introduction
PageRank is a way of measuring the importance of website pages. The Pagerank algorithm looks at links from page A to page B, if a link exists, this is interpreted as a "vote" (page B is voting for page A). 

### How we can use PageRank?
The Web is a graph, a set of nodes corresponds to the various pages and the hyperlinks are the arcs.

We start from a random page then:
* Randomly pick an outogoing link with probability λ
* Randomly jump into a page with probability 1-λ

The PageRank of page x Pr(x) is the probability of being on page x at a random moment in time.

### Algorithm
PageRank can be computed either iteratively or algebraically. In the iterative method we start initializing the PageRank of every node:

![](http://latex.codecogs.com/gif.latex?PR%28x%29%3D%5Cfrac%7B1%7D%7BN%7D%5Cquad%20%5Cforall%20x%20%5Cquad%20N%3Anumber%5C%2Cof%5C%2Cnodes%5C%2Cin%5C%2Cthe%5C%2Cgraph)

At each step we compute the PageRank as:

![](http://latex.codecogs.com/gif.latex?PR%28x%29%3D%5Cfrac%7B1-%5Clambda%7D%7BN%7D&plus;%5Clambda%20%5Csum_%7By%20%5Crightarrow%20x%7D%5Cfrac%7BPr%28y%29%7D%7Bout%28y%29%7D%5Cquad%5Cforall%20x)

Pr(x) corresponds to the probability to be ant node x at the current moment. I can get there in 2 ways, on the previous step I flip a coin and :

* if I get tail (1-λ), I perform a random hop and go to x with  ![](http://latex.codecogs.com/gif.latex?Pr%3D%5Cfrac%7B1%7D%7BN%7D)
* if I get head (λ), the only way to go to x is to be in a node y which points to x and select that arc that connects y to x with ![](http://latex.codecogs.com/gif.latex?Pr%3D%5Cfrac%7B1%7D%7Bnumber%5C%2Cof%5C%2Coutgoing%5C%2Clinks%7D%5Cquad)
&nbsp; y is not the only node that points to x so I have to sum up over all the nodes that points to x.

We procede in a new iteration if &nbsp; ![](http://latex.codecogs.com/gif.latex?%5Cleft%20%7CPR%28t&plus;1%29-PR%28t%29%20%5Cright%20%7C%3C%20%5Cepsilon)

## Computing PageRank using MapReduce
Given a graph, we implement PageRank algorithm using Hadoop.

## Input
We will use as graph this file [links-simple-sorted.7z](http://haselgrove.id.au/wikipedia/20160305/links-simple-sorted.7z). This file is derived from the 5 March 2016 version of the english language wikipedia data dump. It contains all the links between proper Wikipedia pages. The format of the lines is:
```
from1: to11 to12 to13 ...
from2: to21 to22 to23 ...
    ...
```
##  Algorithm
We can subdivide the entire work in 3 phases: parsing, calculating and sorting.

### Parsing
The parser is a map job that takes a line of the input text and emits as <em>key</em>em> the node number and as <em>value</em> the initial pagerank of that node and the list of adjacent nodes.

```
Node id 	PageRank 	adjacency list of nodes 
x 		PR(x) 		y1 y2 ...
```
I get the number of nodes counting the numbers of line in the input file and than I pass this value to the parser (and also to the reducer).

### Calculating

#### Map
The map phase takes as input the output file of the parser and for each line: 

* First of all it emits a pair <em>key,value</em> in which the <em>key</em> contains the node id and the <em>value</em> contains the PR(x) and the list of adjacencies:
	```
	<x,PR(x)	y1 y2>	
	...
	```
	This will be used by the reducer to reconstruct the graph.

* Subsequently the mapper emits for every node of the adjacency list a pair <em>key,value</em> in which the <em>key</em> is the id of the node in the list and the <em>value</em> is the PageRank of the starting node divided by the number of items into the adjacency list (which is the number of outgoing links from the starting node).

	```
	<y1,PR(x)/out(x)>	
	<y2,PR(x)/out(x)> 
	...
	```

#### Reduce
The reduce phase takes all the sorted pair <em>key,value</em> and checks the type of value:

* If it contanins more than one string this means that value represents an adjacency list. So the reducer use this entry to reconstructs the graph. 
* If it contains only one value this means that  it is a PageRank, so this is summed to the variable sum to compute the final pagerank.

Than the final pagerank is computed using the formula. The reducer emits a pair <em>key,value</em> in which the <em>key</em> is the node id and the <em>value</em> is the final pagerank and the list of adjacent nodes.

```
<x,PR(x)	y1 y2>	
...
```

### Sorting
The sorter is a map job that takes as input the output file from the reducer and simply emits a pair <em>key,value</em> in which the <em>key</em> is the PageRank and the <em>value</em> is the node id. Then I have defined a <em>DescendingKeyComparator </em> which is used to sort the key in descending order.


## Tests
For the first toy test I have used this graph:
 ```
B: C
C: B
D: A B
E: F B D
F: B E
G: E B
H: E B
I: E B
J: E
K: E
```
After 7 iterations (≅ 3 min) the resulted output is:
 ```
0.31782666429629636	B
0.30773291298765443	C
0.09403062360493825	E
0.04515423130864196	D
0.04515423130864196	F
0.038030623604938266	A
0.019999999999999997	G
0.019999999999999997	H
0.019999999999999997	I
0.019999999999999997	J
0.019999999999999997	K
```
After that I have executed a second test in which the input file is links-simple-sorted.7z. This file decompressed is over 3GB and contains 12297550 entries. 
 ```
1: 493929 3115604 3734773 10499689
2: 3 174 198455 446046 862606 1770885 2359241 2906855 3259093 3259427 3734773 4237771 4738596 7335079 8044082 9170219
3: 8 174 832954 1171601 1451305 1612236 1638604 2045538 2529665 2552072 2914036 2914043 3301583 3744796 3971302 4408189 4705908 4934414 4972072 4989674 5208521 5241492 5257576 5528287 5783097 5988550 6178646 6420136 6765447 7125672 7623659 7625182 7627419 7651800 7653463 7685976 7744217 7760299 7865224 7893075 8213785 8258394 8744769 8753681 8875927 9089458 9100988 9108063 9519111 9614543 10363498 10386257 10505525 10758717 10784545 10806147 10822380 10867704 10985681 11112901 11225589 11290471 11291320 11618549 11753369 11786048
4: 4112142
...
```
After 5 iterations (≅ 6 hours) the resulted output is:
 ```
0.0041363224338001915	4605230
0.003717645634672935	11872047
0.0030096046572160315	5059662
0.0027715286821695237	3115604
0.002250982133637694	3115596
0.001716748634884192	4251397
0.0016695685911265925	5257561
0.0011082999588016853	11396222
9.94255559090909E-4	11618549
7.50773214487942E-4	2621806
...
```
Using this file [titles-sorted.7z](http://haselgrove.id.au/wikipedia/20160305/titles-sorted.7z) i can find the page title that corresponds to each node, for the previous results we obtain:
```
H:S
Wikilink
Hyperlink
Diacritical_mark
Diacritic
Geographic_coordinate_system
International_Standard_Book_Number
United_States
Virtual_International_Authority_File
Common_name
 ```
