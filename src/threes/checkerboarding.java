package threes;

/**
 *
 * @author
 */
public class checkerboarding {
  public static void test() {
    int[] v = {
      12,12,48,0,
      0,0,0,0,
      0,0,0,0,
      0,0,0,0
    };
    
    Board b = new Board(v);
    System.out.println(b);
    System.out.println(b.checkerboarding3());    
  }
}
