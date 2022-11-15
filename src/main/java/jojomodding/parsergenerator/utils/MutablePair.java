package jojomodding.parsergenerator.utils;

public class MutablePair<K,V> {
    private K first;
    private V second;

    public MutablePair(K k, V v) {
        this.first = k;
        this.second = v;
    }

    public K getFirst() {
        return first;
    }

    public void setFirst(K first) {
        this.first = first;
    }

    public V getSecond() {
        return second;
    }

    public void setSecond(V second) {
        this.second = second;
    }
}
