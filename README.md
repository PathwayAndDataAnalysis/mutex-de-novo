# Mutual exclusivity analysis for de novo mutation datasets

This project searches for mutual exclusivity in the given mutation matrix, for the given list of gene sets.

## Dependencies
Java JDK v1.8
Maven v3.0
Python v3
Snakemake v3.13

## Copy

```
git clone https://github.com/PathwayAndDataAnalysis/mutex-de-novo.git
```

## Build

```
cd mutex-de-novo
mvn compile
mvn assembly:single
```
The last command will generate the `mutex-de-novo.jar` file under the `target` directory. Feel free to move this jar into a convenient location to use in your analyses.

## Analysis inputs

### Mutation matrix

Prepare the mutation matrix as a tab-delimited text file. Example:

|  |Sample1|Sample2|Sample3|Sample4|
|---|---|---|---|---|
|Gene1|0 |1 |0 |1
|Gene2|1 |1 |0 |0
|Gene3|0 |1 |1 |0

Use the HGNC Symbol for the genes and make sure each sample name is unique.

### Gene sets file

Prepare the gene sets file as a two-column, tab-delimited text file. The first column should have a unique name or ID for the gene set. The second column should have the genes separated with a space. Example:

<table>
    <tr>
        <td>Set1</td>
        <td>Gene1 Gene2 Gene3</td>
    </tr>
    <tr>
        <td>Set2</td>
        <td>Gene4 Gene5 Gene1 Gene3</td>
    </tr>
    <tr>
        <td>Set3</td>
        <td>Gene2 Gene5 Gene7</td>
    </tr>
</table>

Use the HGNC Symbol for the genes and make sure each set name is unique.

## Execution
Assume you have the following in your current directory.

`mutex-de-novo.jar:` The jar file that was previously generated.<br>
`matrix.txt:` The tab-delimited mutation matrix as described above.<br>
`gene-sets.txt:` The tab-delimited gene sets file as described above.

Run mutex-de-novo using the command below:
```
java -jar mutex-de-novo.jar calculate matrix.txt gene-sets.txt output-directory 1000
```
Here, `output-directory` is the desired name for the output directory that will be generated during execution. `1000` is the randomization parameter that will be directly proportional to the run time. Use a small value, like `10`, for testing, and use a large value, like `10000` for actual analysis.
