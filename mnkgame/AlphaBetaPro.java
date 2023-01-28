package mnkgame;

import java.util.*;

class AlphaBetaPro implements MNKPlayer {
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

    private HashMap<String, Board> tTable1;

    public AlphaBetaPro() {
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
        tTable1 = new HashMap<>();

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

    private double alphaBetaStandard(MNKBoard board, boolean playerA, double alpha, double beta, int depth) {
        MNKCell fc[] = board.getFreeCells();
        double eval;
        double alphaOrig = alpha;
        
        Board ttEntry = null;
        /* Controllo transposition table */
        if (!tTable1.isEmpty()) {
            ttEntry = tTable1.get(hash(board));
            if (ttEntry != null) {
                if (ttEntry.flag == Board.FLAG.EXACT)
                    return ttEntry.val;
                else if (ttEntry.flag == Board.FLAG.LOWER_BOUND)
                    alpha = Math.max(alpha, ttEntry.val);
                else
                    beta = Math.min(beta, ttEntry.val);

                if (alpha >= beta) return ttEntry.val;
            }
        }

        if (depth == 0 || board.gameState != MNKGameState.OPEN || TEMPO_SCADUTO) {
            eval = eval(board);
        } else if (!playerA) {   // mi calcolo il valore del ramo
            eval = -2;
            for (MNKCell d : fc) {
                board.markCell(d.i, d.j);
                eval = Math.max(eval, alphaBetaStandard(board, true, alpha, beta, depth-1));
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
                eval = Math.min(eval, alphaBetaStandard(board, false, alpha, beta, depth-1));
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
                ttEntry = new Board(board, eval,Board.FLAG.UPPER_BOUND);
            else if (eval >= beta)
                ttEntry = new Board(board, eval, Board.FLAG.LOWER_BOUND);
            else
                ttEntry = new Board(board, eval, Board.FLAG.EXACT);
            tTable1.put(hash(ttEntry.board), ttEntry);
        }
        
        return eval;
    }
    
    public double iterativeDeepeing(MNKBoard board, boolean playerA, int depth) {
        double alpha = -2;
        double beta = 2;
        double eval = -2;
        for(int k=0; k<depth; k++) {
            if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (90.0 / 100.0)) {
                break;
            }
            eval = alphaBeta(board, playerA, alpha, beta, depth);
        }
        return eval;
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

    private void mark(MNKCell c, MNKBoard B) {
        B.markCell(c.i, c.j);
        Q.remove(c, B);
        current_hash = current_hash ^ zobristHash(c, B);
        max.update(c, B, Q);
        min.update(c, B, Q);

        // Debug messages

        /*
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
        else { // TODO!
            double eval = 0;
            if (myTurn(B)) {
                if (max.winning() >= 1)
                    return +1;
                if (min.winning() >= 2)
                    return -1;
                else if (min.winning() == 1)
                    eval -= 0.2;
                /*
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
                 */
            } else {
                if (min.winning() >= 1)
                    return -1;
                if (max.winning() >= 2)
                    return +1;
                else if (max.winning() == 1)
                    eval += 0.2;
                /*
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
                 */
            }

            if (max.size() > min.size())
                eval += 0.3;
            else if (min.size() > max.size())
                eval -= 0.3;
            return eval;
        }
    }

