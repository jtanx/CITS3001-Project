import sys,os,re

def main(args=sys.argv):
    if len(args) != 3:
        print("Usage: {0} board.in.txt sequence_slice".format(args[0]))
        print("Retrieves the sequences from the input board according to")
        print("a simplified slice format.")
        return

    ms = re.match("^(-?[0-9]*):?(-?[0-9]*)$", args[2])
    if not ms:
        print("Invalid slice notation: {0}".format(ms))
        return

    with open(args[1]) as fpi:
        ilines = fpi.readlines()
        if len(ilines) < 8:
            print("Invalid input format.")
            return

    moves = []
    for line in ilines[7:]:
        moves += [x for x in re.split("[^0-9]", line) if x]

    a = ms.group(1)
    b = ms.group(2)
    if not a and b:
        moves = moves[:int(b)]
    elif a and not b:
        moves = moves[int(a):]
    elif a and b:
        moves = moves[int(a):int(b)]

          #Derp
    fmt = " " + " ".join(moves[i] if (i+1) % 30 else str(moves[i]) + "\n" \
                   for i in range(len(moves)))
    print(fmt)
        
    
    return moves
    

main()
    
