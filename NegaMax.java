package mnkgame;

import java.util.HashMap;
import java.util.Random;

/**
 *  Implementazione basata su algoritmo negamax con alpha-beta pruning e
 *  utilizzo di una Transposition Table per memorizzare il valore delle configurazioni
 *  di gioco già valutate.
 */
public class NegaMax implements MNKPlayer {
    private Random rand;
    private MNKBoard B;
    private MNKGameState myWin;
    private MNKGameState yourWin;
    private int TIMEOUT;
    private long start;

    /**
     *  Classe utilizzata per memorizzare le configurazioni di gioco
     *  nella Transposition Table.
     */
    private class Board {
        private enum FLAG {
            EXACT, LOWER_BOUND, UPPER_BOUND
        }

        private final MNKBoard board;
        public Board.FLAG flag;
        public double val;
        public int depth;

        Board(MNKBoard b, double val, int depth, Board.FLAG flag) {
            board = b;
            this.val = val;
            this.flag = flag;
            this.depth = depth;
        }
    }

    private HashMap<String, Board> tTable;

    /**
     *  Funzione hash che restituisce una stringa univoca per ogni
     *  configurazione di gioco.
     *  @param board configurazione di gioco da codificare
     *  @return stringa corrispondente
     */
    private String hash(MNKBoard board) {
        /* 0 casella vuota; 1 se playerA; 2 se playerB */
        String hashcode = "";
        for (int i = 0; i < board.N; i++) {
            for (int j = 0; j < board.M; j++) {
                if (board.B[j][i] == MNKCellState.FREE)
                    hashcode = hashcode.concat("0");
                else if (board.B[j][i] == MNKCellState.P1)
                    hashcode = hashcode.concat("1");
                else
                    hashcode = hashcode.concat("2");
            }
        } return hashcode;
    }

    NegaMax(){
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        rand    = new Random(System.currentTimeMillis());
        B       = new MNKBoard(M, N, K);
        myWin   = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        yourWin = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
        TIMEOUT = timeout_in_secs;
        tTable  = new HashMap<>();
    }

    /**
     *  Funzione che valuta il risultato di una partita (valutazione euristica per
     *  configurazioni intermedie, altrimenti esatte).
     *  @param board configurazione da valutare
     *  @return valore della partita
     */
    private double eval(MNKBoard board) {
        // TODO: aggiornare la funzione in modo che riesca ad effettuare
        //       una valutazione di configurazioni intermedie di gioco tramite
        //       metodi euristici.
        if (board.gameState == myWin)
            return -1;
        else if (board.gameState == yourWin)
            return 1;
        else
            return 0;
    }

    private double negaMax(MNKBoard board, int depth, double alpha, double beta, int color) {
        double alphaOrig = alpha;
        Board ttEntry = null;

        /* Controllo transposition table */
        if (!tTable.isEmpty()) {
            ttEntry = tTable.get(hash(board));
            if (ttEntry != null && ttEntry.depth >= depth) {
                if (ttEntry.flag == Board.FLAG.EXACT)
                    return ttEntry.val;
                else if (ttEntry.flag == Board.FLAG.LOWER_BOUND)
                    alpha = Math.max(alpha, ttEntry.val);
                else
                    beta = Math.min(beta, ttEntry.val);

                if (alpha >= beta) return ttEntry.val;
            }
        }

        if (board.gameState != MNKGameState.OPEN || depth == 0)
            return color * eval(board);

        double val = -9999;
        for (MNKCell c : board.getFreeCells()) {
            board.markCell(c.i,c.j);
            val = Math.max(val, -negaMax(board, depth - 1, -beta, -alpha, -color));
            alpha = Math.max(alpha, val);

            board.unmarkCell();
            if (alpha >= beta) break;
        }

        /* Salva nella transposition table */
        if (ttEntry == null) {
            if (val <= alphaOrig)
                ttEntry = new Board(board, val, depth, Board.FLAG.UPPER_BOUND);
            else if (val >= beta)
                ttEntry = new Board(board, val, depth, Board.FLAG.LOWER_BOUND);
            else
                ttEntry = new Board(board, val, depth, Board.FLAG.EXACT);
            tTable.put(hash(ttEntry.board), ttEntry);
        }
        return val;
    }

    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
        start = System.currentTimeMillis();

        /* Recupera l'ultima mossa dell'avversario */
        if (MC.length > 0) {
            MNKCell d = MC[MC.length - 1];
            B.markCell(d.i, d.j);
        }

        /* Se esiste una sola mossa possibile, selezionala */
        if (FC.length == 1) return FC[0];
        /* Primo turno della partita -> selezione casuale */
        if (MC.length == 0) {
            MNKCell d = FC[rand.nextInt(FC.length)];
            B.markCell(d.i, d.j);
            return d;
        }

        /* Controllo mosse che restituiscano immediatamente la vittoria */
        for(MNKCell c : FC) {
            if (B.markCell(c.i, c.j) == myWin) return c;
            else B.unmarkCell();
        }

        double score, bestVal = 9999;
        MNKCell sel = FC[rand.nextInt(FC.length)];
        for (MNKCell c : FC) {
            /* Tempo quasi scaduto -> ritorna l'ultima mossa scelta */
            if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0))
                break;

            B.markCell(c.i, c.j);
            score = negaMax(B, B.M * B.N, -9999, 9999, 1); // profondità massima: n = M x N

            if (score < bestVal) {
                bestVal = score;
                sel = c;
            } B.unmarkCell();
        } B.markCell(sel.i, sel.j);
        return sel;
    }

    public String playerName() {
        return "NegaMax";
    }
}
