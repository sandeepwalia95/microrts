package QMSajidWaliaMarciszewicz.Strategies;

import ai.abstraction.AbstractAction;
import ai.abstraction.pathfinding.PathFinding;

import ai.evaluation.SimpleSqrtEvaluationFunction3;
import exercise8.PlayerAbstractActionGenerator;
import rts.*;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import rts.units.Unit;
import rts.UnitAction;
import rts.units.UnitTypeTable;
import util.Pair;
import exercise8.AbstractActionBuilder;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;

import java.nio.file.Path;
import java.util.*;

public class OEP extends QMStrategy {

    private int idGenerator =0;
    private int TIME_BUDGET;
    private int ITERATIONS_BUDGET;
    private int _populationSize;

    private int _playerID;
    PlayerAbstractActionGenerator genomesGenerator = null;

    //parameter used for selection of parents for new generation
    private double _kparents=0.0; // had to switch back to double. was getting zeroed.
    private int _lookahead; //how far in future are we looking/ how long is the genome
    private int _numMutations = 1;

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
        GameState phenotype;
        float fitness; //result of fitness function/ evaluation of this sequence
        HashMap<Unit, AbstractAction> abstractActions = new HashMap<>();

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

        PathFinding _pf = pf;
        _playerID = player;
        this.genomesGenerator = new PlayerAbstractActionGenerator(utt);
        long start = System.currentTimeMillis();
        int nruns = 0;




        //Only execute an action if the player can execute any.
        if(gs.canExecuteAnyAction(player))
        {
            //1. Initialize the population for t=0
            if(generatePopulation(gs))
            {
               // repairGenomes(gs, pf); // for repairing the randomly generated population

                while(true) {
                    if (TIME_BUDGET>0 && (System.currentTimeMillis() - start)>=TIME_BUDGET) break;
                    if (ITERATIONS_BUDGET>0 && nruns>=ITERATIONS_BUDGET) break;

                    //2. Evaluate population
                    evaluatePopulation(gs); // if run for the first time, will assign a value to the fitness variable
                    // of the instance
                    //3. Select k individuals for the new population (remove the ones with lowest fitness)
                    _kparents = (_population.individuals.size()/3)*2; // Might move if we find a nicer place
                    //Creating an ArrayList of parents which is k^ long
                    ArrayList<Genome> parents = new ArrayList<>(selectParents((int)(Math.ceil(_kparents))));

                    //4. Create pairs from selected individuals
                    ArrayList<Pair<Genome,Genome>> couples = pairIndividuals(parents);

                    //5. Crossover
                    ArrayList<Genome> kids = crossover(couples);//kids' datatype should be same as population
                    //5a. repair
                    //kids = repairGenomes(kids, gs, pf);//repair only after mutation?

                    //6. Mutate newly created individuals.
                    kids = mutation(kids, _numMutations, gs);

                    //6a. repair
                    //kids = repairGenomes(kids, gs,pf);

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


        return bestIndividual(gs);
    }

    private int findPopulationSize(GameState gs)
    {
        int n =1;

        for(Unit u : gs.getUnits())
        {
            if(u.getPlayer() == _playerID)
            {
                n*=u.getUnitActions(gs).size();
            }
        }
        return n<_populationSize?n:_populationSize;
    }

    private boolean generatePopulation(GameState gs) throws Exception
    {
        boolean choicesThisCycle = genomesGenerator.reset(gs, _playerID);

        if(!choicesThisCycle)
            return false; //there is no available action for this player

        genomesGenerator.randomizeOrder();

        for(int i=0;i<findPopulationSize(gs);i++)
        {
            //generate new genome and add it to population
            Genome newGenome = new Genome();
            newGenome.ID = idGenerator;
            idGenerator++;

            newGenome.genes = genomesGenerator.getRandom(newGenome.abstractActions);
            _population.individuals.add(newGenome);
        }
        return true; //there are some available actions for this player
    }

    void evaluatePopulation(GameState gs)
    {
        for(int index = 0; index < _population.individuals.size(); index++)
        {

            //evaluate the sequence of playeractions

            int maxTime = 0;//pick the longest action
            //int minTime = 1000;//pick the shortest action

            //gets the duration for the longest or shortest unit action. Loop has to be modified.
            for (Pair <Unit,UnitAction> uaa:_population.individuals.get(index).genes.getActions())
            {
                if(uaa.m_b.ETA(uaa.m_a)>maxTime)
                //if(uaa.m_b.ETA(uaa.m_a)<minTime)
                    maxTime = uaa.m_b.ETA(uaa.m_a);
            }

            //instantiate phenotype for this genome
            _population.individuals.get(index).phenotype = gs.clone();
            _population.individuals.get(index).phenotype.issue(_population.individuals.get(index).genes);

            //Creating variables to be used below
            UnitActionAssignment uaa;

            //Cycle forward by the number of cycles
            for (int i = 0; i < maxTime; i++) {
                try {
                    _population.individuals.get(index).phenotype.cycle();

                    //for (int countUA = 0; countUA <_population.individuals.get(index))
                    //Modify current PA here so that it doesnt issue the same cycle again
                    //Between cycles check for resource usage
                    //montecarlo simulate function

                }
                catch(Exception e)
                {
                    _population.individuals.get(index).phenotype.cycle();

                    int a =0;
                }
            }

            //evaluate fitness after the number of cycles
            _population.individuals.get(index).fitness = new SimpleSqrtEvaluationFunction3().base_score(_playerID,_population.individuals.get(index).phenotype);
        }

    }

/*    PriorityQueue<Genome> selectParents(int k)
    {
        //return list of k parents selected from the population

        PriorityQueue<Genome> parents = new PriorityQueue<>();
        for (int i = 0; i <k;i++)
        {
            parents.add(_population.individuals.poll());

        }
        return parents;
    }*/

    ArrayList<Genome> selectParents(int k)
    {
        //return list of k parents selected from the population

        ArrayList<Genome> parents = new ArrayList<>();

        Collections.sort(_population.individuals);

        for (int i = 0; i <k;i++)
        {
            parents.add(_population.individuals.get(_population.individuals.size()-i-1));// starting from highest
            //index so picking the best parents.
            //Note to self - write clearer code. This wont make sense to you in the morning.
        }
        return parents;
    }

    /*  ArrayList<Pair<Genome,Genome>> pairIndividuals(PriorityQueue<Genome> parents)
      {
          //pick right number of parents from the parameter list and make pairs using roullete or tournaments
          //For now this is pairing the best together
          //For future, we might pair differently to introduce diversity
          ArrayList <Pair<Genome,Genome>> pairs = new ArrayList<>();
          for(int i = 0; i < parents.size(); i+=2) {
              pairs.add(new Pair <>(parents.poll(),parents.poll()));
          }
          return pairs;
      }
  */
    ArrayList<Pair<Genome,Genome>> pairIndividuals(ArrayList<Genome> parents)
    {
        //pick right number of parents from the parameter list and make pairs using roullete or tournaments
        //For now this is pairing the best together
        //For future, we might pair differently to introduce diversity
        ArrayList <Pair<Genome,Genome>> pairs = new ArrayList<>();
        //Sorting should not even be required as they are inserted after being sorted
        for(int i = 0; i<parents.size(); i+=2) {
            pairs.add(new Pair <>(parents.get(parents.size()-i-1),parents.get(parents.size()-i-2)));
        }
        return pairs;
    }

    ArrayList<Genome> crossover(ArrayList<Pair<Genome,Genome>> couples)
    {
        //perform crossover on given pairs
        ArrayList<Genome> kids = new ArrayList<>();
        Crossover crossover = new Crossover();

        for(Pair<Genome,Genome> parents: couples)
        {
            Genome kid = new Genome();
            kid.ID = idGenerator;
            idGenerator++;
            kid.genes = crossover.uniformCrossover(parents);
            kids.add(kid);
        }

        return kids;
    }

    void repairGenomes(GameState gs, PathFinding pf)
    {
//        ArrayList <Genome> l = new ArrayList(_population.individuals);
        repairGenomes(_population.individuals, gs, pf);
//        _population.individuals = new PriorityQueue<>();
        //_population.individuals.addAll(l);
        //Hack. Get this fixed. Either repeat the whole function, or change to consistent array list / pq usage

    }

    ArrayList<Genome> repairGenomes(ArrayList<Genome> genomes, GameState gs, PathFinding pf) {
        //Get physical game state
        PhysicalGameState pgs = gs.getPhysicalGameState();

        //flags to be used later
        boolean consistent = false; boolean conflictingPos = false;

        //The list of Repaired Genomes to be returned
        ArrayList<Genome> returnedGenomes = new ArrayList<>(genomes);

        int index = 0; int size = genomes.size();

        //Looping through every genome (playerAction)
        while (index < size) {

            ArrayList <Pair<Unit, UnitAction>> uuaList = new ArrayList<>(genomes.get(index).genes.getActions()) ;
            //Looping through every Unit, UnitAction pair (gene) in that genome
            for (Pair<Unit, UnitAction> uua : uuaList) {

                //Resource Usage for current action
                ResourceUsage ru = uua.m_b.resourceUsage(uua.m_a, pgs);

                //Possibly check here for conflicting positions when adding new units
                //Check this unitAction's position with every other units in the genome
               /* if (uua.m_b.getType() == rts.UnitAction.TYPE_PRODUCE) //produces a unit in the target direction
                {
                    int targetx = uua.m_a.getX();
                    int targety = uua.m_a.getY();
                    switch (uua.m_b.getDirection()) {
                        case rts.UnitAction.DIRECTION_UP:
                            targety--;
                            break;
                        case rts.UnitAction.DIRECTION_RIGHT:
                            targetx++;
                            break;
                        case rts.UnitAction.DIRECTION_DOWN:
                            targety++;
                            break;
                        case rts.UnitAction.DIRECTION_LEFT:
                            targetx--;
                            break;
                    }
                    //check if targetx and targety are free

                    //Iterating all units in the gs
                    //Note - ask Diego/Marta/Sandeep if there is a func for it
                    for (Unit existingUnit : gs.getUnits()) {
                        if (targetx == existingUnit.getX() && targety == existingUnit.getY())
                        {
                            conflictingPos = true;
                            break;
                        }
                    }

                    // find a new targetx and targety
                    int newPos = findPosition(gs.getPlayer(_playerID),pgs, 3);

                    if(conflictingPos)
                    //if there was a conflict in position for Produce
                    {
                        //UnitAction move = pf.findPathToAdjacentPosition(uua.m_a, targetx+targety*pgs.getWidth(), gs, ru);
                        UnitAction move = pf.findPathToAdjacentPosition(uua.m_a,newPos*pgs.getWidth(), gs, ru);
                        //try to set new parameter for unit action???
                        returnedGenomes.get(index).genes.removeUnitAction(uua.m_a,uua.m_b);
                        returnedGenomes.get(index).genes.addUnitAction(uua.m_a, move);

                    }
                }*/

                if (!(returnedGenomes.get(index).genes.consistentWith(ru, gs)) || !(uua.m_a.canExecuteAction(uua.m_b, gs)) )//Legality checks
                {
                    Random r = new Random();
                    {
                        List<Pair<Unit, List<AbstractAction>>> choices = genomesGenerator.getChoices();

                        // COPIED FROM GET RANDOM
                        for (Pair<Unit, List<AbstractAction>> unitChoices : choices) {

                            if (unitChoices.m_a == uua.m_a) {
                                List<AbstractAction> l = new LinkedList<AbstractAction>();
                                l.addAll(unitChoices.m_b);
                                Unit u = unitChoices.m_a;
                                do {
                                    GameState gsCopy = gs.clone();

                                    AbstractAction aa;
                                    UnitAction ua;

                                    if(l.size()>0) {
                                        aa = l.remove(r.nextInt(l.size()));
                                        ua = aa.execute(gsCopy);
                                    }
                                    else ua = new UnitAction(UnitAction.TYPE_NONE);

                                    if (ua != null) {
                                        ResourceUsage r2 = ua.resourceUsage(u, pgs);
                                        if (returnedGenomes.get(index).genes.getResourceUsage().consistentWith(r2, gs)) {
                                            returnedGenomes.get(index).genes.getResourceUsage().merge(r2);// put checks here again and see what to do with this
                                            returnedGenomes.get(index).genes.removeUnitAction(u, uua.m_b);
                                            returnedGenomes.get(index).genes.addUnitAction(u, ua);
                                            consistent = true;
                                        }
                                    }
                                } while (!consistent);
                            }
                        }
                    }
                }
            }
            index++;
        }
        return returnedGenomes;
    }

    ArrayList<Genome> mutation(ArrayList<Genome> individuals, int numMutations, GameState gs)
    {
        ArrayList<Genome> mutatedIndividuals = new ArrayList<>();

        for (Genome g : individuals)
        {

            for (int i = 0; i <= numMutations; i++)
            {
                // Pick a random number to select a random gene
                int picked = new Random().nextInt(g.genes.getActions().size());

                // picks a random gene from the genome
                Pair<Unit, UnitAction> genePicked = g.genes.getActions().get(picked);

                // Extract unit from the gene
                Unit unit = genePicked.m_a;

                // If we want to ensure that the same action is not picked again
                UnitAction ua = genePicked.m_b;

                // All possible actions for the unit
                List<UnitAction> listOfActions = unit.getUnitActions(gs);

                // Remove the action that is already set from the list so that it is not picked again
                listOfActions.remove(ua);

                // Select a random action from the units possible actions
                UnitAction newUnitAction = listOfActions.get(new Random().nextInt(listOfActions.size()));

                // Set new action to the gene
                genePicked.m_b = newUnitAction;

                // Update the gene within the genome
                g.genes.getActions().set(picked, genePicked);

                mutatedIndividuals.add(g);
            }
        }

        return mutatedIndividuals;
    }

    Genome mutate(Genome genome)
    {
        //perform mutation on genome and return it
        return genome;
    }

    PlayerAction bestIndividual( GameState gs)
    {
        //evaluate the population and pick the best individual to be returned as a result of the analysis
        evaluatePopulation(gs);
        Collections.sort(_population.individuals);
//        return _population.individuals.poll().genes;
        return _population.individuals.get(_population.individuals.size()-1).genes;
    }


    @Override
    public QMStrategy clone() {
        return new OEP(TIME_BUDGET,ITERATIONS_BUDGET,_populationSize, _lookahead);
    }

    // COPIED FUNCTION//
    /**
     * Finds a position at 'displacement' cells away from the one of the player's bases. Iteratively tries to find
     * an empty spot along the way.
     * @param p Player whose base will be used as a starting point.
     * @param pgs Game state
     * @return The position found. -1 if no positions are free.
     */
    private int findPosition(Player p, PhysicalGameState pgs, int displacement)
    {
        int basePos = -1;
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() == p.getID() && u.getType().isStockpile) {
                basePos = u.getPosition(pgs);
            }
        }

        if(basePos == -1) return -1;

        boolean placed = false;
        boolean[][] free = pgs.getAllFree();
        while(!placed)
        {
            basePos += displacement;
            int x = basePos % pgs.getWidth();
            int y = basePos / pgs.getWidth();

            if(basePos > pgs.getWidth()*pgs.getHeight())
                break;

            if (free[x][y])
                placed = true;
        }

        if(placed)
            return basePos;
        return -1;
    }

}

