package threes;

import threes.Board.Direction;

/**
 *
 * @author
 */
public class Solver {
  private static final int MAX_DEPTH = 8;
  private static final int BOARD_WIDTH = Board.BOARD_WIDTH;
  private Board fbest = null;
  private int fbest_score = -1;
  
  //Zeros, checkerboarding2/3, gthree, smoothness
  //private int[] factors = {18, 0, 6, 10}; //Gets 187004
  //private int[] factors = {18, 1, 11, 9}; //Gets  186920, 593529 for lb2
  //private int[] factors = {18,1,2,4}; //Gets ~204k on lb1 using cb3, but only ~200k for lb2
  //Zeros, smoothness, gthree, lowuncombo,
  //private int[] factors = {18, 9, 11, 1 }; //~266k on lb3 using cb2
  private int[] factors = {18, 10, 10, 12};
  
  

  private int evaluate(Board b, int[] s) {
    if (b.dof() != b.dof2()) {
      System.out.printf("WTF");
      
    }
    b.nMoves();
    return (int)(Math.pow(4, b.dof()) + factors[0] * b.zeros() + 
           factors[1] * b.gthree() + 
            factors[2] * b.smoothness() + 
            factors[3] * b.checkerboarding2());
  }
  
  public void learn_factors(Board b, int[] s) {
      int[] best = new int[factors.length];
      int best_score = -1;
      Board best_board = null;
      float pc = 0;
      for (int i = 18; i < 19; i++) {
          for (int j = 10; j < 19; j++) {
              for (int k = 7; k < 14; k++) {
                for (int l = 0; l < 14; l++) {
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
                System.out.printf("%.2f%%\n", pc * 100);
                if (best_board != null) {
                    System.out.println(best_board);
                    System.out.println(best_board.score());
                }
                for (int v : best) {
                    System.out.printf("%d ", v);
                }
                System.out.println();
              }
              
            System.out.println("Currently at: (not best)");
            for (int v : factors) {
              System.out.printf("%d ", v);
            }
            System.out.println();
          }
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
        if (candidate == null && next.finished()) {
          candidate = next;
        }
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
