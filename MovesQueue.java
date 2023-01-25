package mnkgame;

import java.util.*;

public class MovesQueue {
    private final PriorityQueue<MNKCellPriority> Q;
    private final HashMap<Integer, MNKCell> hTable; // "Bodyguard": permette di verificare in O(1)
    // se una data cella è presente nella Priority Queue

    private final HashMap<Integer, Integer> pTable; // Priority-table

    public final MNKCellState player;
    private final int N;

    public static final class MNKCellPriority extends MNKCell implements Comparable<MNKCellPriority> {
        private final int priority;

        public MNKCellPriority(int i, int j, MNKCellState state, int priority) {
            super(i, j, state);
            this.priority = priority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            MNKCellPriority that = (MNKCellPriority) o;
            return that.i == i && that.j == j;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public int compareTo(MNKCellPriority c) {
            if (priority == c.priority) {
                return 0;
            } else if (priority < c.priority) {
                return +1;
            }
            return -1;

            // return Integer.compare(priority, c.priority);
        }

        @Override
        public String toString() {
            return "[" + i + "," + j + "] - priority=" + priority;
        }
    }

    public MovesQueue(MNKBoard B, MNKCellState p) {
        Q = new PriorityQueue<>(B.M * B.N / 2);
        hTable = new HashMap<>(B.M * B.N);
        pTable = new HashMap<>(B.M * B.N);
        N = B.N;
        player = p;

        for (MNKCell c : B.getFreeCells())
            pTable.put(cellIndex(c), 0);
    }

    private int cellIndex(MNKCell c) {
        return c.i * N + c.j;
    }

    /**
     * Data una cella c, aggiorna la sua priorità nella coda di mosse.
     * Complessità: O(n), dove n è la dimensione della
     * coda, se c è contenuto; O(log n) altrimenti
     * 
     * @param c        cella di cui si vuole aggiornare la priorità
     * @param priority nuova priorità
     */
    public void shiftPriority(MNKCell c, MNKBoard B, int priority) {
        if (B.cellState(c.i, c.j) != MNKCellState.FREE)
            throw new IllegalStateException("Can't change priority of marked cell");
        if (priority < 0)
            throw new IllegalStateException("Invalid priority!");

        MNKCellPriority cp = new MNKCellPriority(c.i, c.j, B.cellState(c.i, c.j), priority);
        pTable.put(cellIndex(c), priority); // Aggiornamento priorità nella tabella

        if (priority == 0)
            remove(c, B);
        else { // Rimozione e re-inserimento a priorità aggiornata
            if (hTable.get(cellIndex(c)) != null)
                Q.remove(cp);
            else
                hTable.put(cellIndex(c), c);
            Q.add(cp);
        }

        if (Q.size() != hTable.size())
            throw new IllegalStateException("Mismatch between queue and hash table.");
    }

    /**
     * Complessità: O(n log n), dove n è la dimensione della coda
     * 
     * @return mosse nella coda in ordine di priorità
     */
    public MNKCell[] moves() {
        if (Q.size() == 0)
            return null;
        MNKCell[] queue = Q.toArray(new MNKCell[0]);
        Arrays.sort(queue);
        return queue;
    }

    /**
     * Rimuove una mossa c marcata dalla coda.
     * Complessità: O(n); dove n è la dimensione della
     * coda, se c è contenuto; O(1) altrimenti
     * 
     * @param c cella marcata
     */
    public void remove(MNKCell c, MNKBoard B) {
        if (hTable.get(cellIndex(c)) != null) {
            hTable.remove(cellIndex(c));
            boolean found = Q.remove(new MNKCellPriority(c.i, c.j, B.cellState(c.i, c.j), -1)); // Valore fittizio

            if (!found)
                throw new IllegalStateException("Can't find cell that should be in the queue");
            if (Q.size() != hTable.size())
                throw new IllegalStateException("Mismatch between queue and hash table.");
        }
    }

    /**
     * Ripristina la cella c smarcata nella coda di mosse.
     * Complessità: O(log n), dove n è la dimensione della coda
     * 
     * @param c cella appena smarcata
     */
    public void undo(MNKBoard B, MNKCell c) {
        if (hTable.get(cellIndex(c)) != null)
            throw new IllegalStateException("[" + c.i + "," + c.j + "] should not be in the queue!");

        if (pTable.get(cellIndex(c)) != 2) {
            hTable.put(cellIndex(c), c);
            Q.add(new MNKCellPriority(c.i, c.j, B.cellState(c.i, c.j), pTable.get(cellIndex(c))));
            // Nota: re-inseriamo c nella coda con la priorità che aveva nel momento in cui
            // è stata marcata.

            if (Q.size() != hTable.size())
                throw new IllegalStateException("Mismatch between queue and hash table.");
        }
    }

    // Test
    public void printQueue() {
        System.out.println("Queue size: " + Q.size());
        for (MNKCellPriority cp : Q)
            System.out.println(cp);
        System.out.print('\n');
    }

    public boolean isContained(MNKCell c) {

        return hTable.get(cellIndex(c)) != null;

    }

    public int getPriority(MNKCell c) {
        return pTable.get(cellIndex(c));
    }
}
