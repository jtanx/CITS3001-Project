package threes;

import java.util.Stack;

/**
 * POS PSO
 * @author
 */
public class ida {
  private static int BOARD_WIDTH = Board.BOARD_WIDTH;
  private static int BOARD_SPACE = Board.BOARD_SPACE;
  
  private static boolean okay(Board b) {
    return b.dof() >= 3 && b.nCombinable() > 10;
  }
  
  public static Board solve_mdfs(int[] s, Board b) {
    Board.Direction[] directions = {Board.Direction.LEFT, Board.Direction.UP, 
                              Board.Direction.RIGHT, Board.Direction.DOWN};
    Stack<Board> nt = new Stack<Board>();
    Board best = null;
    int best_score = -1;
    nt.push(b);
    
    while (!nt.isEmpty()) {
      Board cur = nt.pop();
      
      for (int i = 0; i < BOARD_WIDTH; i++) {
        Board next = new Board(cur);
        boolean ret = next.move(s, directions[i]); 
        if (next.finished()) {
          int score = next.score();
          if (score > best_score) {
            best_score = score;
            best = next;
            System.out.println(best);
            System.out.println(score);
          }
        } else if (ret && ida.okay(next)) {
          nt.push(next);
        }
      }
    }
    
    return best;
  }
}
