package mnkgame;

import java.util.HashMap;
import java.util.Random;

public class MyPlayer implements MNKPlayer {
    private Random rand;
    private MNKBoard B;
    private MNKGameState myWin;
    private MNKGameState yourWin;
    private int TIMEOUT;
    private long start;
    private HashMap<String, Board> tTable;

    public MyPlayer() {

    }

    private static class Board {
        private enum FLAG {
            MAXNODE, MINNODE
        }

        public Board.FLAG flag;
        public double lower_bound;
        public double upper_bound;
        public int depth;
        private final MNKBoard board;

        Board(MNKBoard b, double lower_bound, double upper_bound, int depth) {
            board = b;
            this.lower_bound = lower_bound;
            this.upper_bound = upper_bound;
            this.depth = depth;
        }
    }

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
        }
        return hashcode;
    }

    private double eval(MNKBoard board) {
        // TODO: aggiornare la funzione in modo che riesca ad effettuare
        // una valutazione di configurazioni intermedie di gioco tramite
        // metodi euristici.
        if (board.gameState == myWin)
            return -1;
        else if (board.gameState == yourWin)
            return 1;
        else
            return 0;
    }

    /*------------------------------------------------------------- */
    /*--------------------INIZIO ALGORITMO MTD--------------------- */
    /*------------------------------------------------------------- */

    /*
     * public double iterative_deepening(MNKBoard board, int depth) {
     * double firstguess = 0;
     * for (int d = 1; d < depth; d++) {
     * firstguess = MTD(board, firstguess, depth);
     * /*
     * if times_up(){
     * break;
     * }
     * 
     * }
     * return firstguess;
     * 
     * }
     */

    public double MTD(MNKBoard board, double f, int d) {
        double g = f;
        double upperbound = +9;
        double lowerbound = -9;
        double beta;
        while (lowerbound < upperbound) {
            if (g == lowerbound) {
                System.out.println("g == lowerbound");
                beta = g + 1;
            } else {
                System.out.println("g non è uguale a lowerbound");
                beta = g;
            }

            g = AlphaBetaWithMemory(board, beta - 1, beta, d);
            if (g < beta) {
                System.out.println("g < beta");
                upperbound = g;
            } else {
                System.out.println("g non è minore di beta");
                lowerbound = g;
            }
        }
        return g;

    }

    private double AlphaBetaWithMemory(MNKBoard board, double alpha, double beta, int depth) {
        double g = 0;
        double betaOrig = beta;
        double alphaOrig = alpha;
        MNKBoard c = board;
        System.out.println("sono qui");
        Board ttEntry = tTable.get(hash(board));
        System.out.println("sono qui 2");
        if (ttEntry != null) {
            System.out.println("ttEntry != null");
            if (ttEntry.lower_bound >= beta) {
                System.out.println("ttEntry.lower_bound >=beta");
                return ttEntry.lower_bound;

            }
            if (ttEntry.upper_bound <= alpha) {
                System.out.println("ttEntry.upper_bound <= alpha");
                return ttEntry.upper_bound;
            }
            alpha = Math.max(alpha, ttEntry.lower_bound);
            beta = Math.min(beta, ttEntry.upper_bound);
        }

        if (depth == 0) {
            System.out.println("depth==0");
            g = eval(board);
        } else if (ttEntry.flag == Board.FLAG.MAXNODE) {
            System.out.println("ttEntry.flag == Board.FLAG.MAXNODE");
            g = -9;
            alphaOrig = alpha;
            // c = firstchild(board) diventa:
            MNKCell[] children_of_c = c.getFreeCells();
            int i = 0;
            while (g < beta && i < children_of_c.length) {
                System.out.println("Dentro primo ciclo while di AlphaBetaWithMemory");
                MNKCell sel = children_of_c[i];
                c.markCell(sel.i, sel.j);
                g = Math.max(g, AlphaBetaWithMemory(c, alphaOrig, beta, depth - 1));
                alphaOrig = Math.max(alphaOrig, g);
                i++;
            }
        } else { // MINNODE
            System.out.println("ttEntry è un MINNODE");
            g = +9;
            betaOrig = beta;
            MNKCell[] children_of_c = c.getFreeCells();
            int i = 0;
            while (g > alpha && i < children_of_c.length) {
                System.out.println("Dentro secondo ciclo while di AlphaBetaWithMemory");
                MNKCell sel = children_of_c[i];
                c.markCell(sel.i, sel.j);
                g = Math.min(g, AlphaBetaWithMemory(c, alpha, betaOrig, depth - 1));
                betaOrig = Math.min(betaOrig, g);
                i++;

            }
        }

        if (g <= alpha) { // fail-low node implica un upper-bound
            System.out.println("fail-low node");
            ttEntry.upper_bound = g;
            // TODO: store ttEntry.upperbound
            ttEntry = new Board(board, -9999, ttEntry.upper_bound, depth);
            tTable.put(hash(ttEntry.board), ttEntry);
        }
        if (g > alpha && g < beta) {
            System.out.println("exact node");
            ttEntry.lower_bound = g;
            ttEntry.upper_bound = g;
            // TODO: store ttEntry.lowerbound
            // TODO: store ttEntry.upperbound
            ttEntry = new Board(board, ttEntry.lower_bound, ttEntry.upper_bound, depth);
            tTable.put(hash(ttEntry.board), ttEntry);
        }
        if (g >= beta) { // fail-high node implica un lower-bound
            System.out.println("fail-high node");
            ttEntry.lower_bound = g;
            // TODO: store ttEntry.lowerbound
            ttEntry = new Board(board, ttEntry.lower_bound, 9999, depth);
            tTable.put(hash(ttEntry.board), ttEntry);
        }
        System.out.println("Return di AlphaBetaWithMemory");
        return g;
    }

    /*------------------------------------------------------------- */
    /*--------------------FINE ALGORITMO MTD----------------------- */
    /*------------------------------------------------------------- */

    @Override
    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        rand = new Random(System.currentTimeMillis());
        B = new MNKBoard(M, N, K);
        myWin = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        yourWin = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
        TIMEOUT = timeout_in_secs;
        tTable = new HashMap<>();
    }

    @Override
    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
        start = System.currentTimeMillis();

        /* Primo turno della partita -> selezione casuale */
        if (MC.length == 0) {
            System.out.println("Primo turno mio: mossa casuale");
            MNKCell d = FC[rand.nextInt(FC.length)];
            B.markCell(d.i, d.j);
            return d;
        }
        /* Recupera l'ultima mossa dell'avversario */
        if (MC.length > 0) {
            MNKCell d = MC[MC.length - 1];
            System.out.println(
                    "Turno mio. L'ultima mossa dell'avversario è: " + "[" + String.valueOf(d.i) + ","
                            + String.valueOf(d.j) + "]");
            B.markCell(d.i, d.j);

        }

        /* Se esiste una sola mossa possibile, selezionala */
        if (FC.length == 1) {
            System.out.println("è rimasta solo una casella disponibile e cioè: " + String.valueOf(FC[0]));
            return FC[0];
        }

        /* Controllo mosse che restituiscano immediatamente la vittoria */
        for (MNKCell c : FC) {
            if (B.markCell(c.i, c.j) == myWin) {
                System.out.println("La mossa: " + "[" + String.valueOf(c.i) + "," + String.valueOf(c.j) + "]"
                        + " mi porta alla vittoria");
                return c;
            } else {
                B.unmarkCell();

            }
        }
        /*------------------------------------------------------------- */
        /*----------------------APPLICAZIONE MTD----------------------- */
        /*------------------------------------------------------------- */
        System.out.println("Inizia applicazione MTD");
        double bestVal = -9999;
        double firstguess = 0;
        MNKCell sel = FC[rand.nextInt(FC.length)];
        for (MNKCell c : FC) {
            System.out.println("Ciclo for applicazione MTD");
            B.markCell(c.i, c.j);
            firstguess = MTD(B, firstguess, B.M * B.N);
            /* Tempo quasi scaduto -> ritorna l'ultima mossa scelta */
            if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0)) {
                System.out.println("tempo scaduto");
                break;
            }
            if (firstguess > bestVal) {
                System.out.println("ho trovato un valore migliore di bestval");
                bestVal = firstguess;
                sel = c;
            }
            B.unmarkCell();

        }
        B.markCell(sel.i, sel.j);
        System.out.println("Fine applicazione MTD");
        return sel;

        /*------------------------------------------------------------- */
        /*-------------------FINE APPLICAZIONE MTD--------------------- */
        /*------------------------------------------------------------- */

    }

    @Override
    public String playerName() {

        return "MyPlayer";
    }

}
