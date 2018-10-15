package QMSajidWaliaMarciszewicz;

import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

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
     * @return
     * @throws Exception
     */
    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {

        _actionCounter++;
        _playerID = player;

        //getting my units
        if(gs.canExecuteAnyAction(_playerID))
        {
            for(Unit u : gs.getUnits())
            {
                if(u.getPlayer() == _playerID)
                    ;
            }
        }

        //object containing the actions agent's units will execute in the game
        PlayerAction move = new PlayerAction();

        return move;
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

        List<ParameterSpecification> params = new List<ParameterSpecification>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<ParameterSpecification> iterator() {
                return null;
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return null;
            }

            @Override
            public boolean add(ParameterSpecification parameterSpecification) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends ParameterSpecification> c) {
                return false;
            }

            @Override
            public boolean addAll(int index, Collection<? extends ParameterSpecification> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {

            }

            @Override
            public ParameterSpecification get(int index) {
                return null;
            }

            @Override
            public ParameterSpecification set(int index, ParameterSpecification element) {
                return null;
            }

            @Override
            public void add(int index, ParameterSpecification element) {

            }

            @Override
            public ParameterSpecification remove(int index) {
                return null;
            }

            @Override
            public int indexOf(Object o) {
                return 0;
            }

            @Override
            public int lastIndexOf(Object o) {
                return 0;
            }

            @Override
            public ListIterator<ParameterSpecification> listIterator() {
                return null;
            }

            @Override
            public ListIterator<ParameterSpecification> listIterator(int index) {
                return null;
            }

            @Override
            public List<ParameterSpecification> subList(int fromIndex, int toIndex) {
                return null;
            }
        };

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
     * @throws Exception
     */
    @Override
    public void preGameAnalysis(GameState gs, long milliseconds) throws Exception {
        super.preGameAnalysis(gs, milliseconds);
    }
}
