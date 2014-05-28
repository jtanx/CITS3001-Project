#!/usr/bin/env bash
#Place boards in the 'boards' folder.
#opts="-v"
opts="-v -a 4 -u 300 -q 20000"
mkdir -p out
mkdir -p logs

for f in $(ls boards); do
	filename=$(basename "$f")
	opts="$opts boards/$f -o out/out-$filename"
	
	java -jar threes.jar $opts 2>&1 | tee "logs/log-$filename"
done
