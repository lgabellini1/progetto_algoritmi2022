package mnkgame;

import java.util.*;

public class StrategySet {
    private final ArrayList<MNKStrategy> set;
    public  final MNKCellState player, adv;
    private final ArrayDeque<Integer> generated_stack;
    private final ArrayDeque<ArrayList<InvMNKStrategy>> invalid_stack;
    private final int K;
    private int win_count;

    private static class InvMNKStrategy {
        private final MNKStrategy S;
        private final int index;

        private InvMNKStrategy(MNKStrategy S, int i) {
            this.S = S;
            index  = i;
        }

        @Override
        public String toString() {
            return S.toString();
        }
    }

    // O(1)
    public StrategySet(MNKBoard B, MNKCellState player) {
        set             = new ArrayList<>(4*B.M*B.N);
        this.player     = player;
        generated_stack = new ArrayDeque<>(B.getFreeCells().length);
        invalid_stack   = new ArrayDeque<>(B.getFreeCells().length);
        adv             = player == MNKCellState.P1 ? MNKCellState.P2 : MNKCellState.P1;
        K               = B.K;
        win_count       = 0;
    }

    // O(n log K), where n is the size of the set
    public void update(MNKCell c, MNKBoard B) {
        if (B.cellState(c.i,c.j)==MNKCellState.FREE)
            throw new IllegalArgumentException("Unmarked cell passed as argument!");

        Iterator<MNKStrategy> iter = set.iterator(); int t = 0;
        ArrayList<InvMNKStrategy> invalids = new ArrayList<>();
        invalid_stack.push(invalids);

        while (iter.hasNext()) {
            MNKStrategy S = iter.next();
            if (!S.valid())
                throw new IllegalStateException("Invalid MNStrategy " + S + " found in update.");

            if (S.contains(c)) {
                boolean winning = S.winning();
                S.add(c, B);
                if (!S.valid()) {
                    invalids.add(new InvMNKStrategy(S, t));
                    iter.remove();
                    if (winning) win_count--;
                } else if (!winning && S.winning())
                    win_count++;
            }

            t++;
        }

        if (B.cellState(c.i, c.j) == adv)
            generated_stack.push(0);
        else {
            int generated = 0;

            // Up
            if (c.i - (K - 1) >= 0) {
                MNKStrategy up = new MNKStrategy(B, c);

                for (int i = c.i - (K - 1); i <= c.i && up.valid(); i++) {
                    MNKCell a = new MNKCell(i, c.j, B.cellState(i, c.j));
                    up.setRange(a);
                    if (B.cellState(a.i, a.j) != MNKCellState.FREE)
                        up.add(a, B);
                } if (up.valid() && !set.contains(up)) {
                    set.add(up);
                    if (up.winning()) win_count++;
                    generated++;
                }
            }

            // Down
            if (c.i + K - 1 < B.M) {
                MNKStrategy down = new MNKStrategy(B, c);

                for (int i = c.i; i <= c.i + K - 1 && down.valid(); i++) {
                    MNKCell a = new MNKCell(i, c.j, B.cellState(i, c.j));
                    down.setRange(a);
                    if (B.cellState(a.i, a.j) != MNKCellState.FREE)
                        down.add(a, B);
                }
                if (down.valid() && !set.contains(down)) {
                    set.add(down);
                    if (down.winning()) win_count++;
                    generated++;
                }
            }

            // Left
            if (c.j - (K - 1) >= 0) {
                MNKStrategy left = new MNKStrategy(B, c);

                for (int j = c.j - (K - 1); j <= c.j && left.valid(); j++) {
                    MNKCell a = new MNKCell(c.i, j, B.cellState(c.i, j));
                    left.setRange(a);
                    if (B.cellState(a.i, a.j) != MNKCellState.FREE)
                        left.add(a, B);
                }
                if (left.valid() && !set.contains(left)) {
                    set.add(left);
                    if (left.winning()) win_count++;
                    generated++;
                }
            }

            // Right
            if (c.j + K - 1 < B.N) {
                MNKStrategy right = new MNKStrategy(B, c);

                for (int j = c.j; j <= c.j + K - 1 && right.valid(); j++) {
                    MNKCell a = new MNKCell(c.i, j, B.cellState(c.i, j));
                    right.setRange(a);
                    if (B.cellState(a.i, a.j) != MNKCellState.FREE)
                        right.add(a, B);
                }
                if (right.valid() && !set.contains(right)) {
                    set.add(right);
                    if (right.winning()) win_count++;
                    generated++;
                }
            }

            // Up-left
            if (c.i - (K - 1) >= 0 && c.j - (K - 1) >= 0) {
                MNKStrategy upleft = new MNKStrategy(B, c);

                for (int i = c.i - (K - 1), j = c.j - (K - 1); i <= c.i && j <= c.j && upleft.valid(); i++, j++) {
                    MNKCell a = new MNKCell(i, j, B.cellState(i, j));
                    upleft.setRange(a);
                    if (B.cellState(a.i, a.j) != MNKCellState.FREE)
                        upleft.add(a, B);
                }
                if (upleft.valid() && !set.contains(upleft)) {
                    set.add(upleft);
                    if (upleft.winning()) win_count++;
                    generated++;
                }
            }

            // Up-right
            if (c.i - (K - 1) >= 0 && c.j + K - 1 < B.N) {
                MNKStrategy upright = new MNKStrategy(B, c);

                for (int i = c.i - (K - 1), j = c.j + K - 1; i <= c.i && j >= c.j && upright.valid(); i++, j--) {
                    MNKCell a = new MNKCell(i, j, B.cellState(i, j));
                    upright.setRange(a);
                    if (B.cellState(a.i, a.j) != MNKCellState.FREE)
                        upright.add(a, B);
                }
                if (upright.valid() && !set.contains(upright)) {
                    set.add(upright);
                    if (upright.winning()) win_count++;
                    generated++;
                }
            }

            // Down-left
            if (c.i + K - 1 < B.M && c.j - (K - 1) >= 0) {
                MNKStrategy downleft = new MNKStrategy(B, c);

                for (int i = c.i, j = c.j; i <= c.i + K - 1 && j >= c.j - (K - 1) && downleft.valid(); i++, j--) {
                    MNKCell a = new MNKCell(i, j, B.cellState(i, j));
                    downleft.setRange(a);
                    if (B.cellState(a.i, a.j) != MNKCellState.FREE)
                        downleft.add(a, B);
                }
                if (downleft.valid() && !set.contains(downleft)) {
                    set.add(downleft);
                    if (downleft.winning()) win_count++;
                    generated++;
                }
            }

            // Down-right
            if (c.i + K - 1 < B.M && c.j + K - 1 < B.N) {
                MNKStrategy downright = new MNKStrategy(B, c);

                for (int i = c.i, j = c.j; i <= c.i + K - 1 && j <= c.j + K - 1 && downright.valid(); i++, j++) {
                    MNKCell a = new MNKCell(i, j, B.cellState(i, j));
                    downright.setRange(a);
                    if (B.cellState(a.i, a.j) != MNKCellState.FREE)
                        downright.add(a, B);
                }
                if (downright.valid() && !set.contains(downright)) {
                    set.add(downright);
                    if (downright.winning()) win_count++;
                    generated++;
                }
            }

            generated_stack.push(generated);
        }

        // Test
        /*if (win_count < 0)
            throw new IllegalStateException("Negative win count");
        if (win_count > set.size())
            throw new IllegalStateException("More winning strategies (" + win_count +
                    ") than factual strategies (" + set.size());

        if (set.size() + invalid_size() != gen_size())
            throw new IllegalStateException(player + ": invariant broken!\nSet is: " + set + "\nInvalids are: " +
                    invalids + "\nGenerated number is: " + gen_size());*/
    }

