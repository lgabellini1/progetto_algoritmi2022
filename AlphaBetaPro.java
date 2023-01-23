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

    private enum Flag { EXACT, LOWERBOUND, UPPERBOUND }
    private record MNKBoardPlus(Flag flag, double eval, int depth) {}

    public AlphaBetaPro() {}

    @Override
    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        B            = new MNKBoard(M, N, K);
        rand         = new Random(System.currentTimeMillis());
        this.first   = first;
        myWin        = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        yourWin      = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
        zobristTable = new long[M*N][2];
        tTable       = new HashMap<>();
        Q            = new MovesQueue (B, first ? MNKCellState.P1 : MNKCellState.P2);
        max          = new StrategySet(B, first ? MNKCellState.P1 : MNKCellState.P2);
        min          = new StrategySet(B, first ? MNKCellState.P2 : MNKCellState.P1);
        TIMEOUT      = timeout_in_secs;

        for (int i = 0; i < M*N; i++) {
            zobristTable[i][0] = rand.nextLong();
            zobristTable[i][1] = rand.nextLong();
        }
    }

    /**
     *  Complessità: O(1)
     *  @param B configurazione attuale di gioco
     *  @return True se è il turno di questo giocatore; False altrimenti
     */
    private boolean myTurn(MNKBoard B) {
        return first ? (B.currentPlayer()==0) : (B.currentPlayer()==1);
    }

    private int cellIndex(MNKCell c) {
        return c.i * B.N + c.j;
    }

    /**
     *  Complessità: O(1)
     *  @param c cella di cui si vuole conoscere il valore hash
     *  @param B configurazione attuale di gioco
     *  @return hash di c
     */
    private long zobristHash(MNKCell c, MNKBoard B) {
        if (B.cellState(c.i,c.j) == MNKCellState.FREE)
            throw new IllegalStateException("Hashing of free cell requested.");
        return B.cellState(c.i,c.j) == MNKCellState.P1 ? zobristTable[cellIndex(c)][0] : zobristTable[cellIndex(c)][1];
    }

    private void mark(MNKCell c, MNKBoard B) {
        B.markCell(c.i, c.j);
        Q.remove(c,B);
        current_hash = current_hash ^ zobristHash(c,B);
        max.update(c,B,Q);
        min.update(c,B,Q);

        // Debug messages
        /*
        System.out.println("Marked [" + c.i + "," + c.j + "]");
        print(B);
        max.print();
        min.print();
        Q.printQueue();
        System.out.print('\n');
        System.out.println("Marked [" + c.i + "," + c.j + "]");
        print(B);
        Q.printQueue();

         */
    }

    private void unmark(MNKCell c, MNKBoard B) {
        Q.undo(B,c);
        max.undo(c,B,Q);
        min.undo(c,B,Q);
        current_hash = current_hash ^ zobristHash(c,B);
        B.unmarkCell();

        // Debug messages
        /*
        System.out.println("Unmarked [" + c.i + "," + c.j + "]");
        print(B);
        max.print();
        min.print();
        Q.printQueue();
        System.out.print('\n');
        System.out.println("Unmarked [" + c.i + "," + c.j + "]");
        print(B);
        Q.printQueue();
         */
    }

    private double eval(MNKBoard B) {
        if (B.gameState() == myWin)
            return +1;
        else if (B.gameState() == yourWin)
            return -1;
        else if (B.gameState() == MNKGameState.DRAW)
            return  0;
        else { // TODO: ampliare il criterio di valutazione
            double eval = 0;
            if (myTurn(B)) {
                if (max.winning()>=1) return +1;
                if (min.winning()>=2) return -1;
                else if (min.winning()==1)
                    eval -= 0.2;

                if (max.intersections.winning(B))
                    return +1;
            } else {
                if (min.winning()>=1) return -1;
                if (max.winning()>=2) return +1;
                else if (max.winning()==1)
                    eval += 0.2;

                if (min.intersections.winning(B))
                    return -1;
            }

            if (max.size()>min.size())
                eval += 0.3;
            else if (min.size()> max.size())
                eval -= 0.3;
            return eval;
        }
    }

    private double alphaBeta(MNKBoard B, boolean max_player, double alpha, double beta, int depth) {
        // Transposition table lookup
        MNKBoardPlus ttEntry = tTable.get(current_hash);
        if (ttEntry != null && ttEntry.depth >= depth) {
            switch(ttEntry.flag) {
                case EXACT -> { return ttEntry.eval; }
                case LOWERBOUND -> alpha = Math.max(alpha, ttEntry.eval);
                case UPPERBOUND -> beta  = Math.min(beta,  ttEntry.eval);
            } if (alpha >= beta) return ttEntry.eval;
        }

        double eval;
        double alphaOrig = alpha, betaOrig = beta;

        if (depth == 0 || B.gameState() != MNKGameState.OPEN)
            eval = eval(B);
        else if (max_player) {
            eval = -999;
            MNKCell[] FC = B.getFreeCells(), queue_moves = Q.moves();

            for (MNKCell c : queue_moves!=null ? queue_moves : FC) {
                if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0))
                    break;

                mark(c,B);
                eval = Math.max(eval, alphaBeta(B, false, alpha, beta, depth - 1));
                unmark(c,B);

                alpha = Math.max(alpha, eval);

                if (alpha >= beta)
                    break;
            }
        } else {
            eval = +999;
            MNKCell[] FC = B.getFreeCells();

            for (MNKCell c : FC) {
                if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0))
                    break;

                mark(c,B);
                eval = Math.min(eval, alphaBeta(B, true, alpha, beta, depth - 1));
                unmark(c,B);

                beta = Math.min(beta, eval);

                if (alpha >= beta)
                    break;
            }
        }

        // Transposition table store
        if (eval <= alphaOrig)
            tTable.put(current_hash, new MNKBoardPlus(Flag.UPPERBOUND, eval, depth));
        else if (eval >= betaOrig)
            tTable.put(current_hash, new MNKBoardPlus(Flag.LOWERBOUND, eval, depth));
        else
            tTable.put(current_hash, new MNKBoardPlus(Flag.EXACT,      eval, depth));

        return eval;
    }

    private MNKCell parentAlphaBeta(MNKBoard B) {
        double eval, best_eval = -999;
        MNKCell[] FC = B.getFreeCells(), queue_moves = Q.moves();
        MNKCell selected = FC[rand.nextInt(FC.length)];

        for (MNKCell c : queue_moves!=null ? queue_moves : FC) {
            mark(c,B);
            eval = alphaBeta(B, false, -1, 1, FC.length - 1);

            if (eval > best_eval) {
                best_eval = eval;
                selected = c;
            }

            unmark(c,B);
        }

        return selected;
    }

    private MNKCell iterativeDeepening(MNKBoard B) {
        MNKCell[] FC = B.getFreeCells();
        int MAX_DEPTH = FC.length; MNKCell c = FC[rand.nextInt(FC.length)];
        for (int d = 0; d < MAX_DEPTH; d++) {
            c = parentAlphaBeta(B);

            if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0))
                break;
        } return c;
    }

    @Override
    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
        start = System.currentTimeMillis();
        long starting_hash = 0;

        // Retrieve adversary move
        if (MC.length > 0) {
            MNKCell c = MC[MC.length - 1];
            mark(c,B);
            starting_hash = current_hash;
        }

        // One-move win/lose check
        if (max.winning()>=1 || min.winning()>=1) {
            MNKCell c = max.winning()>=1 ? max.winningCell(B) : min.winningCell(B);
            mark(c,B);
            return c;
        }

        if (FC.length == 1)
            return FC[0];

        // Starting move: play in the middle of the board
        if (MC.length == 0) {
            MNKCell c = new MNKCell(B.M / 2, B.N / 2);
            mark(c,B);
            return c;
        }

        MNKCell c = iterativeDeepening(B);
        mark(c,B);
        if ((starting_hash ^ zobristHash(c,B)) != current_hash)
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
            } System.out.println();
        }
    }
}
