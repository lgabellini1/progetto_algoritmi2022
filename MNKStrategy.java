package mnkgame;

import java.util.ArrayList;

public class MNKStrategy {
    private int my_cells, adv_cells;
    private final ArrayList<MNKCell> range;
    private final int M,N,K;
    public  final MNKCellState player;
    private boolean valid, prev_gen;

    // O(1)
    public MNKStrategy(MNKBoard B, MNKCell c) {
        my_cells = adv_cells = 0;
        range    = new ArrayList<>(B.K);
        M        = B.M; 
        N        = B.N; 
        K        = B.K;
        player   = B.cellState(c.i, c.j);
        valid    = true;

        if (player == MNKCellState.FREE)
            throw new IllegalArgumentException("Cell in input is unmarked: player is FREE.");
    }

    // O(1)
    private int cellIndex(MNKCell c) {
        return c.i * N + c.j;
    }

    private boolean rec_contains(MNKCell c, int s, int e) {
        if (s > e) return false;

        int m = Math.floorDiv(s + e, 2);
        if (cellIndex(c) == cellIndex(range.get(m)))
            return true;
        else if (cellIndex(c) > cellIndex(range.get(m)))
            return rec_contains(c, m + 1, e);
        else 
            return rec_contains(c, s, m - 1);
    }

    // O(log K)
    public boolean contains(MNKCell c) {
        return rec_contains(c, 0, K - 1);
    }

    // O(1)
    public void setRange(MNKCell c) {
        range.add(c);
        if (range.size() > K)
            throw new IllegalStateException("Range too large for MNKStrategy");
    }

    // O(1)
    public void add(MNKCell c, MNKBoard B) {
        if (B.cellState(c.i, c.j) == MNKCellState.FREE)
            throw new IllegalArgumentException("Free cell added to MNKStrategy!");

        if (B.cellState(c.i, c.j) == player)
            my_cells++;
        else {
            adv_cells++;
            valid = false;
        }

        if (my_cells > K || adv_cells > K) 
            throw new IllegalStateException("More than K cells in a single strategy!");
    }

    // O(1)
    public void remove(MNKCell c, MNKBoard B) {
        if (B.cellState(c.i, c.j) == MNKCellState.FREE)
            throw new IllegalArgumentException("Free cell removed from MNKStrategy!");

        if (B.cellState(c.i, c.j) == player) {
            my_cells--;
            valid = my_cells > 0;
        } else {
            adv_cells--;
            valid = adv_cells == 0;
        }

        if (my_cells < 0 || adv_cells < 0)
            throw new IllegalStateException("Negative number of cells in strategy!");
    }

    // O(1)
    public int size() {
        return my_cells;
    }

    // O(1)
    public boolean winning() {
        return my_cells >= K-1;
    }
    
    // O(1)
    public boolean valid() {
        return valid;
    }

    // O(K log K)
    public ArrayList<MNKIntersection> intersects(MNKStrategy S, MNKBoard B) {
        if (S.equals(this)) return null;

        ArrayList<MNKIntersection> intersection = new ArrayList<>(K-1);
        for (MNKCell c : range) {
            if (B.cellState(c.i, c.j) == MNKCellState.FREE && S.contains(c))
                intersection.add(new MNKIntersection(c, B, new MNKStrategy[] { this, S }));
        }

        if (intersection.size() > K-1)
            throw new IllegalStateException("Two strategies cannot intersect in more than K-1 cells");
        return intersection.size() > 0 ? intersection : null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof MNKStrategy)) return false;

        MNKStrategy S = (MNKStrategy) o;
        return cellIndex(range.get(0)) == cellIndex(S.range.get(0)) && 
            cellIndex(range.get(K-1)) == cellIndex(S.range.get(K-1));
    }

    @Override
    public int hashCode() {
        return range.hashCode();
    }

    @Override
    public String toString() {
        return "MNKStrategy from [" + range.get(0).i + "," + range.get(0).j + "] to [" +
            range.get(K-1).i + "," + range.get(K-1).j + "]";
    }
}