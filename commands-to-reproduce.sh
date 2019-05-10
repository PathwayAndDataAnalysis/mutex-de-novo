#!/bin/bash

# Randomization amount to use for matrix shuffling
ITER="10000"

# Prefix of the command to run mutex-de-novo
CMD="java -jar mutex-de-novo.jar"

# FDR cutoffs to use in exploring the result sizes
FDRS="0.01 0.05 0.1 0.2 0.3 0.4 0.5"

# Runs mutual exclusivity test on SFARI genes and Reactome pathways
# param 1: data identifier
calculate () 
{
	# Test SFARI genes
	$CMD calculate $1 SFARI SFARI/$1 $ITER

	# Test Reactome pathways
	$CMD calculate $1 Reactome Reactome/$1 $ITER

	# Explore significances and thresholds on Reactome results
	$CMD explore-significance-in-results Reactome/$1/results.txt Reactome/$1/results-significance-explored.txt mutex $FDRS
}

# Prepares a table with the members of the specificed pathway
# param 1: data identifier
# param 2: pathway ID
# param 3: pathway name (no spaces or funky characters)
members()
{
	$CMD annotate-set-members Reactome/$1/$2-mutex.txt $1 Reactome/$1/results-$3-members-table.txt
}

# Yuen only
calculate yuen-autism

# Turner only
calculate turner-autism

# An only
calculate an-autism

# Yuen + An
calculate yuen-an-autism

# Only intron mutations from Yuen + An
calculate yuen-an-autism-intron

# Excluding intron mutations from Yuen + An
calculate yuen-an-autism-not-intron

# Yuen + Turner
calculate yuen-turner-autism

# Autism samples from Yuen, control samples from Turner
calculate yuen-autism-turner-control

# Autism samples from Yuen, control samples from An
calculate yuen-autism-an-control

# Table 9. Members of Circadian Clock pathway annotated for Yuen+An dataset
members yuen-an-autism R-HSA-400253 circadian

# Table 10. Members of Circadian Clock pathway annotated for Yuen dataset
members yuen-autism R-HSA-400253 circadian

# Table 11. Members of PI3K/AKT Signaling pathway annotated for Yuen + Turner datasets
members yuen-turner-autism R-HSA-1257604 PI3K

