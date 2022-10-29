package mnkgame;

import java.util.Random;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Andrea
 */
public class MyPlayerMTD implements MNKPlayer {

    private Random rand;
    private MNKBoard B;
    private MNKGameState myWin;
    private MNKGameState yourWin;
    private int TIMEOUT;
    private long start;

    /**
     * Default empty constructor
     */
    public MyPlayerMTD() {
    }

    private static class Board {
        private enum FLAG {
            EXACT, LOWER_BOUND, UPPER_BOUND
        }

        private final MNKBoard board;
        public Board.FLAG flag;
        public double val;

        Board(MNKBoard b, double val, Board.FLAG flag) {
            board = b;
            this.val = val;
            this.flag = flag;
        }
    }

    private HashMap<String, Board> tTable;

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        B = new MNKBoard(M, N, K);
        myWin = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        yourWin = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
        TIMEOUT = timeout_in_secs;
        tTable = new HashMap<>();
    }

    private int evaluation(MNKBoard board) {
        int eval = 0;
        if (board.gameState == myWin) {
            eval = 1;
        } else if (board.gameState == yourWin) {
            eval = -1;
        } else if (board.gameState == MNKGameState.DRAW) {
            eval = 0;
        } else {
            int libere_x = 0; // Giocatore 1
            int libere_o = 0; // Giocatore 2
            int giocatore_analizzato = 0;
            for (MNKCell marked : board.MC) {
                // System.out.println("Casella giocata: " + String.valueOf(marked.i) +
                // String.valueOf(marked.j));
                // System.out.print("sonoqui");
                if (giocatore_analizzato % 2 == 0) {
                    // nelle posizioni pari dell'array MC ci sono le mosse di X
                    // Horizontal check
                    int riga_libera = 1;
                    for (int k = 0; k < board.N; k++) {
                        // System.out.print("sonoqui1.1");
                        if (board.B[marked.i][k] != marked.state && board.B[marked.i][k] != MNKCellState.FREE) {
                            riga_libera = 0;
                            break;
                        }
                    }
                    if (riga_libera == 1) {
                        libere_x++;
                    }
                    riga_libera = 1;

                    // vertical check
                    for (int k = 0; k < board.M; k++) {
                        // System.out.print("sonoqui1.2");
                        if (board.B[k][marked.j] != marked.state && board.B[k][marked.j] != MNKCellState.FREE) {
                            riga_libera = 0;
                            break;
                        }
                    }
                    if (riga_libera == 1) {
                        libere_x++;
                    }

                } else {
                    // System.out.print("sonoqui2.0");
                    // nelle caselle dispari dell'array MC ci sono le mosse del giocatore "O"
                    // Horizontal check
                    int riga_libera = 1;
                    for (int k = 0; k < board.N; k++) {
                        // System.out.print("sonoqui2.1");
                        if (board.B[marked.i][k] != marked.state && board.B[marked.i][k] != MNKCellState.FREE) {
                            riga_libera = 0;
                            break;
                        }
                    }
                    if (riga_libera == 1) {
                        libere_o++;
                    }
                    riga_libera = 1;

                    // vertical check
                    for (int k = 0; k < board.M; k++) {
                        // System.out.print("sonoqui2.2");
                        if (board.B[k][marked.j] != marked.state && board.B[k][marked.j] != MNKCellState.FREE) {
                            riga_libera = 0;
                            break;
                        }
                    }
                    if (riga_libera == 1) {
                        libere_o++;
                    }

                }
                giocatore_analizzato++;
            }
            for (MNKCell free : board.FC) {
                // Horizontal check
                int riga_libera = 1;
                int caselle_vuote = 0;

                for (int k = free.j; k < board.N; k++) {
                    caselle_vuote++;
                    if (k == board.N - 1 && caselle_vuote < 3) {
                        riga_libera = 0;
                        break;
                    }
                    if (board.B[free.i][k] != MNKCellState.FREE) {
                        riga_libera = 0;
                        break;
                    }

                }
                if (riga_libera == 1) {
                    libere_o++;
                    libere_x++;
                }
                riga_libera = 1;
                caselle_vuote = 0;
                // vertical check
                for (int k = free.i; k < board.M; k++) {
                    caselle_vuote++;
                    if (k == board.M - 1 && caselle_vuote < 3) {
                        riga_libera = 0;
                        break;
                    }
                    if (board.B[k][free.j] != MNKCellState.FREE) {
                        riga_libera = 0;
                        break;
                    }

                }
                if (riga_libera == 1) {
                    libere_o++;
                    libere_x++;
                }

            }

            // controllo sulle due diagonali della matrice
            int riga_libera_x = 1;
            int riga_libera_o = 1;
            // backward diagonal check (diagonal from (0,0) to (2,2)) al momento funziona
            // solo per matrici (3,3), va ampliato come concetto
            for (int h = 1, k = 1; k >= 0 && h >= 0; k--, h--) {
                // System.out.print("sonoqui1.3");
                if (board.B[h][k] != MNKCellState.P1 && board.B[h][k] != MNKCellState.FREE) {
                    riga_libera_x = 0;

                }
                if (board.B[h][k] != MNKCellState.P2 && board.B[h][k] != MNKCellState.FREE) {
                    riga_libera_o = 0;

                }

            }
            if (riga_libera_x == 1 || riga_libera_o == 1) {
                // forward diagonal check (diagonal from (0,0) to (2,2)) al momento funziona
                // solo per matrici (3,3), va ampliato come concetto
                for (int h = 1, k = 1; k < board.N && h < board.M; k++, h++) {
                    // System.out.print("sonoqui1.4");
                    if (board.B[h][k] != MNKCellState.P1 && board.B[h][k] != MNKCellState.FREE) {
                        riga_libera_x = 0;

                    }
                    if (board.B[h][k] != MNKCellState.P2 && board.B[h][k] != MNKCellState.FREE) {
                        riga_libera_o = 0;

                    }
                }
            }
            if (riga_libera_x == 1) {
                libere_x++;
            }
            if (riga_libera_o == 1) {
                libere_o++;
            }
            riga_libera_x = 1;
            riga_libera_o = 1;
            // backward diagonal check (diagonal from (0,2) to (2,0)) al momento funziona
            // solo per matrici (3,3), va ampliato come concetto
            for (int h = 1, k = 1; k >= 0 && h < board.M; k--, h++) {
                // System.out.print("sonoqui1.3");
                if (board.B[h][k] != MNKCellState.P1 && board.B[h][k] != MNKCellState.FREE) {
                    riga_libera_x = 0;

                }
                if (board.B[h][k] != MNKCellState.P2 && board.B[h][k] != MNKCellState.FREE) {
                    riga_libera_o = 0;

                }
            }
            if (riga_libera_x == 1 || riga_libera_o == 1) {
                // forward diagonal check (diagonal from (0,2) to (2,0)) al momento funziona
                // solo per matrici (3,3), va ampliato come concetto
                for (int h = 1, k = 1; k < board.N && h >= 0; k++, h--) {
                    // System.out.print("sonoqui1.4");
                    if (board.B[h][k] != MNKCellState.P1 && board.B[h][k] != MNKCellState.FREE) {
                        riga_libera_x = 0;

                    }
                    if (board.B[h][k] != MNKCellState.P2 && board.B[h][k] != MNKCellState.FREE) {
                        riga_libera_o = 0;

                    }
                }
            }
            if (riga_libera_x == 1) {
                libere_x++;
            }
            if (riga_libera_o == 1) {
                libere_o++;
            }
            if (myWin == MNKGameState.WINP1) {
                System.out.println("WINP1: " + board.MC.toString());
                System.out.println("libere_x WINP1: " + libere_x);
                System.out.println("libere_o WINP1: " + libere_o);
                eval = libere_x - libere_o;
                System.out.println("eval WINP1: " + eval);
            } else {
                System.out.println("WINP2: " + board.MC.toString());
                // System.out.println("libere_x WINP2: " + libere_x);
                // System.out.println("libere_o WINP2: " + libere_o);
                eval = libere_o - libere_x;
                System.out.println("eval WINP2: " + eval);
            }

        }
        return eval;

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

    // FC = celle libere, MC = celle occupate
    private double alphaBeta(MNKBoard board, boolean playerA, double alpha, double beta, int depth) {
        MNKCell fc[] = board.getFreeCells();
        double eval;
        double alphaOrig = alpha;

        Board ttEntry = null;
        /* Controllo transposition table */
        if (!tTable.isEmpty()) {
            ttEntry = tTable.get(hash(board));
            if (ttEntry != null) {
                if (ttEntry.flag == Board.FLAG.EXACT)
                    return ttEntry.val;
                else if (ttEntry.flag == Board.FLAG.LOWER_BOUND)
                    alpha = Math.max(alpha, ttEntry.val);
                else
                    beta = Math.min(beta, ttEntry.val);

                if (alpha >= beta)
                    return ttEntry.val;
            }
        }

        if (depth == 0 || board.gameState != MNKGameState.OPEN
                || ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0))) {
            if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0)) {
                System.out.println("temposcaduto");
            }

            eval = evaluation(board);
        } else if (!playerA) { // mi calcolo il valore del ramo
            eval = -9;
            for (MNKCell d : fc) {
                board.markCell(d.i, d.j);
                eval = Math.max(eval, alphaBeta(board, true, alpha, beta, depth - 1));
                alpha = Math.max(eval, alpha);

                board.unmarkCell();
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            eval = 9;
            for (MNKCell d : fc) {
                board.markCell(d.i, d.j);
                eval = Math.min(eval, alphaBeta(board, false, alpha, beta, depth - 1));
                beta = Math.min(eval, beta);

                board.unmarkCell();
                if (beta <= alpha) {
                    break;
                }
            }
        }
        /* Salva nella transposition table */
        if (ttEntry == null) {
            if (eval <= alphaOrig)
                ttEntry = new Board(board, eval, Board.FLAG.UPPER_BOUND);
            else if (eval >= beta)
                ttEntry = new Board(board, eval, Board.FLAG.LOWER_BOUND);
            else
                ttEntry = new Board(board, eval, Board.FLAG.EXACT);
            tTable.put(hash(ttEntry.board), ttEntry);
        }

        return eval;
    }

    public double iterativeDeepeing(MNKBoard board, boolean playerA, int depth) {
        double alpha = -2;
        double beta = 2;
        double eval = -2;
        for (int k = 0; k < depth; k++) {
            eval = alphaBeta(board, playerA, alpha, beta, depth);
        }
        return eval;
    }

    public double MTD(MNKBoard board, double f, int depth) {
        double g = f;
        double upperbound = 9;
        double lowerbound = -9;
        double beta = 0;
        do {
            if (g == lowerbound) {
                beta = g + 1;
            } else {
                beta = g;
            }
            // since alpha is always one less than beta (da
            // http://people.csail.mit.edu/plaat/mtdf.html#abmem )
            g = alphaBeta(B, true, beta - 1, beta, depth);
            if (g < beta) {
                upperbound = g;
            } else {
                lowerbound = g;
            }
        } while (lowerbound >= upperbound);

        return g;
    }

    public double MTDF(MNKBoard board, MNKCell[] FC) {
        double firstguess = 0;
        for (int d = 1; d < FC.length; d++) {
            firstguess = MTD(board, firstguess, d);
            // If time is running out, return the randomly selected cell
            if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0)) {
                break;
            }
        }
        return firstguess;
    }

    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
        start = System.currentTimeMillis();
        System.out.println("---------------------------------------------------------------------------------------");
        MNKCell ultima_mossa;
        int pos = rand.nextInt(FC.length);
        // add to the local board the last opponent move
        if (MC.length > 0) {
            ultima_mossa = MC[MC.length - 1]; // Recover the last move from MC
            B.markCell(ultima_mossa.i, ultima_mossa.j); // Save the last move in the local MNKBoard
        }

        // If there is just one possible move, return immediately
        if (FC.length == 1) {
            return FC[0];
        }

        // Check whether there is single move win
        for (MNKCell c : FC) {
            if (B.markCell(c.i, c.j) == myWin) {
                System.out.println("hovinto");
                return c;
            } else {
                B.unmarkCell();
            }
        }

        /*
         * System.out.println("La mossa non fa vincere l'avversario: " + "[" + d.i + ","
         * + d.j + "]");
         * for (MNKCell f_c : FC) {
         * 
         * if (B.markCell(f_c.i, f_c.j) == yourWin) {
         * System.out.println("l'avversario può vincere e quindi gli blocco la mossa");
         * return f_c;
         * } else {
         * B.unmarkCell();
         * }
         * }
         */

        /*------------------------------------------------------------- */
        /*-------------------APPLICAZIONE ALGORITMO-------------------- */
        /*------------------------------------------------------------- */

        double score, maxEval = -9;
        MNKCell result = FC[pos]; // random move
        for (MNKCell currentCell : FC) {

            // If time is running out, return the randomly selected cell
            if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0)) {
                System.out.println("temposcaduto!!!!!!!");
                break;

            } else {
                score = MTDF(B, FC);
                if (score > maxEval) {
                    maxEval = score;
                    result = currentCell;
                }
            }
        }

        B.markCell(result.i, result.j);
        for (MNKCell libera : FC) {
            // If time is running out, return the randomly selected cell
            if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0)) {
                return result;
            } else if (libera != result) {
                if (B.markCell(libera.i, libera.j) == yourWin) {
                    B.unmarkCell(); // undo adversary move e cioè rimuove la mossa "d"
                    B.unmarkCell(); // undo my move e cioè rimuove la mossa "c"
                    B.markCell(libera.i, libera.j); // select his winning position
                    return libera; // return his winning position
                } else {
                    B.unmarkCell(); // undo adversary move to try a new one
                }
            }
        }
        System.out.println("---------------------------------------------------------------------------------------");
        return result;
    }

    public String playerName() {
        return "MyPlayer";
    }
}