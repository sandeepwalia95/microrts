package QMSajidWaliaMarciszewicz.Solutions;

import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.units.UnitTypeTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MonteCarloSearch extends Solution {

    private long MAXACTIONS;
    private int MAXSIMULATIONTIME;
    private int TIME_BUDGET;
    private int ITERATIONS_BUDGET;
    private AI _policy;

    // Function to evaluate the quality of a state after a rollout.
    private EvaluationFunction _ef = null;

    // Random number generator.
    Random r = new Random();
    long max_actions_so_far = 0;

    PlayerActionGenerator moveGenerator = null;
    boolean allMovesGenerated = false;

    GameState gs_to_start_from = null; //Starting game state
    int run = 0;
    int playerForThisComputation;

    // statistics:
    public long total_runs = 0;
    public long total_cycles_executed = 0;
    public long total_actions_issued = 0;

    // Inner class to hold cumulative score (accum_evaluation) and number of rollouts (visit_count)
    // that started from a PlayerAction (pa)
    public class PlayerActionTableEntry {
        PlayerAction pa;
        float accum_evaluation = 0;
        int visit_count = 0;
    }
    //and a list with all the entries.
    List<PlayerActionTableEntry> actions = null;


    /**
     * Constructor of a class initiating its private fields. Can be used for creating a new instance of a monte carlo strategy.
     * @param timeBudget indicates the time the agent has to select an action. If -1 is passed, it’ll be infinite.
     * @param iterationBudget indicates the number of maximum iteration certain algorithms can run. It’ll
     *      be normally -1, which also means infinite.
     * @param lookahead how far in the future the rollout goes.
     * @param maxactions maximum number of actions.
     * @param policy policy for the rollouts.
     * @param a_ef evaluation function
     */
    public MonteCarloSearch(int timeBudget, int iterationBudget, int lookahead, long maxactions, AI policy, EvaluationFunction a_ef){
        this.MAXSIMULATIONTIME = lookahead;
        this.MAXACTIONS = maxactions;
        this.TIME_BUDGET = timeBudget;
        this.ITERATIONS_BUDGET = iterationBudget;
        this._policy = policy;
        this._ef = a_ef;
    }

    /**
     * Function returning actions for agent's units.
     * @param player player assigned to the agent.
     * @param gs current game state.
     * @param utt a reference to an object that contains information about the available unit
     *      types in the game and their settings.
     * @param pf a reference to an object that can run path-planning queries in the map.
     * @return
     * @throws Exception
     */
    @Override
    public PlayerAction execute(int player, GameState gs, UnitTypeTable utt, PathFinding pf) throws Exception
    {
        //Only execute an action if the player can execute any.
        if (gs.canExecuteAnyAction(player)) {

            //Reset MonteCarlo
            startNewComputation(player,gs.clone());

            //Iterate MC as much as possible according to budget
            computeDuringOneGameFrame();

            //Decide on the best action and return it
            return getBestActionSoFar();
        } else {
            //Nothing to do: empty player action
            return new PlayerAction();
        }
    }

    /**
     * Resets Monte Carlo to start a new search.
     * @param a_player the current player
     * @param gs the game state where the action will be taken
     * @throws Exception
     */
    public void startNewComputation(int a_player, GameState gs) throws Exception {

        playerForThisComputation = a_player;
        gs_to_start_from = gs;
        moveGenerator = new PlayerActionGenerator(gs,playerForThisComputation);
        moveGenerator.randomizeOrder();
        allMovesGenerated = false;
        actions = null;
        run = 0;
    }

    /**
     * Iterates
     * @throws Exception
     */
    public void computeDuringOneGameFrame() throws Exception {
        long start = System.currentTimeMillis();
        int nruns = 0;
        long cutOffTime = (TIME_BUDGET>0 ? System.currentTimeMillis() + TIME_BUDGET:0);
        if (TIME_BUDGET<=0) cutOffTime = 0;

        //1. Fill the actions table
        if (actions==null) {
            actions = new ArrayList<>();
            if (MAXACTIONS>0 && moveGenerator.getSize()>2*MAXACTIONS) {
                for(int i = 0;i<MAXACTIONS;i++) {
                    PlayerActionTableEntry pate = new PlayerActionTableEntry();
                    pate.pa = moveGenerator.getRandom();
                    actions.add(pate);
                }
                max_actions_so_far = Math.max(moveGenerator.getSize(),max_actions_so_far);

            } else {
                PlayerAction pa;
                long count = 0;
                do{
                    pa = moveGenerator.getNextAction(cutOffTime);
                    if (pa!=null) {
                        PlayerActionTableEntry pate = new PlayerActionTableEntry();
                        pate.pa = pa;
                        actions.add(pate);
                        count++;
                        if (MAXACTIONS>0 && count>=2*MAXACTIONS) break; // this is needed since some times, moveGenerator.size() overflows
                    }
                }while(pa!=null);
                max_actions_so_far = Math.max(actions.size(),max_actions_so_far);

                while(MAXACTIONS>0 && actions.size()>MAXACTIONS) actions.remove(r.nextInt(actions.size()));
            }
        }

        //2. Until the budget is over, do another monte carlo rollout.
        while(true) {
            if (TIME_BUDGET>0 && (System.currentTimeMillis() - start)>=TIME_BUDGET) break;
            if (ITERATIONS_BUDGET>0 && nruns>=ITERATIONS_BUDGET) break;
            monteCarloRun(playerForThisComputation, gs_to_start_from);
            nruns++;
        }

        total_cycles_executed++;
    }
    /**
     * Executes a monte carlo rollout.
     * @param player  this player
     * @param gs state to roll the state from.
     * @throws Exception
     */
    public void monteCarloRun(int player, GameState gs) throws Exception {
        int idx = run%actions.size();
        // Take the next ActionTableEntry to execute
        PlayerActionTableEntry pate = actions.get(idx);

        // Given the current game state, execute the starting PlayerAction and clone the state
        GameState gs2 = gs.cloneIssue(pate.pa);

        //Make a copy of the resultant state for the rollout
        GameState gs3 = gs2.clone();

        //Perform random actions until time is up for a simulation or the game is over.
        simulate(gs3,gs3.getTime() + MAXSIMULATIONTIME);

        //time holds the difference in time ticks between the initial state and the one reached at the end.
        int time = gs3.getTime() - gs2.getTime();

        //Evaluate the state reached at the end (g3) and correct with a discount factor
        pate.accum_evaluation += _ef.evaluate(player, 1-player, gs3)*Math.pow(0.99,time/10.0);
        pate.visit_count++;
        run++;
        total_runs++;
    }

    /**
     * Simulates, according to a policy (this.randomAI), actions until the game is over or we've reached
     * the limited depth specified (time)
     * @param gs Game state to start the simulation from.
     * @param time depth, or number of game ticks, that limits the simulation.
     * @throws Exception
     */
    public void simulate(GameState gs, int time) throws Exception {
        boolean gameover = false;

        do{
            //isComplete() returns true if actions for all units have been issued. When this happens,
            // it advances the state forward.
            if (gs.isComplete()) {
                //cycle() advances the state forward AND returns true if the game is over.
                gameover = gs.cycle();
            } else {
                //Issue actions for BOTH players.
                gs.issue(_policy.getAction(0, gs));
                gs.issue(_policy.getAction(1, gs));
            }
            //Continue until the game is over or we've reached the desired depth.
        }while(!gameover && gs.getTime()<time);
    }

    /**
     * Out of all the PlayerActions tried from the current state, this returns the one with the highest average return
     * @return the best PlayerAction found.
     */
    public PlayerAction getBestActionSoFar() {

        PlayerActionTableEntry best = null;

        // Find the best. For each action in the table:
        for(PlayerActionTableEntry pate:actions) {
            //If the average return is higher (better) than the current best, keep this one as current best.
            if (best==null || (pate.accum_evaluation/pate.visit_count)>(best.accum_evaluation/best.visit_count)) {
                best = pate;
            }
        }

        //This shouldn't happen. Essentially means there's no entry in the table. Escape by applying random actions.
        if (best==null) {
            PlayerActionTableEntry pate = new PlayerActionTableEntry();
            pate.pa = moveGenerator.getRandom();
            System.err.println("MonteCarlo.getBestActionSoFar: best action was null!!! action.size() = " + actions.size());
        }

        total_actions_issued++;

        //Return best action.
        return best.pa;
    }

    /**
     *
     * @return
     */
    public Solution clone() {
        return new MonteCarloSearch(TIME_BUDGET, ITERATIONS_BUDGET, MAXSIMULATIONTIME, MAXACTIONS, _policy, _ef);
    }

    /**
     *
     */
    public void reset() {
        moveGenerator = null;
        actions = null;
        gs_to_start_from = null;
        run = 0;
    }

    /**
     *
     */
    public void resetSearch() {
        gs_to_start_from = null;
        moveGenerator = null;
        actions = null;
        run = 0;
    }

}
