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
  //private int[] factors = {18, 11, 5, 2}; //Maxes out lb2, pretty good for b1, pretty crap for exampleinput, 190k for lb1
  //zeros, checkerboarding3, smoothness, gtaverage
  //private int[] factors = {18, 2, 2, 9}; //holy shit using gtaverge 
  //zeros, checkerboarding3, smoothness, ncombinable
  //private int[] factors = {18, 1, 3, 10}; //hmm quite good, using ncombinable
  //private int[] factors = {18,2,2,9}; //The best
  private int[] factors = {18,6,2,0};
  //private int[] factors = {18,2,3,10}; //Modified factors to continue after 18,2,2,9 fails
//private int[] factors = {18, 5, 10, 9}; //ncombinable not to confuse with gta
  //private int[] factors = {18,2,1,8}; //ncombinable2
  //private int[] closefactors = {18, 3, 1, 9}; //nc2 763 for eg1, 1012 for b1
  //private int[] closefactors = {18, 5, 1, 10}; //nc1 559 569 5610
  //private int[] closefactors = {18, 5, 6, 10}; //nc1 559 569 5610 - 763
  private int[] closefactors = {18, 5, 10, 9}; //nc1 559 569 5610 - oh jackpot
  //private int[] closefactors = {18, 5, 10, 10}; //nc1 559 569 5610 - oh jackpot
  //private int[] closefactors = {18, 5, 11, 9}; //nc1 559 569 5610 - oh jackpot
  //private int[] closefactors = {18, 7,8,11}; //Oh jackpot, but not for lb2
  //private int[] closefactors = {18, 7,9,11}; //Oh jackpot, but not for lb2
  //private int[] closefactors = {18, 7,9,12}; //Oh jackpot, but not for lb2
  //18,4,7,9 for nc1
  //TODO:
  //Edge case: As we're approaching the end of a sequence, try to maximise score...
  //Possible change to ncombinable: Weight combinables that increase the score significantly
  private int evaluate(Board b, int[] s) {
    int[] thefactors = factors;
    if (b.dof() != b.dof2()) {
      System.out.printf("WTF");
    }
    
    //if (false) {
    //We are close to the end of the sequence! Use different weights!
    if (b.nMoves() + MAX_DEPTH * 2 >= s.length) {
      //System.out.println(b.nMoves());
      return (int)(Math.pow(4, b.dof()) + closefactors[0] * b.zeros() + 
              closefactors[1] * b.checkerboarding3() +
              closefactors[2] * b.smoothness() +
              closefactors[3] * b.nCombinable());
    }
    return (int)(Math.pow(4, b.dof()) + factors[0] * b.zeros() + 
           factors[1] * b.checkerboarding3() + 
            factors[2] * b.smoothness() + 
            factors[3] * b.nCombinable());
  }
  
  public void learn_factors(Board b, int[] s) {
      int[] best = new int[factors.length];
      int best_score = -1;
      Board best_board = null;
      
      for (int i = 18; i < 19; i++) {
          for (int j = 0; j < 19; j++) {
              for (int k = 0; k < 14; k++) {
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
                    System.out.printf("Current score: %d (%d moves)\n", 
                            score, n.nMoves());
                    for (int v : factors) {
                      System.out.printf("%d ", v);
                    }
                    System.out.println();
                }
                if (best_board != null) {
                    System.out.println("Current best:");
                    System.out.println(best_board);
                    System.out.printf("%d (%d moves)\n", 
                            best_board.score(), best_board.nMoves() );
                }
                for (int v : best) {
                    System.out.printf("%d ", v);
                }
                System.out.println();
              }
          }
      }
    for (int v : best)
      System.out.printf("%d ", v);
    System.out.println();
  }
  
  public void learn_cfactors(Board b, int[] s) {
      int[] best = new int[closefactors.length];
      int best_score = -1;
      Board best_board = null;
      
      for (int i = 18; i < 19; i++) {
          for (int j = 1; j < 19; j++) {
              for (int k = 1; k < 14; k++) {
                for (int l = 1; l < 14; l++) {
                    closefactors[0] = i; closefactors[1] = j;
                    closefactors[2] = k; closefactors[3] = l; 

                    Board n = solve_idfs(s, b);
                    int score = n.score();
                    if (score > best_score) {
                        System.arraycopy(closefactors, 0, best, 0, best.length);
                        best_score = score;
                        best_board = n;
                    }
                    System.out.printf("Current score: %d (%d moves)\n", 
                            score, n.nMoves());
                    for (int v : closefactors) {
                      System.out.printf("%d ", v);
                    }
                    System.out.println();
                }
                if (best_board != null) {
                    System.out.println("Current best:");
                    System.out.println(best_board);
                    System.out.printf("%d (%d moves)\n", 
                            best_board.score(), best_board.nMoves() );
                }
                for (int v : best) {
                    System.out.printf("%d ", v);
                }
                System.out.println();
              }
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
    Board input = b;
    fbest_score = -1;
    fbest = null;
    while (b != null && !b.finished()) {
      b = solve_dfs(s, MAX_DEPTH, b, 0);
      if (b != null) {
        //System.out.println(b);
        //System.out.println(b.score());
        //System.out.println(b.nMoves());
        //Thought: What if we stored say the past 5 such moves,
        //And if we hit a dead end, we backtrack and use another heuristic????
      }
    }
    
    //Super edge-case: The input board can't be moved...
    return fbest == null ? input : fbest;
  }
  
  public Board solve_mdfs(int[] s, Board b) {
    Board[] ringbuffer = new Board[2];
    Board current = b;
    char p = 0;
    
    fbest_score = -1;
    fbest = null;
    while (current != null && !current.finished()) {
      current = solve_dfs(s, MAX_DEPTH, current, 0);
      if (current != null) {
        ringbuffer[p] = current;
        p = (char)(1 - p);
      } else if (ringbuffer[1 - p] != null) {
        current = ringbuffer[1 - p];
        ringbuffer[1 - p] = null;
        p = (char)(1 - p);
      }
    }
    
    return fbest == null ? b : fbest;
  }
  
}
