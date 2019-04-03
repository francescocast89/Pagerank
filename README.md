# Pagerank
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

