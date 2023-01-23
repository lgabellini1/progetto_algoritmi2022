package mnkgame;

import java.util.*;

/**
 *  Rappresenta l'insieme di MNKStrategy possibili associate a uno specifico giocatore.
 *  L'algoritmo mantiene quindi due StrategySet distinti: uno per il giocatore che massimizza
 *  e l'altro per il giocatore che minimizza.
 */
public class StrategySet {

    /**
     *  Set delle MNKStrategy
     */
    private final ArrayList<MNKStrategy> set;

    public final IntersectionSet intersections;

    public  final MNKCellState player, adv;

    /**
     *  Stack delle MNKStrategy generate. Per ogni turno, mantiene il numero
     *  di MNKStrategy che il player genera con una data mossa.
     */
    private final ArrayDeque<Integer> generated_stack;

    /**
     *  Stack delle MNKStrategy invalide. Mantiene, per ogni turno, la lista
     *  di MNKStrategy ha reso invalide con la sua mossa.
     */
    private final ArrayDeque<ArrayList<InvMNKStrategy>> invalid_stack;

    private final int N,K;

    /**
     *  Numero di MNKStrategy vincenti (a K-1 simboli) nel set.
     */
    private int win_count;

    /**
     *  Classe ausiliaria per invalid_stack. Memorizza una MNKStrategy con
     *  il suo indice nel set nel momento in cui è stata resa invalida.
     */
    private record InvMNKStrategy(MNKStrategy S, int index) {}

    /**
     * Complessità: O(1)
     * @param B MNKBoard di gioco
     * @param player giocatore a cui è associato il set
     */
    public StrategySet(MNKBoard B, MNKCellState player) {
        set             = new ArrayList<>(4*B.M*B.N);
        intersections   = new IntersectionSet(B);
        this.player     = player;
        generated_stack = new ArrayDeque<>(B.getFreeCells().length);
        invalid_stack   = new ArrayDeque<>(B.getFreeCells().length);
        adv             = player == MNKCellState.P1 ? MNKCellState.P2 : MNKCellState.P1;
        N               = B.N;
        K               = B.K;
        win_count       = 0;
    }

    /**
     *  Complessità: O(K)
     *  @param S MNKStrategy che si vuole aggiungere al set
     *  @param B configurazione attuale di gioco
     *  @param Q attuale priority queue delle mosse
     *  @param index indice al quale viene aggiunto S
     */
    private void add(MNKStrategy S, MNKBoard B, MovesQueue Q, int index) {
        set.add(index,S);
        if (S.winning())
            win_count++;

        for (MNKCell cell : S.range()) {
            MNKIntersection i = intersections.get(cell);
            i.add(S);

            if (Q.player==S.player && B.cellState(cell.i,cell.j)==MNKCellState.FREE) {
                int p = i.valid(B) ? 0 : 1;
                Q.shiftPriority(cell, B, p);
            }
        }
    }

    /**
     *  Complessità: O(K)
     *  @param S MNKStrategy che si vuole rimuovere dal set (poiché invalidata)
     *  @param B configurazione attuale di gioco
     *  @param Q attuale priority queue delle mosse
     */
    private void remove(MNKStrategy S, MNKBoard B, MovesQueue Q) {
        if (S.winning()) win_count--;

        for (MNKCell cell : S.range()) {
            MNKIntersection i = intersections.get(cell);
            i.remove(S);

            if (Q.player==S.player && B.cellState(cell.i,cell.j)==MNKCellState.FREE) {
                int p = i.valid(B) ? 0 : 2;
                Q.shiftPriority(cell,B,p);
            }
        }
    }

    /**
     *  Funzione "compagno" di markCell(c). E' così strutturata:
     *
     *  1) per ogni MNKStrategy S tale che S contiene c, aggiunge
     *     c ad S;
     *
     *  2) genera a partire da c al più 8 nuove MKNStrategy (una per
     *     direzione) e aggiungi quelle valide al set.
     *
     *  Complessità: O(n), dove n è la dimensione del set
     *  @param c cella marcata nell'algoritmo
     *  @param B MNKBoard di gioco
     */
    public void update(MNKCell c, MNKBoard B, MovesQueue Q) {
        if (B.cellState(c.i,c.j)==MNKCellState.FREE)
            throw new IllegalArgumentException("Unmarked cell passed as argument!");

        //                       1)
        /* -------------------------------------------------- */
        Iterator<MNKStrategy> iter = set.iterator(); int t = 0;

        /* Si costruisce l'insieme di MNKStrategy rese invalide in questo turno e
           lo si spinge sullo stack. Notare che questo insieme sarà vuoto se la cella c marcata
           è una mossa del giocatore a cui appartiene il set, poiché un giocatore non può invalidare
           le sue stesse MNKStrategy. */
        ArrayList<InvMNKStrategy> invalids = new ArrayList<>();
        invalid_stack.push(invalids);

        while (iter.hasNext()) {
            MNKStrategy S = iter.next();
            if (!S.valid())
                throw new IllegalStateException("Invalid MNStrategy " + S + " found in update.");

            if (S.contains(c)) {
                boolean winning = S.winning();
                S.add(c, B);
                if (!S.valid()) { // S è stata invalidata
                    invalids.add(new InvMNKStrategy(S, t));
                    remove(S,B,Q);
                    iter.remove();
                } else if (!winning && S.winning())
                    win_count++;
            }

            t++;
        }
        /* -------------------------------------------------- */


        //                       2)
        /* -------------------------------------------------- */
        if (B.cellState(c.i, c.j) == adv)
            generated_stack.push(0); // Se c appartiene all'avversario, allora sicuramente le generate saranno 0
        else {
            // Numero di MNKStrategy generate nel dato turno.
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
                    add(up,B,Q,set.size());
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
                } if (down.valid() && !set.contains(down)) {
                    add(down,B,Q,set.size());
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
                } if (left.valid() && !set.contains(left)) {
                    add(left,B,Q,set.size());
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
                } if (right.valid() && !set.contains(right)) {
                    add(right,B,Q,set.size());
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
                } if (upleft.valid() && !set.contains(upleft)) {
                    add(upleft,B,Q,set.size());
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
                } if (upright.valid() && !set.contains(upright)) {
                    add(upright,B,Q,set.size());
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
                } if (downleft.valid() && !set.contains(downleft)) {
                    add(downleft,B,Q,set.size());
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
                } if (downright.valid() && !set.contains(downright)) {
                    add(downright,B,Q,set.size());
                    generated++;
                }
            }

