package QMSajidWaliaMarciszewicz.Strategies;

import rts.*;
import rts.units.Unit;
import util.Pair;

import java.util.*;

public class Crossover {

    private Random r;
    private boolean _replacementStrategy; //true - pick other random function; false - return TYPE_NONE action

    public Crossover(boolean strategy)
    {
        r= new Random(System.currentTimeMillis());
        _replacementStrategy = strategy;
    }

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

    private UnitAction pickReplacementAction(Unit u, PlayerAction pa, GameState gs, PhysicalGameState pgs)
    {
        UnitAction action;
        if(_replacementStrategy)
        {
            List<UnitAction> listOfActions = u.getUnitActions(gs);
            boolean consistent = false;
            Random r = new Random(System.currentTimeMillis());
            do {
                if(listOfActions.size()==0)
                    action = new UnitAction(UnitAction.TYPE_NONE);
                else
                    action = listOfActions.remove(r.nextInt(listOfActions.size()));

                ResourceUsage res = action.resourceUsage(u, pgs);

                if (pa.getResourceUsage().consistentWith(res, gs)) 		{
                    pa.getResourceUsage().merge(res);
                    consistent = true;
                }
            } while (!consistent);
        }
        else
            action = new UnitAction(UnitAction.TYPE_NONE);
        return  action;
    }

    /**
     * Function used to execute uniform crossover returning 2 children. (Each gene in a genome is picked randomly from one of the parents.)
     * @param parents pair of Genomes used for creation of a new individual
     * @param gs current game state
     * @return list of 2 newly created individuals
     */
    ArrayList<PlayerAction> uniformCrossover2(Pair<OEP.Genome,OEP.Genome> parents, GameState gs)
    {
        //create a genes sequences for the children
        PlayerAction genesSequence1 = new PlayerAction();
        PlayerAction genesSequence2 = new PlayerAction();
        //take the physical state of the game
        PhysicalGameState pgs = gs.getPhysicalGameState();
        //collecting resources that are currently used in the game state
        gatherResources(genesSequence1,gs,pgs);
        gatherResources(genesSequence2,gs,pgs);

        //iterating through all the genes in a sequence
        for(int i=0;i<parents.m_b.genes.getActions().size();i++)
        {
            //take the unit from the position i
            Unit u = parents.m_a.genes.getActions().get(i).m_a;
            UnitAction action1;
            UnitAction action2;

            //parent A
            if(r.nextInt(20)>10) {
                action1 = parents.m_a.genes.getAction(u);             //parent A
                action2 = parents.m_b.genes.getAction(u);             //parent A
            }
            else {   //parent B
                action1 = parents.m_b.genes.getAction(u);             //parent A
                action2 = parents.m_a.genes.getAction(u);             //parent A
            }

            //check if the new action for the unit is consistent
            ResourceUsage r1 = action1.resourceUsage(u, pgs);
            if (!genesSequence1.getResourceUsage().consistentWith(r1, gs)) {
                //if it is not consistent pick action NONE
                action1 =  pickReplacementAction(u,genesSequence1,gs,pgs);
            }
            ResourceUsage r2 = action2.resourceUsage(u, pgs);
            if (!genesSequence2.getResourceUsage().consistentWith(r2, gs)) {
                //if it is not consistent pick action NONE
                action2 = pickReplacementAction(u,genesSequence2,gs,pgs);
            }

            //merge resourcs for the new ction
            genesSequence1.getResourceUsage().merge(action1.resourceUsage(u, pgs));
            genesSequence2.getResourceUsage().merge(action2.resourceUsage(u, pgs));

            //add new pair to the genes sequence (PlayerAction)
            genesSequence1.addUnitAction(u,action1);
            genesSequence2.addUnitAction(u,action2);
        }

        ArrayList<PlayerAction> kidsGenes  = new ArrayList<PlayerAction>();
        kidsGenes.add(genesSequence1); kidsGenes.add(genesSequence2);
        return kidsGenes;
    }

