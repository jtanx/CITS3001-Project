package threes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author
 */
public class Threes {

  private static int[] parseFile(String file, int[] b) throws IOException {
    BufferedReader br = null;
    ArrayList<Integer> sequence = new ArrayList<Integer>();
    int[] result;
    
    if (b.length != Board.BOARD_SPACE) {
      throw new IllegalArgumentException("Invalid board size");
    }

    try {
      String c;
      br = new BufferedReader(new FileReader(file));

      br.readLine();
      br.readLine(); //Skip first two lines

      for (int i = 0; i < Board.BOARD_WIDTH; i++) {
        c = br.readLine();
        String[] p;
        if (c == null) {
          throw new IOException("Malformed file");
        }

        p = c.split("[^0-9]");
        for (int j = 0, k = 0; j < Board.BOARD_WIDTH && k < p.length; k++) {
          if (!p[k].isEmpty()) {
            b[i * Board.BOARD_WIDTH + j++] = Integer.parseInt(p[k]);
          }
        }
      }
      br.readLine(); //Skip line

      while ((c = br.readLine()) != null) {
        String[] p = c.split("[^0-9]");
        for (int k = 0; k < p.length; k++) {
          if (!p[k].isEmpty()) {
            Integer a = Integer.parseInt(p[k]);
            sequence.add(a);
          }
        }
      }
    } finally {
      if (br != null) {
        br.close();
      }
    }
    
    result = new int[sequence.size()];
    int i = 0;
    for (int j : sequence) {
      result[i++] = j;
    }
    return result;
  }
  
  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    int[] bt = new int[Board.BOARD_SPACE], s;
    if (args.length != 1){
      System.out.println("Usage: threes file_in.txt");
      return;
    }
    
    try {
      s = parseFile(args[0], bt);
    } catch (IOException e) {
      System.out.printf("Invalid file %s: %s\n", args[0], e.getMessage());
      return;
    }
    
    Solver solver = new Solver();
    //solver.learn_factors(new Board(bt), s);
    //if (true) return;
    long tim = System.nanoTime();
    Board bs = solver.solve_idfs(s, new Board(bt));
    //Board bs = ida.solve_mdfs(s, new Board(bt));
    tim = System.nanoTime() - tim;
    System.out.println(bs);
    System.out.printf("%d, %d, %d, %d, %d\n",
                      bs.score(), bs.dof(), 
                      bs.zeros(), bs.checkerboarding2(),
                      bs.smoothness());
    System.out.printf("Used %d/%d available moves in %.2f seconds. (%.2f m/s)\n", 
                      bs.moves().length(), s.length, 
                      tim / 1000000000.0,
                      bs.moves().length() / (tim / 1000000000.0));
    System.out.println(bs.moves());
  }
}
