package mnkgame;

import java.util.LinkedList;

public class MNKIntersection {
    public final MNKCell c;
    private final LinkedList<MNKStrategy> strategies;
    private final int K;
    private int K_minus2; // Number of strategies at K-2 symbols.

    // O(1)
    public MNKIntersection(MNKCell c, MNKBoard B, MNKStrategy[] strats) {
        this.c     = c;
        strategies = new LinkedList<>();
        K          = B.K;
        K_minus2   = 0;

        for (MNKStrategy S : strats)
            add(S);
    }

    // O(1)
    public void add(MNKStrategy S) {
        if (S.valid()) {
            strategies.add(S);
            if (S.size() >= K-2) 
                K_minus2++;
        }
    }

    // O(n), where n is the number of strategies intersecting in I
    public void merge(MNKIntersection I) {
        if (I.equals(this)) {
            for (MNKStrategy S : I.strategies)
                if (!strategies.contains(S))
                    add(S);
        } 
    }

    // O(1)
    public boolean winning() {
        return K_minus2>=2;
    }

    // O(1)
    public int cardinality() {
        return strategies.size();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof MNKIntersection)) return false;

        MNKIntersection I = (MNKIntersection) o;
        if (I.c.i == c.i && I.c.j == c.j && I.c.state != c.state)
            throw new IllegalStateException("Same cell should have the same state");
        return I.c.equals(c);
    }

    @Override
    public int hashCode() {
        return c.hashCode();
    }

    @Override
    public String toString() {
        return "Intersection at " + c.toString();
    }
}