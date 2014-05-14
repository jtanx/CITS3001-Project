package threes;

import java.util.Arrays;
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
  
  //private int[] factors = {0,1,3,1};
  //private int[] factors = {5,4,2};
  private int[] factors = {12,1,2};
  private int evaluate(Board b, int[] s) {
    return (int)(Math.pow(4, b.dof()) + factors[0] * b.zeros() + 
           factors[1] * b.checkerboarding() + 
           factors[2] * b.gthree());
  }
  
  public void learn_factors(Board b, int[] s) {
      int[] best = new int[3];
      int best_score = -1;
      Board best_board = null;
      float pc = 0;
      for (int i = 3; i < 15; i++) {
          for (int j = 1; j < 15; j++) {
              for (int k = 0; k < 15; k++) {
                    factors[0] = i; factors[1] = j;
                    factors[2] = k; 

                    Board n = solve_idfs(s, b);
                    int score = n.score();
                    if (score > best_score) {
                        System.arraycopy(factors, 0, best, 0, best.length);
                        best_score = score;
                        best_board = n;
                    }
              }
              pc += 15.0/(15*15*15);
              System.out.printf("%.2f%%\n", pc*100);
          }
          if (best_board != null) {
              System.out.println(best_board);
              System.out.println(best_board.score());
          }
          System.out.printf("%d,%d,%d\n", best[0], best[1], best[2]);
      }
      
      System.out.printf("%d,%d,%d\n", best[0], best[1], best[2]);
  }
  
  
  private Board solve_idfs(int[] s, int depthLimit, Board b, int depth) {
    Direction[] directions = {Direction.LEFT, Direction.UP, 
                              Direction.RIGHT, Direction.DOWN};
    if (depth >= depthLimit) { //Cutoff test
      return b;
    }
    
    Board best = null;
    int best_score = -1;
    int considered = 0;
    
    for (int i = 0; i < BOARD_WIDTH; i++) {
      Board next = new Board(b);
      if (next.move(s, directions[i])) {
        Board candidate = solve_idfs(s, depthLimit, next, depth + 1);

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
        considered++;
      }
    }
    
    if (considered == 0) { //Terminal state: Cannot move in any direction
      b.finished(true);
      best = b;
    }
    return best;
  }
  
  public Board solve_idfs(int[] s, Board b) {
    while (b != null && !b.finished()) {
      b = solve_idfs(s, MAX_DEPTH, b, 0);
      if (b != null) {
        //System.out.println(b);
        //System.out.println(b.score());
      }
    }
    return fbest;
  }
  
}
