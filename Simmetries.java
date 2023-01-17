package mnkgame;

import java.util.*;

class Simmetries implements MNKPlayer {
    private MNKBoard[] B;
    private int M,N,K;
    private Random rand;
    private boolean first;
    private MNKGameState myWin, yourWin;
    private long start;
    private long[] current_hashes;
    private long[][] zobristTable;
    private HashMap<Long, MNKBoardPlus> tTable;
    private StrategySet max, min;
    private int TIMEOUT;

    /* Debug */
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    private static class MNKBoardPlus {
        public enum Flag {
            EXACT, LOWERBOUND, UPPERBOUND
        } private Flag flag;
        private double eval;
        private int depth;

        public MNKBoardPlus(Flag f, double val, int d) {
            flag   = f;
            eval   = val;
            depth  = d;
        }
    }

    private static void print(MNKBoard B) {
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

    @Override
    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        B = new MNKBoard[8];
        for (int i=0; i<8; i++)
            B[i] = new MNKBoard(M,N,K);
        this.M       = M;
        this.N       = N;
        this.K       = K;
        rand         = new Random(System.currentTimeMillis());
        this.first   = first;
        myWin        = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        yourWin      = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
        current_hashes = new long[8];
        for (int i=0; i<8; i++)
            current_hashes[i] = 0;
        zobristTable = new long[M*N][2];
        tTable       = new HashMap<>();
        max          = new StrategySet(B[0], first ? MNKCellState.P1 : MNKCellState.P2);
        min          = new StrategySet(B[0], first ? MNKCellState.P2 : MNKCellState.P1);
        TIMEOUT      = timeout_in_secs;

        for (int i = 0; i < M*N; i++) {
            zobristTable[i][0] = rand.nextLong();
            zobristTable[i][1] = rand.nextLong();
        }
    }

    /**
     *  Complessità: O(1)
     *  @param c cella marcata
     *  @return la medesima cella c, vista rispetto a una riflessione
     *          della MNKBoard rispetto all'asse verticale
     */
    private MNKCell y_reflection(MNKCell c) {
        if (c==null) return null;
        return new MNKCell(c.i,(N-1) - c.j, c.state);
    }

    /**
     *  Complessità: O(1)
     *  @param c cella marcata
     *  @return la medesima cella c, vista rispetto a una riflessione
     *          della MNKBoard rispetto all'asse orizzontale
     */
    private MNKCell x_reflection(MNKCell c) {
        if (c==null) return null;
        return new MNKCell((M-1) - c.i,c.j, c.state);
    }

    /**
     * Complessità: O(1)
     * @param c cella marcata
     * @param B MNKBoard originale di gioco
     * @return la medesima cella c invertita (ovvero come se fosse
     *         marcata dall'altro giocatore)
     */
    private MNKCell invert(MNKCell c, MNKBoard B) {
        if (c==null) return null;
        if (B.cellState(c.i,c.j)==MNKCellState.FREE)
            throw new IllegalStateException("Cell should not be FREE!");
        return B.cellState(c.i,c.j)==MNKCellState.P1 ? new MNKCell(c.i,c.j,MNKCellState.P2) :
                new MNKCell(c.i,c.j,MNKCellState.P1);
    }

    private void hash_update(MNKCell c) {
        current_hashes[0] = current_hashes[0] ^ zobristHash(c, B[0]);

        current_hashes[1] = current_hashes[1] ^ zobristHash(y_reflection(c), B[1]);
        current_hashes[2] = current_hashes[2] ^ zobristHash(x_reflection(c), B[2]);
        current_hashes[3] = current_hashes[3] ^ zobristHash(x_reflection(y_reflection(c)), B[3]);

        current_hashes[4] = current_hashes[4] ^ zobristHash(invert(c,B[0]),B[4]);
        current_hashes[5] = current_hashes[5] ^ zobristHash(invert(y_reflection(c), B[1]),B[5]);
        current_hashes[6] = current_hashes[6] ^ zobristHash(invert(x_reflection(c), B[2]),B[6]);
        current_hashes[7] = current_hashes[7] ^ zobristHash(invert(x_reflection(y_reflection(c)),B[3]),B[7]);
    }

    private MNKBoardPlus ttTableRetrieval() {
        MNKBoardPlus ttEntry = null; int i=0;
        while (ttEntry==null && i<8)
            ttEntry = tTable.get(current_hashes[i++]);
        return ttEntry;
    }

