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
    private int max_set_size;

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

    public AlphaBetaPro() {}

    @Override
    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        B            = new MNKBoard(M, N, K);
        rand         = new Random(System.currentTimeMillis());
        this.first   = first;
        myWin        = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        yourWin      = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
        zobristTable = new long[M * N][2];
        tTable       = new HashMap<>();
        Q            = new MovesQueue(B, first ? MNKCellState.P1 : MNKCellState.P2);
        max          = new StrategySet(B, first ? MNKCellState.P1 : MNKCellState.P2);
        min          = new StrategySet(B, first ? MNKCellState.P2 : MNKCellState.P1);
        TIMEOUT      = timeout_in_secs;
        max_set_size = 0;

        for (int i = 0; i < M * N; i++) {
            zobristTable[i][0] = rand.nextLong();
            zobristTable[i][1] = rand.nextLong();
        }
    }

    /**
     * Complessità: O(1)
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
        // if (B.cellState(c.i, c.j) == MNKCellState.P1) {
        // System.out.println("Marked [" + c.i + "," + c.j + "]");
        // print(B);
        // max.print();
        // min.print();
        // Q.printQueue();
        // System.out.print('\n');
        // }
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
        if (B.gameState() == myWin) {
            return +1;
        } else if (B.gameState() == yourWin) {
            return -1;
        } else if (B.gameState() == MNKGameState.DRAW) {
            return 0;
        } else {
            double eval = 0;
            if (myTurn(B)) {
                if (max.winning() >= 1) {
                    return +1;
                }
                if (min.winning() >= 2) {
                    return -1;
                } else if (min.winning() == 1) {
                    eval -= 0.2;
                }

            } else {
                if (min.winning() >= 1) {
                    return -1;
                }
                if (max.winning() >= 2) {
                    return +1;
                } else if (max.winning() == 1) {
                    eval += 0.2;
                }

            }

            if (max.size() > min.size()) {
                eval += 0.3;
            } else if (min.size() > max.size())
                eval -= 0.3;
            return eval;
        }
    }

    private double alphaBeta(MNKBoard B, boolean max_player, double alpha, double beta, int depth) {
        // Transposition table lookup
        MNKBoardPlus ttEntry = tTable.get(current_hash);
        if (ttEntry != null && ttEntry.depth >= depth) {
            switch (ttEntry.flag) {
                case EXACT: { return ttEntry.eval; }
                case LOWERBOUND:
                    alpha = Math.max(alpha, ttEntry.eval);
                case UPPERBOUND:
                    beta = Math.min(beta, ttEntry.eval);
            } if (alpha >= beta) return ttEntry.eval;
        }

        double eval;
        double alphaOrig = alpha, betaOrig = beta;

        if (depth == 0 || B.gameState() != MNKGameState.OPEN || TEMPO_SCADUTO)
            eval = eval(B);
        else if (max_player) {
            eval = -999;
            MNKCell[] queue_moves = Q.moves();

            if (queue_moves == null) {
                return 0;
            }

            for (MNKCell c : queue_moves) {
                if (TEMPO_SCADUTO) {
                    break;
                }
                
                mark(c, B);
                eval = Math.max(eval, alphaBeta(B, false, alpha, beta, depth - 1));
                unmark(c, B);

                alpha = Math.max(alpha, eval);

                if (alpha >= beta)
                    break;
            }
        } else {
            eval = +999;
            MNKCell[] queue_moves = Q.moves();

            if (queue_moves == null) {
                return 0;
            }

            for (MNKCell c : queue_moves) {

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

    private double alphaBetaStandard(MNKBoard B, boolean max_player, double alpha, double beta, int depth) {
        MNKCell[] FC = B.getFreeCells();
        double eval;
        double alphaOrig = alpha, betaOrig = beta;

        // Transposition table lookup
        MNKBoardPlus ttEntry = tTable.get(current_hash);
        if (ttEntry != null && ttEntry.depth >= depth) {
            switch (ttEntry.flag) {
                case EXACT: { return ttEntry.eval; }
                case LOWERBOUND:
                    alpha = Math.max(alpha, ttEntry.eval);
                case UPPERBOUND:
                    beta = Math.min(beta, ttEntry.eval);
            } if (alpha >= beta) return ttEntry.eval;
        }

        if (depth == 0 || B.gameState() != MNKGameState.OPEN || TEMPO_SCADUTO) {
            eval = eval(B);
        } else if (!max_player) {
            eval = -2;
            for (MNKCell c : FC) {
                B.markCell(c.i, c.j);
                current_hash = current_hash ^ zobristHash(c, B);

                eval = Math.max(eval, alphaBetaStandard(B, true, alpha, beta, depth - 1));
                alpha = Math.max(eval, alpha);

                current_hash = current_hash ^ zobristHash(c, B);
                B.unmarkCell();

                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            eval = 2;
            for (MNKCell c : FC) {
                B.markCell(c.i, c.j);
                current_hash = current_hash ^ zobristHash(c, B);

                eval = Math.min(eval, alphaBetaStandard(B, false, alpha, beta, depth - 1));
                beta = Math.min(eval, beta);

                current_hash = current_hash ^ zobristHash(c, B);
                B.unmarkCell();

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

    private MNKCell parentAlphaBeta(MNKBoard B) {
        double eval, best_eval = -999;
        MNKCell[] FC = B.getFreeCells(), queue_moves = Q.moves();
        MNKCell selected = FC[rand.nextInt(FC.length)];
        if (queue_moves == null) {
            return selected;
        }
        for (MNKCell c : queue_moves) {
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
            if (TEMPO_SCADUTO)
                break;
            c = parentAlphaBeta(B);
        } return c;
    }

    @Override
    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
        start = System.currentTimeMillis();
        TEMPO_SCADUTO = false;
        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                TEMPO_SCADUTO = true;
            }
        }, (TIMEOUT) * 950);
        long starting_hash = 0;

        if (max.size()>max_set_size) {
            max_set_size = max.size();
        }

        // add to the local board the last opponent move
        if (MC.length > 0) {
            MNKCell c = MC[MC.length - 1]; // Recover the last move from MC
            mark(c,B); // Save the last move in the local MNKBoard
            starting_hash = current_hash;
        }
        

        // If there is just one possible move, return immediately
        if (FC.length == 1) {
            timer.cancel();
            return FC[0];
        }

        // One-move win/lose check
        if (max.winning() >= 1 || min.winning() >= 1) {
            MNKCell c = max.winning() >= 1 ? max.winningCell(B) : min.winningCell(B);
            mark(c, B);
            timer.cancel();
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

            // se vinciamo in un modo in due mosse
            for (MNKCell mossa : queue_moves) {
                // System.out.println("------------------------------------------------");
                // System.out.println("Sto analizzando la casella: [" + mossa.i + "," + mossa.j
                // + "]");
                // B.markCell(mossa.i, mossa.j);
                mark(mossa, B);
                int strategie_vincenti = 0;
                for (MNKStrategy s : max.getStrategie(mossa)) {
                    if (s.winning()) {
                        // System.out.println("La strategia " + s + " è vincente");
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
                mark(mossa, B);
                for (MNKCell mossa_avversario : queue_moves) {
                    if ((mossa.i != mossa_avversario.i) || (mossa.j != mossa_avversario.j)) {
                        mark(mossa_avversario, B);
                        int strategie_vincenti = 0;
                        for (MNKStrategy s : min.getStrategie(mossa_avversario)) {

                            if (s.winning()) {
                                strategie_vincenti++;
                            }
                            if (strategie_vincenti == 2) {
                                unmark(mossa_avversario, B);
                                unmark(mossa, B);
                                mark(mossa_avversario, B);
                                timer.cancel();
                                return mossa_avversario;
                            }
                        }
                        unmark(mossa_avversario, B);
                    }
                }
                unmark(mossa, B);
            }

        }

        //if (FC.length <= 16) {
        if (B.M * B.N <= 16) {
            // System.out.println("ALPHABETASTANDARD");
            int pos = rand.nextInt(FC.length);

            double score, maxEval = -2;
            MNKCell result = FC[pos]; // random move

            // System.out.println("celle libere: " + FC.length);
            for (MNKCell currentCell : FC) {
                // If time is running out, return the randomly selected cell
                if (TEMPO_SCADUTO) {
                    break;
                } else {
                    B.markCell(currentCell.i, currentCell.j);
                    current_hash = current_hash ^ zobristHash(currentCell, B);

                    score = alphaBetaStandard(B, true, -2, 2, FC.length);

                    if (score > maxEval) {
                        maxEval = score;
                        result = currentCell;
                    }
                    current_hash = current_hash ^ zobristHash(currentCell, B);
                    B.unmarkCell();
                }
            }

            B.markCell(result.i, result.j);
            current_hash = current_hash ^ zobristHash(result, B);
            timer.cancel();
            return result;
        } else {
            // Starting move: play in the middle of the board
            if (MC.length == 0) {

                MNKCell c = new MNKCell(B.M / 2, B.N / 2);

                mark(c, B);
                timer.cancel();
                return c;
            }

            if (MC.length == 1) {
                MNKCell c = MC[0];
                if (c.i - 1 >= 0 && c.j - 1 >= 0 && B.cellState(c.i - 1, c.j - 1) == MNKCellState.FREE) {
                    MNKCell d = new MNKCell(c.i - 1, c.j - 1);
                    mark(d, B);
                    return d;
                } else if (c.i + 1 < B.M && c.j + 1 < B.N && B.cellState(c.i + 1, c.j + 1) == MNKCellState.FREE) {
                    MNKCell d = new MNKCell(c.i + 1, c.j + 1);
                    mark(d, B);
                    return d;
                } else if (c.i - 1 >= 0 && c.j + 1 < B.N && B.cellState(c.i - 1, c.j + 1) == MNKCellState.FREE) {
                    MNKCell d = new MNKCell(c.i - 1, c.j + 1);
                    mark(d, B);
                    return d;
                } else if (c.i + 1 < B.M && c.j - 1 >= 0 && B.cellState(c.i + 1, c.j - 1) == MNKCellState.FREE) {
                    MNKCell d = new MNKCell(c.i + 1, c.j - 1);
                    mark(d, B);
                    return d;
                }
            }

            if (MC.length == 2) {
                MNKCell c = MC[0];

                if (B.cellState(c.i - 1, c.j - 1) == MNKCellState.FREE
                        && B.cellState(c.i + 1, c.j + 1) == MNKCellState.FREE) {
                    MNKCell d = new MNKCell(c.i - 1, c.j - 1);
                    mark(d, B);
                    timer.cancel();
                    return d;
                }

                if (B.cellState(c.i + 1, c.j + 1) == MNKCellState.FREE
                        && B.cellState(c.i - 1, c.j - 1) == MNKCellState.FREE) {
                    MNKCell d = new MNKCell(c.i + 1, c.j + 1);
                    mark(d, B);
                    timer.cancel();
                    return d;
                }

                if (B.cellState(c.i - 1, c.j + 1) == MNKCellState.FREE
                        && B.cellState(c.i + 1, c.j - 1) == MNKCellState.FREE) {
                    MNKCell d = new MNKCell(c.i - 1, c.j + 1);
                    mark(d, B);
                    timer.cancel();
                    return d;
                }

                if (B.cellState(c.i + 1, c.j - 1) == MNKCellState.FREE
                        && B.cellState(c.i - 1, c.j + 1) == MNKCellState.FREE) {
                    MNKCell d = new MNKCell(c.i + 1, c.j - 1);
                    mark(d, B);
                    timer.cancel();
                    return d;
                }
            }

            // controllo qui

            MNKCell c = iterativeDeepening(B);
            mark(c, B);
            
            if ((starting_hash ^ zobristHash(c, B)) != current_hash)
                throw new IllegalStateException("Error in hashing!");
            
            return c;
            
        }
    }

    @Override
    public String playerName() {
        return "AlphaBetaPro";
    }

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