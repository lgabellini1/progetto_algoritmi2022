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

    private final int K;

    /**
     * Numero di MNKStrategy vincenti (a K-1 simboli) nel set.
     */
    private int win_count;

    private Map<Integer, ArrayList<MNKCell>> celleConPriorita;

    private HashMap<MNKCell, Integer> celleDaPriorizzare;

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
        celleDaPriorizzare = new HashMap<>();
        set = new ArrayList<>(4 * B.M * B.N);
        celleConPriorita = new HashMap<Integer, ArrayList<MNKCell>>();
        this.player = player;
        generated_stack = new ArrayDeque<>(B.getFreeCells().length);
        invalid_stack = new ArrayDeque<>(B.getFreeCells().length);
        adv = player == MNKCellState.P1 ? MNKCellState.P2 : MNKCellState.P1;
        K = B.K;
        win_count = 0;
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
    public void update(MNKCell c, MNKBoard B) {
        if (B.cellState(c.i, c.j) == MNKCellState.FREE)
            throw new IllegalArgumentException("Unmarked cell passed as argument!");

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
                if (S.valid()) {
                    int priorita_da_rimuovere = celleDaPriorizzare.get(c);
                    celleDaPriorizzare.remove(c);
                    ArrayList<MNKCell> array = celleConPriorita.remove(priorita_da_rimuovere);
                    array.remove(c);
                    celleConPriorita.put(priorita_da_rimuovere, array);

                }

                if (!S.valid()) {
                    // private Map<Integer, ArrayList<MNKCell>> celleConPriorita;
                    // private HashMap<MNKCell, Integer> celleDaPriorizzare;
                    ArrayList<MNKCell> celleDellaStrategia = S.getRange();
                    for (MNKCell cella : celleDellaStrategia) {
                        int priorita_da_rimuovere = celleDaPriorizzare.get(cella);
                        if (priorita_da_rimuovere == 1) {
                            celleDaPriorizzare.remove(cella);
                            ArrayList<MNKCell> array = celleConPriorita.remove(priorita_da_rimuovere);
                            array.remove(cella);
                            celleConPriorita.put(priorita_da_rimuovere, array);
                        } else if (priorita_da_rimuovere == 3) {
                            int nuova_priorita = 1;
                            celleDaPriorizzare.replace(cella, priorita_da_rimuovere, nuova_priorita);
                            ArrayList<MNKCell> array = celleConPriorita.remove(priorita_da_rimuovere);
                            array.remove(cella);
                            celleConPriorita.put(priorita_da_rimuovere, array);
                            if (celleConPriorita.get(nuova_priorita) != null) {
                                ArrayList<MNKCell> array2 = celleConPriorita.remove(nuova_priorita);
                                array2.add(cella);
                                celleConPriorita.put(nuova_priorita, array2);
                            } else {
                                ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                array2.add(cella);
                                celleConPriorita.put(nuova_priorita, array2);
                            }

                        } else if (priorita_da_rimuovere > 3) {
                            int nuova_priorita = priorita_da_rimuovere - 1;
                            celleDaPriorizzare.replace(cella, priorita_da_rimuovere, nuova_priorita);
                            ArrayList<MNKCell> array = celleConPriorita.remove(priorita_da_rimuovere);
                            array.remove(cella);
                            celleConPriorita.put(priorita_da_rimuovere, array);
                            if (celleConPriorita.get(nuova_priorita) != null) {
                                ArrayList<MNKCell> array2 = celleConPriorita.remove(nuova_priorita);
                                array2.add(cella);
                                celleConPriorita.put(nuova_priorita, array2);
                            } else {
                                ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                array2.add(cella);
                                celleConPriorita.put(nuova_priorita, array2);
                            }
                        }
                    }
                    invalids.add(new InvMNKStrategy(S, t));
                    iter.remove();
                    if (winning)
                        win_count--;
                } else if (!winning && S.winning())
                    win_count++;
            }

            t++;
        }
        /* -------------------------------------------------- */

        // 2)
        /* -------------------------------------------------- */
        if (B.cellState(c.i, c.j) == adv) {
            /*
             * Se c appartiene all'avversario, allora sicuramente le generate saranno 0.
             * Infatti l'avversario potrebbe generare delle MNKStrategy, ma solo nel set
             * a lui corrispondente.
             */
            generated_stack.push(0);
        } else {
            // Numero di MNKStrategy generate nel dato turno.
            int generated = 0;

            // Up
            // private Map<Integer, ArrayList<MNKCell>> celleConPriorita;
            // private HashMap<MNKCell, Integer> celleDaPriorizzare;
            if (c.i - (K - 1) >= 0) {
                MNKStrategy up = new MNKStrategy(B, c);

                for (int i = c.i - (K - 1); i <= c.i && up.valid(); i++) {
                    MNKCell a = new MNKCell(i, c.j, B.cellState(i, c.j));

                    up.setRange(a);
                    if (B.cellState(a.i, a.j) != MNKCellState.FREE)
                        up.add(a, B);
                }
                if (up.valid() && !set.contains(up)) {
                    set.add(up);
                    ArrayList<MNKCell> celleDellaStrategia = up.getRange();
                    for (int i = 0; i < celleDellaStrategia.size(); i++) {
                        System.out.println("Le celle della strategia sono: " + celleDellaStrategia.get(i));
                    }

                    for (MNKCell cella : celleDellaStrategia) {
                        if (cella.state == MNKCellState.FREE) {
                            if (celleDaPriorizzare.size() == 0) {
                                ArrayList<MNKCell> array = new ArrayList<>();
                                array.add(cella);
                                celleConPriorita.put(1, array);
                                celleDaPriorizzare.put(cella, 1);
                            } else {

                                if (celleDaPriorizzare.get(cella) != null) {
                                    // arriva cella con priorita 1
                                    // 1) rimuovo tutte le celle (l'array in celleConPriorita) con priorita 1
                                    // 2) rimuovo dall'array delle celle con priorita 1 quella che sto analizzando
                                    // della strategia che ho appena aggiunto
                                    // 3) l'array delle celle con priorita 1 lo rimetto dentro
                                    // 4) modifico la priorita della cella appena aggiunta da 1 a 3 in
                                    // celleDaPriorizzare
                                    // 5) rimuovo tutte le celle (l'array in celleConPriorita) con priorita 3
                                    // 6) aggiungo alle celle con priorita 3 la cella che sto analizzando
                                    // 7) l'array delle celle con priorita 3 lo rimetto dentro
                                    int priorita_casella = celleDaPriorizzare.get(cella);

                                    if (priorita_casella == 1) {
                                        int nuova_priorita = 3;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, 3);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }

                                    } else if (priorita_casella >= 3) {
                                        int nuova_priorita = priorita_casella + 1;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, nuova_priorita);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }
                                    }
                                } else {
                                    ArrayList<MNKCell> array = celleConPriorita.remove(1);
                                    array.add(cella);
                                    celleConPriorita.put(1, array);
                                    celleDaPriorizzare.put(cella, 1);
                                }

                            }
                        }

                    }

                    if (up.winning())
                        win_count++;
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
                    ArrayList<MNKCell> celleDellaStrategia = down.getRange();
                    for (int i = 0; i < celleDellaStrategia.size(); i++) {
                        System.out.println("Le celle della strategia sono: " + celleDellaStrategia.get(i));
                    }
                    for (MNKCell cella : celleDellaStrategia) {
                        if (cella.state == MNKCellState.FREE) {
                            if (celleDaPriorizzare.size() == 0) {
                                ArrayList<MNKCell> array = new ArrayList<>();
                                array.add(cella);
                                celleConPriorita.put(1, array);
                                celleDaPriorizzare.put(cella, 1);
                            } else {

                                if (celleDaPriorizzare.get(cella) != null) {
                                    int priorita_casella = celleDaPriorizzare.get(cella);

                                    if (priorita_casella == 1) {
                                        int nuova_priorita = 3;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, 3);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }

                                    } else if (priorita_casella >= 3) {
                                        int nuova_priorita = priorita_casella + 1;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, nuova_priorita);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }
                                    }
                                } else {
                                    ArrayList<MNKCell> array = celleConPriorita.remove(1);
                                    array.add(cella);
                                    celleConPriorita.put(1, array);
                                    celleDaPriorizzare.put(cella, 1);
                                }

                            }
                        }

                    }
                    if (down.winning())
                        win_count++;
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
                    ArrayList<MNKCell> celleDellaStrategia = left.getRange();
                    for (int i = 0; i < celleDellaStrategia.size(); i++) {
                        System.out.println("Le celle della strategia sono: " + celleDellaStrategia.get(i));
                    }
                    for (MNKCell cella : celleDellaStrategia) {
                        if (cella.state == MNKCellState.FREE) {
                            if (celleDaPriorizzare.size() == 0) {
                                ArrayList<MNKCell> array = new ArrayList<>();
                                array.add(cella);
                                celleConPriorita.put(1, array);
                                celleDaPriorizzare.put(cella, 1);
                            } else {

                                if (celleDaPriorizzare.get(cella) != null) {
                                    int priorita_casella = celleDaPriorizzare.get(cella);

                                    if (priorita_casella == 1) {
                                        int nuova_priorita = 3;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, 3);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }

                                    } else if (priorita_casella >= 3) {
                                        int nuova_priorita = priorita_casella + 1;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, nuova_priorita);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }
                                    }
                                } else {
                                    ArrayList<MNKCell> array = celleConPriorita.remove(1);
                                    array.add(cella);
                                    celleConPriorita.put(1, array);
                                    celleDaPriorizzare.put(cella, 1);
                                }

                            }
                        }

                    }
                    if (left.winning())
                        win_count++;
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
                    ArrayList<MNKCell> celleDellaStrategia = right.getRange();
                    for (int i = 0; i < celleDellaStrategia.size(); i++) {
                        System.out.println("Le celle della strategia sono: " + celleDellaStrategia.get(i));
                    }
                    for (MNKCell cella : celleDellaStrategia) {
                        if (cella.state == MNKCellState.FREE) {
                            if (celleDaPriorizzare.size() == 0) {
                                ArrayList<MNKCell> array = new ArrayList<>();
                                array.add(cella);
                                celleConPriorita.put(1, array);
                                celleDaPriorizzare.put(cella, 1);
                            } else {

                                if (celleDaPriorizzare.get(cella) != null) {
                                    int priorita_casella = celleDaPriorizzare.get(cella);

                                    if (priorita_casella == 1) {
                                        int nuova_priorita = 3;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, 3);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }

                                    } else if (priorita_casella >= 3) {
                                        int nuova_priorita = priorita_casella + 1;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, nuova_priorita);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }
                                    }
                                } else {
                                    ArrayList<MNKCell> array = celleConPriorita.remove(1);
                                    array.add(cella);
                                    celleConPriorita.put(1, array);
                                    celleDaPriorizzare.put(cella, 1);
                                }

                            }
                        }

                    }
                    if (right.winning())
                        win_count++;
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
                    ArrayList<MNKCell> celleDellaStrategia = upleft.getRange();
                    for (int i = 0; i < celleDellaStrategia.size(); i++) {
                        System.out.println("Le celle della strategia sono: " + celleDellaStrategia.get(i));
                    }

                    for (MNKCell cella : celleDellaStrategia) {
                        if (cella.state == MNKCellState.FREE) {
                            if (celleDaPriorizzare.size() == 0) {
                                ArrayList<MNKCell> array = new ArrayList<>();
                                array.add(cella);
                                celleConPriorita.put(1, array);
                                celleDaPriorizzare.put(cella, 1);
                            } else {

                                if (celleDaPriorizzare.get(cella) != null) {
                                    int priorita_casella = celleDaPriorizzare.get(cella);

                                    if (priorita_casella == 1) {
                                        int nuova_priorita = 3;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, 3);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }

                                    } else if (priorita_casella >= 3) {
                                        int nuova_priorita = priorita_casella + 1;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, nuova_priorita);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }
                                    }
                                } else {
                                    ArrayList<MNKCell> array = celleConPriorita.remove(1);
                                    array.add(cella);
                                    celleConPriorita.put(1, array);
                                    celleDaPriorizzare.put(cella, 1);
                                }

                            }
                        }

                    }
                    if (upleft.winning())
                        win_count++;
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
                    ArrayList<MNKCell> celleDellaStrategia = upright.getRange();
                    for (int i = 0; i < celleDellaStrategia.size(); i++) {
                        System.out.println("Le celle della strategia sono: " + celleDellaStrategia.get(i));
                    }

                    for (MNKCell cella : celleDellaStrategia) {
                        if (cella.state == MNKCellState.FREE) {
                            if (celleDaPriorizzare.size() == 0) {
                                ArrayList<MNKCell> array = new ArrayList<>();
                                array.add(cella);
                                celleConPriorita.put(1, array);
                                celleDaPriorizzare.put(cella, 1);
                            } else {

                                if (celleDaPriorizzare.get(cella) != null) {
                                    int priorita_casella = celleDaPriorizzare.get(cella);

                                    if (priorita_casella == 1) {
                                        int nuova_priorita = 3;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, 3);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }

                                    } else if (priorita_casella >= 3) {
                                        int nuova_priorita = priorita_casella + 1;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, nuova_priorita);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }
                                    }
                                } else {
                                    ArrayList<MNKCell> array = celleConPriorita.remove(1);
                                    array.add(cella);
                                    celleConPriorita.put(1, array);
                                    celleDaPriorizzare.put(cella, 1);
                                }

                            }
                        }

                    }
                    if (upright.winning())
                        win_count++;
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
                    ArrayList<MNKCell> celleDellaStrategia = downleft.getRange();
                    for (int i = 0; i < celleDellaStrategia.size(); i++) {
                        System.out.println("Le celle della strategia sono: " + celleDellaStrategia.get(i));
                    }
                    for (MNKCell cella : celleDellaStrategia) {
                        if (cella.state == MNKCellState.FREE) {
                            if (celleDaPriorizzare.size() == 0) {
                                ArrayList<MNKCell> array = new ArrayList<>();
                                array.add(cella);
                                celleConPriorita.put(1, array);
                                celleDaPriorizzare.put(cella, 1);
                            } else {

                                if (celleDaPriorizzare.get(cella) != null) {
                                    int priorita_casella = celleDaPriorizzare.get(cella);

                                    if (priorita_casella == 1) {
                                        int nuova_priorita = 3;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, 3);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }

                                    } else if (priorita_casella >= 3) {
                                        int nuova_priorita = priorita_casella + 1;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, nuova_priorita);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }
                                    }
                                } else {
                                    ArrayList<MNKCell> array = celleConPriorita.remove(1);
                                    array.add(cella);
                                    celleConPriorita.put(1, array);
                                    celleDaPriorizzare.put(cella, 1);
                                }

                            }
                        }

                    }
                    if (downleft.winning())
                        win_count++;
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
                    ArrayList<MNKCell> celleDellaStrategia = downright.getRange();
                    for (int i = 0; i < celleDellaStrategia.size(); i++) {
                        System.out.println("Le celle della strategia sono: " + celleDellaStrategia.get(i));
                    }
                    for (MNKCell cella : celleDellaStrategia) {
                        if (cella.state == MNKCellState.FREE) {
                            if (celleDaPriorizzare.size() == 0) {
                                ArrayList<MNKCell> array = new ArrayList<>();
                                array.add(cella);
                                celleConPriorita.put(1, array);
                                celleDaPriorizzare.put(cella, 1);
                            } else {

                                if (celleDaPriorizzare.get(cella) != null) {

                                    int priorita_casella = celleDaPriorizzare.get(cella);

                                    if (priorita_casella == 1) {
                                        int nuova_priorita = 3;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, 3);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }

                                    } else if (priorita_casella >= 3) {
                                        int nuova_priorita = priorita_casella + 1;
                                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_casella);
                                        array.remove(cella);
                                        celleConPriorita.put(priorita_casella, array);
                                        celleDaPriorizzare.replace(cella, priorita_casella, nuova_priorita);
                                        if (celleConPriorita.get(nuova_priorita) != null) {
                                            ArrayList<MNKCell> array2 = celleConPriorita
                                                    .remove(nuova_priorita);
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        } else {
                                            ArrayList<MNKCell> array2 = new ArrayList<MNKCell>();
                                            array2.add(cella);
                                            celleConPriorita.put(nuova_priorita, array2);
                                        }
                                    }
                                } else {
                                    ArrayList<MNKCell> array = celleConPriorita.remove(1);
                                    array.add(cella);
                                    celleConPriorita.put(1, array);
                                    celleDaPriorizzare.put(cella, 1);
                                }

                            }
                        }

                    }
                    if (downright.winning())
                        win_count++;
                    generated++;
                }
            }

            generated_stack.push(generated);
        }
        /* -------------------------------------------------- */

        // Test
        /*
         * if (win_count < 0)
         * throw new IllegalStateException("Negative win count");
         * if (win_count > set.size())
         * throw new IllegalStateException("More winning strategies (" + win_count +
         * ") than factual strategies (" + set.size());
         * 
         * if (set.size() + invalid_size() != gen_size())
         * throw new IllegalStateException(player + ": invariant broken!\nSet is: " +
         * set + "\nInvalids are: " +
         * invalids + "\nGenerated number is: " + gen_size());
         */
        ArrayList<MNKCell> priorita1 = celleConPriorita.get(1);
        // ArrayList<MNKCell> priorita3 = celleConPriorita.get(3);
        for (MNKCell cc : priorita1) {
            System.out.println("Priorita1: [" + cc.i + "," + cc.j + "] ");
        }

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
    public void undo(MNKCell c, MNKBoard B) {
        if (B.cellState(c.i, c.j) == MNKCellState.FREE)
            throw new IllegalArgumentException("Input cell is FREE: cell should be marked!");

        // 1)
        /* -------------------------------------------------- */
        int MAX = generated_stack.pop();
        int n = set.size() - 1;
        for (int i = n; i > n - MAX; i--) {
            if (set.get(i).winning())
                win_count--;
            MNKStrategy strategiaDaRimuovere = set.remove(i);
            ArrayList<MNKCell> celleDellaStrategia = strategiaDaRimuovere.getRange();
            for (MNKCell cella : celleDellaStrategia) {
                int priorita_da_rimuovere = celleDaPriorizzare.get(cella);
                if (priorita_da_rimuovere == 1) {
                    celleDaPriorizzare.remove(cella);
                    ArrayList<MNKCell> array = celleConPriorita.remove(priorita_da_rimuovere);
                    array.remove(cella);
                    celleConPriorita.put(priorita_da_rimuovere, array);
                } else if (priorita_da_rimuovere == 3) {
                    celleDaPriorizzare.replace(cella, 3, 1);
                    ArrayList<MNKCell> array = celleConPriorita.remove(3);
                    array.remove(cella);
                    celleConPriorita.put(3, array);
                    ArrayList<MNKCell> array2 = celleConPriorita.remove(1);
                    array2.add(cella);
                    celleConPriorita.put(1, array2);
                } else if (priorita_da_rimuovere > 3) {
                    int nuova_priorita = priorita_da_rimuovere - 1;
                    celleDaPriorizzare.replace(cella, priorita_da_rimuovere, nuova_priorita);
                    ArrayList<MNKCell> array = celleConPriorita.remove(priorita_da_rimuovere);
                    array.remove(cella);
                    celleConPriorita.put(priorita_da_rimuovere, array);
                    ArrayList<MNKCell> array2 = celleConPriorita.remove(nuova_priorita);
                    array2.add(cella);
                    celleConPriorita.put(nuova_priorita, array2);
                }
            }
        }
        /* -------------------------------------------------- */

        // 2)
        /* -------------------------------------------------- */
        ArrayList<InvMNKStrategy> invalids = invalid_stack.pop();
        for (InvMNKStrategy S : invalids) {
            set.add(S.index, S.S);
            ArrayList<MNKCell> celleDellaStrategia = S.S.getRange();
            for (MNKCell cella : celleDellaStrategia) {
                if (celleDaPriorizzare.get(cella) != null) {
                    int priorita_cella = celleDaPriorizzare.get(cella);
                    if (priorita_cella == 1) {
                        ArrayList<MNKCell> array = celleConPriorita.remove(celleDaPriorizzare.get(cella));
                        array.remove(cella);
                        celleConPriorita.put(celleDaPriorizzare.get(cella), array);
                        celleDaPriorizzare.replace(cella, celleDaPriorizzare.get(cella), 3);
                        ArrayList<MNKCell> array2 = celleConPriorita.remove(celleDaPriorizzare.get(cella));
                        array2.add(cella);
                        celleConPriorita.put(celleDaPriorizzare.get(cella), array2);
                    } else if (priorita_cella == 3) {
                        int nuova_priorita = priorita_cella + 1;
                        celleDaPriorizzare.replace(cella, 3, nuova_priorita);
                        ArrayList<MNKCell> array = celleConPriorita.remove(3);
                        array.remove(cella);
                        celleConPriorita.put(3, array);
                        if (celleConPriorita.get(nuova_priorita) == null) {
                            ArrayList<MNKCell> array2 = new ArrayList<>();
                            array2.add(cella);
                            celleConPriorita.put(nuova_priorita, array2);
                        } else {
                            ArrayList<MNKCell> array2 = celleConPriorita.remove(nuova_priorita);
                            array2.add(cella);
                            celleConPriorita.put(nuova_priorita, array2);
                        }
                    } else if (priorita_cella > 3) {
                        int nuova_priorita = priorita_cella + 1;
                        celleDaPriorizzare.replace(cella, priorita_cella, nuova_priorita);
                        ArrayList<MNKCell> array = celleConPriorita.remove(priorita_cella);
                        array.remove(cella);
                        celleConPriorita.put(priorita_cella, array);
                        if (celleConPriorita.get(nuova_priorita) == null) {
                            ArrayList<MNKCell> array2 = new ArrayList<>();
                            array2.add(cella);
                            celleConPriorita.put(nuova_priorita, array2);
                        } else {
                            ArrayList<MNKCell> array2 = celleConPriorita.remove(nuova_priorita);
                            array2.add(cella);
                            celleConPriorita.put(nuova_priorita, array2);
                        }
                    }

                } else {
                    ArrayList<MNKCell> array = celleConPriorita.remove(1);
                    array.add(cella);
                    celleConPriorita.put(1, array);
                    celleDaPriorizzare.put(cella, 1);
                }

            }
            if (S.S.winning())
                win_count++;

        }
        /* -------------------------------------------------- */

        // 3)
        /* -------------------------------------------------- */
        for (MNKStrategy S : set) {
            if (S.contains(c)) {
                boolean winning = S.winning();
                S.remove(c, B);
                int nuova_priorita = 0;
                Iterator<MNKStrategy> iteratore = set.iterator();
                while (iteratore.hasNext()) {
                    MNKStrategy strategia = iteratore.next();
                    if (strategia.contains(c)) {
                        nuova_priorita++;
                    }
                }
                celleDaPriorizzare.put(c, nuova_priorita);
                if (celleConPriorita.get(nuova_priorita) != null) {
                    ArrayList<MNKCell> array = celleConPriorita.remove(nuova_priorita);
                    array.add(c);
                    celleConPriorita.put(nuova_priorita, array);
                } else {
                    ArrayList<MNKCell> array = new ArrayList<MNKCell>();
                    array.add(c);
                    celleConPriorita.put(nuova_priorita, array);
                }
                if (!S.valid())
                    throw new IllegalStateException("Invalid strategy " + S + " left in the set");

                if (winning && !S.winning())
                    win_count--;
                else if (!winning && S.winning()) {
                    win_count++;
                }
            }
        }
        /* -------------------------------------------------- */

        // Test
        /*
         * if (win_count < 0)
         * throw new IllegalStateException("Negative win count");
         * if (win_count > set.size())
         * throw new IllegalStateException("More winning strategies (" + win_count +
         * ") than factual strategies (" + set.size() + ")");
         * 
         * if (set.size() + invalid_size() != gen_size())
         * throw new IllegalStateException(player + ": invariant broken! Set is: " + set
         * + "\nInvalids are: " +
         * invalids + "\nGenerated number is: " + gen_size());
         */
    }

    /**
     * Genera tutte le MNKIntersection possibili dal set. Confronta cioè ogni coppia
     * di MNKStrategy nel set e calcolane le intersezioni.
     * Complessità: O(n^2 x K), dove n è la dimensione del set
     * 
     * @param B MNKBoard di gioco
     * @return set delle intersezioni generato
     */
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
        }
        return iSet.size() > 0 ? iSet : null;
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

    // Test functions
    public MNKStrategy[] set() {
        return set.toArray(new MNKStrategy[0]);
    }

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

    public int cellIndex(MNKCell c, MNKBoard B) {
        return c.i * B.N + c.j;
    }

    public Map<Integer, ArrayList<MNKCell>> getCelleConPriorita() {
        return celleConPriorita;
    }
}