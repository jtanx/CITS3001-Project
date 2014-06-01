#!/usr/bin/env python3
'''
Extracts the results for part 2
'''
import sys,os,re
import numpy, matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter
from matplotlib import rc

font = {'family' : 'serif'}
rc('font', **font)
rc('axes', color_cycle=['r', 'g', 'b', 'c', 'm', 'y', 'k', '#bb11bb'])

boards = ["exampleinput.txt", "B1.txt", "short-range.txt", "medium-range.txt",
          "long-range.txt", "longboard1.txt", "longboard2.txt",
          "longboard3.txt"]


#Each line: filename, lookahead, score, usedTiles, nTiles, speed
ret = []
yaxis1 = []
yaxis2 = []
yaxis3 = []
for board in boards:
    yax1 = []
    yax2 = []
    yax3 = []
    print(board)
    for i in range(1, 9):
        retl = [board, i]
        fn = "".join(["out-", "a", str(i), "-", board])
        fn = os.path.join("results-part2", fn)
        
        with open(fn) as fp:
            lines = fp.readlines()[:2]
            score = re.match("^(\d+)[^\d].*$", lines[0]).group(1)
            tileUsage = re.search("(\d+)/(\d+)", lines[1]).groups()
            tileUsage = (100.0 * int(tileUsage[0])) / int(tileUsage[1])
            speed = re.search("\((\d+.\d+) m/s\)", lines[1]).group(1)
            retl.append(score)
            retl.append(tileUsage)
            retl.append(speed)

            yax1.append(speed)
            yax2.append(tileUsage)
            yax3.append(score)
            ret.append(retl)
            print(tileUsage)
    yaxis1.append(yax1)
    yaxis2.append(yax2)
    yaxis3.append(yax3)

xaxis = range(1,9)
markers = ['|', 'o', 'd', '^', 'x', '*', 's', 'p']
for i in range(len(yaxis1)):
    plt.plot(xaxis, yaxis1[i], label=boards[i], \
             marker = markers[i])

handles, labels = plt.gca().get_legend_handles_labels()
plt.gca().legend(handles, labels, loc=1,prop={'size':10})
plt.gca().set_yscale('log')
plt.xlabel("Lookahead depth")
plt.ylabel("Speed (moves/second)")
plt.title("Comparison of solving speed to lookahead depth")
plt.show()

plt.clf()
plt.close()

for i in [3, 4, 6,7]:
    plt.plot(xaxis, yaxis2[i], label=boards[i], \
             marker = markers[i])

handles, labels = plt.gca().get_legend_handles_labels()
plt.gca().legend(handles, labels, loc=2,prop={'size':10})
#plt.gca().set_yscale('log')
plt.xlabel("Lookahead depth")
plt.ylabel("Tile usage (%)")
plt.title("Comparison of tile usage to lookahead depth")
plt.show()


plt.clf()
plt.close()

for i in range(len(yaxis3)):
    plt.plot(xaxis, yaxis3[i], label=boards[i], \
             marker = markers[i])

handles, labels = plt.gca().get_legend_handles_labels()
plt.gca().legend(handles, labels, loc=2,prop={'size':6})
plt.gca().set_yscale('log')
plt.xlabel("Lookahead depth")
plt.ylabel("Final score")
plt.title("Comparison of board score to lookahead depth")
plt.show()

print(ret)
            
            
