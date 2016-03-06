import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	private UnitType unitToBuild;

	public MacroService(EconomyService economyService) {
		this.economyService = economyService;
	}

	@Override
	public void onFrame(Game game, Player player) {
		if (toBuild.isEmpty()) {
			toBuild.add(new TypeWithCount(UnitType.Terran_Marine, 3, -1));
			toBuild.add(new TypeWithCount(UnitType.Terran_Medic, 1, -1));
			toBuild.add(new TypeWithCount(UnitType.Terran_Firebat, 0, -1));
			toBuild.add(new TypeWithCount(UnitType.Terran_Goliath, 0, -1));
			toBuild.add(new TypeWithCount(UnitType.Terran_Science_Vessel, 1, 3));
			toBuild.add(new TypeWithCount(UnitType.Terran_Siege_Tank_Tank_Mode, 1, -1));
			for (TypeWithCount twc : toBuild) {
				counts.put(twc.type, new AtomicInteger(0));
			}
		}

		Map<UnitType, AtomicInteger> internalCounts = new HashMap<>();
		for (Entry<UnitType, AtomicInteger> entry : counts.entrySet()) {
			internalCounts.put(entry.getKey(), new AtomicInteger(entry.getValue().intValue()));
		}
		List<UnitToBuild> nextToBuild = new ArrayList<>();
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
		unitToBuild = null;
		for (TypeWithCount type : toBuild) {
			if (type.count > 0 && game.canMake(type.type)) {
				double difference = (type.count * multiplier) - internalCounts.get(type.type).intValue();
				if (internalCounts.get(type.type).intValue() < type.maxCount || type.maxCount == -1) {
					nextToBuild.add(new UnitToBuild(type.type, internalCounts.get(type.type).intValue(), (int) difference));
				}
			}
		}
		// sort 
		Collections.sort(nextToBuild, new Comparator<UnitToBuild>() {

			@Override
			public int compare(UnitToBuild o1, UnitToBuild o2) {
				if (o1.missing == o2.missing)
					return 0;
				return o1.missing > o2.missing ? -1 : 1;
			}
		});
		StringBuffer b = new StringBuffer();
		for (UnitToBuild n : nextToBuild) {
			b.append(n.type + " c: " + n.count + " m: " + n.missing + "\n");
		}
		game.drawTextScreen(30, 100, b.toString());

		while (!nextToBuild.isEmpty()) {
			unitToBuild = nextToBuild.remove(0).type;
			//			game.drawTextScreen(30, 90, "Want to build: " + unitToBuild);
			for (UnitEntity entity : buildings) {
				if (!entity.getUnit().isTraining() && entity.getUnit().isCompleted()) {
					// hadle goliath expicitly I don't want them to be build in factory with addons
					if (entity.getUnit().canTrain(unitToBuild) && !entity.getUnit().isTraining() && economyService.availableMinerals() >= unitToBuild.mineralPrice()
							&& player.gas() >= unitToBuild.gasPrice()) {
						if (unitToBuild == UnitType.Terran_Goliath && entity.getUnit().getAddon() != null) {
							continue;
						}
						entity.getUnit().train(unitToBuild);
						internalCounts.get(unitToBuild).incrementAndGet();
						return;
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
			UnitType type = entity.getUnit().getType();
			if (type == UnitType.Terran_Siege_Tank_Siege_Mode) {
				type = UnitType.Terran_Siege_Tank_Tank_Mode;
			}
			if (counts.containsKey(type)) {
				counts.get(type).decrementAndGet();
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

	private class UnitToBuild {
		public UnitType type;
		public int count;
		public int missing;

		public UnitToBuild(UnitType type, int count, int missing) {
			this.type = type;
			this.count = count;
			this.missing = missing;
		}

	}

	private class TypeWithCount {
		public UnitType type;
		public int count;
		public int maxCount;

		public TypeWithCount(UnitType type, int count, int maxCount) {
			this.type = type;
			this.count = count;
			this.maxCount = maxCount;
		}
	}

}
