import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bwapi.Game;
import bwapi.Player;
import bwapi.TechType;
import bwapi.UnitType;
import bwapi.UpgradeType;

public class ResearchService implements GameEntity {

	private EconomyService economyService;
	private List<ResearchOrder> researchOrder = new ArrayList<>();
	private List<UnitEntity> buildings = new ArrayList<>();
	private TechType techType;
	private List<UpgradeTask> upgradeTasks = new ArrayList<>();

	public ResearchService(EconomyService economyService) {
		this.economyService = economyService;

	}

	@Override
	public void onFrame(Game game, Player player) {
		if (researchOrder.isEmpty()) {
			researchOrder.add(new ResearchOrder(35, TechType.Stim_Packs, UpgradeType.None));
			researchOrder.add(new ResearchOrder(60, TechType.None, UpgradeType.U_238_Shells));
			researchOrder.add(new ResearchOrder(50, TechType.None, UpgradeType.Terran_Infantry_Weapons));
			researchOrder.add(new ResearchOrder(55, TechType.None, UpgradeType.Terran_Infantry_Armor));
			researchOrder.add(new ResearchOrder(40, TechType.Tank_Siege_Mode, UpgradeType.None));
		}
		if (game.getFrameCount() % 40 == 0) {
			for (ResearchOrder r : researchOrder) {
				if (economyService.getSupply() > r.supply) {
					if (r.techType != TechType.None) {
						if (techType == null && game.canResearch(r.techType) && !player.isResearching(r.techType) && !player.hasResearched(r.techType)) {
							economyService.reserveGas(r.techType.gasPrice());
							economyService.reserveMinerales(r.techType.mineralPrice());
							techType = r.techType;
							System.out.println("tech type picked: " + techType);
						}
					} else if (r.upgradeType != UpgradeType.None) {
						if (!upgradeTasks.contains(new UpgradeTask(r.upgradeType, 0)) && game.canUpgrade(r.upgradeType) && !player.isUpgrading(r.upgradeType)
								&& player.getUpgradeLevel(r.upgradeType) < r.upgradeType.maxRepeats()) {
							int targetUpgradeLevel = player.getUpgradeLevel(r.upgradeType);
							upgradeTasks.add(new UpgradeTask(r.upgradeType, targetUpgradeLevel));
							System.out.println("upgrade type picked: " + r.upgradeType);
						}
					}
				}
			}
		}

		if (techType != null) {
			if (!(player.isResearching(techType) || player.hasResearched(techType))) {
				if (techType.gasPrice() <= player.gas() && techType.mineralPrice() <= economyService.getMinerals()) {
					for (UnitEntity entity : buildings) {
						if (entity.getUnit().canResearch(techType)) {
							System.out.println("Trying to research: " + techType);
							entity.getUnit().research(techType);
							economyService.freeGas(techType.gasPrice());
							economyService.freeMinerals(techType.mineralPrice());
						}
					}
				}
			}
			if (player.hasResearched(techType)) {
				techType = null;
			}
		}
		Iterator<UpgradeTask> itt = upgradeTasks.iterator();
		while (itt.hasNext()) {
			UpgradeTask task = itt.next();
			UpgradeType upgradeType = task.upgradeType;
			int targetUpgradeLevel = task.targetUpgradeLevel;
			if (!(player.isUpgrading(upgradeType) || player.getUpgradeLevel(upgradeType) > targetUpgradeLevel)) {
				if (upgradeType.gasPrice() <= economyService.availableGas() && upgradeType.mineralPrice() <= economyService.availableMinerals()) {
					for (UnitEntity entity : buildings) {
						if (entity.getUnit().canUpgrade(upgradeType) && !entity.getUnit().isUpgrading()) {
							System.out.println("Trying to upgrade: " + upgradeType);
							entity.getUnit().upgrade(upgradeType);
						}
					}
				}
			}
			if (player.getUpgradeLevel(upgradeType) > targetUpgradeLevel) {
				// remove task
				itt.remove();
			}
		}

	}

	@Override
	public void onEntityCreate(UnitEntity entity) {
		for (ResearchOrder r : researchOrder) {
			if (entity.getUnit().canResearch(r.techType) || entity.getUnit().canUpgrade(r.upgradeType)) {
				buildings.add(entity);
			}
		}
		if (entity.getUnit().getType() == UnitType.Terran_Academy) {
			buildings.add(entity);
		}
		if (entity.getUnit().getType() == UnitType.Terran_Engineering_Bay) {
			buildings.add(entity);
		}
		if (entity.getUnit().getType() == UnitType.Terran_Machine_Shop) {
			buildings.add(entity);
		}

	}

	@Override
	public void onEntityDestroyed(UnitEntity entity) {
		// TODO Auto-generated method stub

	}

	private class ResearchOrder {
		public int supply;
		public TechType techType;
		public UpgradeType upgradeType;

		public ResearchOrder(int suply, TechType techType, UpgradeType upgradeType) {
			this.supply = suply;
			this.techType = techType;
			this.upgradeType = upgradeType;
		}

	}

	private class UpgradeTask {
		public UpgradeType upgradeType;
		public int targetUpgradeLevel = 0;

		public UpgradeTask(UpgradeType upgradeType, int targetUpgradeLevel) {
			this.upgradeType = upgradeType;
			this.targetUpgradeLevel = targetUpgradeLevel;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof UpgradeTask))
				return false;
			UpgradeTask that = (UpgradeTask) obj;
			return upgradeType.equals(that.upgradeType);
		}

		@Override
		public int hashCode() {
			return upgradeTasks.hashCode();
		}

	}

}
