package mnkgame;

import java.util.*;

class AlphaBetaPro2 implements MNKPlayer {
    private MNKBoard B;
    private Random rand;
    private boolean first;
    private MNKGameState myWin, yourWin;
    private long start, current_hash;
    private long[][] zobristTable;
    private HashMap<Long, MNKBoardPlus> tTable;
    private MovesQueue Q;
    private StrategySet max, min;
    private int TIMEOUT;
    private boolean TEMPO_SCADUTO;
    MNKBoardPlus ttEntry;

    private enum Flag {
        EXACT, LOWERBOUND, UPPERBOUND
    }

    private static class MNKBoardPlus {
        public enum Flag {
            EXACT, LOWERBOUND, UPPERBOUND
        }

        private Flag flag;
        private double eval;
        private int depth;

        public MNKBoardPlus(Flag f, double val, int d) {
            flag = f;
            eval = val;
            depth = d;
        }
    }

    public AlphaBetaPro2() {
    }

    @Override
    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        B = new MNKBoard(M, N, K);
        rand = new Random(System.currentTimeMillis());
        this.first = first;
        myWin = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        yourWin = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
        zobristTable = new long[M * N][2];
        tTable = new HashMap<>();
        Q = new MovesQueue(B, first ? MNKCellState.P1 : MNKCellState.P2);
        max = new StrategySet(B, first ? MNKCellState.P1 : MNKCellState.P2);
        min = new StrategySet(B, first ? MNKCellState.P2 : MNKCellState.P1);
        TIMEOUT = timeout_in_secs;
        tTable = new HashMap<>();

