package org.webrtc;

public interface Predicate<T> {
  boolean test(T paramT);
  
  default Predicate<T> or(final Predicate<? super T> other) {
    return new Predicate<T>() {
        public boolean test(T arg) {
          return (Predicate.this.test(arg) || other.test(arg));
        }
      };
  }
  
  default Predicate<T> and(final Predicate<? super T> other) {
    return new Predicate<T>() {
        public boolean test(T arg) {
          return (Predicate.this.test(arg) && other.test(arg));
        }
      };
  }
  
  default Predicate<T> negate() {
    return new Predicate<T>() {
        public boolean test(T arg) {
          return !Predicate.this.test(arg);
        }
      };
  }
}
