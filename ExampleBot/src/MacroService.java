import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import bwapi.Game;
import bwapi.Player;
import bwapi.Race;
import bwapi.UnitType;

public class MacroService implements GameEntity {

	private EconomyService economyService;
	private List<UnitEntity> buildings = new ArrayList<>();
	private List<TypeWithCount> toBuild = new ArrayList<>();
	private Map<UnitType, AtomicInteger> counts = new HashMap<>();

	public MacroService(EconomyService economyService) {
		this.economyService = economyService;
	}

	@Override
	public void onFrame(Game game, Player player) {
		if (toBuild.isEmpty()) {
			toBuild.add(new TypeWithCount(UnitType.Terran_Marine, 3, false));
			toBuild.add(new TypeWithCount(UnitType.Terran_Medic, 1, false));
			toBuild.add(new TypeWithCount(UnitType.Terran_Firebat, 0, false));
			toBuild.add(new TypeWithCount(UnitType.Terran_Science_Vessel, 4, true));
			for (TypeWithCount twc : toBuild) {
				counts.put(twc.type, new AtomicInteger(0));
			}
		}
		for (UnitEntity entity : buildings) {

			Map<UnitType, AtomicInteger> internalCounts = new HashMap<>();
			for (Entry<UnitType, AtomicInteger> entry : counts.entrySet()) {
				internalCounts.put(entry.getKey(), new AtomicInteger(entry.getValue().intValue()));
			}

			if (!entity.getUnit().isTraining() && entity.getUnit().isCompleted()) {
				// get what to build
				// first count what I have currently
				double multiplier = 0;
				for (TypeWithCount twc : toBuild) {
					if (twc.count > 0 && internalCounts.containsKey(twc.type)) {
						double myMultiplier = internalCounts.get(twc.type).doubleValue() / twc.count;
						if (myMultiplier > multiplier) {
							multiplier = myMultiplier;
						}
					}
				}
				UnitType unitToBuild = null;
				double maxDifference = 0;
				for (TypeWithCount type : toBuild) {
					if (type.count > 0 && game.canMake(type.type)) {
						double difference = (type.count * multiplier) - internalCounts.get(type.type).intValue();
						if ((!type.isAbsolute && difference >= maxDifference) || (type.isAbsolute && (internalCounts.get(type.type).intValue() < type.count))) {
							unitToBuild = type.type;
							maxDifference = difference;
						}
					}
				}
				if (unitToBuild == null)
					unitToBuild = UnitType.Terran_Marine;
				if (entity.getUnit().canTrain(unitToBuild) && !entity.getUnit().isTraining() && economyService.availableMinerals() >= unitToBuild.mineralPrice()
						&& player.gas() >= unitToBuild.gasPrice()) {
					entity.getUnit().train(unitToBuild);
					internalCounts.get(unitToBuild).incrementAndGet();
				} else {
					UnitType marine = UnitType.Terran_Marine;
					if (economyService.availableMinerals() >= marine.mineralPrice()) {
						entity.getUnit().train(marine);
						internalCounts.get(unitToBuild).incrementAndGet();
					}
				}
			}
		}
	}

	@Override
	public void onEntityCreate(UnitEntity entity) {
		if (entity.getUnit().getType() == UnitType.Terran_Barracks || (entity.getUnit().getType().getRace() == Race.Terran && entity.getUnit().getType().isBuilding())) {
			buildings.add(entity);
		}
		if (!entity.getUnit().getType().isBuilding()) {
			if (counts.containsKey(entity.getUnit().getType())) {
				counts.get(entity.getUnit().getType()).incrementAndGet();
			} else {
				counts.put(entity.getUnit().getType(), new AtomicInteger(1));
			}
		}
	}

	@Override
	public void onEntityDestroyed(UnitEntity entity) {
		if (!entity.getUnit().getType().isBuilding() && !entity.getUnit().getType().isWorker()) {
			if (counts.containsKey(entity.getUnit().getType())) {
				counts.get(entity.getUnit().getType()).decrementAndGet();
			}
		}

	}

	public void alterBuild(UnitType unitType, int i) {
		alterBuild(unitType, i, false);
	}

	public void alterBuild(UnitType unitType, int i, boolean add) {
		String operation = "=";
		if (add) {
			if (i > 0)
				operation = "+";
			else
				operation = "";
		}
		System.out.println("Altering build: " + unitType + " " + operation + i);
		for (TypeWithCount twc : toBuild) {
			if (twc.type == unitType) {
				if (add) {
					twc.count = twc.count + i;
				} else {
					twc.count = i;
				}
			}
		}
	}

	private class TypeWithCount {
		public UnitType type;
		public int count;
		public boolean isAbsolute;

		public TypeWithCount(UnitType type, int count, boolean isAbsolute) {
			this.type = type;
			this.count = count;
			this.isAbsolute = isAbsolute;
		}
	}

}
