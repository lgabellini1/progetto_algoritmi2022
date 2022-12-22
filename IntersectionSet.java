package mnkgame;

import java.util.ArrayList;
import java.util.Collection;

public class IntersectionSet {
    private final ArrayList<MNKIntersection> set;
    private final int M,N,K;
    private int max; // Max number of strategies intersecting in a single cell
    private boolean winning;

    // O(1)
    public IntersectionSet(MNKBoard B) {
        set     = new ArrayList<>(4*B.M*B.N);
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

    private void rec_add(MNKIntersection I, int s, int e) {
        if (s > e) {
            set.add(s, I);

            if (set.get(s).cardinality() > max) 
                max = set.get(s).cardinality();
            if (set.get(s).winning()) 
                winning = true;
            return;
        } 

        int m = Math.floorDiv(s + e, 2);
        if (set.get(m).equals(I)) {
            set.get(m).merge(I);

            if (set.get(m).cardinality() > max) 
                max = set.get(m).cardinality();
            if (set.get(m).winning()) 
                winning = true;
        } else if (cellNum(I.c) > cellNum(set.get(m).c))
            rec_add(I, m + 1, e);
        else 
            rec_add(I, s, m - 1);
    }

    // O(log n)
    public void add(MNKIntersection I) {
        if (set.size() == 0 || cellNum(I.c) > cellNum(set.get(set.size() - 1).c))
            set.add(I);
        else if (cellNum(I.c) < cellNum(set.get(0).c))
            set.add(0,I);
        else 
            rec_add(I, 0, set.size() - 1);
    }

    // O(m log n), where m is the size of the input collection
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