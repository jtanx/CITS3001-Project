#!/usr/bin/env python3
'''
    Determines the tile proportions as a percentage for a given board.
'''

import sys,os,re

def main(args):
    if len(args) < 2:
        print("Usage: %s board boards..." % args[0])
        return

    for fn in args[1:]:
        with open(fn) as fp:
            #Skip first 8 lines
            lines = fp.readlines()[7:]
            
        num = []
        for line in lines:
            num += [int(x) for x in re.split("[^0-9]", line) if x]

        last = -1
        cts = 0
        maxCts = 0
        maxTile = 0
        maxCount = 0
        count = 0
        props = {}
        for n in num:
            count += 1
            if n == last:
                cts += 1
            else:
                if cts > maxCts:
                    maxCts = cts
                    maxTile = last
                    maxCount = count
                cts = 0
            last = n
            if n not in props:
                props[n] = 1
            else:
                props[n] += 1

        print("Tile count: %d" % len(num))
        for k in sorted(props):
            print("%d, %.1f" % (k, (100 * props[k]) / len(num)))
        print("Max sequence: %d (Tile %d at %d)" % \
              (maxCts + 1, maxTile, maxCount - maxCts - 1))
        #print(num[maxCount - maxCts - 2 : maxCount + maxCts + 10])
        

main(sys.argv)
