package threes;

import java.util.*;
import java.util.concurrent.*;
import threes.Board.Direction;
import static threes.Threes.log_info;

/**
 * 'Solves' the game using a depth-limited depth first search.
 * Should not be used - use ASSolver instead.
 * @author Jeremy Tan, 20933708
 */
public class DLDFSolver implements Solver {
  private static final int MAX_DEPTH = 8;
  private static final int BOARD_WIDTH = Board.BOARD_WIDTH;
  private static final Direction[] directions = {
    Direction.LEFT, Direction.UP, Direction.RIGHT, Direction.DOWN
  };
  private static final int THREAD_COUNT;
  static {
    int nt = Runtime.getRuntime().availableProcessors();
    nt = nt <= 1 ? 1 : nt > 4 ? 4 : nt;
    THREAD_COUNT = nt;
  }
  
  private final int[] learning_starts = {18,0,0,0};
  private final int[] tileSequence;
  private final int nThreads;
  private final boolean useBacktracking;
  private Board fbest = null;
  private int fbest_score = -1;
  
  
  private final int[] factors = {18,2,2,9}; //The best all-rounder
  private final int[] lb3factors = {18,1,2,13};
  private final int[] lb3factors2 = {18,3,2,13};
  private final int[] lb3factors3 = {18,2,1,6};
  private final int[] lowfactors = {18,0,0,9}; 
  private final int[] lowfactors3 = {18,1,0,1}; 
  private final int[] closefactors = {18, 5, 10, 9}; //nc1 559 569 5610 
  private final int[][] choicefactors = {factors, lb3factors, lb3factors2, lb3factors3, lowfactors, lowfactors3};
  private int[] currentfactors = choicefactors[0];
  
  public DLDFSolver(int[] s, int[] learning_startfactors, boolean singleThreaded, boolean useBacktracking) {
    log_info("Heuristic weights: %s", Arrays.deepToString(choicefactors));
    log_info("Reported number of processors: %d", Runtime.getRuntime().availableProcessors());
    nThreads = (singleThreaded || THREAD_COUNT == 1) ? 1 : THREAD_COUNT;
    log_info("No. of threads to be used: %d\n", nThreads);
    this.useBacktracking = useBacktracking;
    log_info("Use backtracking?: %s", useBacktracking ? "Yes" : "No");
    
    this.tileSequence = new int[s.length];
    System.arraycopy(s, 0, this.tileSequence, 0, s.length);
    
    if (learning_startfactors != null) {
      if (learning_startfactors.length != learning_starts.length) {
        throw new IllegalArgumentException("Invalid learning factor size");
      }
      System.arraycopy(learning_startfactors, 0, 
              learning_starts, 0, learning_starts.length);
    }
  }
  
  public DLDFSolver(int[] s, boolean singleThreaded, boolean useBacktracking) {
    this(s, null, singleThreaded, useBacktracking);
  }
  
