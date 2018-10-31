package QMSajidWaliaMarciszewicz.Strategies;

import ai.abstraction.pathfinding.PathFinding;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.*;
import rts.units.Unit;
import rts.UnitAction;
import rts.units.UnitTypeTable;
import util.Pair;
import java.util.*;


public class OEP extends QMStrategy {

    private int idGenerator =0;
    private int TIME_BUDGET;
    private int ITERATIONS_BUDGET;
    private int _populationSize;

    private int _playerID;
    PlayerActionGenerator genomesGenerator = null;

    //parameter used for selection of parents for new generation
    private double _kparents=0.0; // had to switch back to double. was getting zeroed.
    private int _lookahead; //how far in future are we looking/ how long is the genome
    private double _numMutations = 1.0;

    public class Population{
        ArrayList<Genome> individuals;
        public Population()
        {
            individuals = new ArrayList<>();
        }
    }

    public class Genome implements Comparable<Genome>{
        int ID; //maybe id appears to be not needed then we can get rid of it
        PlayerAction genes;
        float fitness; //result of fitness function/ evaluation of this sequence

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

    private Population _population;



    public OEP(int timeBudget, int iterationBudget, int populationSize, int lookahead){
        this.TIME_BUDGET = timeBudget;
        this.ITERATIONS_BUDGET = iterationBudget;
        this._populationSize = populationSize;
        this._lookahead = lookahead;
        this._population = new Population();
        this._kparents = 0;
    }

    @Override
    public PlayerAction execute(int player, GameState gs, UnitTypeTable utt, PathFinding pf) throws Exception {

        _playerID = player;
        GameState _gs = gs.clone();
        long start = System.currentTimeMillis();
        int nruns = 0;

        //Only execute an action if the player can execute any.
        if(gs.canExecuteAnyAction(player))
        {
            this.genomesGenerator = new PlayerActionGenerator(_gs,_playerID);
            //1. Initialize the population for t=0
            if(generatePopulation(_gs))
            {
                //repairGenomes(_gs, pf); // for repairing the randomly generated population

                while(true) {
                    if (TIME_BUDGET>0 && (System.currentTimeMillis() - start)>=TIME_BUDGET) break;
                    if (ITERATIONS_BUDGET>0 && nruns>=ITERATIONS_BUDGET) break;

                    //2. Evaluate population
                    evaluatePopulation(_gs);
                    //3. Select k individuals for the new population (remove the ones with lowest fitness)
                    _kparents = (_population.individuals.size()/3)*2; // Might move if we find a nicer place

                    //_kparents=_population.individuals.size()/2;

                    //Creating an ArrayList of parents which is k^ long
                    ArrayList<Genome> parents = new ArrayList<>(selectParents((int)(Math.ceil(_kparents))));

                    //4. Create pairs from selected individuals
                    ArrayList<Pair<Genome,Genome>> couples = pairIndividuals(parents);

                    //5. Crossover
                    ArrayList<Genome> kids = crossover(couples, _gs);//kids' datatype should be same as population

                    //5a. repair
                    //kids = repairGenomes(kids, gs, pf);//repair only after mutation?

                    //6. Mutate newly created individuals.
                    kids = mutation(kids, _numMutations, _gs);

                    //6a. repair
                    //kids = repairGenomes(kids, _gs,pf);

                    //7. Create population for t+1
                    _population.individuals = new ArrayList<>(parents);
                    _population.individuals.addAll(kids);

                    nruns++;
                }
            }
        } else {
            //Nothing to do: empty player action
            return new PlayerAction();
        }

        //pick the best individual from the population
        return bestIndividual(_gs);
    }

    private int findPopulationSize(GameState gs)
    {
        int n =1;

        for(Unit u : gs.getUnits())
        {
            if(u.getPlayer() == _playerID)
            {
                int actionsNumber = u.getUnitActions(gs).size();
                if(actionsNumber!=0)
                    n*=actionsNumber;
            }
        }
        return n<_populationSize?n:_populationSize;
    }

    private boolean generatePopulation(GameState gs) throws Exception
    {
        genomesGenerator.randomizeOrder();
        int N = findPopulationSize(gs); //so to not calculate it every time the loop is being executed

        for(int i=0;i<N;i++)
        {
            //generate new genome and add it to population
            Genome newGenome = new Genome();
            newGenome.ID = idGenerator;
            idGenerator++;

            newGenome.genes = genomesGenerator.getRandom();
            _population.individuals.add(newGenome);
        }
        return true; //there are some available actions for this player
    }

