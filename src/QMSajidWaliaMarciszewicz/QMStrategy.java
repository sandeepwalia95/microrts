package QMSajidWaliaMarciszewicz;

import QMSajidWaliaMarciszewicz.Solutions.MonteCarloSearch;
import ai.RandomAI;
import ai.abstraction.pathfinding.PathFinding;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.*;
import rts.units.Unit;
import rts.units.UnitTypeTable;

public class QMStrategy {

    /**
     * The time the agent has to select an action.
     */
    private int _timeBudget;
    /**
     * The number of maximum iteration certain algorithms can run.
     */
    private int _iterationBudget;

    /**
     * A constructor of a class initiating its private fields. Can be used for creating a new strategy for the game.
     * @param timeBudget indicates the time the agent has to select an action. If -1 is passed, it’ll be infinite.
     * @param iterationBudget indicates the number of maximum iteration certain algorithms can run. It’ll
     *         be normally -1, which also means infinite.
     */
    public QMStrategy(int timeBudget, int iterationBudget){
        this._timeBudget = timeBudget;
        this._iterationBudget = iterationBudget;
    }


    /**
     * Method that picks the strategy for the agent on the basis of whatever circumstances.
     * @param player player assigned to the agent.
     * @param gs current game state.
     * @param utt a reference to an object that contains information about the available unit
     *      types in the game and their settings.
     * @param pf a reference to an object that can run path-planning queries in the map.
     * @return object containing actions for all agent's units for a single game tick
     */
    PlayerAction execute(int player, GameState gs, UnitTypeTable utt, PathFinding pf) throws Exception {
        //We have one global behaviour. Think of this method as a hub to decide between different
        // behaviours at given times under whatever circumstances. For now, we just do a simple behaviour:

        //object containing the actions agent's units will execute in the game
        PlayerAction move = new PlayerAction();

        /*
        //getting my units
        if(gs.canExecuteAnyAction(player))
        {
            for(Unit u : gs.getUnits())
            {
                if(u.getPlayer() == player)
                    ;
            }
        }
        */

        //return move;
        return new MonteCarloSearch(_timeBudget,_iterationBudget,100,1000,new RandomAI(), new SimpleSqrtEvaluationFunction3())
                .execute(player,gs,utt,pf);
    }

    /**
     * Computes the Manhattan distance between two units.
     * @param a One unit.
     * @param b Another unit.
     * @return the Manhattan distance. As expected.
     */
    private int manhattanDistance(Unit a, Unit b)
    {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    /**
     * Returns the position (x,y) flattened. This is, the location of that cell in a vector instead of a matrix.
     * @param gs Game state, needed for the with of the board or map.
     * @param x x position
     * @param y y position
     * @return position in a vector.
     */
    private int flattenedPosition(GameState gs, int x, int y)
    {
        return x + y * gs.getPhysicalGameState().getWidth();
    }

    /**
     * Method to check resources needed are available before adding an action assignment to the PlayerAction
     * @param nextAction    The action to be added to PlayerAction
     * @param u             The unit the action is assigned to
     * @param pa            The PlayerAction object to receive new unit assignment
     * @param gs            The current game state
     */
    private void checkResourcesAndAddAction(UnitAction nextAction, Unit u, PlayerAction pa, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        //If there are enough resources and the action is allowed for this unit
        if (nextAction != null && nextAction.resourceUsage(u, pgs).consistentWith(pa.getResourceUsage(), gs) &&
                (gs.isUnitActionAllowed(u, nextAction)) ) {
            addActionWithResourceUsage(nextAction, u, pa, gs);
        }
    }

    /**
     * Method to add an action assignment and resource usage to the PlayerAction
     * @param nextAction    The action to be added to PlayerAction
     * @param u             The unit the action is assigned to
     * @param pa            The PlayerAction object to receive new unit assignment
     * @param gs            The current game state
     */
    private void addActionWithResourceUsage(UnitAction nextAction, Unit u, PlayerAction pa, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        ResourceUsage ru = nextAction.resourceUsage(u, pgs);
        pa.getResourceUsage().merge(ru);
        pa.addUnitAction(u, nextAction);
    }

    /**
     * Method used to clone the QMStrategy class object.
     * @return instance of a class QMStrategy
     */
    public QMStrategy clone() {
        QMStrategy instance = new QMStrategy(_timeBudget,_iterationBudget);
        return instance;
    }
}
