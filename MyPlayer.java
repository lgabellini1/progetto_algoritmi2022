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

        Board(MNKBoard b, double lower_bound, double upper_bound, Board.FLAG flag) {
            this.board = b;
            this.lower_bound = lower_bound;
            this.upper_bound = upper_bound;
            this.flag = flag;

        }
    }

    private String hash(Board board) {
        /* 0 casella vuota; 1 se playerA; 2 se playerB */
        String hashcode = "";
        for (int i = 0; i < board.board.N; i++) {
            for (int j = 0; j < board.board.M; j++) {
                if (board.board.B[j][i] == MNKCellState.FREE)
                    hashcode = hashcode.concat("0");
                else if (board.board.B[j][i] == MNKCellState.P1)
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

    public double MTD(Board board, double f, int d) {
        double g = f;
        double upperbound = +9999;
        double lowerbound = -9999;
        double beta = 0;
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

    private double AlphaBetaWithMemory(Board board, double alpha, double beta, int depth) {
        double g = 0;
        double betaOrig = beta;
        double alphaOrig = alpha;
        Board c = board;
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
            g = eval(board.board);
        } else if (board.flag == Board.FLAG.MAXNODE) {
            System.out.println("ttEntry.flag == Board.FLAG.MAXNODE");
            g = -9999;
            alphaOrig = alpha;
            // c = firstchild(board) diventa:
            MNKCell[] children_of_c = c.board.getFreeCells();
            int i = 0;
            while (g < beta && i < children_of_c.length) {
                System.out.println("Dentro primo ciclo while di AlphaBetaWithMemory");
                MNKCell sel = children_of_c[i];
                c.board.markCell(sel.i, sel.j);
                g = Math.max(g, AlphaBetaWithMemory(c, alphaOrig, beta, depth - 1));
                alphaOrig = Math.max(alphaOrig, g);
                c.board.unmarkCell();
                i++;
            }
        } else { // MINNODE
            System.out.println("ttEntry è un MINNODE");
            g = +9999;
            betaOrig = beta;
            MNKCell[] children_of_c = c.board.getFreeCells();
            int i = 0;
            while (g > alpha && i < children_of_c.length) {
                System.out.println("Dentro secondo ciclo while di AlphaBetaWithMemory");
                MNKCell sel = children_of_c[i];
                c.board.markCell(sel.i, sel.j);
                g = Math.min(g, AlphaBetaWithMemory(c, alpha, betaOrig, depth - 1));
                betaOrig = Math.min(betaOrig, g);
                c.board.unmarkCell();
                i++;

            }
        }

        if (g <= alpha) { // fail-low node implica un upper-bound
            System.out.println("fail-low node");
            board.upper_bound = g;
            // TODO: store ttEntry.upperbound
            if (depth % 2 == 0) {
                ttEntry = new Board(board.board, -9999, board.upper_bound, Board.FLAG.MINNODE);
            } else {
                ttEntry = new Board(board.board, -9999, board.upper_bound, Board.FLAG.MAXNODE);
            }
            tTable.put(hash(ttEntry), ttEntry);
        }
        if (g > alpha && g < beta) {
            System.out.println("exact node");
            board.lower_bound = g;
            board.upper_bound = g;
            // TODO: store ttEntry.lowerbound
            // TODO: store ttEntry.upperbound
            if (depth % 2 == 0) {
                ttEntry = new Board(board.board, board.lower_bound, board.upper_bound, Board.FLAG.MINNODE);
            } else {
                ttEntry = new Board(board.board, board.lower_bound, board.upper_bound, Board.FLAG.MAXNODE);
            }
            tTable.put(hash(ttEntry), ttEntry);
        }
        if (g >= beta) { // fail-high node implica un lower-bound
            System.out.println("fail-high node");
            board.lower_bound = g;
            // TODO: store ttEntry.lowerbound
            if (depth % 2 == 0) {
                ttEntry = new Board(board.board, board.lower_bound, 9999, Board.FLAG.MINNODE);
                System.out.println("sonoqui");
            } else {
                System.out.println("sonoqui2");
                ttEntry = new Board(board.board, board.lower_bound, 9999, Board.FLAG.MAXNODE);
            }
            tTable.put(hash(ttEntry), ttEntry);
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
        int caselle_giocate = 0;
        MNKCell sel = FC[rand.nextInt(FC.length)];
        System.out.println("La mossa casuale prima dell'applicazione di MTD è: " + "[" + sel.i + "," + sel.j + "]");
        for (int d = 1; d < FC.length; d++) {
            System.out.println("il numero di caselle libere è: " + String.valueOf(FC.length));
            // MNKCell sel = FC[rand.nextInt(FC.length)];
            for (MNKCell c : FC) {
                B.markCell(c.i, c.j);
                Board configurazione_attuale;
                if (d % 2 == 0) {
                    configurazione_attuale = new Board(B, -9999, 9999, Board.FLAG.MINNODE);
                } else {
                    configurazione_attuale = new Board(B, -9999, 9999, Board.FLAG.MAXNODE);
                }

                firstguess = MTD(configurazione_attuale, firstguess, d);
                /* Tempo quasi scaduto -> ritorna l'ultima mossa scelta */
                if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0)) {
                    System.out.println("tempo scaduto");
                    break;
                }
                configurazione_attuale.board.unmarkCell();
                if (firstguess > bestVal) {
                    System.out.println("ho trovato un valore migliore di bestval");
                    bestVal = firstguess;
                    sel = c;
                }

            }
            d++;
        }
        System.out.println("Fine applicazione MTD");
        System.out.println("La mossa che vado a scegliere dopo MTD è: " + "[" + sel.i + "," + sel.j + "]");
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
