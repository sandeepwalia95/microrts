package QMSajidWaliaMarciszewicz.Strategies;

import ai.RandomAI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.EvaluationFunctionForwarding;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.*;
import rts.units.Unit;
import rts.UnitAction;
import rts.units.UnitTypeTable;
import util.Pair;
import java.util.*;

/**
 * Class containing the implementation of the whole OEP algorithm being the main startegy used by the bot QMSajidWaliaMarciszewicz
 * Class contains definition off all needed methods used at every stage of the algorithm.
 */
public class OEP {

    /**
     * Field used for generating ids of genomes in a population.
     */
    private int idGenerator =0;
    /**
     * Variable storing the time budget available for calculations of a strategy
     */
    private int TIME_BUDGET;
    /**
     * Variable storing th iterations budget for the calculations of a strategy
     */
    private int ITERATIONS_BUDGET;
    /**
     * Field storing the size of a population given as a parameter in the constructor of a function
     */
    private int _populationSize;

    /**
     * Id of a player for which the PlayerAction object is being generated.
     */
    private int _playerID;
    /**
     * Object used for generating random PlayerAction object valid for the given game state. These PlayerAction objects
     * are later on used as gene sequences in a Genome object
     */
    PlayerActionGenerator genomesGenerator = null;

    /**
     * Parameter determining number of parents that should be selected from the generation
     */
    private double _kparents=0.0;
    /**
     * Field storing the value that is depicting how far in the future are we looking while using the Monte Carlo in the evaluation
     * function.
     */
    private int _lookahead;
    /**
     * Field being a percentage of genes in a genome that should undergo mutation.
     */
    private double _numMutations = 0.0;
    /**
     * Field storing the AI bot being used in a simulation phase of the Monte Carlo algorithm that is being used in the
     * evaluation stage of the OEP algorithm.
     */
    AI randomAI;

    /**
     * Class used for storing the population evaluated in a game cycle.
     */
    public class Population{
        /**
         * List of individuals in a population. List of Genome objects.
         */
        ArrayList<Genome> individuals;

        /**
         * Constructor initializing the list of individuals.
         */
        public Population()
        {
            individuals = new ArrayList<>();
        }
    }

    /**
     * Class used for a representation of the genome in the algorithm.
     */
    public class Genome implements Comparable<Genome>{
        /**
         * Field storing ID of a genome
         */
        int ID;
        /**
         * Sequence of genes representing the genome stored in an object PlayerAction.
         */
        PlayerAction genes;
        /**
         * Value of the fitness function of a genome showing the results of the genome evaluation.
         */
        float fitness;

