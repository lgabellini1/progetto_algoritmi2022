package mnkgame;

import java.util.HashMap;

/**
 *  Set delle intersezioni. Di fatto si tratta di una hash table che
 *  ad ogni cella del gioco associa la sua intersezione.
 */
class IntersectionSet {
    private final HashMap<Integer, MNKIntersection> set;

    private final int N;

    public IntersectionSet(MNKBoard B) {
        set = new HashMap<>(B.M*B.N);
        N   = B.N;
        for (MNKCell c : B.getFreeCells())
            set.put(cellIndex(c),new MNKIntersection(c,B));
    }

    private int cellIndex(MNKCell c) {
        return c.i * N + c.j;
    }

    /**
     *  Complessità: O(1)
     *  @param c cella da recuperare nel set
     *  @return la MNKIntersection associata a c;
     */
    public MNKIntersection get(MNKCell c) {
        MNKIntersection i = set.get(cellIndex(c));
        if (i==null)
            throw new IllegalStateException("Null intersection in set!");
        return i;
    }

    // Test functions
    /*
    public void print(MNKBoard B) {
        for (MNKIntersection i : set.values())
            if (i.valid(B))
                System.out.println(i);
        System.out.print('\n');
    }

    // Test
    public int size(MNKBoard B) {
        int sz=0;
        for (MNKIntersection i : set.values())
            if (i.valid(B)) sz++;
        return sz;
    }
    */
}
