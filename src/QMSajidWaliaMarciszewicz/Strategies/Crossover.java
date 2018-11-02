package QMSajidWaliaMarciszewicz.Strategies;

import rts.*;
import rts.units.Unit;
import util.Pair;
import java.util.*;

/**
 * Class responsible for delivering different methods of crossover used in the OEP algorithm.
 */
class Crossover {

    /**
     * Object used for generating random values.
     */
    private Random r;

    /**
     * Parameter deciding whether we are picking UnitAction.TYPE_NONE for an illegal action or we are trying to find some
     * other legal action at random.
     * TRUE - pick other random function
     * FALSE - return TYPE_NONE action
     */
    private boolean _replacementStrategy;

    /**
     * Constructor of a class initiating its fields and parameters.
     *
     * @param strategy parameter deciding whether we are picking UnitAction.TYPE_NONE for an illegal action or we are trying to find some
     * other legal action at random.
     */
    Crossover(boolean strategy)
    {
        r= new Random(System.currentTimeMillis());
        _replacementStrategy = strategy;
    }

    /**
     * Method used to update resources of a PlayerAction object, so that they reflect current game state.
     *
     * @param pa PlayerAction object to be updated
     * @param gs current game state
     * @param pgs current physical game state
     */
    private void gatherResources(PlayerAction pa, GameState gs, PhysicalGameState pgs)
    {
        for (Unit u : pgs.getUnits()) {
            UnitActionAssignment uaa = gs.getUnitActions().get(u);
            if (uaa != null) {
                ResourceUsage ru = uaa.action.resourceUsage(u, pgs);
                pa.getResourceUsage().merge(ru);
            }
        }
    }

    /**
     * Method used to pick the action to replace the illegal action in a PlayerAction object. Depending on the parameter
     * passed to the class in a constructor it either return UnitAction.TYPE_NONE or iterates through all the possible
     * actions for a particular unit picking one of them at random.
     *
     * @param u unit for which we are picking the action
     * @param pa PlayerAction object which contains all the actions and units we are currently analysing
     * @param gs current game state
     * @param pgs current physical game state
     * @return UnitAction to be assigned to a Unit instead of the illegal action assigned previously to a unit
     */
    private UnitAction pickReplacementAction(Unit u, PlayerAction pa, GameState gs, PhysicalGameState pgs)
    {
        UnitAction action;
        if(_replacementStrategy)
        {
            //get the list of all available actions for the uni
            List<UnitAction> listOfActions = u.getUnitActions(gs);
            boolean consistent = false;
            Random r = new Random(System.currentTimeMillis());
            do {
                if(listOfActions.size()==0)
                    action = new UnitAction(UnitAction.TYPE_NONE); //return action NONE if no available actions are left
                else
                    action = listOfActions.remove(r.nextInt(listOfActions.size())); //get randomly picked action from a list

                //check whether newly picked action is consistent with the resource usage in the PlayerAction object and current game state
                ResourceUsage res = action.resourceUsage(u, pgs);

                if (pa.getResourceUsage().consistentWith(res, gs)) 		{
                    pa.getResourceUsage().merge(res);
                    consistent = true;
                }
            } while (!consistent);
        }
        else
            action = new UnitAction(UnitAction.TYPE_NONE); //return action NONE
        return  action;
    }


    /**
     * Function used to execute uniform crossover. (Each gene in a genome is picked randomly from one of the parents.)
     * @param parents pair of Genomes used for creation of a new individual
     * @param gs current game state
     * @return newly created individual
     */
    PlayerAction uniformCrossover(Pair<OEP.Genome, OEP.Genome> parents, GameState gs)
    {
        //create a genes sequence for the child
        PlayerAction genesSequence = new PlayerAction();
        //take the physical state of the game
        PhysicalGameState pgs = gs.getPhysicalGameState();
        //collecting resources that are currently used in the game state
        gatherResources(genesSequence,gs,pgs);

        //iterating through all the genes in a sequence
        for(int i=0;i<parents.m_a.genes.getActions().size();i++)
        {
            //take the unit from the position i
            Unit u = parents.m_a.genes.getActions().get(i).m_a;
            UnitAction action;

            //take the UnitAction
            if(r.nextInt(20)>10)
                action = parents.m_a.genes.getAction(u);             //parent A
            else
                action = parents.m_b.genes.getAction(u);            //parent B

            //check if the new action for the unit is consistent
            ResourceUsage r2 = action.resourceUsage(u, pgs);
            if (!genesSequence.getResourceUsage().consistentWith(r2, gs)) {
                action = pickReplacementAction(u,genesSequence,gs,pgs); //pick a replacement action for illegal action
            }

            //merge resources for the new action
            genesSequence.getResourceUsage().merge(action.resourceUsage(u, pgs));
            //add new pair to the genes sequence (PlayerAction)
            genesSequence.addUnitAction(u,action);
        }

        return genesSequence;
    }

