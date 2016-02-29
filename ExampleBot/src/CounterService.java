import java.util.HashSet;
import java.util.Set;

import bwapi.Game;
import bwapi.Player;
import bwapi.Race;
import bwapi.Unit;
import bwapi.UnitType;

public class CounterService implements GameEntity, EnemyUnitDestroyedListener, EnemyUnitDiscoveredListener {

	private MacroService macroService;
	private Race enemyRace = null;
	private Set<Unit> zealots = new HashSet<>();
	private int zealotAdjustment = 0;
	private int zealotsLastAdjusmentFrame = -10000;
	private SixPoolDefenseService sixPoolDefenseService;
	private BaseBuildingService baseBuildingService;
	private boolean lurkerAdjustment = false;

	public CounterService(MacroService macroService, SixPoolDefenseService sixPoolDefenseService, BaseBuildingService baseBuildingService) {
		this.macroService = macroService;
		this.sixPoolDefenseService = sixPoolDefenseService;
		this.baseBuildingService = baseBuildingService;
	}

	@Override
	public void onFrame(Game game, Player player) {
		// adjust based on protoss race
		if (enemyRace == null && game.enemy().getRace() != Race.Unknown) {
			handleUnitRace(game.enemy().getRace());
		}

		// adjust based on zealot count
		if (zealots.size() > 5 && zealotAdjustment == 0 && game.getFrameCount() - zealotsLastAdjusmentFrame > 1200) {
			zealotAdjustment = 1;
			macroService.alterBuild(UnitType.Terran_Firebat, +1, true);
			macroService.alterBuild(UnitType.Terran_Marine, -1, true);
		} else if (zealots.size() <= 5 && zealotAdjustment == 1 && game.getFrameCount() - zealotsLastAdjusmentFrame > 1200) {
			zealotAdjustment = 0;
			macroService.alterBuild(UnitType.Terran_Firebat, -1, true);
			macroService.alterBuild(UnitType.Terran_Marine, +1, true);
		}
	}

	private void handleUnitRace(Race race) {
		enemyRace = race;
		System.out.println("handling race: " + race);
		if (enemyRace == Race.Protoss) {
			macroService.alterBuild(UnitType.Terran_Firebat, 1);
		}
	}

	@Override
	public void onEntityCreate(UnitEntity unit) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEntityDestroyed(UnitEntity unit) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEnemyEntityDiscovered(Unit unit) {
		if (enemyRace == null) {
			handleUnitRace(unit.getType().getRace());
		}
		if (unit.getType() == UnitType.Protoss_Zealot) {
			zealots.add(unit);
		} else if (unit.getType() == UnitType.Zerg_Lurker) {
			if (!lurkerAdjustment) {
				baseBuildingService.alterBuild(UnitType.Terran_Factory, -1, 1); // build factory as soon as possible to trigger 
				lurkerAdjustment = true;
			}
		}
	}

	@Override
	public void onEnemyEntityDestoyed(Unit unit) {
		if (unit.getType() == UnitType.Protoss_Zealot) {
			zealots.remove(unit);
		}
	}

}
