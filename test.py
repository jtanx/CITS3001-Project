import sys, os, re


def pb(i,j):
    for k in range(4):
        for l in range(4):
            if (i == k and j == l):
                print("|", end=" ")
            else:
                print("-", end=" ")
        print()

def pb2(i, j):
    #print("%d" % (i * 4 + j), end = " ")
    print("(%d, %d)" % (i, j), end = " ")

def test(dirn='L'):
    if dirn == 'L':
        init = [0,0]
        m = [[1,0],[0,1]]
    elif dirn == 'R':
        init = [3,3]
        m = [[-1,0],[0,-1]]
    elif dirn == 'U':
        init = [0,0]
        m = [[0,1],[1,0]]
    elif dirn == 'D':
        init = [3,3]
        m = [[0,-1],[-1,0]]
    for i in range(0,4):
        for j in range(0,4):
            k = init[0] + i * m[0][0] + j * m[0][1]
            l = init[1] +  i * m[1][0] + j * m[1][1]
            pb2(k,l)
            print(end=" ")
