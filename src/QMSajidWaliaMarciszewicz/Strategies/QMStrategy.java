package QMSajidWaliaMarciszewicz.Strategies;

import ai.abstraction.pathfinding.PathFinding;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public abstract class QMStrategy {
    abstract public PlayerAction execute(int player, GameState gs, UnitTypeTable utt, PathFinding pf) throws Exception;
    abstract public QMStrategy clone();



    //------------------------------LEFTOVER FROM QMStrategy CLASS--------------------------------------

    /**
     * Computes the Manhattan distance between two units.
     * @param a One unit.
     * @param b Another unit.
     * @return the Manhattan distance. As expected.
     */
    /*
    private int manhattanDistance(Unit a, Unit b)
    {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }*/

    /**
     * Returns the position (x,y) flattened. This is, the location of that cell in a vector instead of a matrix.
     * @param gs Game state, needed for the with of the board or map.
     * @param x x position
     * @param y y position
     * @return position in a vector.
     */
    /*private int flattenedPosition(GameState gs, int x, int y)
    {
        return x + y * gs.getPhysicalGameState().getWidth();
    }*/

    /**
     * Method to check resources needed are available before adding an action assignment to the PlayerAction
     * @param nextAction    The action to be added to PlayerAction
     * @param u             The unit the action is assigned to
     * @param pa            The PlayerAction object to receive new unit assignment
     * @param gs            The current game state
     */
    /*private void checkResourcesAndAddAction(UnitAction nextAction, Unit u, PlayerAction pa, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        //If there are enough resources and the action is allowed for this unit
        if (nextAction != null && nextAction.resourceUsage(u, pgs).consistentWith(pa.getResourceUsage(), gs) &&
                (gs.isUnitActionAllowed(u, nextAction)) ) {
            addActionWithResourceUsage(nextAction, u, pa, gs);
        }
    }*/

    /**
     * Method to add an action assignment and resource usage to the PlayerAction
     * @param nextAction    The action to be added to PlayerAction
     * @param u             The unit the action is assigned to
     * @param pa            The PlayerAction object to receive new unit assignment
     * @param gs            The current game state
     */
    /*private void addActionWithResourceUsage(UnitAction nextAction, Unit u, PlayerAction pa, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        ResourceUsage ru = nextAction.resourceUsage(u, pgs);
        pa.getResourceUsage().merge(ru);
        pa.addUnitAction(u, nextAction);
    }*/

    /**
     * Method used to clone the QMStrategy class object.
     * @return instance of a class QMStrategy
     */
    /*public QMStrategy clone() {
        QMStrategy instance = new QMStrategy(_timeBudget,_iterationBudget);
        return instance;
    }
     */
}
