package QMSajidWaliaMarciszewicz.Strategies;

import ai.abstraction.AbstractAction;
import ai.abstraction.pathfinding.PathFinding;
import exercise8.PlayerAbstractActionGenerator;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import rts.GameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;
import util.Pair;

import java.util.*;

public class OEP extends QMStrategy {

    private int idGenerator =0;
    private int TIME_BUDGET;
    private int ITERATIONS_BUDGET;
    private int _populationSize;

    private int _playerID;
    PlayerAbstractActionGenerator genomesGenerator = null;

    //parameter used for selection of parents for new generation
    private int _kparents=0;
    private int _lookahead; //how far in future are we looking/ how long is the genome

    public class Population{
        PriorityQueue<Genome> individuals; //maybe better if some priority queue not just simple list
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
    }

    @Override
    public PlayerAction execute(int player, GameState gs, UnitTypeTable utt, PathFinding pf) throws Exception {

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
                repairGenomes();

                while(true) {
                    if (TIME_BUDGET>0 && (System.currentTimeMillis() - start)>=TIME_BUDGET) break;
                    if (ITERATIONS_BUDGET>0 && nruns>=ITERATIONS_BUDGET) break;

                    //2. Evaluate population
                    evaluatePopulation(gs);
                    //3. Select k individuals for the new population (remove the ones with lowest fitness)
                    PriorityQueue<Genome> parents = selectParents(_kparents);
                    //4. Create pairs from selected individuals
                    ArrayList<Pair<Genome,Genome>> couples = pairIndividuals(parents);
                    //5. Crossover
                    ArrayList<Genome> kids = crossover(couples);
                    //5a. repair
                    kids = repairGenomes(kids);
                    //6. Mutate newly created individuals.
                    kids = mutation(kids);
                    //6a. repair
                    kids = repairGenomes(kids);
                    //7. Create population for t+1
                    _population.individuals = new PriorityQueue<>(parents);
                    _population.individuals.addAll(kids);

                    nruns++;
                }
            }
        } else {
            //Nothing to do: empty player action
            return new PlayerAction();
        }


        return bestIndividual();
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
        for(Genome g:_population.individuals)
        {
            //fill in phenotype feature
            g.phenotype = gs.cloneIssue(g.genes); //shouldnt I clone the gs?---------ASK
            //evaluate the sequence of playeractions

        }

    }

    PriorityQueue<Genome> selectParents(int k)
    {
        //return list of k parents selected from the population
        return new PriorityQueue<>();
    }

    ArrayList<Pair<Genome,Genome>> pairIndividuals(PriorityQueue<Genome> parents)
    {
        //pick right number of parents from the parameter list and make pairs using roullete or tournaments
        return new ArrayList<>();
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

    void repairGenomes()
    {
        for(Genome g:_population.individuals)
        {
            //check if sequence is correct, if its wrong fix the genome
        }
    }

    ArrayList<Genome> repairGenomes(ArrayList<Genome> genomes)
    {
        for(Genome g:genomes)
        {
            //check if sequence is correct, if its wrong fix the genome
        }
        return new ArrayList<>();
    }

    ArrayList<Genome> mutation(ArrayList<Genome> individuals)
    {
        ArrayList<Genome> mutatedIndividuals = new ArrayList<>();
        for (Genome g:individuals)
        {
            mutatedIndividuals.add(mutate(g));
        }
        return mutatedIndividuals;
    }

    Genome mutate(Genome genome)
    {
        //perform mutation on genome and return it
        return genome;
    }

    PlayerAction bestIndividual()
    {
        //evaluate the population and pick the best individual to be returned as a result of the analysis
        return new PlayerAction();
    }


    @Override
    public QMStrategy clone() {
        return new OEP(TIME_BUDGET,ITERATIONS_BUDGET,_populationSize, _lookahead);
    }
}
