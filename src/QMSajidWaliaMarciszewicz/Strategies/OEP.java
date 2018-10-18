package QMSajidWaliaMarciszewicz.Strategies;

import ai.abstraction.pathfinding.PathFinding;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import util.Pair;

import java.util.ArrayList;

public class OEP extends QMStrategy {

    private int TIME_BUDGET;
    private int ITERATIONS_BUDGET;
    private int _populationSize;

    //parameter used for selection of parents for new generation
    private int _kparents=0;

    public class Population{
        ArrayList<Genome> individuals; //maybe better if some priority queue not just simple list
    }

    public class Genome{
        int ID;
        PlayerAction[] genes;
        GameState phenotype;
        float fitness; //result of fitness function/ evaluation of this sequence
    }

    private Population _population;


    public OEP(int timeBudget, int iterationBudget, int populationSize){
        this.TIME_BUDGET = timeBudget;
        this.ITERATIONS_BUDGET = iterationBudget;
        this._populationSize = populationSize;
    }

    @Override
    public PlayerAction execute(int player, GameState gs, UnitTypeTable utt, PathFinding pf) throws Exception {

        long start = System.currentTimeMillis();
        int nruns = 0;

        //Only execute an action if the player can execute any.
        if(gs.canExecuteAnyAction(player))
        {
            //1. Initialize the population for t=0
            generatePopulation();

            while(true) {
                if (TIME_BUDGET>0 && (System.currentTimeMillis() - start)>=TIME_BUDGET) break;
                if (ITERATIONS_BUDGET>0 && nruns>=ITERATIONS_BUDGET) break;

                //2. Evaluate population
                evaluatePopulation();
                //3. Select k individuals for the new population (remove the ones with lowest fitness)
                ArrayList<Genome> parents = selectParents(_kparents);
                //4. Create pairs from selected individuals
                ArrayList<Pair<Genome,Genome>> couples = pairIndividuals(parents);
                //5. Crossover
                ArrayList<Genome> kids = crossover(couples);
                //5a. repair
                repairGenomes();
                //6. Mutate newly created individuals.
                kids = mutation(kids);
                //6a. repair
                repairGenomes();
                //7. Create population for t+1
                _population.individuals = parents;
                _population.individuals.addAll(kids);

                nruns++;
            }

            /*
            for(Unit u : gs.getUnits())
            {
                if(u.getPlayer() == player)
                    ;
            }*/
        } else {
            //Nothing to do: empty player action
            return new PlayerAction();
        }


        return null;
    }


    void generatePopulation()
    {
        for(int i=0;i<_populationSize;i++)
        {
            //generate new genome and add it to population
        }
    }

    void evaluatePopulation()
    {
        for(Genome g:_population.individuals)
        {
            //evaluate the sequence of playeractions
        }

    }

    ArrayList<Genome> selectParents(int k)
    {
        //return list of k parents selected from the population
        return new ArrayList<>();
    }

    ArrayList<Pair<Genome,Genome>> pairIndividuals(ArrayList<Genome> parents)
    {
        //pick right number of parents from the parameter list and make pairs using roullete or tournaments
        return new ArrayList<>();
    }

    ArrayList<Genome> crossover(ArrayList<Pair<Genome,Genome>> couples)
    {
        //perform crossover on given pairs
        return new ArrayList<>();
    }

    void repairGenomes()
    {
        for(Genome g:_population.individuals)
        {
            //check if sequence is correct, if its wrong fix the genome
        }
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

    @Override
    public QMStrategy clone() {
        return new OEP(TIME_BUDGET,ITERATIONS_BUDGET,_populationSize);
    }
}
