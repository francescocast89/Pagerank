# PageRank

##Introduction
PageRank is a way of measuring the importance of website pages. The Pagerank algorithm looks at links from page A to page B, if a link exists, this is interpreted as a "vote" (page B is voting for page A). 

###How we can use PageRank?
The Web is a graph, a set of nodes corresponds to the various pages and the hyperlinks are the arcs.
We start from a random page than:
* Pick at random an outogoing link with probability λ
* Jump at random into a page with probability 1-λ

The PageRank of page x Pr(x) is the probability of being on page x at a random moment in time.

###Algorithm
We start initializing the PageRank of every node:
```
http://latex.codecogs.com/gif.latex?Pr%28x%29%3D%5Cfrac%7B1%7D%7BN%7D%5Cquad%5Cforall%20x  Where N is the number of nodes in the graph
```
Than we compute the PageRank
```
http://latex.codecogs.com/gif.latex?Pr%28x%29%3D%5Cfrac%7B1-%5Clambda%7D%7BN%7D&plus;%5Clambda%20%5Csum_%7By%20%5Crightarrow%20x%7D%5Cfrac%7BPr%28y%29%7D%7Bout%28y%29%7D%5Cquad%5Cforall%20x
```

##Computing PageRank using MapReduce
Given a graph, we are going to implement PageRank algorithm using Hadoop. We will need the following information:
