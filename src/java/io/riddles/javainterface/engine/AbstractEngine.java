/*
 * Copyright 2016 riddles.io (developers@riddles.io)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 *     For the full copyright and license information, please view the LICENSE
 *     file that was distributed with this source code.
 */

package io.riddles.javainterface.engine;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.riddles.javainterface.game.player.AbstractPlayer;
import io.riddles.javainterface.game.processor.AbstractProcessor;
import io.riddles.javainterface.game.state.AbstractState;
import io.riddles.javainterface.io.IOHandler;

/**
 * io.riddles.javainterface.engine.AbstractEngine - Created on 2-6-16
 *
 * DO NOT EDIT THIS FILE
 *
 * The engine in the main project should extend this abstract class.
 * This class handles everything the game engine needs to do to start, run and finish.
 * Quite a lot of methods have already been implemented, but some need to
 * be Overridden in the Subclass. An object of the Subclass of AbstractEngine should
 * be created in the Main method of the project and then the engine is started with
 * engine.run()
 *
 * @author Jim van Eeden - jim@riddles.io
 */
public abstract class AbstractEngine<Pr extends AbstractProcessor,
        Pl extends AbstractPlayer, S extends AbstractState> {

    protected final static Logger LOGGER = Logger.getLogger(AbstractEngine.class.getName());

    protected String[] botInputFiles;

    protected IOHandler ioHandler;
    protected ArrayList<Pl> players;
    protected Pr processor;
    protected HashMap<String, Object> configuration;

    // Can be overridden in subclass constructor
    protected GameLoop gameLoop;

    protected AbstractEngine() {
        this.players = new ArrayList<>();
        this.gameLoop = new SimpleGameLoop();
        this.ioHandler = new IOHandler();
    }

    /**
     * Initializes the engine in debug mode
     * @param wrapperInputFile Input file from the wrapper
     * @param botInputFiles Input files for the bots
     */
    protected AbstractEngine(String wrapperInputFile, String[] botInputFiles) {
        this.players = new ArrayList<>();
        this.gameLoop = new SimpleGameLoop();
        this.ioHandler = new IOHandler(wrapperInputFile);
        this.botInputFiles = botInputFiles;
    }

    /**
     * This method starts the engine. Should be called from the main
     * method in the project.
     */
    public void run() {
        LOGGER.info("Starting...");

        setup();

        if (this.processor == null) {
            throw new NullPointerException("Processor has not been set");
        }

        LOGGER.info("Running pre-game phase...");

        this.processor.preGamePhase();

        LOGGER.info("Starting game loop...");

        S initialState = getInitialState();
        this.gameLoop.run(initialState, this.processor);

        finish(initialState);
    }

    /**
     * Does everything needed before the game can start, such as
     * getting the amount of players, setting the processor and sending
     * the game settings to the bots.
     */
    protected void setup() {
        LOGGER.info("Setting up engine. Waiting for initialize...");

        this.ioHandler.waitForMessage("initialize");
        this.ioHandler.sendMessage("ok");

        LOGGER.info("Got initialize. Parsing settings...");

        try {
            String line = "";
            while (!line.equals("start")) { // from "start", setup is done
                line = this.ioHandler.getNextMessage();
                parseSetupInput(line);
            }
        } catch(IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }

        this.processor = createProcessor();

        LOGGER.info("Got start. Sending game settings to bots...");

        this.players.forEach(this::sendGameSettings);

        LOGGER.info("Settings sent. Setting up engine done...");
    }

    /**
     * Does everything needed to send the GameWrapper the results of
     * the game.
     * @param initialState The start-of-game state
     */
    protected void finish(S initialState) {

        // let the wrapper know the game has ended
        this.ioHandler.sendMessage("end");

        // send game details
        this.ioHandler.waitForMessage("details");

        AbstractPlayer winner = this.processor.getWinner();
        String winnerId = "null";
        if (winner != null) {
            winnerId = winner.getId() + "";
        }

        JSONObject details = new JSONObject();
        details.put("winner", winnerId);
        details.put("score", this.processor.getScore());

        this.ioHandler.sendMessage(details.toString());

        // send the game file
        this.ioHandler.waitForMessage("game");
        this.ioHandler.sendMessage(getPlayedGame(initialState));

        System.exit(0);
    }

    /**
     * Parses everything the engine wrapper API sends
     * we need to start the engine, like IDs of the bots
     * @param input Input from engine wrapper
     */
    protected void parseSetupInput(String input) {
        String[] split = input.split(" ");
        String command = split[0];
        if (command.equals("bot_ids")) {
            String[] ids = split[1].split(",");
            for (int i = 0; i < ids.length; i++) {
                Pl player = createPlayer(Integer.parseInt(ids[i]));

                if (this.botInputFiles != null)
                    player.setInputFile(this.botInputFiles[i]);

                this.players.add(player);
            }
        }
    }

    /**
     * Implement this to return the initial (mostly empty) game state.
     * @return The initial state of the game, should be Subclass of AbstractState
     */
    protected abstract S getInitialState();

    /**
     * Implement this to return a player in the game.
     * @param id Id of the player
     * @return Object that is Subclass of AbstractPlayer
     */
    protected abstract Pl createPlayer(int id);

    /**
     * Implement this to return the processor for the game.
     * @return Object that is Subclass of AbstractProcessor
     */
    protected abstract Pr createProcessor();

    /**
     * Send the settings to the player (bot) that are specific to this game
     * @param player Player to send the settings to
     */
    protected abstract void sendGameSettings(Pl player);

    /**
     * Return the string representation of the entire game to use in
     * the visualizer
     * @param initialState The initial state of the game (can be used
     *                     to go the next game states).
     * @return String representation of the entire game
     */
    protected abstract String getPlayedGame(S initialState);

    /**
     * @return The players for the game
     */
    public ArrayList<Pl> getPlayers() {
        return this.players;
    }

    /**
     * @return The processor for the game
     */
    public Pr getProcessor() {
        return this.processor;
    }
}
