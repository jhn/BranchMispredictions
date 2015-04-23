# BranchMispredictions

### Introduction
An implementation of the algorithm described in [Selection Conditions in Main
Memory](http://www.cs.columbia.edu/~kar/pubsk/selcondsTODS.pdf) by Ross. This
algorithm finds a query plan with optimal cost from an initial list of functions
with estimated costs. It does this by taking into account the costs of branch
misprediction.

### Description
First we parse both the query file and the configuration file. We setup the
possible subsets and selectivities. A simple list of booleans that emulated a
bitset was used to keep track of the positions of each selectivity. Once we have
each of the subsets created from the selectivities, we apply algorithm 4.11 from
the paper and print out a final, optimal query plan.

The code contains some unit tests that were used to test low level functionality
like algebraic operations on bitsets.

### Language
This implementation is written in Java 8, and makes use functional constructs to
make the code elegant, less verbose and easy to follow.

The code is (mostly) optimized for readability, has decent documentation and
variable naming.

Each query optimization can be easily executed in its own thread to speed up
computation speed.

