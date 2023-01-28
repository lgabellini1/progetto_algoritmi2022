package mnkgame;


class MNKIntersection {

    /**
     *  Cella dove avviene l'intersezione.
     */
    public final MNKCell c;

    /**
     *  Numero di MNKStrategy intersecanti in c: perché l'intersezione
     *  sia sensata, deve essere strats>=2.
     */
    private int strats;

    private final int N;

    public MNKIntersection(MNKCell c, MNKBoard B) {
        this.c = c;
        strats = 0;
        N      = B.N;
    }

    private int cellIndex(MNKCell c) {
        return c.i * N + c.j;
    }

    /**
     *  Aggiunge una MNKStrategy passante per c.
     *  Complessità: O(1)
     *  @param S MNKStrategy contenente c
     */
    public void add(MNKStrategy S) {
        if (!S.contains(c))
            throw new IllegalStateException("This MNKStrategy does not contain c");
        strats++;
    }

    /**
     *  Rimuove una MNKStrategy passante per c.
     *  Complessità: O(1)
     *  @param S MNKStrategy contenente c
     */
    public void remove(MNKStrategy S) {
        if (!S.contains(c))
            throw new IllegalStateException("This MNKStrategy does not contain c");
        strats--;

        if (strats<0)
            throw new IllegalStateException("Negative number of MNKStrategy passes through this cell");
    }

    /**
     *  Complessità: O(1)
     *  @return numero di MNKStrategy intersecanti qui
     */
    public int cardinality() {
        return strats;
    }

    /**
     *  Complessità: O(1)
     *  @param B configurazione attuale di gioco
     *  @return True se l'intersezione è valida (cioè libera) ed è
     *      attraversata da più di due MNKStrategy; False altrimenti
     */
    public boolean valid(MNKBoard B) {
        return B.cellState(c.i,c.j)==MNKCellState.FREE && strats>=2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MNKIntersection that = (MNKIntersection) o;
        return c.i == that.c.i && c.j == that.c.j;
    }

    @Override
    public String toString() {
        return "[" + c.i + "," + c.j + "] - strats=" + strats;
    }

    @Override
    public int hashCode() {
        return cellIndex(c);
    }
}