        for (int i = 0; i < M * N; i++) {
            zobristTable[i][0] = rand.nextLong();
            zobristTable[i][1] = rand.nextLong();
        }
    }

    /**
     * Complessità: O(1)
     * 
     * @param B configurazione attuale di gioco
     * @return True se è il turno di questo giocatore; False altrimenti
     */
    private boolean myTurn(MNKBoard B) {
        return first ? (B.currentPlayer() == 0) : (B.currentPlayer() == 1);
    }

    private int cellIndex(MNKCell c) {
        return c.i * B.N + c.j;
    }

    /**
     * Complessità: O(1)
     * 
     * @param c cella di cui si vuole conoscere il valore hash
     * @param B configurazione attuale di gioco
     * @return hash di c
     */
    private long zobristHash(MNKCell c, MNKBoard B) {
        if (B.cellState(c.i, c.j) == MNKCellState.FREE)
            throw new IllegalStateException("Hashing of free cell requested.");
        return B.cellState(c.i, c.j) == MNKCellState.P1 ? zobristTable[cellIndex(c)][0] : zobristTable[cellIndex(c)][1];
    }

    private MNKGameState mark(MNKCell c, MNKBoard B) {
        MNKGameState esito = B.markCell(c.i, c.j);
        Q.remove(c, B);
        current_hash = current_hash ^ zobristHash(c, B);
        max.update(c, B, Q);
        min.update(c, B, Q);
        return esito;

        // Debug messages
        /*
         * /
         * System.out.println("Marked [" + c.i + "," + c.j + "]");
         * print(B);
         * max.print();
         * min.print();
         * Q.printQueue();
         * System.out.print('\n');
         */
    }

    private void unmark(MNKCell c, MNKBoard B) {
        Q.undo(B, c);
        max.undo(c, B, Q);
        min.undo(c, B, Q);
        current_hash = current_hash ^ zobristHash(c, B);
        B.unmarkCell();

        // Debug messages
        /*
         * System.out.println("Unmarked [" + c.i + "," + c.j + "]");
         * print(B);
         * max.print();
         * min.print();
         * Q.printQueue();
         * System.out.print('\n');
         */
    }

    private double eval(MNKBoard B) {
        if (B.gameState() == myWin)
            return +1;
        else if (B.gameState() == yourWin)
            return -1;
        else if (B.gameState() == MNKGameState.DRAW)
            return 0;
        return 0;
        /*
         * else { // TODO!
         * double eval = 0;
         * if (myTurn(B)) {
         * if (max.winning() >= 1)
         * return +1;
         * if (min.winning() >= 2)
         * return -1;
         * else if (min.winning() == 1)
         * eval -= 0.2;
         * /*
         * IntersectionSet maxISet = max.generateFrom(B);
         * if (maxISet != null) {
         * //if (maxISet.winning())
         * //return +1;
         * if (maxISet.size() >= 1) {
         * eval += 0.5;
         * //if (maxISet.max() >= 3)
         * //eval += 0.2;
         * }
         * }
         * 
         * } else {
         * if (min.winning() >= 1)
         * return -1;
         * if (max.winning() >= 2)
         * return +1;
         * else if (max.winning() == 1)
         * eval += 0.2;
         * /*
         * IntersectionSet minISet = min.generateFrom(B);
         * if (minISet != null) {
         * //if (minISet.winning())
         * //return -1;
         * if (minISet.size() >= 1) {
         * eval -= 0.5;
         * //if (minISet.max() >= 3)
         * //eval -= 0.2;
         * }
         * }
         * 
         * }
         * 
         * if (max.size() > min.size())
         * eval += 0.3;
         * else if (min.size() > max.size())
         * eval -= 0.3;
         * return eval;
         * }
         */
    }

    private double alphaBeta(MNKBoard B, boolean max_player, double alpha, double beta, int depth) {
        // Transposition table lookup
        ttEntry = tTable.get(current_hash);
        if (ttEntry != null && ttEntry.depth >= depth) {
            switch (ttEntry.flag) {
                case EXACT: {
                    return ttEntry.eval;
                }
                case LOWERBOUND:
                    alpha = Math.max(alpha, ttEntry.eval);
                case UPPERBOUND:
                    beta = Math.min(beta, ttEntry.eval);
            }
            if (alpha >= beta)
                return ttEntry.eval;
        }

        double eval;
        double alphaOrig = alpha, betaOrig = beta;

        if (depth == 0 || B.gameState() != MNKGameState.OPEN)
            eval = eval(B);
        else if (max_player) {
            eval = -999;
            MNKCell[] FC = B.getFreeCells(), queue_moves = Q.moves();

            for (MNKCell c : queue_moves != null ? queue_moves : FC) {
                if (TEMPO_SCADUTO)
                    break;

                mark(c, B);
                eval = Math.max(eval, alphaBeta(B, false, alpha, beta, depth - 1));
                unmark(c, B);

                alpha = Math.max(alpha, eval);

                if (alpha >= beta)
                    break;
            }
        } else {
            eval = +999;
            MNKCell[] FC = B.getFreeCells();

            for (MNKCell c : FC) {
                if (TEMPO_SCADUTO)
                    break;

                mark(c, B);
                eval = Math.min(eval, alphaBeta(B, true, alpha, beta, depth - 1));
                unmark(c, B);

                beta = Math.min(beta, eval);

                if (alpha >= beta)
                    break;
            }
        }

        // Transposition table store
        if (eval <= alphaOrig)
            tTable.put(current_hash, new MNKBoardPlus(MNKBoardPlus.Flag.UPPERBOUND, eval, depth));
        else if (eval >= betaOrig)
            tTable.put(current_hash, new MNKBoardPlus(MNKBoardPlus.Flag.LOWERBOUND, eval, depth));
        else
            tTable.put(current_hash, new MNKBoardPlus(MNKBoardPlus.Flag.EXACT, eval, depth));

        return eval;
    }

    private MNKCell parentAlphaBeta(MNKBoard B) {
        double eval, best_eval = -999;
        MNKCell[] FC = B.getFreeCells(), queue_moves = Q.moves();
        MNKCell selected = FC[rand.nextInt(FC.length)];

        for (MNKCell c : queue_moves != null ? queue_moves : FC) {
            mark(c, B);
            eval = alphaBeta(B, false, -1, 1, FC.length - 1);

            if (eval > best_eval) {
                best_eval = eval;
                selected = c;
            }

            unmark(c, B);
        }

        return selected;
    }

    private MNKCell iterativeDeepening(MNKBoard B) {
        MNKCell[] FC = B.getFreeCells();
        int MAX_DEPTH = FC.length;
        MNKCell c = FC[rand.nextInt(FC.length)];
        for (int d = 0; d < MAX_DEPTH; d++) {
            c = parentAlphaBeta(B);

            if (TEMPO_SCADUTO)
                break;
        }
        return c;
    }

    private double alphaBetaStandard(MNKBoard board, boolean playerA, double alpha, double beta, int depth) {
        MNKCell fc[] = board.getFreeCells();
        double eval;

        ttEntry = tTable.get(current_hash);
        if (ttEntry != null && ttEntry.depth >= depth) {
            switch (ttEntry.flag) {
                case EXACT: {
                    return ttEntry.eval;
                }
                case LOWERBOUND:
                    alpha = Math.max(alpha, ttEntry.eval);
                case UPPERBOUND:
                    beta = Math.min(beta, ttEntry.eval);
            }
            if (alpha >= beta)
                return ttEntry.eval;
        }

        double alphaOrig = alpha, betaOrig = beta;

        if (depth == 0 || board.gameState != MNKGameState.OPEN || TEMPO_SCADUTO) {
            eval = eval(board);
        } else if (!playerA) { // mi calcolo il valore del ramo
            eval = -2;
            for (MNKCell d : fc) {
                board.markCell(d.i, d.j);
                eval = Math.max(eval, alphaBetaStandard(board, true, alpha, beta, depth - 1));
                alpha = Math.max(eval, alpha);

                board.unmarkCell();
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            eval = 2;
            for (MNKCell d : fc) {
                board.markCell(d.i, d.j);
                eval = Math.min(eval, alphaBetaStandard(board, false, alpha, beta, depth - 1));
                beta = Math.min(eval, beta);

                board.unmarkCell();
                if (beta <= alpha) {
                    break;
                }
            }
        }
        // Transposition table store
        if (eval <= alphaOrig)
            tTable.put(current_hash, new MNKBoardPlus(MNKBoardPlus.Flag.UPPERBOUND, eval, depth));
        else if (eval >= betaOrig)
            tTable.put(current_hash, new MNKBoardPlus(MNKBoardPlus.Flag.LOWERBOUND, eval, depth));
        else
            tTable.put(current_hash, new MNKBoardPlus(MNKBoardPlus.Flag.EXACT, eval, depth));

        return eval;
    }

    @Override
    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
        TEMPO_SCADUTO = false;
        start = System.currentTimeMillis();
        long starting_hash = 0;

        // Retrieve adversary move
        if (MC.length > 0) {
            MNKCell c = MC[MC.length - 1];
            mark(c, B);
            // System.out.println("ADV: " + c);
            starting_hash = current_hash;
        }

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                TEMPO_SCADUTO = true;
            }
        }, (TIMEOUT) * 950);

        if (FC.length <= 16) {
            double score, maxEval = -999;
            MNKCell result = FC[rand.nextInt(FC.length)];
            for (MNKCell currentCell : FC) {
                System.out.print("prendo la cella: " + currentCell);
                // If time is running out, return the randomly selected cell
                if (TEMPO_SCADUTO) {
                    break;
                } else {
                    B.markCell(currentCell.i, currentCell.j);

                    score = alphaBetaStandard(B, true, -2, 2, FC.length);
                    // score = MTDF(B, FC);
                    System.out.println(" che ha valore: " + score);
                    /*
                     * if(FC.length <= 16) {
                     * score = iterativeDeepeing(B, true, FC.length);
                     * } else {
                     * score = MTDF(B, FC);
                     * }
                     */

                    if (score > maxEval) {
                        maxEval = score;
                        result = currentCell;
                    }

                    B.unmarkCell();
                }
            }
            B.markCell(result.i, result.j);
            System.out.println("Cella alphabeta: " + result);
            return result;
        }

        // One-move win/lose check
        // System.out.println("max.winning(): " + max.winning());
        if (max.winning() >= 1 || min.winning() >= 1) {
            MNKCell c = max.winning() >= 1 ? max.winningCell(B) : min.winningCell(B);
            // System.out.println("mossa vincente/perdente:" + c);
            mark(c, B);
            return c;
        }
        long start1;
        if (MC.length >= 3 || FC.length > 16) {
            start1 = System.currentTimeMillis();
            MNKGameState esito;
            MNKCell[] queue_moves1;

            // posso vincere in due mosse?
            queue_moves1 = Q.moves();
            if (queue_moves1!=null && queue_moves1.length > 0) {
                // Q.printQueue();
                for (MNKCell cellaMia1 : queue_moves1) {
                    // System.out.println("CellaMia1: " + cellaMia1);
                    mark(cellaMia1, B);
                    MNKCell[] queue_moves_adv1 = Q.moves();
                    if (queue_moves_adv1!=null && queue_moves_adv1.length > 0) {
                        // Q.printQueue();
                        for (MNKCell cellaSua1 : queue_moves_adv1) {
                            mark(cellaSua1, B);
                            // System.out.println("cellaSua1: " + cellaSua1);
                            MNKCell[] queue_moves2 = Q.moves();
                            if (queue_moves2!=null && queue_moves2.length > 0) {
                                // Q.printQueue();
                                for (MNKCell cellaMia2 : queue_moves2) {
                                    esito = mark(cellaMia2, B);
                                    if (esito == myWin) {
                                        /*
                                         * System.out.println("Cella vincente mia trovata in seq:");
                                         * System.out.println(cellaMia1);
                                         * System.out.println(cellaSua1);
                                         * System.out.println(cellaMia2);
                                         */
                                        unmark(cellaMia2, B);
                                        unmark(cellaSua1, B);
                                        unmark(cellaMia1, B);
                                        mark(cellaMia1, B);
                                        // Q.printQueue();
                                        return cellaMia1;
                                    }
                                    unmark(cellaMia2, B);
                                }
                            }
                            unmark(cellaSua1, B);
                        }
                    }
                    unmark(cellaMia1, B);
                }
            }
            // posso perdere in due mosse?
            queue_moves1 = Q.moves();
            if (queue_moves1!=null && queue_moves1.length > 0) {
                // Q.printQueue();
                for (MNKCell cellaMia1 : queue_moves1) {
                    // System.out.println("CellaMia1: " + cellaMia1);
                    mark(cellaMia1, B);
                    MNKCell[] queue_moves_adv1 = Q.moves();
                    if (queue_moves_adv1!=null && queue_moves_adv1.length > 0) {
                        // Q.printQueue();
                        for (MNKCell cellaSua1 : queue_moves_adv1) {
                            mark(cellaSua1, B);
                            // System.out.println("cellaSua1: " + cellaSua1);
                            MNKCell[] queue_moves2 = Q.moves();
                            if (queue_moves2!=null && queue_moves2.length > 0) {
                                // Q.printQueue();
                                for (MNKCell cellaMia2 : queue_moves2) {
                                    mark(cellaMia2, B);
                                    // System.out.println("cellaMia2: " + cellaMia2);
                                    MNKCell[] queue_moves_adv2 = Q.moves();
                                    // Q.printQueue();
                                    if (queue_moves_adv2!=null && queue_moves_adv2.length > 0) {
                                        for (MNKCell cellaSua2 : queue_moves_adv2) {
                                            // System.out.println("cellaSua2: " + cellaSua2);
                                            esito = mark(cellaSua2, B);
                                            if (esito == yourWin) {
                                                /*
                                                 * System.out.println("Cella vincente avversaria trovata in seq:");
                                                 * System.out.println(cellaMia1);
                                                 * System.out.println(cellaSua1);
                                                 * System.out.println(cellaMia2);
                                                 * System.out.println(cellaSua2);
                                                 */
                                                unmark(cellaSua2, B);
                                                unmark(cellaMia2, B);
                                                unmark(cellaSua1, B);
                                                unmark(cellaMia1, B);
                                                mark(cellaSua1, B);
                                                // Q.printQueue();
                                                return cellaSua1;
                                            }
                                            unmark(cellaSua2, B);
                                        }
                                        unmark(cellaMia2, B);
                                    }
                                }
                                unmark(cellaSua1, B);
                            }
                        }
                    }
                    unmark(cellaMia1, B);
                }
            }
            System.out.println("Tempo impiegato: " + (System.currentTimeMillis() - start1));
        }

        if (FC.length == 1) {
            return FC[0];
        }

        // Starting move: play in the middle of the board
        if (MC.length == 0) {
            MNKCell c = new MNKCell(B.M / 2, B.N / 2);
            mark(c, B);
            // Q.printQueue();
            return c;
        }

        // MNKCell c = iterativeDeepening(B);
        MNKCell c = parentAlphaBeta(B);
        mark(c, B);
        if ((starting_hash ^ zobristHash(c, B)) != current_hash)
            throw new IllegalStateException("Error in hashing!");

        // stampe
        // Q.printQueue();

        return c;
    }

    @Override
    public String playerName() {
        return "AlphaBetaPro";
    }

    // Debug
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void print(MNKBoard B) {
        for (int i = 0; i < B.M; i++) {
            for (int j = 0; j < B.N; j++) {
                if (B.B[i][j] == MNKCellState.FREE)
                    System.out.print("/ ");
                else if (B.B[i][j] == MNKCellState.P1)
                    System.out.print("X ");
                else
                    System.out.print("O ");
            }
            System.out.println();
        }
    }
}
