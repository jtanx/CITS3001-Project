
package threes;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author
 */
public class Ringbuffer<T> {
  private LinkedList<T> buf;
  private final int maxSize;
  
  public Ringbuffer(int maxSize) {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("Must have a size greater than 0.");
    }
    this.maxSize = maxSize;
    buf = new LinkedList<T>();
  }
  
  public void push(T it) {
    if (buf.size() > maxSize) {
      buf.removeFirst();
    }
    buf.addLast(it);
  }
  
  public T pop() {
    if (!buf.isEmpty()) {
      T ret = buf.peekFirst();
      buf.clear();
      return ret;
    }
    throw new IllegalStateException("Can't pop an empty ringbuffer");
  }
}
