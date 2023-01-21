package mnkgame;

/*
public class MovesQueue {
    private final HashMap<Integer, MNKCellPriority> pTable;
    private final ArrayDeque<MNKCell> inters, strats, free;
    private final int M,N,K;

    public MovesQueue(MNKBoard B) {
        pTable = new HashMap<>(B.M*B.N);
        inters = new ArrayDeque<>();
        strats = new ArrayDeque<>();
        free   = new ArrayDeque<>(Arrays.asList(B.getFreeCells()));
        M      = B.M;
        N      = B.N;
        K      = B.K;

        for (MNKCell c : B.getFreeCells())
            pTable.put(cellIndex(c), new MNKCellPriority(c,3));
    }

    private int cellIndex(MNKCell c) {
        return c.i * N + c.j;
    }

    private static class MNKCellPriority {
        public final MNKCell c;
        private int priority;

        // O(1)
        public MNKCellPriority(MNKCell c, int priority) {
            this.c = c;
            this.priority = priority;
        }

        // O(1)
        public void setPriority(int priority) {
            this.priority = priority;
        }
    }

    /**
     *  Data una cella c di cui si è modificata la priorità,
     *  aggiornare il suo valore di priorità in pTable e
     *  aggiungere c alla queue opportuna.
     *  Complessità: O(1)
     *  @param c cella di cui si vuole aggiornare la priorità
     *  @param priority nuova priorità

    public void shiftPriority(MNKCell c, int priority) {
        if (priority>2 || priority<0)
            throw new IllegalArgumentException("Invalid priority in input");

        MNKCellPriority cp = pTable.get(cellIndex(c));
        if (cp!=null && cp.priority!=priority) {
            cp.setPriority(priority);
            switch (cp.priority) {
                case 0 -> inters.push(c);
                case 1 -> strats.push(c);
                case 2 -> free.push(c);
            }
        }
    }

    /**
     *  In ordine di priorità: scorri la coda dell'insieme di celle a massima priorità
     *  (ovviamente non vuoto) fino a che non trovi una cella valida (ovvero che sia della priorità
     *  corretta secondo la pTable e che sia libera).
     *  Complessità: O(1) nel caso medio
     *  @return successiva mossa da giocare secondo la queue di mosse

    public MNKCell nextMove(MNKBoard B) {
        MNKCell c;

        if (inters.size()>0) {
            do c = inters.poll();
            while(c!=null && (pTable.get(cellIndex(c)).priority != 0 || B.cellState(c.i,c.j)!=MNKCellState.FREE));

            if (c!=null) return c;
        }

        if (strats.size()>0) {
            do c = strats.poll();
            while(c!=null && (pTable.get(cellIndex(c)).priority != 1 || B.cellState(c.i,c.j)!=MNKCellState.FREE));

            if (c!=null) return c;
        }

        if (free.size()>0) {
            do c = free.poll();
            while(c!=null && (pTable.get(cellIndex(c)).priority != 2 || B.cellState(c.i,c.j)!=MNKCellState.FREE));

            if (c!=null) return c;
        }

        return null;
    }

    /**
     *  Complessità: O(1)
     *  @return dimensione totale del set (uguale a FreeCells)

    public int size(MNKBoard B) {
        int sz = inters.size() + strats.size() + free.size();
        if (sz!=B.getFreeCells().length)
            throw new RuntimeException("Sum of free cells lists length is different than total length.");
        return sz;
    }

    // Test
    public void printQueue() {
        System.out.println("Intersections:");
        System.out.println(inters);
        System.out.println("\nMNKStrategies:");
        for (MNKCell c : strats)
            if (pTable.get(cellIndex(c)).priority==1)
                System.out.println(c);
    }
} */

import java.util.*;

public class MovesQueue {
    private final PriorityQueue<MNKCellPriority> Q;
    private final HashMap<Integer, MNKCell> hTable;
    private final HashMap<Integer, Integer> pTable; // Priority-table

    public final MNKCellState player;
    private final int N;

    private static final int MIN_PRIORITY=2;

    public static final class MNKCellPriority extends MNKCell implements Comparable<MNKCellPriority> {
        private final int priority;

