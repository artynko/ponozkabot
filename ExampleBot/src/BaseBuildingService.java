import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import bwapi.Color;
import bwapi.Game;
import bwapi.Player;
import bwapi.TilePosition;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.Region;

public class BaseBuildingService implements GameEntity {
	// a threshold for base to be still considered as viable build location (area / buildings)
	private static final int BASE_BUILDING_THRESHOLD = 40000;
	private EconomyService economyService;
	private BaseService baseService;
	private List<UnitEntity> buildings = new ArrayList<>();
	private List<BuildOrderEntry> buildOrder = new ArrayList<>();
	private Map<Builder, UnitType> orderedBuildings = new HashMap<>();

	public BaseBuildingService(EconomyService economyService, BaseService baseService) {
		this.economyService = economyService;
		this.baseService = baseService;
	}

	@Override
	public void onFrame(Game game, Player player) {
		if (buildOrder.size() == 0) {
			buildOrder.add(new BuildOrderEntry(10, UnitType.Terran_Barracks, 1, false));
			buildOrder.add(new BuildOrderEntry(12, UnitType.Terran_Barracks, 2, false));
			buildOrder.add(new BuildOrderEntry(22, UnitType.Terran_Academy, 1, false));
			buildOrder.add(new BuildOrderEntry(34, UnitType.Terran_Engineering_Bay, 1, false));
			buildOrder.add(new BuildOrderEntry(42, UnitType.Terran_Barracks, -7, false)); // if it is negative, it means build one for every X workers
			buildOrder.add(new BuildOrderEntry(60, UnitType.Terran_Factory, 1, false));
			buildOrder.add(new BuildOrderEntry(-1, UnitType.Terran_Starport, 1, false));
			buildOrder.add(new BuildOrderEntry(-1, UnitType.Terran_Control_Tower, 1, false));
			buildOrder.add(new BuildOrderEntry(-1, UnitType.Terran_Science_Facility, 1, false));
			buildOrder.add(new BuildOrderEntry(80, UnitType.Terran_Engineering_Bay, 2, false));
			buildOrder.add(new BuildOrderEntry(80, UnitType.Terran_Barracks, -6, false));
			buildOrder.add(new BuildOrderEntry(110, UnitType.Terran_Barracks, -4, false));
		}
		if (economyService.getSupply() >= 10 && game.getFrameCount() % 20 == 5) { // don't build any base until 12 supply
			// figure out what I have currently
			Map<UnitType, AtomicInteger> counts = new HashMap<>();
			for (BuildOrderEntry entry : buildOrder) {
				counts.put(entry.type, new AtomicInteger());
			}

			for (UnitEntity building : buildings) {
				counts.get(building.getUnit().getType()).incrementAndGet();
			}
			// add also ordered buildins
			for (UnitType orderedType : orderedBuildings.values()) {
				counts.get(orderedType).incrementAndGet();
			}

			// now verify what is missing 
			for (BuildOrderEntry entry : buildOrder) {
				if ((entry.supply < economyService.getSupply() || entry.supply < 0) && hasPrereqs(entry.type, counts)) { // I am not going to build anymore
					int count = entry.count;
					if (count < 0) {
						count = baseService.getBuilders().size() / (-count);
					}
					if (counts.get(entry.type).intValue() < count) {
						if (entry.type.isAddon()) {
							for (UnitEntity entity : buildings) {
								System.out.println(entity.getUnit().getType());
								if (entity.getUnit().canBuildAddon(entry.type) && entity.getUnit().getAddon() == null) {
									entity.getUnit().buildAddon(entry.type);
								}
							}
						} else {
							System.out.println("type: " + entry.type + " counts: " + counts.get(entry.type).intValue() + " count: " + count);
							Base baseToBuild = null;
							double bestRatio = 0;
							for (Base b : baseService.getBases()) {
								if (b.isCompleted() && b.getBuilderSize() > 10 && 
										b.getBuildingRatio() > bestRatio) {
									baseToBuild = b;
									bestRatio = b.getBuildingRatio();
								}
							}
							if (baseToBuild == null) {
								baseToBuild = baseService.getMain();
							}

							TilePosition tp = getBuildPosition(baseToBuild, entry.type, game, player);
							Builder b = baseToBuild.getFreeBuilder();
							baseToBuild.increaseBuildings(entry.type);
							b.build(entry.type, tp);
							orderedBuildings.put(b, entry.type);
						}
					}
				}
			}
		}

	}

