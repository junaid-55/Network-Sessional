package util;

import java.io.Serializable;

public class Pair<K, V> implements Serializable {
    private K first;
    private V second;
    
    public Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }
    
    public K first() {
        return first;
    }
    
    public V second() {
        return second;
    }
    
    public void setFirst(K first) {
        this.first = first;
    }
    
    public void setSecond(V second) {
        this.second = second;
    }
}