    // O(n log K), where n the size of the set
    public void undo(MNKCell c, MNKBoard B) {
        if (B.cellState(c.i, c.j) == MNKCellState.FREE)
            throw new IllegalArgumentException("Input cell is FREE: cell should be marked!");

        int MAX = generated_stack.pop();
        int n = set.size() - 1;
        for (int i = n; i > n - MAX; i--) {
            if (set.get(i).winning())
                win_count--;
            set.remove(i);
        }

        ArrayList<InvMNKStrategy> invalids = invalid_stack.pop();
        for (InvMNKStrategy S : invalids) {
            set.add(S.index, S.S);
            if (S.S.winning())
                win_count++;
        }

        for (MNKStrategy S : set) {
            if (S.contains(c)) {
                boolean winning = S.winning();
                S.remove(c,B);

                if (!S.valid())
                    throw new IllegalStateException("Invalid strategy " + S + " left in the set");

                if (winning && !S.winning())
                    win_count--;
                else if (!winning && S.winning()) {
                    win_count++;
                }
            }
        }

        // Test
        /* if (win_count < 0)
            throw new IllegalStateException("Negative win count");
        if (win_count > set.size())
            throw new IllegalStateException("More winning strategies (" + win_count +
                    ") than factual strategies (" + set.size() + ")");

        if (set.size() + invalid_size() != gen_size())
            throw new IllegalStateException(player + ": invariant broken! Set is: " + set + "\nInvalids are: " +
                    invalids + "\nGenerated number is: " + gen_size()); */
    }

    // O(n^2 x K log K), where n is the size of the set
    public IntersectionSet generateFrom(MNKBoard B) {
        IntersectionSet iSet = new IntersectionSet(B);
        for (MNKStrategy S : set) {
            for (MNKStrategy T : set) {
                if (S != T) {
                    ArrayList<MNKIntersection> intersection = S.intersects(T, B);
                    if (intersection != null) {
                        iSet.addAll(intersection);
                        if (intersection.get(0).winning())
                            return iSet;
                    }
                }
            }
        } return iSet.size() > 0 ? iSet : null;
    }

    // O(1)
    public int size() {
        return set.size();
    }

    // O(1)
    public int winning() {
        return win_count;
    }

    // Test functions
    public MNKStrategy[] set() { return set.toArray(new MNKStrategy[0]); }

    public void test_print() {
        System.out.println(player + ": ");
        System.out.println("Set contains " + size() + " strategies.");
        System.out.println("->" + set);
        System.out.println("Set has stored " + invalid_size() + " invalid strategies.");
        System.out.println("->" + invalid_stack);
        System.out.println("Winning strategies are " + win_count);
        System.out.println("Generated strats stack state: " + generated_stack);

        System.out.println();
    }

    private int gen_size() {
        int tot = 0;
        for (int n : generated_stack)
            tot += n;
        return tot;
    }

    private int invalid_size() {
        int tot = 0;
        for (ArrayList<InvMNKStrategy> l : invalid_stack)
            tot += l.size();
        return tot;
    }
}