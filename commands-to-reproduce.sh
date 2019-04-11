#!/bin/bash

# Randomization amount to use for matrix shuffling
ITER="10000"

# Prefix of the command to run mutex-de-novo
CMD="java -jar mutex-de-novo.jar"

# FDR cutoffs to use in exploring the result sizes
FDRS="0.01 0.05 0.1 0.2 0.3 0.4 0.5"

# The next 10 runs have high execution time. They can run in parallel on a cluster.

# 1. Generate original results on SFARI genes
$CMD calculate yuen-turner-autism SFARI SFARI-original $ITER

# 2. Generate original results on Reactome pathways.
$CMD calculate yuen-turner-autism Reactome Reactome-original $ITER

# 3,4. Use only introns
$CMD calculate yuen-turner-autism-intron SFARI SFARI-intron $ITER
$CMD calculate yuen-turner-autism-intron Reactome Reactome-intron $ITER

# 5,6. Use only non-introns
$CMD calculate yuen-turner-autism-not-intron SFARI SFARI-not-intron $ITER
$CMD calculate yuen-turner-autism-not-intron Reactome Reactome-not-intron $ITER

# 7,8.Yuen only
$CMD calculate yuen-autism SFARI SFARI-Yuen $ITER
$CMD calculate yuen-autism Reactome Reactome-Yuen $ITER

# 9,10. Replace the autism samples from Turner dataset with equal amounts of control samples
$CMD calculate yuen-autism-turner-control SFARI SFARI-Yuen-autism-Turner-control $ITER
$CMD calculate yuen-autism-turner-control Reactome Reactome-Yuen-autism-Turner-control $ITER

# The rest need to be executed after the above runs finish, and these have low execution time.

# 11. Explore the result size of Reactome pathways under different filtering conditions and FDR cutoff
$CMD explore-significance-in-results Reactome-original/results.txt Reactome-original/results-significance-explored.txt mutex $FDRS

# 12. Do the same for Yuen-only run
$CMD explore-significance-in-results Reactome-Yuen/results.txt Reactome-Yuen/results-significance-explored.txt mutex $FDRS

# 13. Do the same for the run where Turner controls used instead of autism samples
$CMD explore-significance-in-results Reactome-Yuen-autism-Turner-control/results.txt Reactome-Yuen-autism-Turner-control/results-significance-explored.txt mutex $FDRS

# 14. Generate table for pathway results for testing highest mutated 50 pathways
$CMD filter-results-to-most-hit Reactome-original/results-with-names.txt Reactome-original/results-top50.txt mutex 50

# 15. Generate a table for Circadian Clock genes
$CMD annotate-set-members Reactome-original/R-HSA-400253-mutex.txt yuen-turner-autism Reactome-original/results-circadian-members-table.txt

# 16. Generate a table for PI3K/AKT Signaling genes
$CMD annotate-set-members Reactome-original/R-HSA-1257604-mutex.txt yuen-turner-autism Reactome-original/results-PI3K-members-table.txt
