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
 * This project requires JRE version >= 7.
 * @author Jeremy Tan, 20933708
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
          
        case 's':
          settings.singleThreaded = !settings.singleThreaded;
          break;
          
        case 'd':
          settings.useDLDFS = !settings.useDLDFS;
        break;
          
        case 'a':
          if (pos + 1 >= args.length) {
            return -1;
          }
          settings.lookahead = Integer.parseInt(args[++pos]);
          break;
          
        case 'q':
          if (pos + 1 >= args.length) {
            return -1;
          }
          settings.qui_size = Integer.parseInt(args[++pos]);
          break;
          
        case 'u':
          if (pos + 1 >= args.length) {
            return -1;
          }
          settings.pq_size = Integer.parseInt(args[++pos]);
          break;
          
        case 'i':
          if (pos + 1 >= args.length) {
            return -1;
          }
          settings.ipq_size = Integer.parseInt(args[++pos]);
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
    System.out.println("Usage: threes [-vsnd -l <i,j,k,l> -a <lookahead> -u <pq_sz> -i <ipq_sz> -q <qui_sz> -o <output_file> -m <moves_file>] input_file");
    System.out.println("Options:");
    System.out.println("  -v Enables verbose output to stderr.");
    System.out.println("  -s Runs the solving algorithm in single-threaded mode.");
    System.out.println("  -d Uses depth-limited depth-first search (DLDFS), instead of priority search.");
    System.out.println("  -n DLDFS mode: Disables backtracking.");
    System.out.println("  -a <lookahead> Controls the lookahead. Default is 8 moves.");
    System.out.println("  -q <qui_sz> Changes the quiescence limit. Default is 11000.");
    System.out.println("  -u <pq_sz> Changes the size limit of the main priority queue. Default is 150.");
    System.out.println("  -i <ipq_sz> Changes the size limit of the individual priority queues. Default is 3.");
    System.out.println("  -l <i,j,k,l> (Manual) learning mode. <i,j,k,l> specifies the inital weights");
    System.out.println("  -o <output_file> Writes the moves to the specified file.");
    System.out.println("  -m <moves_file> Reads in a moves file to play the board (benchmarking purposes).");
    System.out.println();
    System.out.println("The output moves will always be printed to stdout.");
    System.out.println("Specifying a moves file with '-m' takes precedence over solving.");
    System.out.println("Unless otherwise specified, options refer to when using priority search (default).");
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
                settings.movesFile, e.getMessage());
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
    
    if (settings.starting_learnfactors != null) {
      System.out.println("Learning factors...");
      System.out.println("Starting with:" + 
                         Arrays.toString(settings.starting_learnfactors));
      DLDFSolver solver = new DLDFSolver(s, settings.starting_learnfactors,
                                         settings.singleThreaded,
                                         !settings.noBacktrack);
      solver.learn_factors(new Board(bt), false);
      return;
    } 
    
    Solver solver;
    if (settings.useDLDFS) {
      solver = new DLDFSolver(s, settings.singleThreaded, !settings.noBacktrack);
    } else {
      solver = new ASSolver(s, settings.singleThreaded, settings.lookahead, 
                            settings.pq_size, settings.ipq_size, settings.qui_size);
    }

    Board bs;
    long runtime = System.nanoTime();
    bs = solver.solve(new Board(bt));
    runtime = System.nanoTime() - runtime;
    
    Formatter f = new Formatter();
    String endl = System.getProperty("line.separator");
    f.format("%d, %d, %d, %d, %d, %d%s",
              bs.score(), bs.dof(), 
              bs.zeros(), bs.checkerboarding3(),
              bs.smoothness(), bs.nCombinable(), endl);
    f.format("Used %d/%d available moves in %.2f seconds. (%.2f m/s)%s", 
              bs.moves().length(), s.length, 
              runtime / 1000000000.0,
              bs.moves().length() / (runtime / 1000000000.0),
              endl);
    f.format(bs.moves());
    
    log_info(bs);
    System.out.println(f.toString());
    if (settings.outputFile != null) {
      try (PrintWriter writer = new PrintWriter(settings.outputFile, "UTF-8")) {
        writer.println(f.toString());
      } catch (IOException e) {
        System.err.printf("Failed to write out to %s: %s\n", 
                settings.outputFile, e.getMessage());
      }
    }
  }
  
  private static class Settings {
    String inputBoard, outputFile, movesFile;
    int[] starting_learnfactors;
    boolean noBacktrack, singleThreaded, useDLDFS;
    int lookahead = -1, pq_size = -1, ipq_size = -1, qui_size = -1;
  }
}
