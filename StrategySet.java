package mnkgame;

import java.util.*;

/**
 * Rappresenta l'insieme di MNKStrategy possibili associate a uno specifico
 * giocatore.
 * L'algoritmo mantiene quindi due StrategySet distinti: uno per il giocatore
 * che massimizza
 * e l'altro per il giocatore che minimizza.
 */
public class StrategySet {

    /**
     * Set delle MNKStrategy
     */
    private final ArrayList<MNKStrategy> set;

    public final IntersectionSet intersections;

    public final MNKCellState player, adv;

    /**
     * Stack delle MNKStrategy generate. Per ogni turno, mantiene il numero
     * di MNKStrategy che il player genera con una data mossa.
     */
    private final ArrayDeque<Integer> generated_stack;

    /**
     * Stack delle MNKStrategy invalide. Mantiene, per ogni turno, la lista
     * di MNKStrategy ha reso invalide con la sua mossa.
     */
    private final ArrayDeque<ArrayList<InvMNKStrategy>> invalid_stack;

    private final int N, K;

    /**
     * Numero di MNKStrategy vincenti (a K-1 simboli) nel set.
     */
    private int win_count;

    /**
     * Classe ausiliaria per invalid_stack. Memorizza una MNKStrategy con
     * il suo indice nel set nel momento in cui è stata resa invalida.
     */
    private static class InvMNKStrategy {
        private final MNKStrategy S;
        private final int index;

        private InvMNKStrategy(MNKStrategy S, int i) {
            this.S = S;
            index = i;
        }

        @Override
        public String toString() {
            return S.toString();
        }
    }

    /**
     * Complessità: O(1)
     * 
     * @param B      MNKBoard di gioco
     * @param player giocatore a cui è associato il set
     */
    public StrategySet(MNKBoard B, MNKCellState player) {
        set = new ArrayList<>(4 * B.M * B.N);
        intersections = new IntersectionSet(B);
        this.player = player;
        generated_stack = new ArrayDeque<>(B.getFreeCells().length);
        invalid_stack = new ArrayDeque<>(B.getFreeCells().length);
        adv = player == MNKCellState.P1 ? MNKCellState.P2 : MNKCellState.P1;
        N = B.N;
        K = B.K;
        win_count = 0;
    }

    /**
     * Complessità: O(K)
     * 
     * @param S     MNKStrategy che si vuole aggiungere al set
     * @param B     configurazione attuale di gioco
     * @param Q     attuale priority queue delle mosse
     * @param index indice al quale viene aggiunto S
     */
    private void add(MNKStrategy S, MNKBoard B, MovesQueue Q, int index) {
        set.add(index, S);
        if (S.winning())
            win_count++;

        for (MNKCell cell : S.range()) {
            if (Q.player == S.player && B.cellState(cell.i, cell.j) == MNKCellState.FREE) {
                if (Q.isContained(cell)) {
                    int priority = Q.getPriority(cell);
                    priority = priority + 1;
                    Q.shiftPriority(cell, B, priority);
                } else {
                    Q.shiftPriority(cell, B, 1);
                }
            }
        }
    }

    /**
     * Complessità: O(K)
     * 
     * @param S MNKStrategy che si vuole rimuovere dal set (poiché invalidata)
     * @param B configurazione attuale di gioco
     * @param Q attuale priority queue delle mosse
     */
    private void remove(MNKStrategy S, MNKBoard B, MovesQueue Q) {
        if (S.winning())
            win_count--;

        for (MNKCell cell : S.range()) {
            MNKIntersection i = intersections.get(cell);
            i.remove(S);

            if (Q.player == S.player && B.cellState(cell.i, cell.j) == MNKCellState.FREE) {
                int p = i.valid(B) ? 0 : 2;
                Q.shiftPriority(cell, B, p);
            }
        }
    }

