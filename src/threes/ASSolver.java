
package threes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import threes.Board.Direction;
import static threes.Threes.log_info;

/**
 * A* solver with default lookahead of 8
 * @author Jeremy Tan, 20933708
 */
public class ASSolver implements Solver{
  private static final int DEFAULT_LOOKAHEAD = 8;
  private static final int DEFAULT_PQ_SIZE = 150;
  private static final int DEFAULT_IPQ_SIZE = 3;
  private static final int DEFAULT_QUI_SIZE = 11000;
  private static final int BOARD_WIDTH = Board.BOARD_WIDTH;
  private static final int[] factors = {18,2,2,9}; //The best all-rounder
  private static final int[] closefactors = {18, 5, 10, 9}; //Focus on combining more tiles
  private static final Board.Direction[] directions = {
    Board.Direction.LEFT, Board.Direction.UP, Board.Direction.RIGHT, Board.Direction.DOWN
  };
  
  private static final int THREAD_COUNT;
  static {
    int nt = Runtime.getRuntime().availableProcessors();
    nt = nt <= 1 ? 1 : nt > 4 ? 4 : nt;
    THREAD_COUNT = nt;
  }
  
  private final Comparator<Board> BOARD_COMPARER;
  private final LimitedQueue<Board> pq;
  private final int[] tileSequence;
  private final long maxTime;
  private final int nThreads, lookahead_depth, ipq_size, qui_size;
  private Board fbest = null;
  private int fbest_score = -1;
  
  public ASSolver(int[] s, boolean singleThreaded, int lookahead, int pq_size, int ipq_size, int qui_size) {
    this.tileSequence = new int[s.length];
    System.arraycopy(s, 0, tileSequence, 0, s.length);
    
    this.lookahead_depth = lookahead < 1 ? DEFAULT_LOOKAHEAD : lookahead;
    this.ipq_size = ipq_size < 1 ? DEFAULT_IPQ_SIZE : ipq_size;
    this.qui_size = qui_size < 1 ? DEFAULT_QUI_SIZE : qui_size;
    this.nThreads = (singleThreaded || THREAD_COUNT == 1) ? 1 : THREAD_COUNT;
    this.maxTime = (s.length / 5) * 1000000000L;
    this.BOARD_COMPARER = new BComparer();
    
    log_info("No. of threads to be used: %d\n", nThreads);
    
    pq_size = pq_size < 1 ? DEFAULT_PQ_SIZE : pq_size;
    this.pq = new LimitedQueue<>(this.BOARD_COMPARER, pq_size);
  }
  
  public ASSolver(int[] s) {
    this(s, true, -1, -1, -1, -1);
  }
  
  private synchronized void updateBest(Board b) {
    int score = b.score();
    if (score > fbest_score) {
      fbest_score = score;
      fbest = b;
    }
  }
  
  /**
   * A recursively defined depth-limited depth first search.
   * @param b The board position to search from
   * @param ret Where to store the result
   * @param depth The current depth
   * @param limit The depth limit
   */
  private void lookahead_dfs(Board b, LimitedQueue<Board> ret, int depth) {
    if (depth >= lookahead_depth) {
      ret.add(b);
      return;
    }
    
    for (int i = 0; i < BOARD_WIDTH; i++) {
      Board next = new Board(b);
      if (next.move(tileSequence, directions[i])) {
        if (next.finished()) {
          updateBest(next);
        } else {
          lookahead_dfs(next, ret, depth + 1);
        }
      }
    }
  }
  
  /**
   * A parallelised depth limited depth-first search.
   * Note that each thread has their own LimitedQueue, which is then
   * combined at the end into one. This is to avoid having to lock/synchronise
   * access to the one return value.
   * @param b The board position to search from
   * @param pool The thread pool in which to submit jobs to
   * @return A LimitedQueue containing the top MAX_QUEUE_SIZE nodes
   */
  private LimitedQueue<Board> lookahead_pdfs(Board b, ExecutorService pool) {
    List<Future<LimitedQueue<Board>>> rets = new ArrayList<>();
    LimitedQueue<Board> fret = new LimitedQueue<>(new BComparer(), ipq_size);
    
    for (Direction d : directions) {
      Board n = new Board(b);
      if (n.move(tileSequence, d)) {
        rets.add(pool.submit(new ParallelDFS(n)));
      }
    }
    
    for (Future<LimitedQueue<Board>> ret : rets) {
      try {
        LimitedQueue<Board> c = ret.get();
        fret.addAll(c);
      } catch (InterruptedException | ExecutionException e) {
        System.err.println("Error: " + e.getMessage());
      }
    }
    
    return fret;
  }
  