        public MNKCellPriority(int i, int j, MNKCellState state, int priority) {
            super(i,j,state);
            this.priority = priority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MNKCellPriority that = (MNKCellPriority) o;
            return that.i == i && that.j == j;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public int compareTo(MNKCellPriority c) {
            return Integer.compare(priority, c.priority);
        }

        @Override
        public String toString() {
            return "[" + i + "," + j + "] - priority=" + priority;
        }
    }

    public MovesQueue(MNKBoard B, MNKCellState p) {
        Q      = new PriorityQueue<>(B.M*B.N / 2);
        hTable = new HashMap<>(B.M*B.N);
        pTable = new HashMap<>(B.M*B.N);
        N      = B.N;
        player = p;

        for (MNKCell c : B.getFreeCells())
            pTable.put(cellIndex(c), MIN_PRIORITY);
    }

    private int cellIndex(MNKCell c) {
        return c.i * N + c.j;
    }

    /**
     *  Data una cella c, aggiorna la sua priorità nella coda di mosse.
     *  Complessità: O(n), dove n è la dimensione della
     *      coda, se c è contenuto; O(log n) altrimenti
     *  @param c cella di cui si vuole aggiornare la priorità
     *  @param priority nuova priorità
     */
    public void shiftPriority(MNKCell c, MNKBoard B, int priority) {
        if (B.cellState(c.i,c.j)!=MNKCellState.FREE)
            throw new IllegalStateException("Can't change priority of marked cell");
        if (priority<0 || priority>MIN_PRIORITY)
            throw new IllegalStateException("Invalid priority!");

        MNKCellPriority cp = new MNKCellPriority(c.i, c.j, B.cellState(c.i, c.j), priority);
        pTable.put(cellIndex(c), priority); // Aggiornamento priorità nella tabella

        if (priority==MIN_PRIORITY)
            remove(c, B);
        else {
            if (hTable.get(cellIndex(c)) != null)
                Q.remove(cp); // Poi la reinseriremo se non è a priorità minima
            else hTable.put(cellIndex(c), c);
            Q.add(cp);
        }

        if (Q.size()!=hTable.size())
            throw new IllegalStateException("Mismatch between queue and hash table.");
    }

    /**
     *  Complessità: O(n log n), dove n è la dimensione della coda
     *  @return mosse nella coda in ordine di priorità
     */
    public MNKCell[] moves() {
        if (Q.size()==0)
            return null;
        MNKCell[] queue = Q.toArray(new MNKCell[0]);
        Arrays.sort(queue);
        return queue;
    }

    /**
     *  Rimuove una mossa c marcata dalla coda.
     *  Complessità: O(n); dove n è la dimensione della
     *      coda, se c è contenuto; O(1) altrimenti
     *  @param c cella marcata
     */
    public void remove(MNKCell c, MNKBoard B) {
        if (hTable.get(cellIndex(c))!=null) {
            hTable.remove(cellIndex(c));
            boolean found=Q.remove(new MNKCellPriority(c.i,c.j,B.cellState(c.i,c.j), -1)); // Valore fittizio

            if (!found)
                throw new IllegalStateException("Can't find cell that should be in the queue");
            if (Q.size()!=hTable.size())
                throw new IllegalStateException("Mismatch between queue and hash table.");
        }
    }

    /**
     *  Ripristina la cella c smarcata nella coda di mosse.
     *  Complessità: O(log n), dove n è la dimensione della coda
     *  @param c cella appena smarcata
     */
    public void undo(MNKBoard B, MNKCell c) {
        if (hTable.get(cellIndex(c))!=null)
            throw new IllegalStateException("[" + c.i + "," + c.j + "] should not be in the queue!");

        if (pTable.get(cellIndex(c))!=2) {
            hTable.put(cellIndex(c), c);
            Q.add(new MNKCellPriority(c.i, c.j, B.cellState(c.i, c.j), pTable.get(cellIndex(c))));
            // Nota: re-inseriamo c nella coda con la priorità che aveva nel momento in cui è stata marcata.

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
}