    void evaluatePopulation(GameState gs)
    {
        for(int index = 0; index < _population.individuals.size(); index++)
        {
            //get the pa to evaluate
            PlayerAction pa_to_eval = _population.individuals.get(index).genes;

            int maxTime = 0;//pick the longest action
            //int minTime = 1000;//pick the shortest action

            //gets the duration for the longest or shortest unit action. Loop has to be modified.
            for (Pair <Unit,UnitAction> uaa:_population.individuals.get(index).genes.getActions())
            {
                if(uaa.m_b.ETA(uaa.m_a)>maxTime)
                    maxTime = uaa.m_b.ETA(uaa.m_a);
            }

            // Given the current game state, execute the starting PlayerAction and clone the state
            GameState gs2 = gs.cloneIssue(pa_to_eval);

            //Make a copy of the resultant state for the rollout
            GameState gs3 = gs2.clone();

            //Cycle forward by the number of cycles
            for (int i = 0; i < maxTime; i++) {
                gs3.cycle();
            }

            //evaluate fitness after the number of cycles
            _population.individuals.get(index).fitness = new SimpleSqrtEvaluationFunction3().base_score(_playerID,gs3);

        }

    }


    ArrayList<Genome> selectParents(int k)
    {
        //return list of k parents selected from the population

        ArrayList<Genome> parents = new ArrayList<>();

        Collections.sort(_population.individuals);

        for (int i = 0; i <k;i++)
        {
            //picking the k best individuals
            parents.add(_population.individuals.get(_population.individuals.size()-i-1));
        }
        return parents;
    }

    ArrayList<Pair<Genome,Genome>> pairIndividuals(ArrayList<Genome> parents)
    {
        //shuffles the parents that were placed by fitness in the array list
        // so that random pairs are generated

        ArrayList <Pair<Genome,Genome>> pairs = new ArrayList<>();

        Collections.shuffle(parents);

        for(int i = 0; i<parents.size(); i+=2) {
            pairs.add(new Pair <>(parents.get(parents.size()-i-1),parents.get(parents.size()-i-2)));
        }
        return pairs;
    }

    ArrayList<Genome> crossover(ArrayList<Pair<Genome,Genome>> couples, GameState gs)
    {
        //perform crossover on given pairs
        ArrayList<Genome> kids = new ArrayList<>();
        Crossover crossover = new Crossover(true);

        for(Pair<Genome,Genome> parents: couples)
        {
            Genome kid = new Genome();
            kid.ID = idGenerator;
            idGenerator++;

            GameState gsClone = gs.clone();
            kid.genes = crossover.singlePointCrossover(parents, gsClone);
            kids.add(kid);
        }

        return kids;
    }

    void repairGenomes(GameState gs)
    {
        repairGenomes(_population.individuals, gs);

    }

    ArrayList<Genome> repairGenomes(ArrayList<Genome> genomes, GameState gs) {

        //flags to be used later
        boolean consistent = false;

        //The list of Repaired Genomes to be returned
        ArrayList<Genome> returnedGenomes = new ArrayList<>(genomes);

        int index = 0; int size = genomes.size();

        //Looping through every genome (playerAction)
        while (index < size) {
            GameState gsCopy = gs.clone();
            //Get physical game state
            PhysicalGameState pgs = gsCopy.getPhysicalGameState();

            ArrayList <Pair<Unit, UnitAction>> uuaList = new ArrayList<>(genomes.get(index).genes.getActions()) ;
            //Looping through every Unit, UnitAction pair (gene) in that genome
            for (Pair<Unit, UnitAction> uua : uuaList) {
                //put in a check for unit positions to not be out of bounds???

                //Resource Usage for current action
                ResourceUsage ru = uua.m_b.resourceUsage(uua.m_a, pgs);
                if (!(returnedGenomes.get(index).genes.consistentWith(ru, gsCopy)) || !(uua.m_a.canExecuteAction(uua.m_b, gsCopy)) )//Legality checks
                {
                    List<Pair<Unit,List<UnitAction>>> choices =  genomesGenerator.getChoices();
                    Random r = new Random();

                    //copying over to get random action assigned to that unit
                    for (Pair<Unit, List<UnitAction>> unitChoices : choices)
                    {
                        if (unitChoices.m_a == uua.m_a) {
                            List<UnitAction> l = new LinkedList<UnitAction>();
                            l.addAll(unitChoices.m_b);

                            //flags to be used later
                            consistent = false;

                            do {
                                UnitAction ua;

                                if(l.size()>0) {
                                    ua = l.remove(r.nextInt(l.size()));
                                }
                                else
                                    ua = new UnitAction(UnitAction.TYPE_NONE);


                                if (ua != null) {
                                    ResourceUsage r2 = ua.resourceUsage(unitChoices.m_a, pgs);
                                    if (returnedGenomes.get(index).genes.getResourceUsage().consistentWith(r2, gsCopy) && uua.m_a.canExecuteAction(ua, gsCopy)) {
                                        returnedGenomes.get(index).genes.getResourceUsage().merge(r2);// put checks here again and see what to do with this
                                        returnedGenomes.get(index).genes.removeUnitAction(unitChoices.m_a, uua.m_b);
                                        returnedGenomes.get(index).genes.addUnitAction(unitChoices.m_a, ua);
                                        consistent = true;
                                    }
                                }
                            } while (!consistent);
                        }
                    }
                }
            }
            index++;
        }
        return returnedGenomes;
    }

