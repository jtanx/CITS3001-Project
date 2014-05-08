package threes;

import java.util.Arrays;

/**
 *
 * @author
 */
public class Board {
  private static final int BOARD_WIDTH = 4;
  private static final int BOARD_SPACE = BOARD_WIDTH * BOARD_WIDTH;
  private static final char[][] g_trn = {
    {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}, //Left
    {0,4,8,12,1,5,9,13,2,6,10,14,3,7,11,15}, //Up
    {15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0}, //Right
    {15,11,7,3,14,10,6,2,13,9,5,1,12,8,4,0} //Down
  };
  
  public enum Direction {
    LEFT,
    RIGHT,
    UP,
    DOWN
  };
  
  private int[] it;
  private int c_sequence;
  private boolean finished;
  
  public Board(int[] board) {
    if (board.length != BOARD_SPACE) {
      throw new IllegalArgumentException("Invalid input board size");
    }
    it = Arrays.copyOf(board, BOARD_SPACE);
  }
  
  public Board(String file) {
    it = new int[BOARD_SPACE];
  }
  
  boolean shift_valid(int from, int to) {
    return (from != 0 && to == 0) || (from == 1 && to == 2)
            || (from == 2 && to == 1) || (from > 2 && from == to);
  }
  
  boolean is_pow2(int v) {
    return ((v) != 0) && (((v) & ((v) - 1) ) == 0);
  }
  
  void insert_sequence(int[] s, char seq_rows, char seq_trn[]) {
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

    //Seems to work... May have to check for the 'most clockwise' rule
    j = 0;
    while ((seq_rows >>= 1) != 0) {
      j++;
    }

    it[seq_trn[j]] = s[c_sequence++];
  }
  
  public boolean move(int[] s, Direction d) {
    boolean local_shift = false;
    char seq_rows = 0;
    char[] trn, seq_trn;
    
    if (finished) {
      return false;
    }
    
    switch(d) {
      case LEFT: trn = g_trn[0]; seq_trn = g_trn[3]; break;
      case UP: trn = g_trn[1]; seq_trn = g_trn[2]; break;
      case RIGHT: trn = g_trn[2]; seq_trn = g_trn[1]; break;
      case DOWN: trn = g_trn[3]; seq_trn = g_trn[0]; break;
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
      insert_sequence(s, seq_rows, seq_trn);
      if (c_sequence == s.length) {
        finished = true; //May have to move this check to after to match the case of checking if seq_rows is 0.
      }
    }
    return true;
  }
  
  int tile_score(int t) {
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

  int score() {
    int i, score;
    for (i = 0, score = 0; i < BOARD_SPACE; i++) {
      score += tile_score(it[i]);
    }
    return score;
  }
}
