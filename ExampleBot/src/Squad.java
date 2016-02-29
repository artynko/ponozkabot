import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bwapi.Color;
import bwapi.Game;
import bwapi.Player;
import bwapi.Position;

public class Squad implements GameEntity {

	private List<EntityWithSquad> units = new ArrayList<>();
	private List<EntityWithSquad> attackUnits = new ArrayList<>();
	private Position attackPosition = null;
	private Position averagePosition = null;
	private Position leadPosition = null;
	private Position medianPosition = null;
	private boolean inCombat = false;
	final private int id;
	private ArmyControlService armyControlService;

	public Squad(int i, ArmyControlService armyControlService) {
		this.id = i;
		this.armyControlService = armyControlService;
	}

	public void assignUnit(EntityWithSquad fightingUnit) {
		if (!fightingUnit.isSupport()) {
			attackUnits.add(fightingUnit);
		}
		units.add(fightingUnit);
	}

	public List<EntityWithSquad> getUnits() {
		return units;
	}

	public Position getAttackPosition() {
		return attackPosition;
	}

	public void setAttackPosition(Position attackPosition) {
		this.attackPosition = attackPosition;
	}

	public boolean isIdle() {
		for (EntityWithSquad entity : attackUnits) {
			if (!entity.getUnit().isIdle()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void onFrame(Game game, Player player) {
		if (attackPosition != null) {
			game.drawCircleMap(attackPosition, 5, Color.Red, true);
			game.drawTextMap(attackPosition, "S" + id);
		}
		if (averagePosition != null) {
			game.drawCircleMap(averagePosition, 5, Color.Green, true);
			game.drawTextMap(averagePosition, "S" + id);
		}
		if (leadPosition != null) {
			game.drawCircleMap(leadPosition, 5, Color.Blue, true);
			game.drawTextMap(leadPosition, "S" + id);
		}
		if (medianPosition != null) {
			game.drawCircleMap(medianPosition, 5, Color.Yellow, true);
			game.drawTextMap(medianPosition, "S" + id);
		}
		if (game.getFrameCount() % 4 == 0 && units.size() > 0) {
			// figure out median location
			List<Integer> xPositions = new ArrayList<>();
			List<Integer> yPositions = new ArrayList<>();

			// find the average location
			double dist = Double.MAX_VALUE;
			Point p = new Point(0, 0);
			for (EntityWithSquad entity : units) {
				xPositions.add(entity.getUnit().getPoint().getX());
				yPositions.add(entity.getUnit().getPoint().getY());
				if (entity.getUnit().exists()) {
					p = p.add(new Point(entity.getUnit().getPosition()));
				}
				double distance = entity.getUnit().getPosition().getDistance(attackPosition);
				if (!entity.isSupport() && distance < dist) {
					dist = distance;
					leadPosition = entity.getUnit().getPosition();
				}

			}
			Collections.sort(xPositions);
			Collections.sort(yPositions);
			Integer sizeD2 = xPositions.size() / 2;
			Integer x = xPositions.get(sizeD2);
			Integer y = yPositions.get(sizeD2);
			medianPosition = new Position(x, y);

			averagePosition = p.multiply((float) 1 / units.size()).toPosition();
			boolean underAttack = false;
			for (EntityWithSquad entity : units) {
				if (!entity.isSupport() && (entity.getUnit().isAttacking() || entity.getUnit().isUnderAttack())) {
					underAttack = true;
					break;
				}
			}
			inCombat = underAttack;
		}
	}

	@Override
	public void onEntityCreate(UnitEntity unit) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEntityDestroyed(UnitEntity unit) {
		if (units.remove(unit)) {
			attackUnits.remove(unit);
			if (attackUnits.size() == 0) {
				System.out.println("all attack units destroyed merging supports");
				armyControlService.squadDestroyedCallback(this);
			}
		}
	}

	public Position getAveragePosition() {
		return averagePosition;
	}

	public boolean isInCombat() {
		return inCombat;
	}

	public List<EntityWithSquad> getAttackUnits() {
		return attackUnits;
	}

	public Position getLeadPosition() {
		return leadPosition;
	}

	public Position getMedianPosition() {
		return medianPosition;
	}

}