    /**
     * Function used to execute single point crossover returning 2 kids. (Pick poistion in the genome and copy one part of it from first
     * parent and the rest from the second one.)
     * @param parents pair of Genomes used for creation of a new individual
     * @param gs current game state
     * @return list of 2 newly created individuals
     */
    ArrayList<PlayerAction> singlePointCrossover2(Pair<OEP.Genome, OEP.Genome> parents, GameState gs)
    {
        int position=0;
        if(parents.m_b.genes.getActions().size()>1)
            position = 1+ r.nextInt(parents.m_b.genes.getActions().size()-1); //choose a position at individual at random

        //create a genes sequences for the children
        PlayerAction genesSequence1 = new PlayerAction();
        PlayerAction genesSequence2 = new PlayerAction();
        //take the physical state of the game
        PhysicalGameState pgs = gs.getPhysicalGameState();
        //collecting resources that are currently used in the game state
        gatherResources(genesSequence1,gs,pgs);
        gatherResources(genesSequence2,gs,pgs);

        //iterating through all the genes in a sequence
        for(int i=0;i<parents.m_b.genes.getActions().size();i++)
        {
            //take the unit from the position i
            Unit u = parents.m_a.genes.getActions().get(i).m_a;
            UnitAction action1;
            UnitAction action2;

            //parent A
            if(i<position){
                action1 = parents.m_a.genes.getAction(u);             //parent A
                action2 = parents.m_b.genes.getAction(u);             //parent A
            }
            else {  //parent B
                action1 = parents.m_b.genes.getAction(u);             //parent A
                action2 = parents.m_a.genes.getAction(u);             //parent A
            }

            //check if the new action for the unit is consistent
            ResourceUsage r1 = action1.resourceUsage(u, pgs);
            if (!genesSequence1.getResourceUsage().consistentWith(r1, gs)) {
                //if it is not consistent pick action NONE
                action1 =  pickReplacementAction(u,genesSequence1,gs,pgs);
            }
            ResourceUsage r2 = action2.resourceUsage(u, pgs);
            if (!genesSequence2.getResourceUsage().consistentWith(r2, gs)) {
                //if it is not consistent pick action NONE
                action2 = pickReplacementAction(u,genesSequence2,gs,pgs);
            }

            //merge resourcs for the new ction
            genesSequence1.getResourceUsage().merge(action1.resourceUsage(u, pgs));
            genesSequence2.getResourceUsage().merge(action2.resourceUsage(u, pgs));

            //add new pair to the genes sequence (PlayerAction)
            genesSequence1.addUnitAction(u,action1);
            genesSequence2.addUnitAction(u,action2);

        }

        ArrayList<PlayerAction> kidsGenes  = new ArrayList<PlayerAction>();
        kidsGenes.add(genesSequence1); kidsGenes.add(genesSequence2);
        return kidsGenes;
    }

