package threes;

import threes.Board.Direction;

/**
 *
 * @author
 */
public class Solver {
  private static final int MAX_DEPTH = 8;
  private static final int BOARD_WIDTH = Board.BOARD_WIDTH;
  //private static final int BOARD_SPACE = Board.BOARD_SPACE;
  
  
  private int evaluate(Board b, int[] s) {
    
    return b.score();
  }
  
  private Board solve_idfs(int[] s, int depthLimit, Board b, int depth) {
    Direction[] directions = {Direction.LEFT, Direction.UP, 
                              Direction.RIGHT, Direction.DOWN};
    if (depth >= depthLimit) { //Cutoff test
      return b;
    }
    
    Board best = b;
    int best_score = evaluate(best, s);
    int considered = 0;
    
    for (int i = 0; i < BOARD_WIDTH; i++) {
      Board next = new Board(b);
      if (next.move(s, directions[i])) {
        Board candidate = solve_idfs(s, depthLimit, next, depth + 1);
        int score = evaluate(candidate, s);
        if (score > best_score) {
          best_score = score;
          best = candidate;
        }
        considered++;
      }
    }
    
    if (considered == 0) { //Terminal state: Cannot move in any direction
      best.finished(true);
    }
    return best;
  }
  
  public Board solve_idfs(int[] s, Board b) {
    while (!b.finished()) {
      b = solve_idfs(s, MAX_DEPTH, b, 0);
      System.out.println(b);
      System.out.println(b.score());
    }
    return b;
  }
  
}