  /**
   * A single-threaded depth-limited depth-first search. Used primarily
   * to verify that the multi-threaded version works as expected.
   * @param b The board position to search from
   * @return A LimitedQueue containing the top MAX_QUEUE_SIZE nodes
   */
  private LimitedQueue<Board> lookahead_ldfs(Board b) {
    LimitedQueue<Board> lq = new LimitedQueue<>(new BComparer(), ipq_size);
    lookahead_dfs(b, lq, 0);
    return lq;
  }
  
  /**
   * Solves the board using a process somewhat akin to A*.
   * This method is not thread-safe.
   * @param b The board to be solved
   * @return The final board
   */
  @Override
  public Board solve(Board b) {
    ExecutorService pool = null;
    long start = System.nanoTime();
    Board prevFBest = null;
    int nFBestSame = 0;
    fbest = null;
    fbest_score = -1;
    
    if (nThreads > 1) {
      pool = Executors.newFixedThreadPool(nThreads);
    }
    
    pq.add(b);
    while (!pq.isEmpty()) {
      long runtime = System.nanoTime() - start;
      if (fbest != null) {
        if (((fbest.nMoves() == tileSequence.length || runtime > maxTime) 
                && nFBestSame >= 5) || nFBestSame >= qui_size) {
          if (pool != null) {
            pool.shutdown();
          }
          return fbest;
        }
        
        if (fbest != prevFBest) {
          prevFBest = fbest;
          nFBestSame = 0;
        } else {
          nFBestSame++;
        }
      }
      
      Board n = pq.pollLast();
      log_info("PQ Size: %d (%d)", pq.size(), nFBestSame);
      log_info(n);
      
      LimitedQueue<Board> lq;
      if (nThreads > 1) {
        lq = lookahead_pdfs(n, pool);
      } else {
        lq = lookahead_ldfs(n);
      }
      pq.addAll(lq);
    }
    
    if (pool != null) {
      pool.shutdown();
    }
    return fbest == null ? b : fbest;
  }
  
  /**
   * Creates a 'Priority Queue' or more like a sorted set that is limited
   * in size. Only items with the highest evaluation gets kept in the queue 
   * if the size limit is reached.
   * @param <T> 
   */
  private class LimitedQueue<T> extends TreeSet<T> {
    private final int sizeLimit;
    public LimitedQueue(Comparator<? super T> comparator, int sizeLimit) {
      super(comparator);
      this.sizeLimit = sizeLimit;
    }
    
    @Override
    public boolean add(T t) {
      boolean ret = super.add(t);
      if (ret) {
        while (size() > sizeLimit) {
          pollFirst();
        }
      }
      return ret;
    }
    
    @Override
    public boolean addAll(Collection<? extends T> c) {
      boolean ret = super.addAll(c);
      if (ret) {
        while (size() > sizeLimit) {
          pollFirst();
        }
      }
      return ret;
    }
  }
  
  /**
   * Comparer class for the boards, using an evaluation function and a cost
   * function. The cost function is the number of moves that a board has made.
   */
  private class BComparer implements Comparator<Board> {
    private int evaluate(Board b) {
      int[] thefactors = factors;

      //We are close to the end of the sequence! Use different weights!
      if (b.nMoves() + lookahead_depth * 2 >= tileSequence.length) {
        thefactors = closefactors;
      }
      return ((int)Math.pow(4, b.dof())) + 
             thefactors[0] * b.zeros() + 
             thefactors[1] * b.checkerboarding3() + 
             thefactors[2] * b.smoothness() + 
             thefactors[3] * b.nCombinable();
    }

    @Override
    public int compare(Board o1, Board o2) {
      int f1 = o1.nMoves() * 6 + evaluate(o1);
      int f2 = o2.nMoves() * 6 + evaluate(o2);
      return f1 - f2;
    }
  }
  
  private class ParallelDFS implements Callable<LimitedQueue<Board>> {
    private final Board input;
    private final LimitedQueue<Board> lq;
    
    public ParallelDFS(Board b) {
      this.input = b;
      this.lq = new LimitedQueue<>(new BComparer(), ipq_size);
    }

    @Override
    public LimitedQueue<Board> call() {
      lookahead_dfs(input, lq, 1);
      return lq;
    }
  } 
}
