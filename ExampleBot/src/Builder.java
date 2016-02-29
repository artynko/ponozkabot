import java.util.Random;

import bwapi.Color;
import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitCommandType;
import bwapi.UnitType;

public class Builder extends AbstractUnitEntity {
	private boolean reserved = false;
	private Base base;
	private Random r = new Random();
	private EconomyService economyService;
	private Unit building = null;
	private UnitType typeToBuild = null;
	private TilePosition buildLocation;
	private int currentSpread = 0;

	public Builder(Unit unit, EconomyService economyService) {
		super(unit);
		this.economyService = economyService;
	}

	@Override
	public void onFrame(Game game, Player player) {
		if (unit.getPosition().getDistance(base.getBaseLocation().getPosition()) < 120) {
			if (unit.getLastCommand().getUnitCommandType() != UnitCommandType.Attack_Move) {
				for (Unit u : game.getUnitsInRadius(unit.getPosition(), 64)) {
					if (u.getPlayer().equals(game.enemy())) {
						System.out.println("Enemey close attacking with builders");
						unit.attack(unit.getPosition());
						return;
					}
				}
			}
		}
		if (!reserved && base != null) {
			if (unit.isIdle() && base.getBaseLocation().getMinerals().size() > 0) { // TODO: if size is 0 transfer workers to another base
				Unit mineral = base.getBaseLocation().getMinerals().get(r.nextInt(base.getBaseLocation().getMinerals().size()));
				if (mineral != null) {
					unit.gather(mineral, false);
				}
			}
		}

		if (typeToBuild != null) {
			game.drawLineMap(unit.getPosition(), buildLocation.getPoint().toPosition(), Color.White);
			if (building != null) {
				unit.rightClick(building);
				game.drawTextScreen(100, 160, "Building created: " + typeToBuild);
				if (building.isCompleted()) { // this means it finished
												// constructing
					System.out.println("Building completed: " + typeToBuild);
					if (building.getType() != UnitType.Terran_Refinery)
						setReserved(false);
					building = null;
					typeToBuild = null;
					currentSpread = 1;
					return;
				}
			}
			if (unit.isConstructing())
				return;
			if (typeToBuild != UnitType.Terran_Command_Center) { // for command centers they just must be placed on the correct spot
				if (game.canBuildHere(buildLocation, typeToBuild)) {
					drawBox(game, buildLocation, typeToBuild.width(), typeToBuild.height());
				} else {
					game.drawTextScreen(100, 120, "Can't find build location for " + typeToBuild);
					TilePosition newBuildLocation = game.getBuildLocation(typeToBuild, buildLocation, currentSpread); // first try if we can't find a better location nearby
					if (!newBuildLocation.isValid()) { // still invalid then pick a completely new build location
						System.out.println("Spread:" + currentSpread + " type: " + typeToBuild + " position-x: " + buildLocation.toPosition().getX() + " y: "
								+ newBuildLocation.toPosition().getY());
						currentSpread++;
						if (currentSpread > 500)
							currentSpread = 0;
						return;
					} else {
						System.out.println("Spread:" + currentSpread + " type: " + typeToBuild + " position-x: " + buildLocation.toPosition().getX() + " y: "
								+ buildLocation.toPosition().getY());
						buildLocation = newBuildLocation;
					}
				}
			}
			// send him to build position
			if (economyService.getMinerals() >= typeToBuild.mineralPrice() - 20 && !unit.isConstructing()
					&& buildLocation.toPosition().getDistance(unit.getPosition()) > -(TilePosition.SIZE_IN_PIXELS * 3) + 16) {
				Position dest = MathUtil.moveToLocation(unit.getPosition(), buildLocation.toPosition(), -(TilePosition.SIZE_IN_PIXELS * 3));
				game.drawCircleMap(dest, 5, Color.Yellow);
				unit.move(dest);
			}
			// if he is not constructing yet give the construct order
			if (!unit.isConstructing() && economyService.getMinerals() >= typeToBuild.mineralPrice()) {
				unit.build(typeToBuild, buildLocation);
			}
		}
	}

	public void assignToBase(Base base) {
		this.base = base;
	}

	/**
	 * Reserves itsef and tries to build a building at position
	 * 
	 * @param unitType
	 * @param position
	 * @param minerals
	 */
	public void build(UnitType unitType, TilePosition position) {
		typeToBuild = unitType;
		buildLocation = position;
		economyService.reserveMinerales(unitType.mineralPrice());
		currentSpread = 1;
		setReserved(true);
	}

	public boolean isReserved() {
		return reserved;
	}

	public void setReserved(boolean reserved) {
		this.reserved = reserved;
	}

	@Override
	public void onEntityCreate(UnitEntity entity) {
		Unit createdUnit = entity.getUnit();
		if (createdUnit.getType() == typeToBuild && unit == createdUnit.getBuildUnit()) {
			building = createdUnit;
			economyService.freeMinerals(typeToBuild.mineralPrice());
		}

	}

	private void drawBox(Game game, TilePosition tp, int sizeX, int sizeY) {
		game.drawBoxMap(tp.toPosition(), new Position(tp.toPosition().getX() + sizeX, tp.toPosition().getY() + sizeY), Color.White);
	}

	@Override
	public void onEntityDestroyed(UnitEntity unit) {
		if (unit.getUnit().equals(this.unit)) {
			if (building == null) {
				build(typeToBuild, buildLocation);
			} else {
				Builder freeBuilder = base.getFreeBuilder();
				freeBuilder.typeToBuild = typeToBuild;
				freeBuilder.buildLocation = buildLocation;
				freeBuilder.currentSpread = 1;
				freeBuilder.building = building;
				freeBuilder.setReserved(true);
				freeBuilder.getUnit().stop();
				freeBuilder.getUnit().rightClick(building);
			}
		}
	}
}
