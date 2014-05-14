package threes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 *
 * @author
 */
public class Board {
  public static final int BOARD_WIDTH = 4;
  public static final int BOARD_SPACE = BOARD_WIDTH * BOARD_WIDTH;
  private static final char[][] g_trn = {
    {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}, //Left
    {0,4,8,12,1,5,9,13,2,6,10,14,3,7,11,15}, //Up
    {15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0}, //Right
    {15,11,7,3,14,10,6,2,13,9,5,1,12,8,4,0} //Down
  };
  
  public enum Direction {
    LEFT("L"),
    RIGHT("R"),
    UP("U"),
    DOWN("D");
    
    private String t;
    private Direction(String t) {
      this.t = t;
    }
    
    @Override public String toString() {
      return t;
    }
    
    public static Direction[] parse(String s) {
      List<Direction> m = new ArrayList<Direction>();
      Direction[] d = new Direction[0];
      for (char c : s.toCharArray()) {
        switch(Character.toLowerCase(c)) {
          case 'l': m.add(LEFT); break;
          case 'u': m.add(UP); break;
          case 'r': m.add(RIGHT); break;
          case 'd': m.add(DOWN); break;
        }
      }
      
      return m.toArray(d);
    }
  };
  
  private int[] it;
  private int c_sequence;
  private boolean finished;
  private int depth;
  private StringBuilder path;
  
  public Board(int[] board) {
    if (board.length != BOARD_SPACE) {
      throw new IllegalArgumentException("Invalid input board size");
    }
    it = Arrays.copyOf(board, BOARD_SPACE);
    path = new StringBuilder();
  }
  
  public Board(Board o) {
    this.it = Arrays.copyOf(o.it, BOARD_SPACE);
    this.c_sequence = o.c_sequence;
    this.finished = o.finished;
    this.depth = o.depth;
    this.path = new StringBuilder(o.path);
  }
  
  private boolean shift_valid(int from, int to) {
    return (from != 0 && to == 0) || (from == 1 && to == 2)
            || (from == 2 && to == 1) || (from > 2 && from == to);
  }
  
  private boolean is_pow2(int v) {
    return ((v) != 0) && (((v) & ((v) - 1) ) == 0);
  }
  
  private char insert_mask(char seq_rows, char seq_trn[]) {
    char i, j;
    //If seq_rows is a power of 2, then only one row left -> sub into that row immediately.
    for (i = 0; i < BOARD_WIDTH && !is_pow2(seq_rows); i++) {
      int min_value = Integer.MAX_VALUE;
      for (j = 0; j < BOARD_WIDTH; j++) {
        if ((seq_rows & (1 << j)) != 0) { //Is a row that sequence can be inserted into
          char idx = seq_trn[i * BOARD_WIDTH + j];
          if (it[idx] < min_value) {
            min_value = it[idx];
            seq_rows &= ~((1 << j) - 1); //All rows previous are out of the running
          } else if (it[idx] > min_value) {
            seq_rows ^= 1 << j; //This row is no longer in the running
          }
        }
      }
    }

    return seq_rows;
  }
  
  public int nMoves() {
    return c_sequence;
  }
  
  public String moves() {
    return path.toString();
  }
  
  public boolean move(int[] s, Direction d) {
    boolean local_shift = false, insert_last;
    char seq_rows = 0;
    char[] trn, seq_trn;
    
    if (c_sequence >= s.length || finished) {
        finished = true; //May have to move this check to after to match the case of checking if seq_rows is 0.
        return false;
    }
    
    switch(d) {
      case LEFT: trn = g_trn[0]; seq_trn = g_trn[3]; insert_last = false; break;
      case UP: trn = g_trn[1]; seq_trn = g_trn[2]; insert_last = true; break;
      case RIGHT: trn = g_trn[2]; seq_trn = g_trn[1]; insert_last = false; break;
      case DOWN: trn = g_trn[3]; seq_trn = g_trn[0]; insert_last = true; break;
      default:
        throw new IllegalArgumentException("I don't even");
    }
    
    for (char i = 0; i < BOARD_WIDTH; i++) {
      for (char j = 1; j < BOARD_WIDTH; j++) {
        char idx = trn[i * BOARD_WIDTH + j];
        char pidx = trn[i * BOARD_WIDTH + j - 1];
        
        if (local_shift) {
          it[pidx] = it[idx];
          it[idx] = 0;
        } else if (shift_valid(it[idx], it[pidx])) {
          seq_rows |= (1 << (BOARD_WIDTH - i - 1)); //Include row which shift occurred in
          local_shift = true;
          it[pidx] += it[idx];
          it[idx] = 0;
        }
      }
      local_shift = false;
    }
    
    if (seq_rows == 0) { //If seq_rows == 0, no rows have been shifted
      //printf("No change!\n");
      finished = true;
      return false;
    } else {
      char j = 0;
      seq_rows = insert_mask(seq_rows, seq_trn);
      if (insert_last)
        while (((seq_rows) >>= 1) != 0) j++;
      else while ((seq_rows & 1) != 1) {
        seq_rows >>= 1; j++;
      }
      
      it[seq_trn[j]] = s[c_sequence++];      
      path.append(d);
      if (c_sequence >= s.length) {
        finished = true;
      }
    }
    return true;
  }
  
