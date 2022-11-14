package chess;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.color.Color;

public abstract class ChessPiece extends Piece {

	private Color color;
	
	public ChessPiece(Board board, Color color) {
		super(board);
		this.color = color;
	}
	
	public Color getColor() {
		return color;
	}
	
	public ChessPosition getChessPiece() {
		return ChessPosition.fromPosition(position);
	}
	
	protected boolean isThereOpponentPiece(Position position) {
		ChessPiece auxPiece = (ChessPiece) getBoard().piece(position);	
		return auxPiece != null && auxPiece.getColor() != color;
	}
}
