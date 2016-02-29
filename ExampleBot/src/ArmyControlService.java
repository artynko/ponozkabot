import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import bwapi.Color;
import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;
import bwta.Chokepoint;

public class ArmyControlService implements GameEntity, EnemyUnitDiscoveredListener, EnemyUnitDestroyedListener {

	private List<EntityWithSquad> beingTrained = new ArrayList<>();
	private List<EntityWithSquad> army = new ArrayList<>();
	private List<Squad> squads = new ArrayList<>();
	private BaseService baseService;
	private Squad currentTrainSquad = null;
	private Position assemblePosition = null;
	private Set<UnitAndPosition> enemyUnits = new HashSet<>();
	private int squadNum = 0;
	private EconomyService economyService;

	public ArmyControlService(BaseService baseService, EconomyService economyService) {
		this.baseService = baseService;
		this.economyService = economyService;
		Squad squad = new Squad(squadNum++, this);
		squads.add(squad);
		currentTrainSquad = squad;
	}

	@Override
	public void onFrame(Game game, Player player) {
		Base base = null;
		if (baseService.basesSize() == 1) {
			base = baseService.getBases().get(0);
		} else {
			base = baseService.getBases().get(1);
		}
		Position enemyBase = getEnemyBasePosition();
		Position chokePointCenter = null;
		int distance = Integer.MAX_VALUE;
		for (Chokepoint ch : base.getBaseLocation().getRegion().getChokepoints()) {
			int currDist = (int) ch.getCenter().getDistance(enemyBase);
			if (currDist < distance) {
				distance = currDist;
				chokePointCenter = ch.getCenter();
			}

		}
		assemblePosition = MathUtil.moveToLocation(base.getBaseLocation().getPosition(), chokePointCenter, -64);
		currentTrainSquad.setAttackPosition(assemblePosition);

		Iterator<EntityWithSquad> itt = beingTrained.iterator();
		while (itt.hasNext()) {
			EntityWithSquad entity = itt.next();
			if (entity.getUnit().isCompleted()) {
				army.add(entity);
				if (entity.getUnit().getType() == UnitType.Terran_Science_Vessel) {
					if (squads.size() == 1) {
						entity.assigntToSquad(currentTrainSquad);
					} else {
						entity.assigntToSquad(squads.get(squads.size() - 2));
					}
				} else {
					entity.assigntToSquad(currentTrainSquad);
				}
				itt.remove(); // move entity to somewhere
			}
		}
		if (currentTrainSquad.getUnits().size() > getSquadSize()) {
			Position attackPos = getAttackPosition();
			currentTrainSquad.setAttackPosition(attackPos);
			Squad squad = new Squad(squadNum++, this);
			squads.add(squad);
			currentTrainSquad = squad;
		}

		for (Squad squad : squads) {
			squad.onFrame(game, player);
			// figure out if squad is idle and is nears its objective
			if (squad != currentTrainSquad) {
				if (squad.isIdle() || (game.isVisible(squad.getAttackPosition().toTilePosition()) && !hasEnemyUnits(squad.getAttackPosition(), game))) {
					squad.setAttackPosition(getAttackPosition());
				}
			}
		}

		for (UnitAndPosition up : enemyUnits) {
			game.drawCircleMap(up.getPosition(), 3, up.unit.exists() ? Color.Green : Color.Orange);
		}
	}

	private int getSquadSize() {
		if (economyService.getSupply() < 60) {
			return 16;
		} else if (economyService.getSupply() < 90) {
			return 24;
		} else {
			return 36;
		}
	}

	private boolean hasEnemyUnits(Position position, Game game) {
		for (Unit unit : game.getUnitsOnTile(position.toTilePosition())) {
			if (unit.getPlayer().equals(game.enemy())) {
				return true;
			}
		}
		return false;
	}

	private Position getAttackPosition() {
		if (enemyUnits.size() > 0) {
			UnitAndPosition unit = (UnitAndPosition) enemyUnits.toArray()[enemyUnits.size() - 1];
			return unit.getPosition();
		}
		return getEnemyBasePosition();
	}

	private Position getEnemyBasePosition() {
		Position attackPos = null;
		for (BaseLocation l : BWTA.getBaseLocations()) {
			if (l.isStartLocation()) {
				if (!baseService.getMain().getBaseLocation().getPosition().equals(l.getPosition())) {
					attackPos = l.getPosition();
				}
			}
		}
		return attackPos;
	}

	@Override
	public void onEntityCreate(UnitEntity unit) {
		if (unit instanceof EntityWithSquad) {
			beingTrained.add((EntityWithSquad) unit);
		}
	}

	@Override
	public void onEntityDestroyed(UnitEntity unit) {
		army.remove(unit);
		for (Squad squad : squads) {
			squad.onEntityDestroyed(unit);
		}

	}

	@Override
	public void onEnemyEntityDiscovered(Unit unit) {
		if (unit.getType().isBuilding()) {
			enemyUnits.add(new UnitAndPosition(unit, unit.getPosition()));
		}
	}

	@Override
	public void onEnemyEntityDestoyed(Unit unit) {
		enemyUnits.remove(new UnitAndPosition(unit, null));
	}

	public void squadDestroyedCallback(Squad squad) {
		squads.remove(squad);
		List<EntityWithSquad> tmp = new ArrayList<>();
		tmp.addAll(squad.getUnits());
		for (EntityWithSquad e : tmp) {
			e.assigntToSquad(currentTrainSquad);
		}

	}

	private class UnitAndPosition {
		private Unit unit;
		private Position position;

		public UnitAndPosition(Unit unit, Position position) {
			this.unit = unit;
			this.position = position;
		}

		public Position getPosition() {
			return position;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof UnitAndPosition))
				return false;

			UnitAndPosition other = (UnitAndPosition) obj;
			return this.unit.equals(other.unit);
		}

		@Override
		public int hashCode() {
			return unit.hashCode();
		}

	}

}
