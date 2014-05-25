package threes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import threes.Board.Direction;

/**
 *
 * @author
 */
public class Threes {
  private static boolean verbose;
  
  public static void log_info(Object o) {
    if (verbose) {
      System.err.println(o);
    }
  }
  public static void log_info(String fmt, Object... args) {
    if (verbose) {
      Formatter f = new Formatter();
      f.format(fmt, args);
      System.err.println(f);
    }
  }
  
  /**
   * Parses an input board and tile sequence.
   * @param file The path to the file
   * @param b The array in which to store the board tiles
   * @return The tile sequence
   * @throws IOException Garbage in Garbage out
   */
  private static int[] parseBoard(String file, int[] b) throws IOException {
    BufferedReader br = null;
    ArrayList<Integer> sequence = new ArrayList<>();
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
            int val = Integer.parseInt(p[k]);
            if (!Board.valid_tile(val)) {
              throw new IllegalArgumentException(
                      "Invalid tile value: " + p[k] + " - " + c);
            }
            b[i * Board.BOARD_WIDTH + j++] = val;
          }
        }
      }
      br.readLine(); //Skip line

      while ((c = br.readLine()) != null) {
        for (String p : c.split("[^0-9]")) {
          if (!p.isEmpty()) {
            Integer a = Integer.parseInt(p);
            if (!Board.valid_tile(a)) {
              throw new IllegalArgumentException(
                      "Invalid tile value: " + p + " - " + c);
            }
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
   * Parses a moves file
   * @param file The path to the moves file
   * @return A list of directions as dictated by the moves file
   * @throws IOException 
   */
  private static List<Direction> parseMoves(String file) throws IOException {
    BufferedReader br = null;
    List<Direction> moves = new ArrayList<>();
    
    try {
      String c;
      br = new BufferedReader(new FileReader(file));
      br.readLine();
      br.readLine();
      
      while ((c = br.readLine()) != null) {
        Direction.parse(moves, c);
      }
    } finally {
      if (br != null) {
        br.close();
      }
    }
    
    return moves;
  }
  
  private static int parseArgs(String[] args, int pos, Settings settings) {
    int cp = pos;
    int length = args[cp].length();
    
    for (int i = 1; i < length; i++) {
      switch(args[cp].charAt(i)) {
        case 'v':
          verbose = !verbose;
          break;
          
        case 'n':
          settings.noBacktrack = !settings.noBacktrack;
          break;
          
        case 'o':
          if (pos + 1 >= args.length) {
            return -1;
          }
          settings.outputFile = args[++pos];
          break;
          
        case 'm':
          if (pos + 1 >= args.length) {
            return -1;
          }
          settings.movesFile = args[++pos];
          break;
          
        case 'l':
          if (pos + 1 >= args.length) {
            return -1;
          }
          String[] nums = args[++pos].split(",");
          settings.starting_learnfactors = new int[nums.length];
          for (int k = 0; k < nums.length; k++) {
            settings.starting_learnfactors[k] = Integer.parseInt(nums[k]);
          }
          break;
          
        default:
          return -1;
      }
    }
    
    return pos;
  }
  
  public static void usage() {
    System.out.println("CITS3001 Threes solver - 2014");
    System.out.println("Usage: threes [-vn -l <i,j,k,l> -o <output_file> -m <moves_file>] input_file");
    System.out.println("Options:");
    System.out.println("  -v Enables verbose output to stderr.");
    System.out.println("  -n Disables backtracking.");
    System.out.println("  -l <i,j,k,l> (Manual) learning mode.");
    System.out.println("  -o <output_file> Writes the moves to the specified file.");
    System.out.println("  -m <moves_file> Reads in a moves file to play the board with (benchmarking purposes).");
    System.out.println();
    System.out.println("The output moves will always be printed to stdout.");
    System.out.println("Specifying a moves file with '-m' takes precedence over solving.");
  }
  
  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    Settings settings = new Settings();
    int[] bt = new int[Board.BOARD_SPACE], s;
    
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("-")) {
        int j = parseArgs(args, i, settings);
        if (j < 0) {
          System.err.println("Invalid value: " + args[i]);
          usage();
          return;
        } else {
          i = j;
        }
      } else {
        settings.inputBoard = args[i];
      }
    }
    
    if (settings.inputBoard == null) {
      System.err.println("No input board file specified.");
      usage();
      return;
    }
    
    try {
      s = parseBoard(settings.inputBoard, bt);
    } catch (IOException e) {
      System.err.printf("Invalid file %s: %s\n", 
              settings.inputBoard, e.getMessage());
      return;
    }
    
    if (settings.movesFile != null) {
      List<Direction> moves;
      try {
        moves = parseMoves(settings.movesFile);
      } catch (IOException e) {
        System.err.printf("Invalid input moves file %s: %s\n",
                args[1], e.getMessage());
        return;
      }
      
      long time = System.nanoTime();
      for (int i = 0; i < 40000; i++) {
        System.out.printf("Move %d\n", i);
        Board b = new Board(bt);
        for (Direction move : moves) {
          if (!b.move(s, move)) {
            System.err.println("Failed to move!");
            break;
          }
        }
      }
      
      time = System.nanoTime() - time;
      System.out.println(time / 1000000000.0);
      
      return;
    }
    
    Solver solver = new Solver(settings.starting_learnfactors);
    if (settings.starting_learnfactors != null) {
      System.out.println("Learning factors...");
      System.out.println("Starting with:" + 
                         Arrays.toString(settings.starting_learnfactors));
      solver.learn_factors(new Board(bt), s, false);
      return;
    }

    Board bs;
    long runtime = System.nanoTime();
    if (settings.noBacktrack) {
      bs = solver.solve_ldfs(s, new Board(bt));
    } else {
      bs = solver.solve_mdfs(s, new Board(bt));
    }
    runtime = System.nanoTime() - runtime;
    
    Formatter f = new Formatter();
    f.format("%d, %d, %d, %d, %d, %d\n",
                  bs.score(), bs.dof(), 
                  bs.zeros(), bs.checkerboarding3(),
                  bs.smoothness(), bs.nCombinable());
    f.format("Used %d/%d available moves in %.2f seconds. (%.2f m/s)\n", 
                      bs.moves().length(), s.length, 
                      runtime / 1000000000.0,
                      bs.moves().length() / (runtime / 1000000000.0));
    f.format(bs.moves());
    f.format("\n");
    
    log_info(bs);
    System.out.print(f.toString());
    
    if (settings.outputFile != null) {
      PrintWriter writer = null;
      try {
        writer = new PrintWriter(settings.outputFile, "UTF-8");
        writer.printf(f.toString());
      } catch (IOException e) {
        System.err.printf("Failed to write out to %s: %s\n", 
                settings.outputFile, e.getMessage());
      } finally {
        if (writer != null) {
          writer.close();
        }
      }
    }
  }
  
  private static class Settings {
    String inputBoard, outputFile, movesFile;
    int[] starting_learnfactors;
    boolean noBacktrack;
  }
}
