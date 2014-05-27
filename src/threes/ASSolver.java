
package threes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
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
 * A* solver with lookahead of 8
 * @author
 */
public class ASSolver {
  private static final int MAX_DEPTH = 8;
  private static final int MAX_QUEUE_SIZE = 4;
  private static final int BOARD_WIDTH = Board.BOARD_WIDTH;
  private static final Board.Direction[] directions = {
    Board.Direction.LEFT, Board.Direction.UP, Board.Direction.RIGHT, Board.Direction.DOWN
  };
  private static final int nThreads;
  static {
    int nt = Runtime.getRuntime().availableProcessors();
    nt = nt <= 1 ? 1 : nt > 4 ? 4 : nt;
    nThreads = nt;
  }
  
  private static final int[] factors = {18,2,2,9}; //The best all-rounder
  private static final int[] closefactors = {18, 5, 10, 9};
  private int[] currentfactors = factors;
  
  private final int[] tileSequence;
  private final LimitedQueue<Board> pq;
  private final long maxTime;
  private Board fbest = null;
  private int fbest_score = -1;
  
  public ASSolver(int[] s) {
    tileSequence = new int[s.length];
    System.arraycopy(s, 0, tileSequence, 0, s.length);
    pq = new LimitedQueue<>(new BComparer(), 150);
    maxTime = (s.length / 5) * 1000000000L;
  }
  
  private synchronized void updateBest(Board b) {
    int score = b.score();
    if (score > fbest_score) {
      fbest_score = score;
      fbest = b;
    }
  }
  
  private LimitedQueue<Board> lookahead_dfs(Board b, LimitedQueue<Board> ret, int depth, int limit) {
    if (depth >= limit) {
      ret.add(b);
      return ret;
    }
    
    for (int i = 0; i < BOARD_WIDTH; i++) {
      Board next = new Board(b);
      if (next.move(tileSequence, directions[i])) {
        if (next.finished()) {
          updateBest(next);
        } else {
          lookahead_dfs(next, ret, depth + 1, limit);
        }
      }
    }
    
    return ret;
  }
  
  private LimitedQueue<Board> lookahead_pdfs(Board b, ExecutorService pool) {
    List<Future<LimitedQueue<Board>>> rets = new ArrayList<>();
    LimitedQueue<Board> fret = new LimitedQueue<>(new BComparer(), MAX_QUEUE_SIZE);
    
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
  
  private LimitedQueue<Board> lookahead_ldfs(Board b) {
    LimitedQueue<Board> lq = new LimitedQueue<>(new BComparer(), MAX_QUEUE_SIZE);
    lookahead_dfs(b, lq, 0, MAX_DEPTH);
    return lq;
  }
  
  public Board solve_astar(Board b) {
    ExecutorService pool = Executors.newFixedThreadPool(nThreads);
    long start = System.nanoTime();
    Board prevFBest = null;
    int nFBestSame = 0;
    fbest = null;
    fbest_score = -1;
    
    pq.add(b);
    while (!pq.isEmpty()) {
      long runtime = System.nanoTime() - start;
      if (fbest != null) {
        if (((fbest.nMoves() == tileSequence.length || runtime > maxTime) 
                && nFBestSame >= 5) || nFBestSame >= 11000) {
          pool.shutdown();
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
      LimitedQueue<Board> lq = lookahead_pdfs(n, pool);
      //LimitedQueue<Board> lq = lookahead_ldfs(n);
      pq.addAll(lq);
    }
    
    pool.shutdown();
    return fbest == null ? b : fbest;
  }
  
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
  
  private class BComparer implements Comparator<Board> {
    private int evaluate(Board b) {
      int[] thefactors = currentfactors;

      //We are close to the end of the sequence! Use different weights!
      if (b.nMoves() + MAX_DEPTH * 2 >= tileSequence.length) {
        thefactors = closefactors;
      }
      return ((int)Math.pow(4, b.dof())) + 
             thefactors[0] * b.zeros() + 
             thefactors[1] * b.checkerboarding3() + 
             thefactors[2] * b.smoothness() + 
             thefactors[3] * b.nCombinable();
    }
    
    //TODO: Add in number of moves made too?
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
      this.lq = new LimitedQueue<>(new BComparer(), MAX_QUEUE_SIZE);
    }

    @Override
    public LimitedQueue<Board> call() {
      lookahead_dfs(input, lq, 1, MAX_DEPTH);
      return lq;
    }
  } 
}