    ArrayList<Genome> mutation(ArrayList<Genome> individuals, double percMutations, GameState gs)
    {
        ArrayList<Genome> mutatedIndividuals = new ArrayList<>();
        Random r = new Random(System.currentTimeMillis());

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

                // picks a random gene from the genome
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
                        newUnitAction = new UnitAction(UnitAction.TYPE_NONE);
                    else
                        // Select a random action from the units possible actions
                        newUnitAction= listOfActions.remove(r.nextInt(listOfActions.size()));

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

    PlayerAction bestIndividual(GameState gs)
    {
        //evaluate the population and pick the best individual to be returned as a result of the analysis
        evaluatePopulation(gs);
        //pick the best individual
        if(!(_population.individuals.size()>0)) return new PlayerAction();

        Collections.sort(_population.individuals);

        PlayerAction bestSolution = _population.individuals.get(_population.individuals.size()-1).genes;
        //get the physical state of the game

        PhysicalGameState pgs = gs.getPhysicalGameState();
        //create an object of the actions that should be issued in this game cycle
        PlayerAction _actionsToBePerformed = new PlayerAction();

        // Generate the reserved resources from the game state:
        /*
        for(Unit u:pgs.getUnits()) {
            UnitActionAssignment uaa = gs.getActionAssignment(u);
            if (uaa!=null) {
                ResourceUsage ru = uaa.action.resourceUsage(u, pgs);
                _actionsToBePerformed.getResourceUsage().merge(ru);
            }
        }*/
        //pick the action for each owned by the player unit that is not busy
        for(Unit u:pgs.getUnits()) {
            if (u.getPlayer()==_playerID) {
                if (gs.getActionAssignment(u)==null) {

                    //if the unit has no assigned action
                    try {
                        //take the action for the unit from the solution generated by the algorithm
                        UnitAction ua = bestSolution.getAction(u);

                        //check if action is consistent with the resources
                      //  if (ua.resourceUsage(u, pgs).consistentWith(_actionsToBePerformed.getResourceUsage(), gs)) {
                            //if action is consistent
                            ResourceUsage ru = ua.resourceUsage(u, pgs);
                            _actionsToBePerformed.getResourceUsage().merge(ru);
 /*                       } else {

                            //pick another random action for a unit
                            List<UnitAction> listOfActions = u.getUnitActions(gs);
                            boolean consistent = false;
                            Random r = new Random(System.currentTimeMillis());
                            do {
                                if(listOfActions.size()==0)
                                    ua = new UnitAction(UnitAction.TYPE_NONE);
                                else
                                    ua = listOfActions.remove(r.nextInt(listOfActions.size()));

                                ResourceUsage r2 = ua.resourceUsage(u, pgs);

                                if (_actionsToBePerformed.getResourceUsage().consistentWith(r2, gs)) 		{
                                    _actionsToBePerformed.getResourceUsage().merge(r2);
                                    consistent = true;
                                }
                            } while (!consistent);
                        }
*/
                        _actionsToBePerformed.addUnitAction(u, ua);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        _actionsToBePerformed.addUnitAction(u, new UnitAction(UnitAction.TYPE_NONE));
                    }
                }
            }
        }

        return _actionsToBePerformed;
    }


    @Override
    public QMStrategy clone() {
        return new OEP(TIME_BUDGET,ITERATIONS_BUDGET,_populationSize, _lookahead);
    }

    public void reset() {
        genomesGenerator = null;
        _population = null;
    }
}
