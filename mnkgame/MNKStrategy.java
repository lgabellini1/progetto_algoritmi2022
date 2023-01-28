package mnkgame;

import java.util.ArrayList;
import java.util.List;

/**
 *  Fila di K celle consecutive nella MNKBoard. Rappresenta una potenziale
 *  strategia di gioco da seguire, ovvero una serie di K celle da marcare
 *  per vincere.
 */
public class MNKStrategy {

    /**
     *  Numero di celle del giocatore corrispondente (my_cells)
     *  e dell'avversario (adv_cells) nella MNKStrategy.
     */
    private int my_cells, adv_cells;

    /**
     *  Array contenente la lista di K celle che rappresentano
     *  la MNKStrategy. Ad esempio la strategia orizzontale
     *  nella prima riga nel Tris sarebbe:
     *  [0,0]->[0,1]->[0,2]
     */
    private final ArrayList<MNKCell> range;

    private final int N,K;

    /**
     *  Giocatore a cui appartiene la MNKStrategy.
     */
    public  final MNKCellState player;

    /**
     *  Condizione di validità; vera se non sono
     *  presenti celle dell'avversario.
     */
    private boolean valid;

    /**
     *  Complessità: O(1)
     *  @param B MNKBoard della partita
     *  @param c cella di partenza per la MNKStrategy
     */
    public MNKStrategy(MNKBoard B, MNKCell c) {
        my_cells = adv_cells = 0;
        range    = new ArrayList<>(B.K);
        N        = B.N; 
        K        = B.K;
        player   = B.cellState(c.i, c.j);
        valid    = true;

        if (player == MNKCellState.FREE)
            throw new IllegalArgumentException("Cell in input is unmarked: player is FREE.");
    }

    public MNKStrategy(MNKStrategy S) {
        my_cells  = S.my_cells;
        adv_cells = S.adv_cells;
        range     = new ArrayList<>(List.of(S.range.toArray(new MNKCell[0])));
        N         = S.N;
        K         = S.K;
        player    = S.player;
        valid     = S.valid;
    }

    /**
     *  Funzione ausiliaria. Restituisce un intero identificativo
     *  univoco per ogni cella (intuitivamente, le celle vengono contate
     *  da 0 a partire dall'angolo in alto a sinistra fino ad arrivare ad
     *  M*N - 1 nell'angolo in basso a destra).
     *  Complessità: O(1)
     *  @param c cella di cui restituire l'indice
     *  @return indice di c
     */
    private int cellIndex(MNKCell c) {
        return c.i * N + c.j;
    }

    /**
     *  Valuta se la cella c in input è contenuta nella MNKStrategy.
     *  Complessità: O(1)
     *  @param c cella in input
     *  @return true se c appartiene a range; false altrimenti
     */
    public boolean contains(MNKCell c) {
        int x = cellIndex(range.get(1)) - cellIndex(range.get(0));
        if ((cellIndex(c)-cellIndex(range.get(0))) % x != 0) return false;
        else {
            int q = (cellIndex(c) - cellIndex(range.get(0))) / x;
            return q >= 0 && q < K;
        }
    }

    /**
     *  Funzione utile all'inizializzazione della MNKStrategy.
     *  Stabilisce che la cella c in input appartiene al range.
     *  Complessità: O(1)
     *  @param c cella a cui settiamo il range
     */
    public void setRange(MNKCell c) {
        range.add(c);
        if (range.size() > K)
            throw new IllegalStateException("Range too large for MNKStrategy");
    }

    /**
     *  Aggiunge la cella c alla MNKStrategy. Valuta inoltre le condizioni
     *  di validità della MKNStrategy.
     *  Complessità: O(1)
     *  @param c cella tale per cui contains(c) restituisce True
     *  @param B riferimento alla MNKBoard di gioco
     */
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
            throw new IllegalStateException("More than K cells in a single strategy!\n" +
                    "Trying to add cell [" + c.i + "," + c.j + "] in range " + range);
    }

    /**
     *  Rimuove la cella c dalla MNKStrategy. Valuta inoltre le condizioni
     *  di validità della MKNStrategy.
     *  Complessità: O(1)
     *  @param c cella tale per cui contains(c) restituisce True
     *  @param B riferimento alla MNKBoard di gioco
     */
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

    /**
     *  Complessità: O(1)
     *  @return numero di celle del giocatore proprietario della MNKStrategy
     */
    public int size() {
        return my_cells;
    }

    /**
     *  Complessità: O(1)
     *  @return True se la MNKStrategy è a una mossa dalla vittoria
     */
    public boolean winning() {
        return my_cells >= K-1;
    }

    /**
     *  Complessità: O(1)
     *  @return validità della MNKStrategy
     */
    public boolean valid() {
        return valid;
    }

    /**
     *  Complessità: O(1)
     *  @return celle nel range della MNKStrategy
     */
    public ArrayList<MNKCell> range() {
        return range;
    }

    /**
     *  Complessità: O(K)
     *  @param B MNKBoard attuale di gioco
     *  @return la cella che, se marcata, porta alla vittoria del
     *      giocatore a cui appartiene la MNKStrategy
     */
    public MNKCell getWinCell(MNKBoard B) {
        for (MNKCell c : range)
            if (B.cellState(c.i,c.j)==MNKCellState.FREE)
                return c;
        throw new IllegalStateException("Should have found a FREE cell in this MNKStrategy");
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
            range.get(K-1).i + "," + range.get(K-1).j + "] - player=" + player;
    }
}
