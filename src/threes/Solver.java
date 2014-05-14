package threes;

import threes.Board.Direction;

/**
 *
 * @author
 */
public class Solver {
  private static final int MAX_DEPTH = 9;
  private static final int BOARD_WIDTH = Board.BOARD_WIDTH;
  private Board fbest = null;
  private int fbest_score = -1;
  
  private double[] factors = {3,4,1,9};
  //private int[] factors = {12,1,2,2};
  /*
   * 62448, 0, 0, 11, 15
RDDDRDLRDDLRDLRLRRRDDDRRURDRDLRDLUDUDDRDLDDDDLRDLUDRDDRRLRDURDRRRURDLDRDUDDRRLRUDLLDRDLDLUULUDRDLULDLRDRUDRLRDRDLDRDDRRRUDRDDRRRDDURDLRDLDRRLDRDDRRURDRRLUDRDRURDRURUDRDDLUDURURUUDUDDRRULDRLRDDLDRUURRRRDLRUURDRUDRDURRDULRDLDRDDRURRDRUDRURUDRLLLRDDRRLLRDURRDLLLDRRDDDLDRUDRDLRLRDLDLUDDDRDDRRDLRRURDLRUDLDRRULRLRDDRUDLULRDDRRDDLDRDLRDULRDRLULRUDDRRRUUDRRRRRLRULDRLRRRDDDRRDLURRDRRRDDLDDRDLDLLRDDLRRLDRDDDDDLUDDDLUDLLDRDRURUDDRRRLRDDRDDUDRUDLLDRULDDLDRRUUDRDRRLRRRUDRRLRDRDRRRRLRURURDURULUURDDDULDRUDDRULRDRDUULRDURDRURRDUDRLURRRDDRURUURDURUURURDRDURDULLUDRURURURUUUURDDRRDDUULURDRUURUDRDLLUDRDRRLRRLRDLDLDRLRRDRLDURDRDRLRLRLDDDLLLURDRRDRUDRLRDUULDRRDDUUURRDDDURDRLDDRUDLRDDRRDDRDLRDDRRLRDUDRLLDRUDDRRLLLRUUUDUU
Used 707/20000 available moves.
   */
  private int evaluate(Board b, int[] s) {
    return (int)(Math.pow(4, b.dof()) + factors[0] * b.zeros() + 
           factors[1] * b.checkerboarding() + 
           factors[2] * b.gthree() + 
            factors[3] * b.smoothness());
  }
  
  public void learn_factors(Board b, int[] s) {
      int[] best = new int[3];
      int best_score = -1;
      Board best_board = null;
      float pc = 0;
      for (int i = 3; i < 15; i++) {
          for (int j = 1; j < 15; j++) {
              for (int k = 0; k < 15; k++) {
                for (int l = 0; l < 15; l++) {
                    factors[0] = i; factors[1] = j;
                    factors[2] = k; factors[3] = l; 

                    Board n = solve_idfs(s, b);
                    int score = n.score();
                    if (score > best_score) {
                        System.arraycopy(factors, 0, best, 0, best.length);
                        best_score = score;
                        best_board = n;
                    }
                }
                pc += 15.0/(15*15*15);
              }
              System.out.printf("%.2f%%\n", pc*100);
          }
          if (best_board != null) {
              System.out.println(best_board);
              System.out.println(best_board.score());
          }
          for (int v : best)
            System.out.printf("%d ", v);
          System.out.println();
      }
    for (int v : best)
      System.out.printf("%d ", v);
    System.out.println();
  }
  
  
  private Board solve_dfs(int[] s, int depthLimit, Board b, int depth) {
    Direction[] directions = {Direction.LEFT, Direction.UP, 
                              Direction.RIGHT, Direction.DOWN};
    if (depth >= depthLimit) { //Cutoff test
      return b;
    }
    
    Board best = null;
    int best_score = -1;

    for (int i = 0; i < BOARD_WIDTH; i++) {
      Board next = new Board(b);
      if (next.move(s, directions[i])) {
        Board candidate = solve_dfs(s, depthLimit, next, depth + 1);
        
        if (candidate != null) {
          if (candidate.finished()) {
            int score = candidate.score();
            if (score > fbest_score) {
              fbest_score = score;
              fbest = candidate;
            }
          } else {
            int score = evaluate(candidate, s);
            if (score > best_score ) {
              best_score = score;
              best = candidate;
            }
          }
        }
      }
    }
    return best;
  }
  
  private Board[] solve_idfs(int[] s, int depthLimit, int depth, Board... b) {
    Direction[] directions = {Direction.LEFT, Direction.UP, 
                              Direction.RIGHT, Direction.DOWN};
    if (depth >= depthLimit) { //Cutoff test
      return b;
    }
    
    Board[] best = new Board[b.length];
    int[] best_score = new int[b.length];
    int considered = 0;
    
    for (int i = 0; i < b.length; i++)
      best_score[i] = -1;
    
    for (int bc = 0; bc < b.length; bc++) {
      if (b[bc] == null)
        continue;
      
      for (int i = 0; i < BOARD_WIDTH; i++) {
        Board next = new Board(b[bc]);
        if (next.move(s, directions[i])) {
          Board[] nn = {next, null};
          Board[] candidates = solve_idfs(s, depthLimit, depth + 1, nn);
          
          for (Board candidate : candidates) {
            if (candidate != null) {
              if (candidate.finished()) {
                int score = candidate.score();
                if (score > fbest_score) {
                  fbest_score = score;
                  fbest = candidate;
                }
              } else {
                int score = evaluate(candidate, s);
                for (int ci = 0; ci < b.length; ci++) {
                  if (score > best_score[ci]) {
                    best_score[ci] = score;
                    best[ci] = candidate;
                    break;
                  }
                }
              }
            }
          }
          considered++;
        }
      }

      if (considered == 0) { //Terminal state: Cannot move in any direction
        b[bc].finished(true);
        int score = b[bc].score();
        if (score > fbest_score) {
          fbest_score = score;
          fbest = b[bc];
        }
      }
    }
    
    return best;
  }
  
  public Board solve_idfs2(int[] s, Board b) {
    Board[] r = {b, null};
    while ((r = solve_idfs(s, MAX_DEPTH, 0, r))[0] != null) {
      System.out.println(r[0]);
      System.out.println(r[0].score());
    }
    return fbest;
  }
  
  public Board solve_idfs(int[] s, Board b) {
    while (b != null && !b.finished()) {
      b = solve_dfs(s, MAX_DEPTH, b, 0);
      if (b != null) {
        System.out.println(b);
        System.out.println(b.score());
      }
    }
    return fbest;
  }
  
}