    /**
     * Function used to execute single point crossover. (Pick position in the genome and copy one part of it from first
     * parent and the rest from the second one.)
     * @param parents pair of Genomes used for creation of a new individual
     * @param gs current game state
     * @return newly created individual
     */
    PlayerAction singlePointCrossover(Pair<OEP.Genome, OEP.Genome> parents, GameState gs)
    {
        //pick the position in a genes sequence at which we are going to mix parents' genomes
        int position=0;
        if(parents.m_b.genes.getActions().size()>1)
            position = 1+ r.nextInt(parents.m_b.genes.getActions().size()-1); //choose a position at individual at random

        //create a genes sequence for the child
        PlayerAction genesSequence = new PlayerAction();
        //take the physical state of the game
        PhysicalGameState pgs = gs.getPhysicalGameState();
        //collecting resources that are currently used in the game state
        gatherResources(genesSequence,gs,pgs);

        //iterating through all the genes in a sequence
        for(int i=0;i<parents.m_b.genes.getActions().size();i++)
        {
            //take the unit from the position i
            Unit u = parents.m_a.genes.getActions().get(i).m_a;
            UnitAction action;


            if(i<position)
                action = parents.m_a.genes.getAction(u); // gene from parent A
            else    //parent B
                action = parents.m_b.genes.getAction(u);

            //check if the new action for the unit is consistent
            ResourceUsage r2 = action.resourceUsage(u, pgs);
            if (!genesSequence.getResourceUsage().consistentWith(r2, gs)) {
                action = pickReplacementAction(u,genesSequence,gs,pgs); //pick the replacement action for the illegal action
            }

            //merge resources for the new action
            genesSequence.getResourceUsage().merge(action.resourceUsage(u, pgs));
            //add new pair to the genes sequence (PlayerAction)
            genesSequence.addUnitAction(u,action);
        }

        return genesSequence;
    }

    /**
     * Function used to execute n-point crossover. (Pick n positions in the genome and randomly decided which
     * parts between positions should be copied from first parent and which from the second one.)
     * @param parents pair of Genomes used for creation of a new individual
     * @param n number of positions picked in the Genome
     * @param gs current game state
     * @return newly created individual
     */
    PlayerAction nPointsCrossover(Pair<OEP.Genome, OEP.Genome> parents, int n, GameState gs)
    {
        //pick n positions
        //first element of the pair is the position
        //second element of the pair is: true - take gene from parent A, false - take gene from parent B
        Map<Integer, Boolean> positions = new HashMap<>();
        while(positions.size()<n && positions.size()!=(parents.m_b.genes.getActions().size()-1))
        {
            int pos =0;
            if(parents.m_b.genes.getActions().size()>1)
                pos = 1+ r.nextInt(parents.m_b.genes.getActions().size()-1);
            if(pos!=0 && !positions.keySet().contains(pos))
                positions.put(pos, r.nextInt(20) > 10);
        }
        positions.put(parents.m_b.genes.getActions().size(), r.nextInt(20) > 10); //add last index - only for the implementation purpose

        //sorting the list of positions
        Map<Integer,Boolean> treePositions = new TreeMap<>(positions);

        //create a genes sequence for the child
        PlayerAction genesSequence = new PlayerAction();
        //take the physical state of the game
        PhysicalGameState pgs = gs.getPhysicalGameState();
        //collecting resources that are currently used in the game state
        gatherResources(genesSequence,gs,pgs);
        int a,b=0; //boundaries of the section to be moved to a kid
        for(Integer pos: treePositions.keySet())
        {
            a=b;
            b=pos;

            for(int i=a;i<b;i++)
            {
                //take the unit from the position i
                Unit u = parents.m_a.genes.getActions().get(i).m_a;
                UnitAction action;

                if( positions.get(pos))
                    action = parents.m_a.genes.getAction(u); // gene from parent A
                else
                    action = parents.m_b.genes.getAction(u); // gene from parent B

                //check if the new action for the unit is consistent
                ResourceUsage r2 = action.resourceUsage(u, pgs);
                if (!genesSequence.getResourceUsage().consistentWith(r2, gs)) {
                    action = pickReplacementAction(u,genesSequence,gs,pgs); //pick the replacement action for illegal action
                }

                //merge resources for the new action
                genesSequence.getResourceUsage().merge(action.resourceUsage(u, pgs));
                //add new pair to the genes sequence (PlayerAction)
                genesSequence.addUnitAction(u,action);
            }
        }

        return genesSequence;
    }

}
