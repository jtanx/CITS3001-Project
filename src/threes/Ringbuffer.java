
package threes;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Buffers the previous n moves made.
 * When retrieving from the buffer, the furthest move made is returned
 * and the buffer is cleared.
 * @author
 */
public class Ringbuffer<T> {
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
