package threes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import threes.Board.Direction;
import static threes.Threes.log_info;

/**
 * Separating the AI from the game mechanics
 * 'Solves' the game using a depth-limited depth first search.
 * @author Jeremy Tan, 20933708
 */
public class Solver {
  private static final int MAX_DEPTH = 8;
  private static final int BOARD_WIDTH = Board.BOARD_WIDTH;
  private static final Direction[] directions = {
    Direction.LEFT, Direction.UP, Direction.RIGHT, Direction.DOWN
  };
  private static final int nThreads;
  static {
    int nt = Runtime.getRuntime().availableProcessors();
    nt = nt <= 1 ? 1 : nt > 4 ? 4 : nt;
    nThreads = nt;
  }
  
  private Board fbest = null;
  private int fbest_score = -1;
  private final int[] learning_starts = {18,0,0,0};
  
  private final int[] factors = {18,2,2,9}; //The best all-rounder
  private final int[] lb3factors = {18,1,2,13}; //Modified factors to continue after 18,2,2,9 fails for lb3
  private final int[] lb1factorsb = {18,2,1,8}; //Continuation of longboard1 but with backtrack 6.
  private final int[] nfactors = {18, 1, 5, 13}; //Found when testing for extension of medium-1 (900 moves ++)
  private final int[] lowfactors = {18,0,0,9}; //Quite a pathological case: Lots of ones --> Maximise local combinability, don't care about anything else.
  private final int[] lowfactors3 = {18,1,0,1}; 
  private final int[] closefactors = {18, 5, 10, 9}; //nc1 559 569 5610 - oh jackpot  
  //private final int[] lb3factors2 = {18,1,3,9};
  //private final int[] lb3factors2 = {18,2,2,12};
  private final int[] lb3factors2 = {18,3,2,13};
  private final int[] lb3factors3 = {18,2,1,6};
  private final int[][] choicefactors = {factors, lb3factors, lb3factors2, lb3factors3, lowfactors, lowfactors3};//, lb1factorsb};//, nfactors, lowfactors3, lowfactors};
  private int[] currentfactors = choicefactors[0];
  
  public Solver(int[] learning_startfactors) {
    log_info("Heuristic weights: %s", Arrays.deepToString(choicefactors));
    log_info("Reported number of processors: %d", Runtime.getRuntime().availableProcessors());
    log_info("No. of threads to be used for multithreaded versions: %d\n", nThreads);
    if (learning_startfactors != null) {
      if (learning_startfactors.length != learning_starts.length) {
        throw new IllegalArgumentException("Invalid learning factor size");
      }
      System.arraycopy(learning_startfactors, 0, 
              learning_starts, 0, learning_starts.length);
    }
  }
  
  public Solver() {
    this(null);
  }
  
  //Edge case: As we're approaching the end of a sequence, try to maximise score...
  //Possible change to ncombinable: Weight combinables that increase the score significantly
  private int evaluate(Board b, int[] s) {
    int[] thefactors = currentfactors;
    
    //We are close to the end of the sequence! Use different weights!
    if (b.nMoves() + MAX_DEPTH * 2 >= s.length) {
      //System.err.println(b.nMoves());
      thefactors = closefactors;
    }
    return ((int)Math.pow(4, b.dof())) + 
           thefactors[0] * b.zeros() + 
           thefactors[1] * b.checkerboarding3() + 
           thefactors[2] * b.smoothness() + 
           thefactors[3] * b.nCombinable();
  }
  
