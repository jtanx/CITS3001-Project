#!/usr/bin/env bash
#Place boards in the 'boards' folder.
mkdir -p out
mkdir -p logs

for x in $(seq 1 5); do
	for f in $(ls boards); do
		echo Lookahead $x, $f
		filename=$(basename "$f")
		opts="-u 220 -q 30000 -a $x"
		opts="$opts boards/$f -o out/out-a$x-$filename"
		
		java -jar threes.jar $opts 2>&1 | tee "logs/log-a$x-$filename"
	done;
done;

for x in $(seq 6 7); do
	for f in $(ls boards); do
		echo Lookahead $x, $f
		filename=$(basename "$f")
		opts="-u 220 -q 20000 -a $x"
		opts="$opts boards/$f -o out/out-a$x-$filename"
		
		java -jar threes.jar $opts 2>&1 | tee "logs/log-a$x-$filename"
	done;
done;

for f in $(ls boards); do
	echo Lookahead 8, $f
	filename=$(basename "$f")
	opts="boards/$f -o out/out-a8-$filename"
	
	java -jar threes.jar $opts 2>&1 | tee "logs/log-a8-$filename"
done;