        /**
         * Function used to define the way two Genome obejcts are compared.
         * Genomes are compared with the regard of the value of fitness function.
         * @param o genome that is being compared with current Genome
         * @return 1 - if current genome is greater than given one; -1 - if current genome is smaller than given one;
         * 0 - if genomes are equal
         */
        @Override
        public int compareTo(Genome o) {
            if(this.fitness > o.fitness) {
                return 1;
            } else if (this.fitness < o.fitness) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Field storing population currently being processed.
     */
    private Population _population;


    /**
     * A constructor of a class initiating its private fields. Used for creating a new instance of a class with algorithm.
     *
     * @param timeBudget indicates the time the agent has to select an action. If -1 is passed, it’ll be infinite
     * @param iterationBudget indicates the number of maximum iteration certain algorithms can run. It’ll
     *    be normally -1, which also means infinite
     * @param populationSize size of the population of genomes being analyzed by the algorithm
     * @param mutationParam percentage of the genes that should be mutated during mutation stage of the algorithm
     * @param lookahead value that is depicting how far in the future are we looking while using the Monte Carlo in the evaluation
     * function.
     */
    public OEP(int timeBudget, int iterationBudget, int populationSize, double mutationParam, int lookahead){
        this.TIME_BUDGET = timeBudget;
        this.ITERATIONS_BUDGET = iterationBudget;
        this._populationSize = populationSize;
        this._lookahead = lookahead;
        this._population = new Population();
        this._kparents = 0;
        this.randomAI = new RandomAI();
        this._numMutations = mutationParam;
    }

    /**
     * Method calculating the OEP algorithm for the given player and game state. It contains all the stages of the OEP
     * algorithm, which are:
     *  - generating a population
     *  - evaluating a population
     *  - selecting k parents
     *  - pairing parents
     *  - crossover
     *  - mutation
     *
     * @param player id of a player for which the actions should be issued
     * @param gs current game state
     * @param utt a reference to an object that contains information about the available unit
     *     types in the game and their settings.
     * @return PLayerAction object that should be issued in the next game cycle, calculated by the algorithm
     * @throws Exception
     */
    public PlayerAction execute(int player, GameState gs, UnitTypeTable utt) throws Exception {

        _playerID = player;
        GameState _gs = gs.clone();
        long start = System.currentTimeMillis(); //start counting the time
        int nruns = 0;

        //Only execute an action if the player can execute any.
        if(gs.canExecuteAnyAction(player))
        {
            //initializing the generator of the genomes
            this.genomesGenerator = new PlayerActionGenerator(_gs,_playerID);
            //1. Initialize the population for t=0
            generatePopulation(_gs);

                while(true) {
                    if (TIME_BUDGET>0 && (System.currentTimeMillis() - start)>=TIME_BUDGET) break;
                    if (ITERATIONS_BUDGET>0 && nruns>=ITERATIONS_BUDGET) break;

                    //2. Evaluate population
                    evaluatePopulation(_gs);
                    //3. Select k individuals for the new population
                    _kparents = (_population.individuals.size()/3)*2;
                    //Creating an ArrayList of parents which is k^ long
                    ArrayList<Genome> parents = new ArrayList<>(selectParents((int)(Math.ceil(_kparents))));
                    //4. Create pairs from selected individuals
                    ArrayList<Pair<Genome,Genome>> couples = pairIndividuals(parents);
                    //5. Crossover
                    ArrayList<Genome> kids = crossover(couples, _gs);
                    //6. Mutate newly created individuals.
                    kids = mutation(kids, _numMutations, _gs);
                    //7. Create population for t+1
                    _population.individuals = new ArrayList<>(parents);
                    _population.individuals.addAll(kids);

                    nruns++;
                }

        } else {
            //Nothing to do: empty player action
            return new PlayerAction();
        }

        //pick the best individual from the population
        return bestIndividual(_gs);
    }

    /**
     * Function used to calculate the size of the population. It picks either the size given as a parameter in the
     * class constructor or the number of all the possible combinations of assigning the actions to units
     * @param gs current game state
     * @return size of the population
     */
    private int findPopulationSize(GameState gs)
    {
        int n =1;

        for(Unit u : gs.getUnits())
        {
            if(u.getPlayer() == _playerID)
            {
                int actionsNumber = u.getUnitActions(gs).size();
                if(actionsNumber!=0)
                    n*=actionsNumber; //preventing from having a 0 as a size of the population
                if(n<0)
                    return _populationSize;
            }
        }
        return n<_populationSize?n:_populationSize;
    }

    /**
     * Method used to generate the population of random individuals
     * @param gs current game state
     * @throws Exception
     */
    private void generatePopulation(GameState gs) throws Exception
    {
        genomesGenerator.randomizeOrder();
        int N = findPopulationSize(gs); //so to not calculate it every time the loop is being executed

        for(int i=0;i<N;i++)
        {
            //generate new genome and add it to population
            Genome newGenome = new Genome();
            newGenome.ID = idGenerator;
            idGenerator++;

            //pick the sequence of genes for the genome with the use of getRandom function
            newGenome.genes = genomesGenerator.getRandom();
            _population.individuals.add(newGenome);
        }
    }


    /**
     * Evaluation of the genome with the use of MonteCarlo.
     * @param pa PlayerAction object being a Genome that is evaluated by the Monte Carlo algorithm
     * @param gs current game state
     * @return value of the evaluation of the genome with the use of MonteCarlo
     */
    private double evaluateMonteCarlo(PlayerAction pa, GameState gs, EvaluationFunction ef)
    {
        GameState gs2 = gs.cloneIssue(pa);
        GameState gs3 = gs2.clone();
        int time=0;
        try {
            //issuing a playoff
            simulate(gs3, gs3.getTime() + _lookahead);
            time = gs3.getTime() - gs2.getTime();
        }catch(Exception e)
        {
            time=-1;
        }

        return ef.evaluate(_playerID, 1-_playerID, gs3)*Math.pow(0.99,time/10.0);
    }

    /**
     * Fuction used for issuing the playoff in the MonteCarlo
     * @param gs current game state
     * @param time time that can be used for performign the playoff
     * @throws Exception
     */
    public void simulate(GameState gs, int time) throws Exception {
        boolean gameover = false;

        do{
            if (gs.isComplete()) {
                gameover = gs.cycle();
            } else {
                //issue random actions for both players
                gs.issue(randomAI.getAction(0, gs));
                gs.issue(randomAI.getAction(1, gs));
            }
        }while(!gameover && gs.getTime()<time);
    }


    /**
     * Method used for evaluation of all the genomes in the population. Method is filling in the fitness field of the genome.
     * @param gs current game state used for the evaluation
     */
    void evaluatePopulation(GameState gs)
    {
        for(int index = 0; index < _population.individuals.size(); index++)
        {
            //get the pa to evaluate
            PlayerAction pa_to_eval = _population.individuals.get(index).genes;

            int maxTime = 0;//pick the longest action

            //gets the duration for the longest unit action
            for (Pair <Unit,UnitAction> uaa:_population.individuals.get(index).genes.getActions())
            {
                if(uaa.m_b.ETA(uaa.m_a)>maxTime)
                    maxTime = uaa.m_b.ETA(uaa.m_a);
            }

            //creating an object of an evaluation function
            EvaluationFunction baseEval = new SimpleSqrtEvaluationFunction3();

            //evaluating the genome with the use of Monte Carlo
            double MCev = evaluateMonteCarlo(_population.individuals.get(index).genes,gs,baseEval);

            // Given the current game state, execute the starting PlayerAction and clone the state
            GameState gs2 = gs.cloneIssue(pa_to_eval);

            //Make a copy of the resultant state for the rollout
            GameState gs3 = gs2.clone();

            //Cycle forward by the number of cycles
            for (int i = 0; i < maxTime; i++) {
                gs3.cycle();
            }

            //evaluate game state after cycling to the moment when all the issued actions have been completed
            float ev = new EvaluationFunctionForwarding(baseEval).evaluate(_playerID,1-_playerID, gs3);

            //calcuate the final value of the fitness fuction with the given weights that represent the proportions between
            //the use of simple evaluation and Monte Carlo evaluation
            _population.individuals.get(index).fitness = (float) (0.9*ev +0.1*MCev);

        }
    }


    /**
     * Method used for selecting k parents from the population for the reproduction purposes
     * @param k number of parents to be selected from the population
     * @return list of Genomes of parents that will generate the new population
     */
    ArrayList<Genome> selectParents(int k)
    {
        //return list of k parents selected from the population
        ArrayList<Genome> parents = new ArrayList<>();

        //sort the list of individuals in the population with the respect to the fitness function
        Collections.sort(_population.individuals);

        for (int i = 0; i <k;i++)
        {
            //picking the k best individuals
            parents.add(_population.individuals.get(_population.individuals.size()-i-1));
        }
        return parents;
    }

    /**
     * Method used for making pairs from the parents
     * @param parents list of individuals selected from the population that should be paired
     * @return list of the pairs that is going to be used in the corssover method
     */
    ArrayList<Pair<Genome,Genome>> pairIndividuals(ArrayList<Genome> parents)
    {
        ArrayList <Pair<Genome,Genome>> pairs = new ArrayList<>();

        //shuffles the parents that were placed by fitness in the array list
        //so that random pairs are generated
        Collections.shuffle(parents);

        //pair parents
        for(int i = 0; i<parents.size(); i+=2) {
            pairs.add(new Pair <>(parents.get(parents.size()-i-1),parents.get(parents.size()-i-2)));
        }
        return pairs;
    }

    /**
     * Method used to produce kids from the list of given pairs of parents. Method is performing the crossover stage of
     * the OEP algorithm
     * @param couples list of pairs that should be used to generate individuals in the new population
     * @param gs current game state
     * @return list of newly created individuals (kids)
     */
    ArrayList<Genome> crossover(ArrayList<Pair<Genome,Genome>> couples, GameState gs)
    {
        //perform crossover on given pairs
        ArrayList<Genome> kids = new ArrayList<>();
        //initialize crossover object
        Crossover crossover = new Crossover(true);

        for(Pair<Genome,Genome> parents: couples)
        {
            //create a new Genome
            Genome kid = new Genome();
            kid.ID = idGenerator;
            idGenerator++;

            GameState gsClone = gs.clone();
            //generate the sequence of genes for the new Genome from the parents genes
            kid.genes = crossover.nPointsCrossover(parents,2, gsClone);
            kids.add(kid);
        }
        return kids;
    }


    /**
     * Method used for mutating genes in kids' genomes. Method performs mutation stage of the OEP algorithm
     * @param individuals list of individuals (kids) that should undergo mutation
     * @param percMutations percentage of the genes from the genome that should be mutated
     * @param gs current game state
     * @return list of mutated genomes
     */
    ArrayList<Genome> mutation(ArrayList<Genome> individuals, double percMutations, GameState gs)
    {
        ArrayList<Genome> mutatedIndividuals = new ArrayList<>();
        Random r = new Random(System.currentTimeMillis());

        //if the list of individuals is empty - return
        if(individuals.size()==0)
            return mutatedIndividuals;

        //calcualte the number of genes that should be mutated
        int numMutations = (int) Math.ceil(individuals.get(0).genes.getActions().size()*percMutations);

        for (Genome g : individuals)
        {
            GameState gsClone = gs.clone();
            PhysicalGameState pgs = gsClone.getPhysicalGameState();

            // Pick random positions of the genes to be mutated
            ArrayList<Integer> positions = new ArrayList<>();
            while(positions.size()<numMutations)
            {
                int pos = r.nextInt(g.genes.getActions().size());
                if(!positions.contains(pos))
                    positions.add(pos);
            }

            for (Integer picked: positions)
            {
                // picks a random gene from the genome (gene from the random position calculated previously)
                Pair<Unit, UnitAction> genePicked = g.genes.getActions().get(picked);

                // Extract unit from the gene
                Unit unit = genePicked.m_a;

                // If we want to ensure that the same action is not picked again
                UnitAction ua = genePicked.m_b;

                // All possible actions for the unit
                List<UnitAction> listOfActions = unit.getUnitActions(gsClone);

                // Remove the action that is already set from the list so that it is not picked again
                if(listOfActions.size()>1)
                    listOfActions.remove(ua);

                boolean consistent = false;
                do {

                    UnitAction newUnitAction;
                    if(listOfActions.size()==0)
                        newUnitAction = new UnitAction(UnitAction.TYPE_NONE); //if no action is avilable return UnitAction.TYPE_NONE
                    else
                        // Select a random action from the units possible actions
                        newUnitAction= listOfActions.remove(r.nextInt(listOfActions.size()));

                    //check whether newly picked action is consistent with the resource usage of the genome
                    ResourceUsage r2 = newUnitAction.resourceUsage(unit, pgs);

                    if (g.genes.getResourceUsage().consistentWith(r2, gsClone)) {
                        g.genes.getResourceUsage().merge(r2);

                        // Set new action to the gene
                        genePicked.m_b = newUnitAction;

                        // Update the gene within the genome
                        g.genes.getActions().set(picked, genePicked);

                        consistent = true;
                    }
                } while (!consistent);

            }
            mutatedIndividuals.add(g);
        }
        return mutatedIndividuals;
    }

    /**
     * Method used at the final stage of the algorithm. It picks the best individual from the generation and returns its
     * sequence of the genes as a PlayerAction that should be issued in the next game cycle
     * @param gs current game state
     * @return PlayerAction object containg all the actions for the units that should be issued in the next game cycle
     */
    PlayerAction bestIndividual(GameState gs)
    {
        //evaluate the population
        evaluatePopulation(gs);
        //if the size of the population is equal to 0 return the empty PlayerAction object
        if(!(_population.individuals.size()>0)) return new PlayerAction();

        //pick the best individual to be returned as a result of the analysis
        Collections.sort(_population.individuals); //sort the population
        return _population.individuals.get(_population.individuals.size()-1).genes;
    }

    /**
     * Method used for cloning the current OEP class object.
     * @return new instance of the OEP class object
     */
    public OEP clone() {
        return new OEP(TIME_BUDGET,ITERATIONS_BUDGET,_populationSize,_numMutations, _lookahead);
    }

    /**
     * Function used to reset the state of the algorithm.
     */
    public void reset() {
        genomesGenerator = null;
        _population = null;
    }
}