  public int tile_score(int t) {
    if (t == 1 || t == 2) {
      return 1;
    } else if (t > 2) {
      int log2 = 0;
      t /= 3;
      while ((t >>= 1) != 0) {
        log2++;
      }
      return (int)Math.pow(3, log2 + 1);
    }

    return 0;
  }

  public int score() {
    int i, score;
    for (i = 0, score = 0; i < BOARD_SPACE; i++) {
      score += tile_score(it[i]);
    }
    return score;
  }
  
  /**
   * Where's my detuning frequency...
   * @return 
   */
  public int dof() {
    char can_shift = 0;
    
    for (char i = 0; i < BOARD_WIDTH && can_shift != 0xF; i++) {
      for (char j = 1; j < BOARD_WIDTH; j++) {
        char idx = (char)(i * BOARD_WIDTH + j);
        char pidx = (char)(idx - 1);
        
        if (shift_valid(it[g_trn[0][idx]], it[g_trn[0][pidx]])) {
          can_shift |= 1;
        }
        if (shift_valid(it[g_trn[1][idx]], it[g_trn[1][pidx]])) {
          can_shift |= 2;
        }
        if (shift_valid(it[g_trn[2][idx]], it[g_trn[2][pidx]])) {
          can_shift |= 4;
        }
        if (shift_valid(it[g_trn[3][idx]], it[g_trn[3][pidx]])) {
          can_shift |= 8;
        }
      }
    }
    
    return (can_shift & 1) + ((can_shift & 2) >> 1) + 
           ((can_shift & 4) >> 2) + ((can_shift & 8) >> 3);
  }
  
  public boolean finished() {
    return finished;
  }
  
  public void finished(boolean is) {
    finished = is;
  }
  
  public int zeros() {
    int n_z = 0;
    for (int i = 0; i < BOARD_SPACE; i++)
      if (it[i] == 0)
        n_z++;
    return n_z;
  }
  
  public int gthree() {
    int n_z = 0;
    for (int i = 0; i < BOARD_SPACE; i++)
      if (it[i] > 2)
        n_z++;
    return n_z;
  }
  
  public int msum() { //Monotonic
    int sum = 0;
    for (int i = 0; i < BOARD_SPACE; i++) {
      sum += it[i] >=3 ? it[i] * 2 : it[i];
    }
    return sum;
  }
  
  //Max theoretical value: 24
  public int checkerboarding() {
    int cb = 0;
    int sl = 0, su = 0;
    for (char i = 0; i < BOARD_WIDTH; i++) {
      for (char j = 1; j < BOARD_WIDTH; j++) {
        int cl = it[g_trn[0][i * BOARD_WIDTH + j]];
        int pl = it[g_trn[0][i * BOARD_WIDTH + j - 1]];
        int cu = it[g_trn[1][i * BOARD_WIDTH + j]];
        int pu = it[g_trn[1][i * BOARD_WIDTH + j - 1]];
        
        if (cl > pl) {
          if (sl < 0)
            cb++;
          sl = 1;
        } else {
          if (sl > 0)
            cb++;
          sl = -1;
        }
        
        if (cu > pu) {
          if (su < 0)
            cb++;
          su = 1;
        } else {
          if (su > 0)
            cb++;
          su = -1;
        }
      }
    }
    
    return cb;
  }
  
  public int monotonicity() { //Not monotonics
    int l = 0, u = 0, r = 0, d = 0;
    
    for (int i = 0; i < BOARD_WIDTH; i++) {
      for (int j = 0; j < BOARD_WIDTH - 1; j++) {
        int c = i * BOARD_WIDTH + j;
        
        if (it[g_trn[0][c+1]] >= it[g_trn[0][c]]) {
          l++;
          if (it[g_trn[0][c+1]] / 2 == it[g_trn[0][c]])
            l++;
        } else
          l--;
        if (it[g_trn[1][c+1]] >= it[g_trn[1][c]]) {
          u++;
          if (it[g_trn[1][c+1]] / 2 == it[g_trn[1][c]])
            u++;
        } else
          u--;
        if (it[g_trn[2][c+1]] >= it[g_trn[2][c]]) {
          r++;
          if (it[g_trn[2][c+1]] / 2 == it[g_trn[2][c]])
            r++;
        } else
          r--;
        if (it[g_trn[3][c+1]] >= it[g_trn[2][c]]) {
          d++;
          if (it[g_trn[3][c+1]] / 2 == it[g_trn[3][c]])
            d++;
        } else
          d--;
      }
    }
    return l+u+r+d;
  }
  