    private void mark(MNKBoard[] B, MNKCell c) {
        B[0].markCell(c.i, c.j);

        B[1].markCell( y_reflection(c).i,
                       y_reflection(c).j);

        B[2].markCell( x_reflection(c).i,
                       x_reflection(c).j);

        B[3].markCell( x_reflection(y_reflection(c)).i,
                       x_reflection(y_reflection(c)).j);

        B[4].markCell( invert(c, B[0]).i,
                       invert(c, B[0]).j);

        B[5].markCell( invert(y_reflection(c), B[1]).i,
                       invert(y_reflection(c), B[1]).j);

        B[6].markCell( invert(x_reflection(c), B[2]).i,
                       invert(x_reflection(c), B[2]).j);

        B[7].markCell( invert(x_reflection(y_reflection(c)), B[3]).i,
                       invert(x_reflection(y_reflection(c)), B[3]).j);
    }

    private void unmark(MNKBoard[] B) {
        for (MNKBoard b : B)
            b.unmarkCell();
    }

    private boolean myTurn(MNKBoard B) {
        return first ? (B.currentPlayer() == 0) : (B.currentPlayer() == 1);
    }

    private int cellIndex(MNKCell c) {
        return c.i * N + c.j;
    }

    private long zobristHash(MNKCell c, MNKBoard B) {
        if (B.cellState(c.i,c.j) == MNKCellState.FREE) {
            IllegalStateException e = new IllegalStateException("Hashing of free cell requested!");
            e.printStackTrace();
            throw e;
        } return B.cellState(c.i,c.j) == MNKCellState.P1 ? zobristTable[cellIndex(c)][0] : zobristTable[cellIndex(c)][1];
    }

    private double eval(MNKBoard B) {
        if (B.gameState() == myWin)
            return +1;
        else if (B.gameState() == yourWin)
            return -1;
        else if (B.gameState() == MNKGameState.DRAW)
            return  0;
        else {
            double eval = 0;
            if (myTurn(B)) {
                if (max.winning()>=1) return +1;
                if (min.winning()>=2) return -1;
                else if (min.winning()==1)
                    eval -= 0.2;
                IntersectionSet maxISet = max.generateFrom(B);
                if (maxISet != null) {
                    if (maxISet.winning())
                        return +1;
                    else if (maxISet.size() >= 1) {
                        eval += 0.5;
                        if (maxISet.max() >= 3)
                            eval += 0.2;
                    }
                }
            } else {
                if (min.winning()>=1) return -1;
                if (max.winning()>=2) return +1;
                else if (max.winning()==1)
                    eval += 0.2;
                IntersectionSet minISet = min.generateFrom(B);
                if (minISet != null) {
                    if (minISet.winning())
                        return -1;
                    else if (minISet.size() >= 1) {
                        eval -= 0.5;
                        if (minISet.max() >= 3)
                            eval -= 0.2;
                    }
                }
            }

            if (max.size()>min.size())
                eval += 0.3;
            else if (min.size()> max.size())
                eval -= 0.3;
            return eval;
        }
    }

