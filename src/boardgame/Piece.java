package boardgame;

public abstract class Piece {
	
	protected Position position;
	private Board board;
	
	public Piece(Board board) {
		this.board = board;
	}
	
	protected Board getBoard() {
		return board;
	}
	
	public abstract boolean[][] possibleMoves();
	
	public boolean possibleMove(Position position) {
		return possibleMoves()[position.getRow()][position.getColumn()];
	}
	
	public boolean isThereAnyPossibleMove() {
		for (int i = 0; i < possibleMoves().length; i++) {
			for (int k = 0; k < possibleMoves().length; k++) {
				if (possibleMoves()[i][k]) {
					return true;
				}
			}
		}
		return false;
	}
}
