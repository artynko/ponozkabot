import bwapi.Game;
import bwapi.Player;
import bwapi.TechType;
import bwapi.Unit;

public class Marine extends FightingUnit {
	

	public Marine(Unit unit) {
		super(unit);
	}

	@Override
	public void onFrame(Game game, Player player) {
		if (squad.isInCombat()) {
			if (player.hasResearched(TechType.Stim_Packs)) {
				if (unit.getHitPoints() > 15 && !unit.isStimmed()) {
					unit.useTech(TechType.Stim_Packs);

				}
			}
		}
		super.onFrame(game, player);
	}

	@Override
	public boolean isSupport() {
		return false;
	}

}