            generated_stack.push(generated);
        }
        /* -------------------------------------------------- */

        // Test di correttezza
        if (win_count < 0)
            throw new IllegalStateException("Negative win count");
        if (win_count > set.size())
            throw new IllegalStateException("More winning strategies (" + win_count +
                    ") than factual strategies (" + set.size());

        /*
        if (set.size() + invalid_size() != gen_size())
            throw new IllegalStateException(player + ": invariant broken!\nSet is: " + set + "\nInvalids are: " +
                    invalids + "\nGenerated number is: " + gen_size());
        ArrayList<MNKIntersection> all = new ArrayList<>();
        for (MNKStrategy S : set)
            for (MNKStrategy T : set) {
                ArrayList<MNKIntersection> I = S.intersects(T,B);
                if (I!=null) {
                    for (MNKIntersection i : I)
                        if (!all.contains(i)) all.add(i);
                }
            }
        if (intersections.size(B)!=all.size()) {
            AlphaBetaPro.print(B);
            System.out.println("Set state:");
            print();
            throw new IllegalStateException("Intersection computation is not correct!\n" +
                    "Correct ones are: " + all + "\n(set size is " + intersections.size(B) + ")");
        }
        */
    }

    /**
     *  Funzione "compagno" di unmarkCell() e speculare ad update().
     *  E' strutturata nel modo seguente:
     *
     *  1) rimuovi dal set un numero di MNKStrategy pari a quello restituito
     *     da generated_stack.pop(). Infatti, queste coincideranno esattamente
     *     con le MNKStrategy generate dalla mossa che stiamo annullando;
     *
     *  2) aggiungi nel set le MNKStrategy restituite da invalid_stack.pop().
     *     Questo equivale al ripristinare tutte le MNKStrategy invalidate
     *     dalla mossa che stiamo annullando;
     *
     *  3) per ogni MNKStrategy S (preesistente a c) tale che S contiene c, rimuove
     *     c da S;
     *
     *  Complessità: O(n), dove n è la dimensione del set
     *  @param c cella smarcata nell'algoritmo
     *  @param B MNKBoard di gioco
     */
    public void undo(MNKCell c, MNKBoard B, MovesQueue Q) {
        if (B.cellState(c.i, c.j) == MNKCellState.FREE)
            throw new IllegalArgumentException("Input cell is FREE: cell should be marked!");


        //                        1)
        /* -------------------------------------------------- */
        int MAX = generated_stack.pop(), n = set.size() - 1;
        for (int i = n; i > n - MAX; i--) {
            remove(set.get(i),B,Q);
            set.remove(i);
        }
        /* -------------------------------------------------- */


        //                        2)
        /* -------------------------------------------------- */
        ArrayList<InvMNKStrategy> invalids = invalid_stack.pop();
        for (InvMNKStrategy S : invalids)
            add(S.S,B,Q,S.index);
        /* -------------------------------------------------- */


        //                        3)
        /* -------------------------------------------------- */
        for (MNKStrategy S : set) {
            if (S.contains(c)) {
                boolean winning = S.winning();
                S.remove(c, B);

                if (!S.valid()) {
                    throw new IllegalStateException("Invalid strategy " + S + " left in the set\n" +
                            "Currently removing cell [" + c.i + "," + c.j + "]");
                }

                if (winning && !S.winning()) win_count--;
                else if (!winning && S.winning()) win_count++;
            }
        }
        /* -------------------------------------------------- */


        // Test di correttezza
        if (win_count < 0)
            throw new IllegalStateException("Negative win count");
        if (win_count > set.size())
            throw new IllegalStateException("More winning strategies (" + win_count +
                    ") than factual strategies (" + set.size() + ")");
        /* if (set.size() + invalid_size() != gen_size())
            throw new IllegalStateException(player + ": invariant broken! Set is: " + set + "\nInvalids are: " +
                    invalids + "\nGenerated number is: " + gen_size()); */
    }

    /**
     * Complessità: O(1)
     * @return dimensione n del set
     */
    public int size() {
        return set.size();
    }

    /**
     * Complessità: O(1)
     * @return MNKStrategy vincenti
     */
    public int winning() {
        return win_count;
    }

    /**
     *  Complessità: O(n), dove n è la dimensione del set
     *  @param B MNKBoard di gioco attuale
     *  @return la cella c che, se marcata, porta alla vittoria
     *          il giocatore possessore del set
     */
    public MNKCell winningCell(MNKBoard B) {
        for (MNKStrategy S : set)
            if (S.valid() && S.winning()) return S.getWinCell(B);
        throw new IllegalStateException("Should have found a single-move win.");
    }

    // Debug
    public void print() {
        System.out.println(player + ": ");
        for (MNKStrategy S : set)
            System.out.println(S);
        System.out.print('\n');
    }
}