    private double alphaBeta(MNKBoard B, boolean max_player, double alpha, double beta, int depth) {
        // Transposition table lookup
        MNKBoardPlus ttEntry = tTable.get(current_hash);
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

        if (depth == 0 || B.gameState() != MNKGameState.OPEN || TEMPO_SCADUTO)
            eval = eval(B);
        else if (max_player) {
            eval = -999;
            MNKCell[] FC = B.getFreeCells(), queue_moves = Q.moves();

            for (MNKCell c : queue_moves != null ? queue_moves : FC) {
                if (TEMPO_SCADUTO) {
                    break;
                }
                /*
                 * if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (90.0 / 100.0))
                 * break;
                 */

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
                /*
                 * if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (90.0 / 100.0))
                 * break;
                 */
                if (TEMPO_SCADUTO) {
                    break;
                }
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
            if (TEMPO_SCADUTO) {
                break;
            }
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
            if (TEMPO_SCADUTO) {
                break;
            }
            c = parentAlphaBeta(B);

            /*
             * if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (90.0 / 100.0))
             * break;
             */
        }
        return c;
    }

    @Override
    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
        start = System.currentTimeMillis();
        TEMPO_SCADUTO = false;
        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //System.out.println("TEMPO SCADUTO");
                TEMPO_SCADUTO = true;
            }
        }, (TIMEOUT) * 950);
        long starting_hash = 0;

        if(FC.length <= 16) {
            //System.out.println("ALPHABETASTANDARD");
            int pos = rand.nextInt(FC.length);

        // add to the local board the last opponent move
        if (MC.length > 0) {
            MNKCell c = MC[MC.length - 1]; // Recover the last move from MC
            B.markCell(c.i, c.j);         // Save the last move in the local MNKBoard
        }
        // If there is just one possible move, return immediately
        if (FC.length == 1) {
            return FC[0];
        }
        
        
        
        // Check whether there is single move win 
        for (MNKCell c : FC) {
            if (B.markCell(c.i, c.j) == myWin)
                return c;
            else
                B.unmarkCell();
        }
        
        // Check whether there is a single move loss:
        // 1. mark a random position
        // 2. check whether the adversary can win
        // 3. if he can win, select his winning position 
        /*
        MNKCell c = FC[pos]; // random move
        B.markCell(c.i, c.j); // mark the random position	
        for (int k = 0; k < FC.length; k++) {
            // If time is running out, return the randomly selected  cell
            if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0)) {
                return c;
            } else if (k != pos) {
                MNKCell d = FC[k];
                if (B.markCell(d.i, d.j) == yourWin) {
                    B.unmarkCell();        // undo adversary move
                    B.unmarkCell();	       // undo my move	 
                    B.markCell(d.i, d.j);   // select his winning position
                    return d;							 // return his winning position
                } else {
                    B.unmarkCell();	       // undo adversary move to try a new one
                }
            }
        }
        */
        
        /*------------------------------------------------------------- */
        /*-------------------APPLICAZIONE ALGORITMO-------------------- */
        /*------------------------------------------------------------- */

        double score, maxEval = -2;
        MNKCell result = FC[pos]; // random move
        
        // Check whether there is a single move loss:
        // 1. mark a random position
        // 2. check whether the adversary can win
        // 3. if he can win, select his winning position 
        //B.markCell(result.i, result.j);
        for (int k = 0; k < FC.length; k++) {
			// If time is running out, return the randomly selected cell
			if (TEMPO_SCADUTO) {
				return result;
			} else if (k != pos) {
				MNKCell d = FC[k];
				if (B.markCell(d.i, d.j) == yourWin) {
					B.unmarkCell(); // undo adversary move
					B.unmarkCell(); // undo my move
					B.markCell(d.i, d.j); // select his winning position
					return d; // return his winning position
				} else {
					B.unmarkCell(); // undo adversary move to try a new one
				}
			}
		}
        
        //System.out.println("celle libere: " + FC.length);
        for (MNKCell currentCell : FC) {

            // If time is running out, return the randomly selected  cell
            if (TEMPO_SCADUTO) {
                break;

            } else {
                B.markCell(currentCell.i, currentCell.j);
                
                score = alphaBetaStandard(B, true, -2, 2, FC.length);
                //score = iterativeDeepeing(B, true, FC.length);
                //score = MTDF(B, FC);
                
                /*
                if(FC.length <= 16) {
                    score = iterativeDeepeing(B, true, FC.length);
                } else {
                    score = MTDF(B, FC);
                }*/
                
                if (score > maxEval) {
                    maxEval = score;
                    result = currentCell;
                }

                B.unmarkCell();
            }
        }
        timer.cancel();
        B.markCell(result.i, result.j);
        return result;
        }

        if (FC.length == 1) {
            return FC[0];
        }
        // Starting move: play in the middle of the board
        if (MC.length == 0) {

            MNKCell c = new MNKCell(B.M / 2, B.N / 2);

            mark(c, B);
            return c;
        }
        // Retrieve adversary move
        if (MC.length > 0) {
            MNKCell c = MC[MC.length - 1];
            mark(c, B);
            starting_hash = current_hash;
        }

        // One-move win/lose check
        if (max.winning() >= 1 || min.winning() >= 1) {
            MNKCell c = max.winning() >= 1 ? max.winningCell(B) : min.winningCell(B);
            mark(c, B);
            return c;
        }

        MNKCell[] queue_moves = Q.moves();
        // Q.printQueue();
        if (queue_moves != null && MC.length >= 3) {
            // se vinciamo in due modi diversi
            for (MNKCell mossa : queue_moves) {
                // System.out.println("------------------------------------------------");
                // System.out.println("Sto analizzando la casella: [" + mossa.i + "," + mossa.j
                // + "]");
                // B.markCell(mossa.i, mossa.j);
                mark(mossa, B);
                int strategie_vincenti = 0;
                for (MNKStrategy s : max.getStrategie(mossa)) {
                    // System.out.println(s);

                    if (s.winning()) {
                        // System.out.println("La strategia " + s + " è vincente");
                        strategie_vincenti++;
                    }
                    if (strategie_vincenti == 2) {
                        timer.cancel();
                        return mossa;
                    }
                }
                // B.unmarkCell();
                unmark(mossa, B);
            }

            // Se perdiamo in due modi diversi
            for (MNKCell mossa : queue_moves) {
                // System.out.println("------------------------------------------------");
                // B.markCell(mossa.i, mossa.j); // nostra
                mark(mossa, B);
                for (MNKCell mossa_avversario : queue_moves) {
                    if ((mossa.i != mossa_avversario.i) || (mossa.j != mossa_avversario.j)) {
                        // B.markCell(mossa_avversario.i, mossa_avversario.j); // avversario
                        mark(mossa_avversario, B);
                        int strategie_vincenti = 0;
                        for (MNKStrategy s : min.getStrategie(mossa_avversario)) {

                            if (s.winning()) {
                                strategie_vincenti++;
                            }
                            if (strategie_vincenti == 2) {
                                // B.unmarkCell();
                                // B.unmarkCell();
                                unmark(mossa_avversario, B);
                                unmark(mossa, B);
                                // B.markCell(mossa_avversario.i, mossa_avversario.j);
                                mark(mossa_avversario, B);
                                timer.cancel();
                                return mossa_avversario;
                            }
                        }
                        unmark(mossa_avversario, B);
                    }

                }
                // B.unmarkCell();
                unmark(mossa, B);
            }

        }

        // MNKCell c = iterativeDeepening(B);
        MNKCell c = parentAlphaBeta(B);

        mark(c, B);
        timer.cancel();
        if ((starting_hash ^ zobristHash(c, B)) != current_hash)
            throw new IllegalStateException("Error in hashing!");
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
