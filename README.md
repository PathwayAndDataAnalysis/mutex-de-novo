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
```
  S1  S2  S3  S4
G1  0 1 0
G2  1 0 0
G3  0 1 1
```
