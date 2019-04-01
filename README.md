# PageRank

## Introduction
PageRank is a way of measuring the importance of website pages. The Pagerank algorithm looks at links from page A to page B, if a link exists, this is interpreted as a "vote" (page B is voting for page A). 

### How we can use PageRank?
The Web is a graph, a set of nodes corresponds to the various pages and the hyperlinks are the arcs.

We start from a random page then:
* Randomly pick an outgoing link with probability λ
* Randomly jump into a page with probability 1-λ

The PageRank of page x Pr(x) is the probability of being on page x at a random moment in time.

### Algorithm
PageRank can be computed either iteratively or algebraically. In the iterative method we start initializing the PageRank of every node:

![](http://latex.codecogs.com/gif.latex?PR%28x%29%3D%5Cfrac%7B1%7D%7BN%7D%5Cquad%20%5Cforall%20x%20%5Cquad%20N%3Anumber%5C%2Cof%5C%2Cnodes%5C%2Cin%5C%2Cthe%5C%2Cgraph)

At each step we compute the PageRank as:

![](http://latex.codecogs.com/gif.latex?PR%28x%29%3D%5Cfrac%7B1-%5Clambda%7D%7BN%7D&plus;%5Clambda%20%5Csum_%7By%20%5Crightarrow%20x%7D%5Cfrac%7BPr%28y%29%7D%7Bout%28y%29%7D%5Cquad%5Cforall%20x)

PR(x) corresponds to the probability to be ant node x at the current moment. I can get there in 2 ways, on the previous step I flip a coin and :

* if I get tail (1-λ), I perform a random hop and go to x with  ![](http://latex.codecogs.com/gif.latex?Pr%3D%5Cfrac%7B1%7D%7BN%7D)
* if I get head (λ), the only way to go to x is to be in a node y which points to x and select that arc that connects y to x with ![](http://latex.codecogs.com/gif.latex?Pr%3D%5Cfrac%7B1%7D%7Bnumber%5C%2Cof%5C%2Coutgoing%5C%2Clinks%7D%5Cquad)
&nbsp; y is not the only node that points to x so I have to sum up over all the nodes that points to x.

We procede in a new iteration if &nbsp; ![](http://latex.codecogs.com/gif.latex?%5Cleft%20%7CPR%28t&plus;1%29-PR%28t%29%20%5Cright%20%7C%3C%20%5Cepsilon)<br/>
We have also to pay attenttion to dangling nodes: nodes in the graph with no outgoing edges.
If werun the simplified PageRank algorithm on a graph with this nodes the total PageRank mass will be not conserved. So we have to redistribute the
"lost" mass on dangling nodes across all the nodes in the graph.
### Input Files
I’ve decided to considering a smaller one as the Wikipedia Graph. Wikimedia Foundation releases data
dumps of Wikipedia and all Wikimedia Foundation projects on a regular basis. Is possible to download the latest dumps
(for the last year) here. I have used two sql files, wikidatawiki-latest-page.sql.gz and wikidatawiki-latest-pagelinks.sql.gz.
The latter contains page-to-page link lists the former, instead contains info about each page.
## Parsing
First of all I have used this Python script to "convert" the two .sql file to a more readable .csv format. Than I have
implemented a Hadoop MapReduce application to parse the .csv file.
Page Parsing: is a MapReuce job in which taken as input enwiki-latest-page.csv and extract the two relevat fields page id
and page title.
Pagelink Parsing: is a bit more complex with respect to the Page Parsing. This is due to the fact that each Wikipedia
Pagelink table contains the page id of the linked page as integerand the page title of the target page as text so I have to
perform an additional "mapping" from the page title to the corresponding page id
## Pagerank Calculation
My PageRank implementation divide the entire work in tree phases: checks for dangling nodes, calculating and sorting.
### Check for Dangling Nodes
This first phase take as input the file produced by PageLink_parser and simply checks if the adjacency list of each node is
empty, and if it is true, it keeps track of the node’s PageRank value using a Counter SINK. At the end of this first Map
SINK will contain how much PageRank was lost at the dangling nodes
### Calculating PageRank
In this second phase the Mapper take as input the file produced by PageLink_parser and:
* Emits a pair key value in which the key is the node id and the value is the Pairwritable containing PR(x) and the
list of adjacencies
*Emits for every items into the adjacency list a pair key value in which the key is the node id and the value is a
Pairwritable in which the PageRank is the one of the starting node divided by the number of items into the
adjacency list (which is the number of outgoing links from the starting node), and the adjacency list is empty.
The reduce phase takes all the sorted pair key value and checks the boolean variable isNode:
* If it is true this means that the entry represents a node, so the reducer use the values of node id and adjacency
list to reconstruct the graph.
* If it is false this means that the entry represents a Pagerank information, so the reducer sum the partial pagerank
contained into val . getPagerank() to the variable sum for computing the final pagerank.
Subsequently the final pagerank is computed using the formula.