    /**
     * Funzione "compagno" di markCell(c). E' così strutturata:
     *
     * 1) per ogni MNKStrategy S tale che S contiene c, aggiunge
     * c ad S;
     *
     * 2) genera a partire da c al più 8 nuove MKNStrategy (una per
     * direzione) e aggiungi quelle valide al set.
     *
     * Complessità: O(n), dove n è la dimensione del set
     * 
     * @param c cella marcata nell'algoritmo
     * @param B MNKBoard di gioco
     */
    public void update(MNKCell c, MNKBoard B, MovesQueue Q) {
        if (B.cellState(c.i, c.j) == MNKCellState.FREE) {
            throw new IllegalArgumentException("Unmarked cell passed as argument!");
        }
        if (B.cellState(c.i, c.j) == adv) {
            System.out.println("\n" + "---------------------------" + "Sono il giocatore " + player
                    + " e il mio avversario ha giocato la cella : [" + c.i + "," + c.j + "] "
                    + "---------------------------" + "\n");
        } else {
            System.out.println("\n" + "---------------------------" + "Sono il giocatore " + player
                    + " e ho giocato la cella : [" + c.i + "," + c.j + "] " + "---------------------------" + "\n");
        }

        // 1)
        /* -------------------------------------------------- */
        Iterator<MNKStrategy> iter = set.iterator();
        int t = 0;

        /*
         * Si costruisce l'insieme di MNKStrategy rese invalide in questo turno e
         * lo si spinge sullo stack. Notare che questo insieme sarà vuoto se la cella c
         * marcata
         * è una mossa del giocatore a cui appartiene il set, poiché un giocatore non
         * può invalidare
         * le sue stesse MNKStrategy.
         */
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
                    remove(S, B, Q);
                    iter.remove();
                } else if (!winning && S.winning())
                    win_count++;
            }

            t++;
        }
        /* -------------------------------------------------- */

        // 2)
        /* -------------------------------------------------- */
        if (B.cellState(c.i, c.j) == adv)
            generated_stack.push(0); // Se c appartiene all'avversario, allora sicuramente le generate saranno 0
        else {
            // Numero di MNKStrategy generate nel dato turno.
            int generated = 0;

            generated += generaOrizzontali(c, B, 0, Q);

            generated += generaVerticali(c, B, 0, Q);

            generated += generaDiagonali(c, B, 0, Q);

            print();
            generated_stack.push(generated);

        }
        /* -------------------------------------------------- */

        // Test di correttezza
        if (win_count < 0)
            throw new IllegalStateException("Negative win count");
        if (win_count > set.size())
            throw new IllegalStateException("More winning strategies (" + win_count +
                    ") than factual strategies (" + set.size());

    }

    /**
     * Funzione "compagno" di unmarkCell() e speculare ad update().
     * E' strutturata nel modo seguente:
     *
     * 1) rimuovi dal set un numero di MNKStrategy pari a quello restituito
     * da generated_stack.pop(). Infatti, queste coincideranno esattamente
     * con le MNKStrategy generate dalla mossa che stiamo annullando;
     *
     * 2) aggiungi nel set le MNKStrategy restituite da invalid_stack.pop().
     * Questo equivale al ripristinare tutte le MNKStrategy invalidate
     * dalla mossa che stiamo annullando;
     *
     * 3) per ogni MNKStrategy S (preesistente a c) tale che S contiene c, rimuove
     * c da S;
     *
     * Complessità: O(n), dove n è la dimensione del set
     * 
     * @param c cella smarcata nell'algoritmo
     * @param B MNKBoard di gioco
     */
    public void undo(MNKCell c, MNKBoard B, MovesQueue Q) {
        if (B.cellState(c.i, c.j) == MNKCellState.FREE) {
            throw new IllegalArgumentException("Input cell is FREE: cell should be marked!");
        }
        if (B.cellState(c.i, c.j) == adv) {
            System.out.println("\n" + "---------------------------" + "Sono il giocatore " + player
                    + " e annullo la cella del mio avversario: [" + c.i + "," + c.j + "] "
                    + "---------------------------" + "\n");
        } else {
            System.out.println("\n" + "---------------------------" + "Sono il giocatore " + player
                    + " e annullo la cella che ho giocato : [" + c.i + "," + c.j + "] " + "---------------------------"
                    + "\n");
        }

        // 1)
        /* -------------------------------------------------- */
        int MAX = generated_stack.pop(), n = set.size() - 1;
        for (int i = n; i > n - MAX; i--) {
            remove(set.get(i), B, Q);
            set.remove(i);
        }
        /* -------------------------------------------------- */

        // 2)
        /* -------------------------------------------------- */
        ArrayList<InvMNKStrategy> invalids = invalid_stack.pop();
        for (InvMNKStrategy S : invalids)
            add(S.S, B, Q, S.index);
        /* -------------------------------------------------- */

        // 3)
        /* -------------------------------------------------- */
        for (MNKStrategy S : set) {
            if (S.contains(c)) {
                boolean winning = S.winning();
                S.remove(c, B);

                if (!S.valid()) {
                    throw new IllegalStateException("Invalid strategy " + S + " left in the set\n" +
                            "Currently removing cell [" + c.i + "," + c.j + "]");
                }

                if (winning && !S.winning())
                    win_count--;
                else if (!winning && S.winning())
                    win_count++;
            }
        }
        /* -------------------------------------------------- */

        // Test di correttezza
        if (win_count < 0)
            throw new IllegalStateException("Negative win count");
        if (win_count > set.size())
            throw new IllegalStateException("More winning strategies (" + win_count +
                    ") than factual strategies (" + set.size() + ")");
        /*
         * if (set.size() + invalid_size() != gen_size())
         * throw new IllegalStateException(player + ": invariant broken! Set is: " + set
         * + "\nInvalids are: " +
         * invalids + "\nGenerated number is: " + gen_size());
         */
    }

    /**
     * Complessità: O(1)
     * 
     * @return dimensione n del set
     */
    public int size() {
        return set.size();
    }

    /**
     * Complessità: O(1)
     * 
     * @return MNKStrategy vincenti
     */
    public int winning() {
        return win_count;
    }

    /**
     * Complessità: O(n), dove n è la dimensione del set
     * 
     * @param B MNKBoard di gioco attuale
     * @return la cella c che, se marcata, porta alla vittoria
     *         il giocatore possessore del set
     */
    public MNKCell winningCell(MNKBoard B) {
        for (MNKStrategy S : set)
            if (S.valid() && S.winning())
                return S.getWinCell(B);
        throw new IllegalStateException("Should have found a single-move win.");
    }

    // Debug
    public void print() {
        System.out.println(player + ": ");
        for (MNKStrategy S : set)
            System.out.println(S);
        System.out.print('\n');
    }

    public int generaOrizzontali(MNKCell c, MNKBoard B, int generated, MovesQueue Q) {

        for (int x = c.j - (B.K - 1); x <= c.j; x++) {
            if (x >= 0 && x < B.N && x + B.K - 1 < B.N) {
                MNKStrategy strategia = new MNKStrategy(B, c);

                for (int y = x; y < B.K + x && y < B.N && strategia.valid(); y++) {
                    MNKCell cella = new MNKCell(c.i, y, B.cellState(c.i, y));
                    strategia.setRange(cella);
                    if (B.cellState(c.i, y) != MNKCellState.FREE) {
                        strategia.add(cella, B);
                    }
                }
                if (strategia.valid() && !set.contains(strategia)) {
                    // aggiunge/modifica tutte le celle di questa strategia alla coda di priorità
                    add(strategia, B, Q, set.size());
                    generated++;

                }

            }
        }

        return generated;
    }

    public int generaVerticali(MNKCell c, MNKBoard B, int generated, MovesQueue Q) {

        for (int x = c.i - (B.K - 1); x <= c.i; x++) {
            if (x >= 0 && x < B.M && x + B.K - 1 < B.M) {
                MNKStrategy strategia = new MNKStrategy(B, c);
                for (int y = x; y < B.K + x && strategia.valid(); y++) {
                    MNKCell cella = new MNKCell(y, c.j, B.cellState(y, c.j));
                    strategia.setRange(cella);
                    if (B.cellState(y, c.j) != MNKCellState.FREE) {
                        strategia.add(cella, B);
                    }
                }
                if (strategia.valid() && !set.contains(strategia)) {
                    // aggiunge/modifica tutte le celle di questa strategia alla coda di priorità
                    add(strategia, B, Q, set.size());
                    generated++;

                }

            }
        }
        return generated;
    }

    public int generaDiagonali(MNKCell c, MNKBoard B, int generated, MovesQueue Q) {

        for (int x = c.j - (B.K - 1), y = c.i - (B.K - 1); x <= c.j && y <= c.i; x++, y++) {
            if ((x >= 0 && x < B.N && x + B.K - 1 < B.N) && (y >= 0 && y < B.M && y + B.K - 1 < B.M)) {
                System.out.println("Sto analizzando la casella [" + x + "," + y + "]");
                MNKStrategy strategia = new MNKStrategy(B, c);
                for (int a = x, b = y; a < B.K + x && b < B.K + y && strategia.valid(); a++, b++) {
                    MNKCell cella = new MNKCell(b, a, B.cellState(b, a));
                    strategia.setRange(cella);
                    if (B.cellState(b, a) != MNKCellState.FREE) {
                        strategia.add(cella, B);
                    }
                }
                if (strategia.valid() && !set.contains(strategia)) {
                    // aggiunge/modifica tutte le celle di questa strategia alla coda di priorità
                    add(strategia, B, Q, set.size());
                    generated++;

                }

            }
        }

        for (int col = c.j + (B.K - 1), row = c.i - (B.K - 1); col >= c.j && row <= c.i; col--, row++) {
            if ((col >= 0 && col < B.N && col - (B.K - 1) >= 0) && (row >= 0 && row < B.M && row + B.K - 1 < B.M)) {
                System.out.println("Sto analizzando la casella [" + col + "," + row + "]");
                MNKStrategy strategia = new MNKStrategy(B, c);
                for (int x = row, y = col; y >= col - (B.K - 1) && x < row + B.K && strategia.valid(); x++, y--) {
                    MNKCell cella = new MNKCell(x, y, B.cellState(x, y));
                    strategia.setRange(cella);
                    if (B.cellState(x, y) != MNKCellState.FREE) {
                        strategia.add(cella, B);
                    }
                }
                if (strategia.valid() && !set.contains(strategia)) {
                    // aggiunge/modifica tutte le celle di questa strategia alla coda di priorità
                    add(strategia, B, Q, set.size());
                    generated++;

                }

            }
        }

        return generated;
    }

}
