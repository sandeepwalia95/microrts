package QMSajidWaliaMarciszewicz;

import QMSajidWaliaMarciszewicz.Strategies.MonteCarloSearch;
import QMSajidWaliaMarciszewicz.Strategies.OEP;
import ai.RandomAI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

import java.util.*;

public class QMSajidWaliaMarciszewicz extends AIWithComputationBudget {

    /**
     * Object that contains information about the available unit types in the game and their settings.
     */
    private UnitTypeTable _utt;

    /**
     * Object that can run path-planning queries in the map.
     */
    private PathFinding _pathFinding;

    /**
     * A variable used to keep track of how many times the bot is asked for an action.
     */
    private int _actionCounter;

    /**
     * ID of a player that is assigned to the bot. Can have a value equal to either 0 or 1.
     */
    private int _playerID;


    /**
     *
     * A constructor of a class initiating its private fields. Can be used for creating a new instance of a bot.
     *
     * @param timeBudget indicates the time the agent has to select an action. If -1 is passed, it’ll be infinite.
     * @param iterationBudget indicates the number of maximum iteration certain algorithms can run. It’ll
     *     be normally -1, which also means infinite.
     * @param utt a reference to an object that contains information about the available unit
     *     types in the game and their settings.
     * @param pathFinding a reference to an object that can run path-planning queries in the map.
     */
    public QMSajidWaliaMarciszewicz(int timeBudget, int iterationBudget, UnitTypeTable utt, PathFinding pathFinding)
    {
        super(timeBudget,iterationBudget);
        this._utt = utt;
        this._pathFinding = pathFinding;

        this._actionCounter=0;
    }

    /**
     *  Method used to reset the agent to an initial configuration.
     */
    @Override
    public void reset() {
        _actionCounter=0;
    }

    /**
     * Method providing actions for agent's units. This function is called at every game tick.
     *
     * @param player ID of the player to move. Use it to check whether units are yours or enemy's
     * @param gs the game state where the action should be performed
     * @return object containing actions for all player's units
     * @throws Exception thrown during strategy execution
     */
    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {

        _actionCounter++;
        _playerID = player;

        //place for solutions(strategies) created on the basis of pregame analysis

        //return new MonteCarloSearch(TIME_BUDGET,ITERATIONS_BUDGET,100,1000,new RandomAI(), new SimpleSqrtEvaluationFunction3())
         //       .execute(player,gs,_utt,_pathFinding);
        return new OEP(TIME_BUDGET, ITERATIONS_BUDGET,6,1).execute(player,gs,_utt,_pathFinding);
    }

    /**
     * Method used to create an exact copy of the agent.
     * @return copy of an agent
     */
    @Override
    public AI clone() {

        QMSajidWaliaMarciszewicz instance = new QMSajidWaliaMarciszewicz(getTimeBudget(),getIterationsBudget(),_utt, _pathFinding);
        instance._playerID=_playerID;
        instance._actionCounter=_actionCounter;
        return instance;
    }

    /**
     * Method that supplies the list of parameters that user is able to inspect in the GUI.
     * @return list of parameters for the GUI
     */
    @Override
    public List<ParameterSpecification> getParameters() {

        List<ParameterSpecification> params = new ArrayList<>();

        params.add(new ParameterSpecification("TimeBudget",int.class,TIME_BUDGET));
        params.add(new ParameterSpecification("IterationsBudget",int.class,ITERATIONS_BUDGET));
        params.add(new ParameterSpecification("UnitTypeTable",UnitTypeTable.class,_utt));
        params.add(new ParameterSpecification("PathFinding",PathFinding.class,_pathFinding));

        return params;
    }

    /**
     * Method that allows agent to analyse the game before it starts.
     *
     * @param gs the initial state of the game about to be played. Even if the game is
     * partially observable, the game state received by this
     * function might be fully observable
     * @param milliseconds time limit to perform the analysis. If zero, you can take as
     * long as you need
     * @throws Exception thrown while analysing the pre game state
     */
    @Override
    public void preGameAnalysis(GameState gs, long milliseconds) throws Exception {
        super.preGameAnalysis(gs, milliseconds);
    }
}
