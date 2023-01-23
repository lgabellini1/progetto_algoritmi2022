/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mnkgame;

import java.util.*;

/**
 *
 * @author Andrea
 */
public class MyPlayer1 implements MNKPlayer {

    private Random rand;
    private MNKBoard B;
    private MNKGameState myWin;
    private MNKGameState yourWin;
    private int TIMEOUT;
    private long start;
    private boolean amIFirstPlayer;

    /**
     * Default empty constructor
     */
    public MyPlayer1() {
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
        tTable  = new HashMap<>();
        amIFirstPlayer = false;
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
    
    public boolean inTable(int i, int j) {
        if((i >= 0 && i < B.M) && (j >= 0 && j < B.N)) {
            return true;
        }
        return false;
    }
    public List<MNKCell> nearMoves(MNKCell[] MC) {
        List<MNKCell> nearYourMoves = new ArrayList<>();
        int i;
        if(amIFirstPlayer) {
            i=1;
        } else {
            i=0;
        }
        for(; i<MC.length; i+=2) {
            
            MNKCell c = MC[i];
            if(inTable(c.i+1, c.j+1) && B.B[c.i+1][c.j+1] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i+1, c.j+1));
            }
            if(inTable(c.i+1, c.j) && B.B[c.i+1][c.j] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i+1, c.j));
            }
            if(inTable(c.i, c.j+1) && B.B[c.i][c.j+1] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i, c.j+1));
            }
            if(inTable(c.i-1, c.j-1) && B.B[c.i-1][c.j-1] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i-1, c.j-1));
            }
            if(inTable(c.i-1, c.j) && B.B[c.i-1][c.j] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i-1, c.j));
            }
            if(inTable(c.i, c.j-1) && B.B[c.i][c.j-1] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i, c.j-1));
            }
            if(inTable(c.i-1, c.j+1) && B.B[c.i-1][c.j+1] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i-1, c.j+1));
            }
            if(inTable(c.i+1, c.j-1) && B.B[c.i+1][c.j-1] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i+1, c.j-1));
            }
        }
        /*
        for(int k=0; k<nearYourMoves.size(); k++) {
            System.out.println(nearYourMoves.get(k));
        }*/
        return nearYourMoves;
    }

    // FC = celle libere, MC = celle occupate
    private double alphaBeta(MNKBoard board, boolean playerA, double alpha, double beta, int depth) {
        MNKCell fc[] = (MNKCell[]) nearMoves(board.getMarkedCells()).toArray();
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

                if (alpha >= beta) return ttEntry.val;
            }
        }

        if (depth == 0 || board.gameState != MNKGameState.OPEN || ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0))) {
            eval = eval(board);
        } else if (!playerA) {   // mi calcolo il valore del ramo
            eval = -2;
            for (MNKCell d : fc) {
                board.markCell(d.i, d.j);
                eval = Math.max(eval, alphaBeta(board, true, alpha, beta, depth-1));
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
                eval = Math.min(eval, alphaBeta(board, false, alpha, beta, depth-1));
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
            tTable.put(hash(ttEntry.board), ttEntry);
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
        
        if(MC.length == 0) {
            amIFirstPlayer = true;
        }

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