  /**
   * 'Learns' the factors that are good.
   * More of, just iterate over all possible heuristic weight combinations
   * and you have to observe which ones are good.
   * 
   * This learning does not utilise backtracking - it sticks to using one
   * weight only.
   * @param b The board to learn against
   * @param s The tile sequence to be used
   * @param learnClose Whether we are learning for the 'close' weight factors or not.
   */
  public void learn_factors(Board b, int[] s, boolean learnClose) {
      int[] fl = learnClose ? closefactors : factors;
      int[] best = new int[fl.length];
      int best_score = -1;
      Board best_board = null;
      ExecutorService pool = Executors.newFixedThreadPool(nThreads);

      for (int i = learning_starts[0]; i < 19; i++) {
          for (int j = learning_starts[1]; j < 19; j++) {
              for (int k = learning_starts[2]; k < 8; k++) {
                for (int l = learning_starts[3]; l < 17; l++) {
                    fl[0] = i; fl[1] = j;
                    fl[2] = k; fl[3] = l; 

                    Board n = solve_pndfs(s, b, pool);
                    int score = n.score();
                    if (score > best_score) {
                        System.arraycopy(factors, 0, best, 0, best.length);
                        best_score = score;
                        best_board = n;
                    }
                    System.out.printf("Current score: %d (%d moves)\n", 
                            score, n.nMoves());
                    for (int v : fl) {
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
    
    pool.shutdown();
  }
  
  private synchronized void updateBest(Board b) {
    int score = b.score();
    if (score > fbest_score) {
      fbest_score = score;
      fbest = b;
    }
  }
  
  private Board solve_dfs(int[] s, int depthLimit, Board b, int depth) {
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
            updateBest(candidate);
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
  
  /**
   * Single threaded depth-limited depth first search, with no backtracking.
   * @param s The tile sequence
   * @param b The board to search
   * @return The final best board state that it can find.
   */
  public Board solve_ldfs(int[] s, Board b) {
    Board input = b;
    fbest_score = -1;
    fbest = null;
    currentfactors = factors;
    while (b != null && !b.finished()) {
      b = solve_dfs(s, MAX_DEPTH, b, 0);
      if (b != null) {
        log_info(b);
      }
    }
    
    //Super edge-case: The input board can't be moved...
    return fbest == null ? input : fbest;
  }
  
  /**
   * Multi-threaded depth-limited depth first search, with no backtracking.
   * @param s The tile sequence
   * @param b The board to search
   * @param pool The thread pool
   * @return The final best board state that it can find.
   */
  private Board solve_pndfs(int[] s, Board b, ExecutorService pool) {
    Board input = b;
    
    fbest_score = -1;
    fbest = null;
    currentfactors = factors;
    
    while (b != null && !b.finished()) {
      b = solve_pdfs(s, b, pool);
      if (b != null) {
        //log_info(b);
      }
    }
    
    //Super edge-case: The input board can't be moved...
    return fbest == null ? input : fbest;
  }
  
  /**
   * Solves a board game using a depth-limited depth first search.
   * Also makes use of some backtracking to further optimise the result.
   * Will also attempt to run multithreadedly.
   * @param s The tile sequence
   * @param b The board to search
   * @return The final best board state that it can find.
   */
  public Board solve_mdfs(int[] s, Board b) {
    Ringbuffer<Board> rb = new Ringbuffer<>(6);
    Board current = b, choke_best = null;
    char fc = 0, foff = 0;
    int tmp = 0;
    //VTEC just kicked in yo
    ExecutorService pool = Executors.newFixedThreadPool(nThreads);
    
    fbest_score = -1;
    fbest = null;
    while (current != null && !current.finished()) {
      rb.push(current);
      current = solve_pdfs(s, current, pool);
      
      if (choke_best != null && choke_best != fbest) {
        log_info("Recovery!: [%d:%s] %d(%d) --> %d(%d)",
                  (int)fc,
                  Arrays.toString(currentfactors),
                  choke_best.score(), choke_best.nMoves(),
                  fbest.score(), fbest.nMoves());
        choke_best = null;
        //Test: Is it always best to stick to 18,2,2,9 where possible?
        //Maybe not, but some factors shouldn't be used for extended periods of time.
        if (fc > 3) {
          log_info("Volatile weights were used; switching back to %s",
                  Arrays.toString(choicefactors[0]));
          fc = 0;
          currentfactors = choicefactors[0];
        }
      }
      
      if (current != null) {
        log_info(current);
      } /*else if (tmp++ >= 2) {
        current = null;
        log_info("TMP");
        log_info(rb.pop());
      }*/ else if (fbest != null && fbest.nMoves() < s.length) {
        current = rb.pop();
        
        if (choke_best != fbest) {
          choke_best = fbest;
          foff = 1;
          fc = (char)((fc + 1) % choicefactors.length);
          currentfactors = choicefactors[fc];
          log_info("Dead-end, back-tracking two steps and trying with different weights!");
          log_info("Starting index: %d (current score %d/%d)",
                    (int)fc, current.score(), current.nMoves());
        } else if (foff++ < choicefactors.length - 1) {
          fc = (char)((fc + 1) % choicefactors.length);
          currentfactors = choicefactors[fc];
          log_info("No improvement, trying factor index %d...", (int)fc);
        } else {
          current = null;
          log_info("Factor exhaustion");
        }
      }
    }
   
    pool.shutdown();
    return fbest == null ? b : fbest;
  }
  
  /**
   * Somewhat multi-threaded depth-first search.
   * Performs a DFS of the subtrees from the current node in parallel.
   * @param s The tile sequence
   * @param b The board to search
   * @param pool The thread pool in which to submit jobs to.
   * @return The board with the highest evaluation or null if no board can
   *         continue.
   */
  private Board solve_pdfs(int[] s, Board b, ExecutorService pool) {
    List<Future<Board>> rets = new ArrayList<>(BOARD_WIDTH);
    Board best = null;
    int best_score = -1;
    
    for (Direction d : directions) {
      Board n = new Board(b);
      if (n.move(s, d)) {
        rets.add(pool.submit(new ParallelDFS(this, s, n)));
      }
    }
    
    for (Future<Board> ret : rets) {
      try {
        Board c = ret.get();
        if (c != null) {
          int score = evaluate(c, s);
          if (score > best_score) {
            best = c;
            best_score = score;
          }
        }
      } catch (InterruptedException | ExecutionException e) {
        System.err.println("Error: " + e.getMessage());
      }
    }
    
    return best;
  }
  
  private static class ParallelDFS implements Callable<Board> {
    private final int[] s;
    private final Board input;
    private final Solver parent;
    
    public ParallelDFS(Solver parent, int[] s, Board b) {
      this.s = s;
      this.input = b;
      this.parent = parent;
    }
    
    /**
     * The plumbing is a bit unusual since this class was written
     * after solve_dfs was...
     * @return
     * @throws Exception 
     */
    @Override
    public Board call() throws Exception {
      return parent.solve_dfs(s, MAX_DEPTH, input, 1);
    }
  } 
}