    private double alphaBeta(MNKBoard[] B, boolean max_player, double alpha, double beta, int depth) {
        /* Transposition table lookup */
        MNKBoardPlus ttEntry = ttTableRetrieval();
        if (ttEntry != null && ttEntry.depth >= depth) {
            switch(ttEntry.flag) {
                case EXACT -> { return ttEntry.eval; }
                case LOWERBOUND -> alpha = Math.max(alpha, ttEntry.eval);
                case UPPERBOUND -> beta  = Math.min(beta, ttEntry.eval);
            } if (alpha >= beta)
                return ttEntry.eval;
        }

        double eval; MNKCell[] FC = B[0].getFreeCells();
        double alphaOrig = alpha, betaOrig = beta;
        if (depth == 0 || B[0].gameState() != MNKGameState.OPEN)
            eval = eval(B[0]);
        else if (max_player) {
            eval = -999;
            for (MNKCell c : FC) {
                if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0))
                    break;

                //MNKStrategy[] maxA = max.set(), minA = min.set();

                mark(B,c);
                //System.out.println(ANSI_RED + "Marked " + new MNKCell(c.i,c.j,B.cellState(c.i,c.j)) + ANSI_RESET);
                hash_update(c);
                max.update(c,B[0]); min.update(c,B[0]);
                //max.test_print(); min.test_print();

                eval = Math.max(eval, alphaBeta(B, false, alpha, beta, depth - 1));

                max.undo(c,B[0]); min.undo(c,B[0]);
                hash_update(c);
                //if (!(Arrays.equals(maxA, max.set()))) throw new RuntimeException("Mismatched max");
                //if (!(Arrays.equals(minA, min.set()))) throw new RuntimeException("Mismatched min");

                //System.out.println(ANSI_RED + "Unmarked " + new MNKCell(c.i,c.j,B.cellState(c.i,c.j)) + ANSI_RESET);
                unmark(B);
                //max.test_print(); min.test_print();

                alpha = Math.max(alpha, eval);

                if (alpha >= beta)
                    break;
            }
        } else {
            eval = +999;
            for (MNKCell c : FC) {
                if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0))
                    break;

                //MNKStrategy[] maxA = max.set(), minA = min.set();

                mark(B,c);
                //System.out.println(ANSI_RED + "Marked " + new MNKCell(c.i,c.j,B.cellState(c.i,c.j)) + ANSI_RESET);
                hash_update(c);
                max.update(c,B[0]); min.update(c,B[0]);
                //max.test_print(); min.test_print();
                eval = Math.min(eval, alphaBeta(B, true, alpha, beta, depth - 1));

                max.undo(c,B[0]); min.undo(c,B[0]);
                hash_update(c);
                //if (!(Arrays.equals(maxA, max.set()))) throw new RuntimeException("Mismatched max");
                //if (!(Arrays.equals(minA, min.set()))) throw new RuntimeException("Mismatched min");

                //System.out.println(ANSI_RED + "Unmarked " + new MNKCell(c.i,c.j,B.cellState(c.i,c.j)) + ANSI_RESET);
                unmark(B);
                //max.test_print(); min.test_print();

                beta = Math.min(beta, eval);

                if (alpha >= beta)
                    break;
            }
        }

        /* Transposition table store */
        for (long hash : current_hashes) {
            if (eval <= alphaOrig)
                tTable.put(hash, new MNKBoardPlus(MNKBoardPlus.Flag.UPPERBOUND, eval, depth));
            else if (eval >= betaOrig)
                tTable.put(hash, new MNKBoardPlus(MNKBoardPlus.Flag.LOWERBOUND, eval, depth));
            else
                tTable.put(hash, new MNKBoardPlus(MNKBoardPlus.Flag.EXACT,      eval, depth));
        }

        return eval;
    }

    private MNKCell parentAlphaBeta(MNKBoard[] B) {
        MNKCell[] FC = B[0].getFreeCells();
        double eval, best_eval = -999;
        MNKCell selected = FC[rand.nextInt(FC.length)];

        for (MNKCell c : FC) {
            //MNKStrategy[] maxA = max.set(), minA = min.set();

            mark(B,c);
            //System.out.println(ANSI_RED + "Marked " + new MNKCell(c.i,c.j,B.cellState(c.i,c.j)) + ANSI_RESET);
            hash_update(c);
            max.update(c, B[0]);
            min.update(c, B[0]);
            //max.test_print(); min.test_print();

            eval = alphaBeta(B, false, -1, 1, FC.length - 1);
            if (eval > best_eval) {
                best_eval = eval;
                selected = c;
            }

            hash_update(c);
            max.undo(c, B[0]);
            min.undo(c, B[0]);
            //if (!(Arrays.equals(maxA, max.set()))) throw new RuntimeException("Mismatched max");
            //if (!(Arrays.equals(minA, min.set()))) throw new RuntimeException("Mismatched min");

            //System.out.println(ANSI_RED + "Unmarked " + new MNKCell(c.i,c.j,B.cellState(c.i,c.j)) + ANSI_RESET);
            unmark(B);
            //max.test_print(); min.test_print();

        } return selected;
    }

    private MNKCell iterativeDeepening(MNKBoard[] B) {
        MNKCell[] FC = B[0].getFreeCells();
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

        if (MC.length > 0) {
            MNKCell c = MC[MC.length - 1];
            mark(B,c);
            //System.out.println(ANSI_RED + "Marked " + new MNKCell(c.i,c.j,B.cellState(c.i,c.j)) + ANSI_RESET);
            hash_update(c);
            max.update(c,B[0]); min.update(c,B[0]);
            //max.test_print(); min.test_print();
            starting_hash = current_hashes[0];
        }

        if (FC.length == 1)
            return FC[0];

        if (MC.length == 0) {
            MNKCell c = new MNKCell(M / 2, N / 2);
            mark(B,c);
            //System.out.println(ANSI_RED + "Marked " + new MNKCell(c.i,c.j,B.cellState(c.i,c.j)) + ANSI_RESET);
            hash_update(c);
            max.update(c,B[0]); min.update(c,B[0]);
            //max.test_print(); min.test_print();
            return c;
        }

        MNKCell c = iterativeDeepening(B);
        mark(B,c);
        //System.out.println(ANSI_RED + "Marked " + new MNKCell(c.i,c.j,B.cellState(c.i,c.j)) + ANSI_RESET);
        hash_update(c);
        max.update(c,B[0]); min.update(c,B[0]);
        if ((starting_hash ^ zobristHash(c,B[0])) != current_hashes[0])
            throw new IllegalStateException("Error in hashing!");
        return c;
    }

    @Override
    public String playerName() {
        return "AlphaBetaPro";
    }
}