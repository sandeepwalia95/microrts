package QMSajidWaliaMarciszewicz.Strategies;

import rts.*;
import rts.units.Unit;
import util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Crossover {

    private Random r;

    public Crossover()
    {
        r= new Random(System.currentTimeMillis());
    }

    ArrayList<PlayerAction> uniformCrossover2(Pair<OEP.Genome,OEP.Genome> parents)
    {
        PlayerAction genesSequence1 = new PlayerAction();
        PlayerAction genesSequence2 = new PlayerAction();
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

            //add new pair to the genes sequence (PlayerAction)
            genesSequence1.addUnitAction(u,action1);
            genesSequence2.addUnitAction(u,action2);
        }

        ArrayList<PlayerAction> kidsGenes  = new ArrayList<PlayerAction>();
        kidsGenes.add(genesSequence1); kidsGenes.add(genesSequence2);
        return kidsGenes;
    }

    ArrayList<PlayerAction> singlePointCrossover2(Pair<OEP.Genome, OEP.Genome> parents)
    {
        int position = 1+ r.nextInt(parents.m_b.genes.getActions().size()-1); //choose a position at individual at random

        PlayerAction genesSequence1 = new PlayerAction();
        PlayerAction genesSequence2 = new PlayerAction();
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

            //add new pair to the genes sequence (PlayerAction)
            genesSequence1.addUnitAction(u,action1);
            genesSequence2.addUnitAction(u,action2);

        }

        ArrayList<PlayerAction> kidsGenes  = new ArrayList<PlayerAction>();
        kidsGenes.add(genesSequence1); kidsGenes.add(genesSequence2);
        return kidsGenes;
    }

    ArrayList<PlayerAction> nPointsCrossover2(Pair<OEP.Genome, OEP.Genome> parents, int n)
    {
        //pick n positions
        //first element of the pair is the position
        //second element of the pair is: true - take gene from parent A, false - take gene from parent B
        Map<Integer, Boolean> positions = new HashMap<>();
        while(positions.size()<n)
        {
            int pos =1+ r.nextInt(parents.m_b.genes.getActions().size()-1);
            if(!positions.keySet().contains(pos))
                positions.put(pos, r.nextInt(20) > 10);
        }
        positions.put(parents.m_b.genes.getActions().size(), r.nextInt(20) > 10);

        PlayerAction genesSequence1 = new PlayerAction();
        PlayerAction genesSequence2 = new PlayerAction();
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

                //add new pair to the genes sequence (PlayerAction)
                genesSequence1.addUnitAction(u,action1);
                genesSequence2.addUnitAction(u,action2);
            }
        }

        ArrayList<PlayerAction> kidsGenes  = new ArrayList<PlayerAction>();
        kidsGenes.add(genesSequence1); kidsGenes.add(genesSequence2);
        return kidsGenes;
    }

    PlayerAction uniformCrossover(Pair<OEP.Genome, OEP.Genome> parents, GameState gs)
    {
        Random r = new Random();

        PlayerAction genesSequence = new PlayerAction();
        PhysicalGameState pgs = gs.getPhysicalGameState();

        for (Unit u : pgs.getUnits()) {
            UnitActionAssignment uaa = gs.getUnitActions().get(u);
            if (uaa != null) {
                ResourceUsage ru = uaa.action.resourceUsage(u, pgs);
                genesSequence.getResourceUsage().merge(ru);
            }
        }

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

            boolean consistent = false;
            do {
                ResourceUsage r2 = action.resourceUsage(u, pgs);

                if (!genesSequence.getResourceUsage().consistentWith(r2, gs)) {
                    action = new UnitAction(UnitAction.TYPE_NONE);
                }
                consistent = true;
            } while (!consistent);

            genesSequence.getResourceUsage().merge(action.resourceUsage(u, pgs));
            //add new pair to the genes sequence (PlayerAction)
            genesSequence.addUnitAction(u,action);
        }

        return genesSequence;
    }

    PlayerAction singlePointCrossover(Pair<OEP.Genome, OEP.Genome> parents)
    {
        Random r = new Random();
        int position = 1+ r.nextInt(parents.m_b.genes.getActions().size()-1); //choose a position at individual at random

        PlayerAction genesSequence = new PlayerAction();
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

            //add new pair to the genes sequence (PlayerAction)
            genesSequence.addUnitAction(u,action);
        }

        return genesSequence;
    }

    PlayerAction nPointsCrossover(Pair<OEP.Genome, OEP.Genome> parents, int n)
    {
        Random r = new Random();

        //pick n positions
        //first element of the pair is the position
        //second element of the pair is: true - take gene from parent A, false - take gene from parent B
        Map<Integer, Boolean> positions = new HashMap<>();
        while(positions.size()<n)
        {
            int pos =1+ r.nextInt(parents.m_b.genes.getActions().size()-1);
            if(!positions.keySet().contains(pos))
                positions.put(pos, r.nextInt(20) > 10);
        }
        positions.put(parents.m_b.genes.getActions().size(), r.nextInt(20) > 10);

        PlayerAction genesSequence = new PlayerAction();
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

                //add new pair to the genes sequence (PlayerAction)
                genesSequence.addUnitAction(u,action);
            }
        }

        return genesSequence;
    }

}
