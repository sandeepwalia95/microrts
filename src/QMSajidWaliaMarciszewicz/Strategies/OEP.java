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

import java.lang.reflect.GenericArrayType;

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
    private int _numMutations = 1;

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
        this._population = new Population();
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
                repairGenomes(gs);//pass something here too

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
                    ArrayList<Genome> kids = crossover(couples);//kids' datatype should be same as population
                    //5a. repair
                    kids = repairGenomes(kids, gs);
                    //6. Mutate newly created individuals.
                    kids = mutation(kids, _numMutations, gs);
                    //6a. repair
                    kids = repairGenomes(kids, gs);
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
            int maxTime = 0;
            for (Pair <Unit,UnitAction> uaa:g.genes.getActions())
            {
                if(uaa.m_b.ETA(uaa.m_a)>maxTime)
                    maxTime = uaa.m_b.ETA(uaa.m_a);
            }
            for (int i = 0; i < maxTime; i++) {
                g.phenotype.cycle();
            }
            g.phenotype.cycle();
            g.fitness = new SimpleSqrtEvaluationFunction3().base_score(_playerID,g.phenotype);
        }

    }

    PriorityQueue<Genome> selectParents(int k)
    {
        //return list of k parents selected from the population

        PriorityQueue<Genome> parents = new PriorityQueue<>();
        for (int i = 0; i < (2/3*_population.individuals.size());i++)
        {
            parents.add(_population.individuals.poll());
            parents.add(_population.individuals.poll());

        }
        return parents;
    }

    ArrayList<Pair<Genome,Genome>> pairIndividuals(PriorityQueue<Genome> parents)
    {
        //pick right number of parents from the parameter list and make pairs using roullete or tournaments
        ArrayList <Pair<Genome,Genome>> pairs = new ArrayList<>();
        for(int i = 0; i < parents.size(); i+=2) {
            pairs.add(new Pair <>(parents.poll(),parents.poll()));
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

    void repairGenomes(GameState gs)
    {
        ArrayList <Genome> l = new ArrayList(_population.individuals);
        repairGenomes(l, gs);
        _population.individuals = new PriorityQueue<>();
        _population.individuals.addAll(l);
        //Hack. Get this fixed. Either repeat the whole function, or change to consistent array list / pq usage

    }

    ArrayList<Genome> repairGenomes(ArrayList<Genome> genomes, GameState gs)
    {
        PhysicalGameState pgs = gs.getPhysicalGameState();

        for(Genome g:genomes)
        {
            for (Pair<Unit, UnitAction> uua : g.genes.getActions())
            {
                ResourceUsage ru = uua.m_b.resourceUsage(uua.m_a, pgs);
                if (!(!g.genes.consistentWith(ru, gs) || uua.m_a.canExecuteAction(uua.m_b, gs)))//Legality checks
                {
                    List<Pair<Unit,List<AbstractAction>>> choices =  genomesGenerator.getChoices();
                    Random r = new Random();

                    //copying over to get random action assigned to that unit
                    for (Pair<Unit, List<AbstractAction>> unitChoices : choices)
                    {
                        if (unitChoices.m_a == uua.m_a)
                        {
                            List<AbstractAction> l = new LinkedList<AbstractAction>();
                            l.addAll(unitChoices.m_b);
                            AbstractAction aa = l.remove(r.nextInt(l.size()));
                            GameState gsCopy = gs.clone();
                            UnitAction ua = aa.execute(gsCopy);
                            ResourceUsage r2 = ua.resourceUsage(unitChoices.m_a, pgs);

                            if (g.genes.getResourceUsage().consistentWith(r2, gs)) {
                                g.genes.getResourceUsage().merge(r2);
                                g.genes.addUnitAction(unitChoices.m_a, ua);
                            }
                        }
                    }
                }
            }
        }        return genomes;
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
