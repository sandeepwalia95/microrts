package QMSajidWaliaMarciszewicz.Solutions;

import ai.abstraction.pathfinding.PathFinding;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public abstract class Solution {
    abstract public PlayerAction execute(int player, GameState gs, UnitTypeTable utt, PathFinding pf) throws Exception;
}
