/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mnkgame;

import java.util.Random;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author Andrea
 */
public class MyPlayer2 implements MNKPlayer {

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

    /**
     * Default empty constructor
     */
    public MyPlayer2() {
    }
    
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
        tTable  = new HashMap<>();
        for (int i = 0; i < M * N; i++) {
            zobristTable[i][0] = rand.nextLong();
            zobristTable[i][1] = rand.nextLong();
        }
    }

    private int cellIndex(MNKCell c) {
        return c.i * B.N + c.j;
    }

    private long zobristHash(MNKCell c, MNKBoard B) {
        if (B.cellState(c.i, c.j) == MNKCellState.FREE)
            throw new IllegalStateException("Hashing of free cell requested.");
        return B.cellState(c.i, c.j) == MNKCellState.P1 ? zobristTable[cellIndex(c)][0] : zobristTable[cellIndex(c)][1];
    }

    private int eval(MNKBoard board) {
        if (board.gameState == myWin) {
            return 1;
        } else if (board.gameState == yourWin) {
            return -1;
        } else if (board.gameState == MNKGameState.DRAW) {
            return 0;
        } else {
            return 0;   // non sono arrivato ad una foglia
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
        } return hashcode;
    }

    // FC = celle libere, MC = celle occupate
    private double alphaBeta(MNKBoard board, boolean playerA, double alpha, double beta, int depth) {
        MNKCell fc[] = board.getFreeCells();
        double eval;
        double alphaOrig = alpha, betaOrig = beta;
        
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

        if (depth == 0 || board.gameState != MNKGameState.OPEN || ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0))) {
            eval = eval(board);
        } else if (!playerA) {   // mi calcolo il valore del ramo
            eval = -2;
            for (MNKCell d : fc) {
                board.markCell(d.i, d.j);
                current_hash = current_hash ^ zobristHash(d, B);
                eval = Math.max(eval, alphaBeta(board, true, alpha, beta, depth-1));
                alpha = Math.max(eval, alpha);

                current_hash = current_hash ^ zobristHash(d, B);
                board.unmarkCell();
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            eval = 2;
            for (MNKCell d : fc) {
                board.markCell(d.i, d.j);
                current_hash = current_hash ^ zobristHash(d, B);
                eval = Math.min(eval, alphaBeta(board, false, alpha, beta, depth-1));
                beta = Math.min(eval, beta);

                current_hash = current_hash ^ zobristHash(d, B);
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
    
    public double MTD(MNKBoard board, double f, int depth) {
        double g = f;
        double upperbound = 2;
        double lowerbound = -2;
        double beta;
        do {
            if(g == lowerbound) {
                beta = g+1;
            } else {
                beta = g;
            }
            //since alpha is always one less than beta (da http://people.csail.mit.edu/plaat/mtdf.html#abmem )
            g = alphaBeta(B, true, beta-1, beta, depth);
            if(g < beta) {
                upperbound = g;
            } else {
                lowerbound = g;
            }
        } while(lowerbound >= upperbound);
        
        return g;
    }
    
    public double MTDF(MNKBoard board, MNKCell[] FC) {
        double firstguess = 0;
        for(int d=1; d<FC.length; d++) {
            firstguess = MTD(board, firstguess, d);
            // If time is running out, return the randomly selected  cell
            if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (90.0 / 100.0)) {
                break;
            }
        }
        return firstguess;
    }

    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
        start = System.currentTimeMillis();
        int pos = rand.nextInt(FC.length);
        long starting_hash = 0;

        // add to the local board the last opponent move
        if (MC.length > 0) {
            MNKCell c = MC[MC.length - 1]; // Recover the last move from MC
            B.markCell(c.i, c.j);         // Save the last move in the local MNKBoard3
            starting_hash = current_hash;
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
        
        //System.out.println("celle libere: " + FC.length);
        for (MNKCell currentCell : FC) {

            // If time is running out, return the randomly selected  cell
            if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0)) {
                break;

            } else {
                B.markCell(currentCell.i, currentCell.j);
                
                //score = alphaBeta(B, true, -2, 2, FC.length);
                score = iterativeDeepeing(B, true, FC.length);
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

        B.markCell(result.i, result.j);

        return result;
    }

    public String playerName() {
        return "MyPlayer1";
    }
}