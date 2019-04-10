# Mutual exclusivity analysis for de novo mutation datasets

This project searches for mutual exclusivity in the given mutation matrix, for the given list of gene sets.

## Copy

```
git pull 
```

## Build

```
cd mutex-de-novo
mvn compile
mvn assembly:single
```
The last command will generate the `mutex-de-novo.jar` file under the `target` directory. Put this jar to a convenient location for analysis.

## Analysis inputs

### Mutation matrix

Prepare the mutation matrix as a tab-delimited text file. Example:

|  |Sample1|Sample2|Sample3|Sample4|
|---|---|---|---|---|
|Gene1|0 |1 |0 |1
|Gene2|1 |1 |0 |0
|Gene3|0 |1 |1 |0

### Gene sets file

Prepare the gene sets file as a two-column tab-delimited text file. First column should have a unique name or ID for the gene set. Second column should have the genes separated with a space. Example:

|---|---|
|Set1|Gene1 Gene2 Gene3|
|Set2|Gene4 Gene5 Gene1 Gene3|
|Set3|Gene2 Gene5 Gene7|

Use the HGNC Symbol for the genes.
 
 