  //Edge case: As we're approaching the end of a sequence, try to maximise score...
  //Possible change to ncombinable: Weight combinables that increase the score significantly
  private int evaluate(Board b) {
    int[] thefactors = currentfactors;
    
    //We are close to the end of the sequence! Use different weights!
    if (b.nMoves() + MAX_DEPTH * 2 >= tileSequence.length) {
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
   * @param learnClose Whether we are learning for the 'close' weight factors or not.
   */
  public void learn_factors(Board b, boolean learnClose) {
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

                    Board n = solve_pndfs(b, pool);
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
  
  private Board solve_dfs(Board b, int depthLimit, int depth) {
    if (depth >= depthLimit) { //Cutoff test
      return b;
    }
    
    Board best = null;
    int best_score = -1;

    for (int i = 0; i < BOARD_WIDTH; i++) {
      Board next = new Board(b);
      if (next.move(tileSequence, directions[i])) {
        Board candidate = solve_dfs(next, depthLimit, depth + 1);
        if (candidate == null && next.finished()) {
          candidate = next;
        }
        if (candidate != null) {
          if (candidate.finished()) {
            updateBest(candidate);
          } else {
            int score = evaluate(candidate);
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
  private Board solve_ldfs(Board b) {
    Board input = b;
    fbest_score = -1;
    fbest = null;
    currentfactors = factors;
    while (b != null && !b.finished()) {
      b = solve_dfs(b, MAX_DEPTH, 0);
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
  private Board solve_pndfs(Board b, ExecutorService pool) {
    Board input = b;
    
    fbest_score = -1;
    fbest = null;
    currentfactors = factors;
    
    while (b != null && !b.finished()) {
      b = solve_pdfs(b, pool);
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
  private Board solve_mdfs(Board b) {
    Ringbuffer<Board> rb = new Ringbuffer<>(6);
    Board current = b, choke_best = null;
    char fc = 0, foff = 0;
    //VTEC just kicked in yo
    ExecutorService pool = Executors.newFixedThreadPool(nThreads);
    
    fbest_score = -1;
    fbest = null;
    while (current != null && !current.finished()) {
      rb.push(current);
      current = solve_pdfs(current, pool);
      
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
      } else if (fbest != null && fbest.nMoves() < tileSequence.length) {
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
  private Board solve_pdfs(Board b, ExecutorService pool) {
    List<Future<Board>> rets = new ArrayList<>(BOARD_WIDTH);
    Board best = null;
    int best_score = -1;
    
    for (Direction d : directions) {
      Board n = new Board(b);
      if (n.move(tileSequence, d)) {
        rets.add(pool.submit(new ParallelDFS(n)));
      }
    }
    
    for (Future<Board> ret : rets) {
      try {
        Board c = ret.get();
        if (c != null) {
          int score = evaluate(c);
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

  @Override
  public Board solve(Board in) {
    if (useBacktracking) {
      return solve_mdfs(in);
    } else {
      if (nThreads == 1) {
        return solve_ldfs(in);
      } else {
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        Board ret = solve_pndfs(in, pool);
        pool.shutdown();
        return ret;
      }
    }
  }
  
  private class ParallelDFS implements Callable<Board> {
    private final Board input;
    
    public ParallelDFS(Board b) {
      this.input = b;
    }
    
    @Override
    public Board call() throws Exception {
      return solve_dfs(input, MAX_DEPTH, 1);
    }
  }

  /**
   * Buffers the previous n moves made. When retrieving from the buffer, the
   * furthest move made is returned and the buffer is cleared.
   */
  private static class Ringbuffer<T> {
    private LinkedList<T> buf;
    private final int maxSize;
    private boolean isBacktracking;

    public Ringbuffer(int maxSize) {
      if (maxSize <= 0) {
        throw new IllegalArgumentException("Must have a size greater than 0.");
      }
      this.maxSize = maxSize;
      buf = new LinkedList<>();
    }

    public void push(T it) {
      //prevents stinker movements TODO: Does this cause looping???
      if ((!isBacktracking && buf.size() > maxSize) || (isBacktracking && buf.size() > maxSize + 1)) {
        buf.removeFirst();
      }
      buf.addLast(it);
    }

    public T pop() {
      if (!buf.isEmpty()) {
        T ret = buf.peekFirst();
        buf.clear();
        isBacktracking = true;
        return ret;
      }
      throw new IllegalStateException("Can't pop an empty ringbuffer");
    }

    public Stack<T> pop(int count) {
      if (!buf.isEmpty()) {
        Stack<T> ret = new Stack<>();
        for (int i = 0; i < count && !buf.isEmpty(); i++) {
          ret.add(buf.removeFirst());
        }
        buf.clear();
        return ret;
      }
      throw new IllegalStateException("Can't pop an empty ringbuffer");
    }

    public void clear() {
      buf.clear();
    }
  }
}