    /**
     * Function used to execute n-point crossover returning 2 children. (Pick n positions in the genome and randomly decided which
     * parts between positions should be copied from first parent and which from the second one.)
     * @param parents pair of Genomes used for creation of a new individual
     * @param n number of positions picked in the Genome
     * @param gs current game state
     * @return list of 2 newly created individuals
     */
    ArrayList<PlayerAction> nPointsCrossover2(Pair<OEP.Genome, OEP.Genome> parents, int n, GameState gs)
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
            if(!positions.keySet().contains(pos))
                positions.put(pos, r.nextInt(20) > 10);
        }
        positions.put(parents.m_b.genes.getActions().size(), r.nextInt(20) > 10);

        //create a genes sequences for the children
        PlayerAction genesSequence1 = new PlayerAction();
        PlayerAction genesSequence2 = new PlayerAction();
        //take the physical state of the game
        PhysicalGameState pgs = gs.getPhysicalGameState();
        //collecting resources that are currently used in the game state
        gatherResources(genesSequence1,gs,pgs);
        gatherResources(genesSequence2,gs,pgs);

        int a=0,b=0; //boundaries of the section to be moved to a kid
        for(Integer pos: positions.keySet())
        {
            a=b;
            b=pos;
            for(int i=a;i<b;i++)
            {
                //take the unit from the position i
                Unit u = parents.m_a.genes.getActions().get(i).m_a;
                UnitAction action1;
                UnitAction action2;

                if(positions.get(pos))
                {
                    action1 = parents.m_a.genes.getAction(u);             //parent A
                    action2 = parents.m_b.genes.getAction(u);             //parent A
                }
                else  {  //parent B
                    action1 = parents.m_b.genes.getAction(u);             //parent A
                    action2 = parents.m_a.genes.getAction(u);             //parent A
                }

                //check if the new action for the unit is consistent
                ResourceUsage r1 = action1.resourceUsage(u, pgs);
                if (!genesSequence1.getResourceUsage().consistentWith(r1, gs)) {
                    //if it is not consistent pick action NONE
                    action1 = pickReplacementAction(u,genesSequence1,gs,pgs);
                }
                ResourceUsage r2 = action2.resourceUsage(u, pgs);
                if (!genesSequence2.getResourceUsage().consistentWith(r2, gs)) {
                    //if it is not consistent pick action NONE
                    action2 = pickReplacementAction(u,genesSequence2,gs,pgs);
                }

                //merge resourcs for the new ction
                genesSequence1.getResourceUsage().merge(action1.resourceUsage(u, pgs));
                genesSequence2.getResourceUsage().merge(action2.resourceUsage(u, pgs));

                //add new pair to the genes sequence (PlayerAction)
                genesSequence1.addUnitAction(u,action1);
                genesSequence2.addUnitAction(u,action2);
            }
        }

        ArrayList<PlayerAction> kidsGenes  = new ArrayList<PlayerAction>();
        kidsGenes.add(genesSequence1); kidsGenes.add(genesSequence2);
        return kidsGenes;
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

            //take the unitaction
            if(r.nextInt(20)>10)
                action = parents.m_a.genes.getAction(u);             //parent A
            else
                action = parents.m_b.genes.getAction(u);            //parent B

            //check if the new action for the unit is consistent
            ResourceUsage r2 = action.resourceUsage(u, pgs);
            if (!genesSequence.getResourceUsage().consistentWith(r2, gs)) {
                action = pickReplacementAction(u,genesSequence,gs,pgs);

            }

            //merge resourcs for the new ction
            genesSequence.getResourceUsage().merge(action.resourceUsage(u, pgs));
            //add new pair to the genes sequence (PlayerAction)
            genesSequence.addUnitAction(u,action);
        }

        return genesSequence;
    }

    /**
     * Function used to execute single point crossover. (Pick poistion in the genome and copy one part of it from first
     * parent and the rest from the second one.)
     * @param parents pair of Genomes used for creation of a new individual
     * @param gs current game state
     * @return newly created individual
     */
    PlayerAction singlePointCrossover(Pair<OEP.Genome, OEP.Genome> parents, GameState gs)
    {
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

            //parent A
            if(i<position)
                action = parents.m_a.genes.getAction(u);
            else    //parent B
                action = parents.m_b.genes.getAction(u);

            //check if the new action for the unit is consistent
            ResourceUsage r2 = action.resourceUsage(u, pgs);
            if (!genesSequence.getResourceUsage().consistentWith(r2, gs)) {
                //if it is not consistent pick action NONE
                action = pickReplacementAction(u,genesSequence,gs,pgs);
            }

            //merge resourcs for the new ction
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
            if(!positions.keySet().contains(pos))
                positions.put(pos, r.nextInt(20) > 10);
        }
        positions.put(parents.m_b.genes.getActions().size(), r.nextInt(20) > 10); //add last index - only for the implementation purpose

        //create a genes sequence for the child
        PlayerAction genesSequence = new PlayerAction();
        //take the physical state of the game
        PhysicalGameState pgs = gs.getPhysicalGameState();
        //collecting resources that are currently used in the game state
        gatherResources(genesSequence,gs,pgs);
        int a=0,b=0; //boundaries of the section to be moved to a kid
        for(Integer pos: positions.keySet())
        {
            a=b;
            b=pos;
            for(int i=a;i<b;i++)
            {
                //take the unit from the position i
                Unit u = parents.m_a.genes.getActions().get(i).m_a;
                UnitAction action;

                if( positions.get(pos))
                    action = parents.m_a.genes.getAction(u);
                else    //parent B
                    action = parents.m_b.genes.getAction(u);

                //check if the new action for the unit is consistent
                ResourceUsage r2 = action.resourceUsage(u, pgs);
                if (!genesSequence.getResourceUsage().consistentWith(r2, gs)) {
                    //if it is not consistent pick action NONE
                    action = pickReplacementAction(u,genesSequence,gs,pgs);
                }

                //merge resourcs for the new ction
                genesSequence.getResourceUsage().merge(action.resourceUsage(u, pgs));
                //add new pair to the genes sequence (PlayerAction)
                genesSequence.addUnitAction(u,action);
            }
        }

        return genesSequence;
    }

}
