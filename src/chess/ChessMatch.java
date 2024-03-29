package chess;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.color.Color;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Queen;
import chess.pieces.Rook;

public class ChessMatch {

	private Board board;
	private int turn;
	private Color currentPlayer;
	private boolean check;
	private boolean checkMate;
	private ChessPiece enPassantVulnerable;
	private ChessPiece promoted;

	private List<Piece> piecesOnTheBoard = new ArrayList<>();
	private List<Piece> capturedPieces = new ArrayList<>();

	public ChessMatch() {
		board = new Board(8, 8);
		turn = 1;
		currentPlayer = Color.WHITE;
		initialSetup();
	}

	public int getTurn() {
		return turn;
	}

	public Color getCurrentPlayer() {
		return currentPlayer;
	}

	public boolean getCheck() {
		return check;
	}

	public boolean getCheckMate() {
		return checkMate;
	}

	public ChessPiece getEnPassantVulnerable() {
		return enPassantVulnerable;
	}

	public ChessPiece getPromoted() {
		return promoted;
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

	public boolean[][] possibleMoves(ChessPosition sourcePosition) {
		return board.piece(sourcePosition.toPosition()).possibleMoves();
	}

	public ChessPiece performChessMove(ChessPosition sourcePosition, ChessPosition targetPosition) {
		Position source = sourcePosition.toPosition();
		Position target = targetPosition.toPosition();
		validateSourcePosition(source);
		validateTargetPosition(source, target);
		Piece capturedPiece = makeMove(source, target);

		if (testCheck(currentPlayer)) {
			undoMove(source, target, capturedPiece);
			throw new ChessException("You can't put yourself in check!");
		}

		ChessPiece movedPiece = (ChessPiece) board.piece(target);

		// special move promotion
		promoted = null;
		if (movedPiece instanceof Pawn) {
			if ((movedPiece.getColor() == Color.WHITE && target.getRow() == 0)
					|| (movedPiece.getColor() == Color.BLACK && target.getRow() == 7)) {
				promoted = (ChessPiece) board.piece(target);
				promoted = replacePromotedPiece("Q");
			}
		}

		check = (testCheck(opponent(currentPlayer))) ? true : false;

		if (testCheckMate(opponent(currentPlayer))) {
			checkMate = true;
		} else {
			nextTurn();
		}

		if (movedPiece instanceof Pawn
				&& (target.getRow() == source.getRow() + 2 || target.getRow() == source.getRow() - 2)) {
			enPassantVulnerable = movedPiece;
		} else {
			enPassantVulnerable = null;
		}

		return (ChessPiece) capturedPiece;
	}

	public ChessPiece replacePromotedPiece(String type) {
		if (promoted == null) {
			throw new IllegalStateException("There is no piece to be promoted");
		}
		if (!type.equals("B") && !type.equals("N") && !type.equals("R") && !type.equals("Q")) {
			throw new ChessException("Invalid type for promotion");
		}

		Position pos = promoted.getChessPiece().toPosition();
		Piece p = board.removePiece(pos);
		piecesOnTheBoard.remove(p);
		
		ChessPiece newPiece = newPiece(type, promoted.getColor());
		board.placePiece(newPiece, pos);
		piecesOnTheBoard.add(newPiece);
		
		return newPiece;
	}

	private ChessPiece newPiece(String type, Color color) {
		if (type.equals("B")) {
			return new Bishop(board, color);
		}
		if (type.equals("N")) {
			return new Knight(board, color);
		}
		if (type.equals("R")) {
			return new Rook(board, color);
		}
		return new Queen(board, color);
	}

	private Piece makeMove(Position source, Position target) {
		ChessPiece p = (ChessPiece) board.removePiece(source);
		p.increaseMoveCount();
		Piece capturedPiece = board.removePiece(target);
		board.placePiece(p, target);

		if (capturedPiece != null) {
			piecesOnTheBoard.remove(capturedPiece);
			capturedPieces.add(capturedPiece);
		}

		// special move castiling kingside rook
		if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
			Position sourceRook = new Position(source.getRow(), source.getColumn() + 3);
			Position targetRook = new Position(source.getRow(), source.getColumn() + 1);
			ChessPiece rook = (ChessPiece) board.removePiece(sourceRook);
			board.placePiece(rook, targetRook);
			rook.increaseMoveCount();
		}

		// special move castling queenside rook
		if (p instanceof King && target.getColumn() == source.getColumn() - 2) {
			Position sourceRook = new Position(source.getRow(), source.getColumn() - 4);
			Position targetRook = new Position(source.getRow(), source.getColumn() - 1);
			ChessPiece rook = (ChessPiece) board.removePiece(sourceRook);
			board.placePiece(rook, targetRook);
			rook.increaseMoveCount();
		}

		// special move en passant
		if (p instanceof Pawn) {
			if (target.getColumn() != source.getColumn() && capturedPiece == null) {
				Position pawnPosition;
				if (p.getColor() == Color.WHITE) {
					pawnPosition = new Position(target.getRow() + 1, target.getColumn());
				} else {
					pawnPosition = new Position(target.getRow() - 1, target.getColumn());
				}
				capturedPiece = board.removePiece(pawnPosition);
				piecesOnTheBoard.remove(capturedPiece);
				capturedPieces.add(capturedPiece);
			}
		}

		return capturedPiece;
	}

