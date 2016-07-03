package io.riddles.bookinggame.game.processor;

import io.riddles.bookinggame.game.data.MoveType;
import io.riddles.bookinggame.game.player.BookingGamePlayer;

import io.riddles.bookinggame.game.move.BookingGameMove;
import io.riddles.bookinggame.game.data.Coordinate;
import io.riddles.bookinggame.game.data.Board;
import io.riddles.javainterface.exception.InvalidInputException;

/**
 * Created by joost on 3-7-16.
 */
public class BookingGameLogic {


    public BookingGameLogic() {
    }

    public Board transformBoard(Board b, BookingGameMove move) throws InvalidInputException {

        BookingGamePlayer p = move.getPlayer();
        int pId = p.getId();
        Coordinate c = b.getPlayerCoordinate(pId);
        Coordinate newC = c;

        switch(move.getMoveType()) {
            case UP:
                if (b.isEmpty(new Coordinate(c.getX(), c.getY()-1))) {
                    newC = new Coordinate(c.getX(), c.getY()-1);
                }
                break;
            case DOWN:
                if (b.isEmpty(new Coordinate(c.getX(), c.getY()+1))) {
                    newC = new Coordinate(c.getX(), c.getY()+1);
                }
                break;
            case RIGHT:
                if (b.isEmpty(new Coordinate(c.getX()+1, c.getY()))) {
                    newC = new Coordinate(c.getX()+1, c.getY());
                }
                break;
            case LEFT:
                if (b.isEmpty(new Coordinate(c.getX()-1, c.getY()))) {
                    newC = new Coordinate(c.getX()-1, c.getY());
                }
                break;
            case WEAPON:
                if (b.isEmpty(new Coordinate(c.getX()-1, c.getY()))) {
                    newC = new Coordinate(c.getX()-1, c.getY());
                }
                break;
        }

        switch (b.getFieldAt(newC)) {
            case "B": /* A bug */
                p.updateSnippets(-3);
                break;
            case "C": /* Code snippet */
                p.updateSnippets(+1);
                break;
            case "W": /* A Weapon */
                p.updateWeapons(1);
                break;

        }
        b.setFieldAt(c, ".");
        b.setFieldAt(newC, String.valueOf(pId));

        return b;
    }
}
