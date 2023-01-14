package mnkgame;

import java.util.Collection;
import java.util.HashMap;

/**
 *  Insieme delle MNKIntersection generato in sede di valutazione di
 *  una data configurazione di gioco.
 */
public class IntersectionSet {

    /**
     *  Tabella hash delle MNKIntersection. La sua dimensione
     *  massima teorica sarebbe MxN (una entry per cella nella MNKBoard).
     */
    private final HashMap<Integer, MNKIntersection> set;

    /**
     *  Massimo numero di MNKStrategy intersecanti in una singola cella.
     *  Alternativamente, la massima cardinality() tra tutte le MNKIntersection
     *  nel set.
     */
    private int max;

    /**
     *  True se è un set che sicuramente porterà alla vittoria.
     */
    private boolean winning;

    /**
     *  Complessità: O(1)
     *  @param B MNKBoard di gioco
     */
    public IntersectionSet(MNKBoard B) {
        set     = new HashMap<>((B.M * B.N) / 2);
        max     = 0;
        winning = false;
    }

    /**
     *  Aggiungi la MNKIntersection I nella HashTable che rappresenta il set.
     *  Se I è gia presente, esegui merge() delle due MNKIntersection.
     *  Complessità: O(1)
     *  @param I MNKIntersection da inserire
     */
    public void add(MNKIntersection I) {
        MNKIntersection i = set.get(I.hashCode());
        if (i!=null) {
            i.merge(I);
            if (i.cardinality()>max)
                max = i.cardinality();
            if (i.winning())
                winning = true;
        } else {
            if (I.cardinality()>max)
                max = I.cardinality();
            if (I.winning())
                winning = true;
            set.put(I.hashCode(),I);
        }
    }

    /**
     *  Aggiungi tutte le MNKIntersection nella collezione in input
     *  nella HashTable.
     *  Complessità: O(m), dove m è la dimensione della collezione
     *  @param collection collezione di MNKIntersection
     */
    public void addAll(Collection<MNKIntersection> collection) {
        for (MNKIntersection I : collection)
            add(I);
    }

    /**
     *  Complessità: O(1)
     *  @return massimo numero di MNKStrategy intersecanti nella stessa cella
     */
    public int max() {
        return max;
    }

    /**
     *  Complessità: O(1)
     *  @return dimensione del set
     */
    public int size() {
        return set.size();
    }

    /**
     *  Complessità: O(1)
     *  @return True se è un set che porta a vittoria certa; False altrimenti
     */
    public boolean winning() {
        return winning;
    }
}