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
  //private static final int BOARD_SPACE = Board.BOARD_SPACE;
  
  
  private int evaluate(Board b, int[] s) {
    return (int)(b.score() + b.dof() * 5 + b.zeros() * 6 + 
            b.monotonicity() * 3 - b.checkerboarding() * 4);
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
        System.out.println(b);
        System.out.println(b.score());
      }
    }
    return fbest;
  }
  
}
