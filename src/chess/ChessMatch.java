package chess;

import boardgame.Board;
import boardgame.Position;
import chess.color.Color;
import chess.pieces.King;
import chess.pieces.Rook;

public class ChessMatch {
	
	private Board board;
	
	public ChessMatch() {
		board = new Board(8, 8);
		initialSetup();
	}

	public ChessPiece[][] getPieces() {
		ChessPiece[][] mat = new ChessPiece[board.getRows()][board.getColumns()];
		for (int i = 0; i < board.getRows(); i++) {
			for (int j = 0; j < board.getColumns(); j++) {
				mat[i][j] = (ChessPiece) board.piece(i, j);
			}
		}
		return mat;
	}
	
	private void placeNewPiece(ChessPosition chessPosition, ChessPiece piece) {
		board.placePiece(piece, chessPosition.toPosition());
	}
	
	public void initialSetup() {
		placeNewPiece(new ChessPosition('a', 8), new Rook(board, Color.BLACK));
		placeNewPiece(new ChessPosition('a', 8), new Rook(board, Color.BLACK));
		placeNewPiece(new ChessPosition('a', 8), new King(board, Color.BLACK));
	}
}