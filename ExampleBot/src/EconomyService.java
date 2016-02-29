import bwapi.Game;
import bwapi.Player;

public class EconomyService implements GameEntity {

	private int minerals;
	private int gas;
	private int reservedGas;
	private int reservedMinerals;
	private int supply;
	private int supplyTotal;

	@Override
	public void onFrame(Game game, Player player) {
		minerals = player.minerals();
		supply = player.supplyUsed();
		gas = player.gas();
		supplyTotal = player.supplyTotal();
		game.drawTextScreen(100, 30, "Reserved minerals: " + reservedMinerals);
		game.drawTextScreen(100, 38, "Supply used: " + getSupply());

	}

	/**
	 * Returns minerals - reserverMinerals
	 * 
	 * @return
	 */
	public int availableMinerals() {
		return minerals - reservedMinerals;
	}

	public void reserveMinerales(int n) {
		System.out.println("Minerals reserved: " + n);
		reservedMinerals += n;
	}

	public void freeMinerals(int n) {
		System.out.println("Minerals freed: " + n);
		reservedMinerals -= n;
	}

	public int availableGas() {
		return gas - reservedGas;
	}

	public void reserveGas(int n) {
		System.out.println("Gas reserved: " + n);
		reservedGas += n;
	}

	public void freeGas(int n) {
		System.out.println("Gas freed: " + n);
		reservedGas -= n;
	}

	public int getMinerals() {
		return minerals;
	}

	@Override
	public void onEntityCreate(UnitEntity unit) {
		// TODO Auto-generated method stub

	}

	public int getSupply() {
		return supply / 2;
	}

	@Override
	public void onEntityDestroyed(UnitEntity unit) {
		// TODO Auto-generated method stub
		
	}

	public int getSupplyTotal() {
		return supplyTotal /2;
	}

}
