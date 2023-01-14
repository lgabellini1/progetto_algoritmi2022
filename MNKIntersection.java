package mnkgame;

import java.util.LinkedList;

/**
 *  Rappresenta la cella in cui due o più
 *  MNKStrategy si intersecano.
 */
public class MNKIntersection {

    /**
     *  Cella d'intersezione.
     */
    public final MNKCell c;

    /**
     *  Elenco delle MNKStrategy che si intersecano in c.
     */
    private final LinkedList<MNKStrategy> strategies;

    private final int K;

    /**
     *  Numero di strategie a K-2 simboli. Se questo valore è
     *  maggiore o uguale di 2, allora la configurazione equivale
     *  a una vittoria.
     */
    private int K_minus2;

    /**
     *  Complessità: O(1)
     *  @param c cella d'intersezione
     *  @param B MNKBoard di gioco
     *  @param strats strategie intersecanti in c
     */
    public MNKIntersection(MNKCell c, MNKBoard B, MNKStrategy[] strats) {
        this.c     = c;
        strategies = new LinkedList<>();
        K          = B.K;
        K_minus2   = 0;

        for (MNKStrategy S : strats)
            add(S);
    }

    /**
     *  Aggiunge un'ulteriore MNKStrategy S che passa per c.
     *  Complessità: O(1)
     *  @param S MNKStrategy aggiunta
     */
    public void add(MNKStrategy S) {
        if (S.valid()) {
            strategies.add(S);
            if (S.size() >= K-2) 
                K_minus2++;
        }
    }

    /**
     *  Data una MNKIntersection I equivalente (ovvero, con la stessa cella c di intersezione),
     *  fondi in uno solo i due insiemi di MNKStrategy.
     *  Complessità: O(n), dove n è il numero di MNKStrategy intersecanti in I
     *  @param I intersezione equivalente
     */
    public void merge(MNKIntersection I) {
        if (I.equals(this)) {
            for (MNKStrategy S : I.strategies)
                if (!strategies.contains(S))
                    add(S);
        } 
    }

    /**
     *  Complessità: O(1)
     *  @return True se sono presenti almeno due MNKStrategy a k-2 simboli
     */
    public boolean winning() {
        return K_minus2>=2;
    }

    /**
     *  Complessità: O(1)
     *  @return numero di MKNStrategy intersecanti in c
     */
    public int cardinality() {
        return strategies.size();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof MNKIntersection)) return false;

        MNKIntersection I = (MNKIntersection) o;
        if (I.c.i == c.i && I.c.j == c.j && I.c.state != c.state)
            throw new IllegalStateException("Same cell should have the same state");
        return I.c.equals(c);
    }

    @Override
    public int hashCode() {
        return c.hashCode();
    }

    @Override
    public String toString() {
        return "Intersection at " + c.toString();
    }
}