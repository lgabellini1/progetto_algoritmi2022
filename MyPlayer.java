/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mnkgame;

import java.util.Random;
import java.util.Arrays;

/**
 *
 * @author Andrea
 */
public class MyPlayer implements MNKPlayer {

    private Random rand;
    private MNKBoard B;
    private MNKGameState myWin;
    private MNKGameState yourWin;
    private int TIMEOUT;
    private long start;

    /**
     * Default empty constructor
     */
    public MyPlayer() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        B = new MNKBoard(M, N, K);
        myWin = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        yourWin = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
        TIMEOUT = timeout_in_secs;
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

    // FC = celle libere, MC = celle occupate
    private int alphaBeta(MNKBoard board, boolean playerA, int alpha, int beta) {
        MNKCell fc[] = board.getFreeCells();
        int eval;

        if (board.gameState != MNKGameState.OPEN || ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0))) {
            eval = eval(board);
        } else if (!playerA) {   // mi calcolo il valore del ramo
            eval = -2;
            for (MNKCell d : fc) {
                board.markCell(d.i, d.j);
                eval = Math.max(eval, alphaBeta(board, true, alpha, beta));
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
                eval = Math.min(eval, alphaBeta(board, false, alpha, beta));
                beta = Math.min(eval, beta);

                board.unmarkCell();
                if (beta <= alpha) {
                    break;
                }
            }
        }
        return eval;
    }

    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
        start = System.currentTimeMillis();

        // add to the local board the last opponent move
        if (MC.length > 0) {
            MNKCell c = MC[MC.length - 1]; // Recover the last move from MC
            B.markCell(c.i, c.j);         // Save the last move in the local MNKBoard
        }
        // If there is just one possible move, return immediately
        if (FC.length == 1) {
            return FC[0];
        }
        
        // scelgo la prima mossa del gioco casualmente (più veloce, ma meno precisa)
        if (myWin == MNKGameState.WINP1 && MC.length == 0) {
            MNKCell c = FC[rand.nextInt(FC.length)];
            B.markCell(c.i, c.j);
            return c;
        }

        double score, maxEval = -2;
        int pos = rand.nextInt(FC.length);
        MNKCell result = FC[pos]; // random move

        for (MNKCell currentCell : FC) {

            // If time is running out, return the randomly selected  cell
            if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0)) {
                break;

            } else {
                B.markCell(currentCell.i, currentCell.j);

                score = alphaBeta(B, true, -2, 2);

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
        return "MyPlayer";
    }
}
