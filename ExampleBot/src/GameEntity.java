import bwapi.Game;
import bwapi.Player;

public interface GameEntity {

	void onFrame(Game game, Player player);
	void onEntityCreate(UnitEntity unit);
	void onEntityDestroyed(UnitEntity unit);

}
