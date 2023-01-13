package mnkgame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class IntersectionSet {
    private final HashMap<Integer, MNKIntersection> set;
    // private final ArrayList<MNKIntersection> set;
    private final int M,N,K;
    private int max; // Max number of strategies intersecting in a single cell
    private boolean winning;

    // O(1)
    public IntersectionSet(MNKBoard B) {
        // set  = new ArrayList<>(4*B.M*B.N);
        set     = new HashMap<>((B.M * B.N) / 2);
        M       = B.M;
        N       = B.N;
        K       = B.K;
        max     = 0;
        winning = false;
    }

    // O(1)
    private int cellNum(MNKCell c) {
        return c.i * N + c.j;
    }

    // O(1)
    public void add(MNKIntersection I) {
        MNKIntersection i = set.get(I.hashCode());
        if (i!=null) {
            i.merge(I);
            if (i.cardinality()>max)
                max = i.cardinality();
            if (i.winning())
                winning = true;
        } else {
            if (I.cardinality()>max)
                max = I.cardinality();
            if (I.winning())
                winning = true;
            set.put(I.hashCode(),I);
        }
    }

    // O(m), where m is the size of the input collection
    public void addAll(Collection<MNKIntersection> collection) {
        for (MNKIntersection I : collection) add(I);
    }

    // O(1)
    public int max() {
        return max;
    }
    
    // O(1)
    public int size() {
        return set.size();
    }
    
    // O(1)
    public boolean winning() {
        return winning;
    }
}