	private void undoMove(Position source, Position target, Piece capturedPiece) {
		ChessPiece p = (ChessPiece) board.removePiece(target);
		p.decreaseMoveCount();
		board.placePiece(p, source);

		if (capturedPiece != null) {
			board.placePiece(capturedPiece, target);
			piecesOnTheBoard.add(capturedPiece);
			capturedPieces.remove(capturedPiece);
		}

		// undo specialmove castiling kingside rook
		if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
			Position sourceRook = new Position(source.getRow(), source.getColumn() + 3);
			Position targetRook = new Position(source.getRow(), source.getColumn() + 1);
			ChessPiece rook = (ChessPiece) board.removePiece(targetRook);
			board.placePiece(rook, sourceRook);
			rook.decreaseMoveCount();
		}

		// undo specialmove castiling queenside rook
		if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
			Position sourceRook = new Position(source.getRow(), source.getColumn() - 4);
			Position targetRook = new Position(source.getRow(), source.getColumn() - 1);
			ChessPiece rook = (ChessPiece) board.removePiece(targetRook);
			board.placePiece(rook, sourceRook);
			rook.decreaseMoveCount();
		}

		// special move en passant
		if (p instanceof Pawn) {
			if (target.getColumn() != source.getColumn() && capturedPiece == enPassantVulnerable) {
				ChessPiece pawn = (ChessPiece) board.removePiece(target);
				Position pawnPosition;
				if (p.getColor() == Color.WHITE) {
					pawnPosition = new Position(3, target.getColumn());
				} else {
					pawnPosition = new Position(4, target.getColumn());
				}
				board.placePiece(pawn, pawnPosition);
			}
		}
	}

	private boolean hasEnemyPiece(Position p) {
		if (board.piece(p) == null) {
			throw new ChessException("Error reading chess position. Valid values are frfom a1 to h8.");
		}
		if (currentPlayer != (((ChessPiece) board.piece(p)).getColor())) {
			return true;
		} else {
			return false;
		}
	}

	public void verifyHasEnemyPiece(ChessPosition source) {
		if (hasEnemyPiece(source.toPosition())) {
			throw new ChessException("The chosen piece is not yours");
		}
	}

	private void validateSourcePosition(Position source) {
		if (!board.thereIsAPiece(source)) {
			throw new ChessException("There is no piece on source position.");
		} else if (hasEnemyPiece(source)) {
			throw new ChessException("The chosen piece is not yours");
		} else if (!board.piece(source).isThereAnyPossibleMove()) {
			throw new ChessException("There is no possible moves for the chosen piece!");
		}
	}

	private void validateTargetPosition(Position source, Position target) {
		if (!board.piece(source).possibleMove(target)) {
			throw new ChessException("The chosen piece can't move to target position.");
		}
	}

	private void placeNewPiece(ChessPosition chessPosition, ChessPiece piece) {
		board.placePiece(piece, chessPosition.toPosition());
		piecesOnTheBoard.add(piece);
	}

	private void nextTurn() {
		turn++;
		currentPlayer = (currentPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
	}

	private Color opponent(Color color) {
		return (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
	}

	private ChessPiece king(Color color) {
		List<Piece> pieces = piecesOnTheBoard.stream().filter(p -> ((ChessPiece) p).getColor() == color)
				.collect(Collectors.toList());

		for (Piece p : pieces) {
			if (p instanceof King) {
				return (ChessPiece) p;
			}
		}
		throw new IllegalStateException("There is no " + color + " king on the board");
	}

	public boolean testCheck(Color color) {
		Position kingPosition = king(color).getChessPiece().toPosition();
		List<Piece> opponentPieces = piecesOnTheBoard.stream()
				.filter(p -> ((ChessPiece) p).getColor() == opponent(color)).collect(Collectors.toList());

		for (Piece p : opponentPieces) {
			if (p.possibleMove(kingPosition)) {
				return true;
			}
		}
		return false;
	}

	public boolean testCheckMate(Color color) {
		if (!testCheck(color)) {
			return false;
		}

		return piecesOnTheBoard.stream().filter(p -> ((ChessPiece) p).getColor() == color).map(p -> {
			boolean[][] mat = p.possibleMoves();
			for (int i = 0; i < board.getRows(); i++) {
				for (int j = 0; j < board.getColumns(); j++) {
					if (mat[i][j]) {
						Position source = ((ChessPiece) p).getChessPiece().toPosition();
						Position target = new Position(i, j);
						Piece capturedPiece = makeMove(source, target);
						boolean testCheck = testCheck(color);
						undoMove(source, target, capturedPiece);
						if (!testCheck) {
							return false;
						}
					}
				}
			}
			return true;
		}).findFirst().get();
	}

	public void initialSetup() {
		placeNewPiece(new ChessPosition('a', 2), new Pawn(board, Color.WHITE, this));
		placeNewPiece(new ChessPosition('b', 2), new Pawn(board, Color.WHITE, this));
		placeNewPiece(new ChessPosition('c', 2), new Pawn(board, Color.WHITE, this));
		placeNewPiece(new ChessPosition('d', 2), new Pawn(board, Color.WHITE, this));
		placeNewPiece(new ChessPosition('e', 2), new Pawn(board, Color.WHITE, this));
		placeNewPiece(new ChessPosition('f', 2), new Pawn(board, Color.WHITE, this));
		placeNewPiece(new ChessPosition('g', 2), new Pawn(board, Color.WHITE, this));
		placeNewPiece(new ChessPosition('h', 2), new Pawn(board, Color.WHITE, this));
		placeNewPiece(new ChessPosition('h', 1), new Rook(board, Color.WHITE));
		placeNewPiece(new ChessPosition('a', 1), new Rook(board, Color.WHITE));
		placeNewPiece(new ChessPosition('d', 1), new Queen(board, Color.WHITE));
		placeNewPiece(new ChessPosition('e', 1), new King(board, Color.WHITE, this));
		placeNewPiece(new ChessPosition('c', 1), new Bishop(board, Color.WHITE));
		placeNewPiece(new ChessPosition('f', 1), new Bishop(board, Color.WHITE));
		placeNewPiece(new ChessPosition('b', 1), new Knight(board, Color.WHITE));
		placeNewPiece(new ChessPosition('g', 1), new Knight(board, Color.WHITE));

		placeNewPiece(new ChessPosition('a', 7), new Pawn(board, Color.BLACK, this));
		placeNewPiece(new ChessPosition('b', 7), new Pawn(board, Color.BLACK, this));
		placeNewPiece(new ChessPosition('c', 7), new Pawn(board, Color.BLACK, this));
		placeNewPiece(new ChessPosition('d', 7), new Pawn(board, Color.BLACK, this));
		placeNewPiece(new ChessPosition('e', 7), new Pawn(board, Color.BLACK, this));
		placeNewPiece(new ChessPosition('f', 7), new Pawn(board, Color.BLACK, this));
		placeNewPiece(new ChessPosition('g', 7), new Pawn(board, Color.BLACK, this));
		placeNewPiece(new ChessPosition('h', 7), new Pawn(board, Color.BLACK, this));
		placeNewPiece(new ChessPosition('h', 8), new Rook(board, Color.BLACK));
		placeNewPiece(new ChessPosition('a', 8), new Rook(board, Color.BLACK));
		placeNewPiece(new ChessPosition('e', 8), new King(board, Color.BLACK, this));
		placeNewPiece(new ChessPosition('d', 8), new Queen(board, Color.BLACK));
		placeNewPiece(new ChessPosition('c', 8), new Bishop(board, Color.BLACK));
		placeNewPiece(new ChessPosition('f', 8), new Bishop(board, Color.BLACK));
		placeNewPiece(new ChessPosition('b', 8), new Knight(board, Color.BLACK));
		placeNewPiece(new ChessPosition('g', 8), new Knight(board, Color.BLACK));
	}
}