	/**
	 * Figures out if all prereqs are met for given unit type
	 * 
	 * @param type
	 * @param counts
	 * @return
	 */
	public boolean hasPrereqs(UnitType type, Map<UnitType, AtomicInteger> counts) {
		if (type == UnitType.Terran_Starport) {
			return hasAtleastOne(counts, UnitType.Terran_Factory);
		} else if (type == UnitType.Terran_Control_Tower) {
			return hasAtleastOne(counts, UnitType.Terran_Starport);
		} else if (type == UnitType.Terran_Science_Facility) {
			return hasAtleastOne(counts, UnitType.Terran_Starport);
		} else if (type == UnitType.Terran_Missile_Turret) {
			return hasAtleastOne(counts, UnitType.Terran_Engineering_Bay);
		}
		return true;
	}

	private boolean hasAtleastOne(Map<UnitType, AtomicInteger> counts, UnitType type) {
		for (UnitEntity building : buildings) {
			if (building.getUnit().isCompleted() && building.getUnit().getType() == type)
				return true;
		}
		return false;
	}

	private TilePosition getBuildPosition(Base base, UnitType type, Game game, Player player) {
		TilePosition pos = null;
		if (player.allUnitCount(type) > 7 || type == UnitType.Terran_Academy || type == UnitType.Terran_Engineering_Bay || type == UnitType.Terran_Factory || type == UnitType.Terran_Starport
				|| type == UnitType.Terran_Science_Facility) {
			pos = getBuildPosition(type, game, base.getBaseLocation().getTilePosition());
		} else {
			pos = MathUtil.moveToLocation(base.getBaseLocation().getPosition(), base.getBaseLocation().getRegion().getChokepoints().get(0).getCenter(),
					TilePosition.SIZE_IN_PIXELS * 8).toTilePosition();
		}
		if (!pos.isValid()) {
			return getBuildPosition(base, type, game, player);
		}
		return pos;
	}

	@Override
	public void onEntityCreate(UnitEntity unit) {
		if (unit.getUnit().getType() == UnitType.Terran_Barracks || unit.getUnit().getType() == UnitType.Terran_Factory
				|| unit.getUnit().getType() == UnitType.Terran_Academy || unit.getUnit().getType() == UnitType.Terran_Science_Facility
				|| unit.getUnit().getType() == UnitType.Terran_Starport || unit.getUnit().getType() == UnitType.Terran_Engineering_Bay
				|| unit.getUnit().getType() == UnitType.Terran_Control_Tower) {
			buildings.add(unit);
			Builder builderToRemove = null;
			for (Builder b : orderedBuildings.keySet()) {
				if (b.getUnit().getBuildUnit().equals(unit.getUnit())) {
					builderToRemove = b;
					break;
				}
			}
			System.out.println("Removing: " + builderToRemove);
			if (builderToRemove != null) {
				orderedBuildings.remove(builderToRemove);
			}
		}
	}

	private TilePosition getBuildPosition(UnitType type, Game game, TilePosition startPosition) {
		Random random = new Random();
		Region r = BWTA.getRegion(startPosition);
		TilePosition tp = r.getPolygon().getPoints().get(random.nextInt(r.getPolygon().getPoints().size())).toTilePosition();
		// move it towards base
		tp = MathUtil.moveToLocation(tp.toPosition(), startPosition.toPosition(), TilePosition.SIZE_IN_PIXELS * 4).toTilePosition();
		game.drawCircleMap(tp.toPosition(), 5, Color.Yellow, true);
		tp = game.getBuildLocation(type, tp, 6);
		game.drawCircleMap(tp.toPosition(), 5, Color.White, true);
		return tp;
	}

	/**
	 * Will find the first occurence of a given unit type and change it
	 * 
	 * @param type
	 * @param supply
	 * @param count
	 */
	public void alterBuild(UnitType type, int supply, int count) {
		System.out.println("Altering BO: " + type + ", supply: " + supply + ", count: " + count);
		for (BuildOrderEntry boe : buildOrder) {
			if (boe.type == type) {
				boe.supply = supply;
				boe.count = count;
			}
		}
	}

	private class BuildOrderEntry {
		public UnitType type;
		public int count;
		public int supply;
		public boolean reserve;

		public BuildOrderEntry(int supply, UnitType type, int count, boolean reserve) {
			this.supply = supply;
			this.type = type;
			this.count = count;
			this.reserve = reserve;
		}

	}

	@Override
	public void onEntityDestroyed(UnitEntity unit) {
		buildings.remove(unit);
	}
}