  private int elevation(int v) {
    return (int)((v/3) <= 0 ? 0 : Math.log(v/3) / Math.log(2));
  }
  
  public int smoothness() {
    int smoothness = 0;
    for (int i = 0; i < BOARD_WIDTH - 1; i++) {
      for (int j = 0; j < BOARD_WIDTH - 1; j++) {
        int c = it[i * BOARD_WIDTH + j];
        if (c > 0) {
          int r = it[i * BOARD_WIDTH + j + 1];
          int d = it[(i + 1) * BOARD_WIDTH + j];
          
          smoothness -= Math.abs(elevation(c) - elevation(r));
          smoothness -= Math.abs(elevation(c) - elevation(d));
        }
      }
    }
    
    //System.out.println(smoothness);
    
    return smoothness;
  }
  
  private int utility(int[] s) {
    int score = 0, max = 0;
    
    for (int i = 0; i < BOARD_WIDTH; i++) {
      for (int j = 0; j < BOARD_WIDTH; j++) {
        if (it[i*BOARD_WIDTH + j] == 0)
          score += 30;
        
        if (max < it[i*BOARD_WIDTH+j])
          max = it[i*BOARD_WIDTH+j];
        
        //if (j+1< BOARD_WIDTH && it[i*BOARD_WIDTH+j+1] > it[i*BOARD_WIDTH+j])
        //  score += 16;
        //if (i+1 < BOARD_WIDTH && it[(i+1)*BOARD_WIDTH+j] > it[i*BOARD_WIDTH+j])
         // score += 16;
        
      }
    }
    //if (monotonicity() > 27)
    //System.out.println(monotonicity());
    score += monotonicity() * 55;
    return (int)(score);
  }
  
  public void solve_dfs(int[] s) {
    Set<Board> candidate = new HashSet<Board>();
    Set<Board> best = new HashSet<Board>();
    int best_score, candidate_score = -1;
    Stack<Board> rem = new Stack<Board>();
    int count = 0;
    long time = System.nanoTime();
    
    best_score = this.utility(s);
    best.add(this);
    while (!best.isEmpty()) { //Got candidate paths left to follow
      if (count++ % 10 == 0) {
        if (!best.isEmpty()) {
          for (Board b : best ) {
            System.out.println(b);
            System.out.println("SCORE: " + Integer.toString(b.score()));
          }
        }
      }
      for (Board b : best) {
        b.depth = 0;
        rem.push(b);
      }
      best.clear();
      
      while (!rem.isEmpty()) {
        Board c = rem.pop();
        
        if (c.depth + 1 < 7) {
          Board[] next = new Board[4];
          Direction[] nextd = {Direction.LEFT, Direction.UP, 
                               Direction.RIGHT, Direction.DOWN};
          for (int i = 0; i < 4; i++) {
            next[i] = new Board(c);
            next[i].move(s, nextd[i]);
            next[i].depth += 1;
            
            if (next[i].finished()) {
              int score = next[i].score();
              if (score > candidate_score) {
                candidate.clear();
                candidate_score = score;
                candidate.add(next[i]);
              } else if (score == candidate_score) {
                candidate.add(next[i]);
              }
            } else {
              rem.push(next[i]);
            }
          }
        } else {
          int score = c.utility(s);
          if (score > best_score) {
            best.clear();
            best_score = score;
            best.add(c);
          } else if (score == best_score) {
            best.add(c);
          }
        }
      }
    }
    
    time = System.nanoTime() - time;
    
    for (Board b : candidate) {
      System.out.println(b);
      System.out.println(b.path.toString());
      System.out.println(b.path.length() / (time / (1000000000.0)));
      break;
    }
    System.out.println(candidate.size());
    System.out.println(candidate_score);
    
  }
  
  @Override public boolean equals(Object o) {
    if (o != null && o instanceof Board) {
      return this.c_sequence == ((Board)o).c_sequence &&
             Arrays.equals(this.it, ((Board)o).it);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 79 * hash + Arrays.hashCode(this.it);
    hash = 79 * hash + this.c_sequence;
    return hash;
  }
  
  @Override
  public String toString() {
    Formatter f = new Formatter();
    String nl = System.getProperty("line.separator");
    
    for (int i = 0; i < BOARD_WIDTH; i++) {
      for (int j = 0; j < BOARD_WIDTH; j++) {
        f.format("%3d ", it[i*BOARD_WIDTH + j]);
      }
      f.format("%s", nl);
    }
    
    return f.toString();
  }
}
