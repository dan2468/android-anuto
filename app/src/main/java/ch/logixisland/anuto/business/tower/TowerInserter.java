package ch.logixisland.anuto.business.tower;

import java.util.Iterator;

import ch.logixisland.anuto.business.game.GameState;
import ch.logixisland.anuto.business.score.ScoreBoard;
import ch.logixisland.anuto.data.setting.tower.TowerSettingsRoot;
import ch.logixisland.anuto.engine.logic.Entity;
import ch.logixisland.anuto.engine.logic.EntityRegistry;
import ch.logixisland.anuto.engine.logic.GameEngine;
import ch.logixisland.anuto.engine.logic.Message;
import ch.logixisland.anuto.entity.Types;
import ch.logixisland.anuto.entity.plateau.Plateau;
import ch.logixisland.anuto.entity.tower.Tower;
import ch.logixisland.anuto.util.math.Vector2;

public class TowerInserter {

    private final GameEngine mGameEngine;
    private final GameState mGameState;
    private final EntityRegistry mEntityRegistry;
    private final TowerSelector mTowerSelector;
    private final TowerAging mTowerAging;
    private final ScoreBoard mScoreBoard;

    private final TowerDefaultValue mTowerDefaultValue;

    private Tower mInsertedTower;
    private Plateau mCurrentPlateau;

    public TowerInserter(GameEngine gameEngine, GameState gameState, EntityRegistry entityRegistry,
                         TowerSelector towerSelector, TowerAging towerAging, ScoreBoard scoreBoard) {
        mGameEngine = gameEngine;
        mGameState = gameState;
        mEntityRegistry = entityRegistry;
        mTowerSelector = towerSelector;
        mTowerAging = towerAging;
        mScoreBoard = scoreBoard;

        mTowerDefaultValue = new TowerDefaultValue(entityRegistry);
    }

    public void insertTower(final String towerName) {
        if (mGameEngine.isThreadChangeNeeded()) {
            mGameEngine.post(new Message() {
                @Override
                public void execute() {
                    insertTower(towerName);
                }
            });
            return;
        }

        if (mInsertedTower == null && !mGameState.isGameOver() &&
                mScoreBoard.getCredits() >= mTowerDefaultValue.getDefaultValue(towerName)) {
            showTowerLevels();
            mInsertedTower = (Tower) mEntityRegistry.createEntity(towerName);
        }
    }

    public void insertTower(final int slot) {
        if (mGameEngine.isThreadChangeNeeded()) {
            mGameEngine.post(new Message() {
                @Override
                public void execute() {
                    insertTower(slot);
                }
            });
            return;
        }

        TowerSettingsRoot towerSettingsRoot = mGameEngine.getGameConfiguration().getTowerSettingsRoot();
        insertTower(towerSettingsRoot.getTowerSlots().getTowerOfSlot(slot));
    }

    public Tower createPreviewTower(int slot) {
        TowerSettingsRoot towerSettingsRoot = mGameEngine.getGameConfiguration().getTowerSettingsRoot();
        return (Tower) mEntityRegistry.createEntity(towerSettingsRoot.getTowerSlots().getTowerOfSlot(slot));
    }

    public void setPosition(final Vector2 position) {
        if (mGameEngine.isThreadChangeNeeded()) {
            mGameEngine.post(new Message() {
                @Override
                public void execute() {
                    setPosition(position);
                }
            });
            return;
        }

        if (mInsertedTower != null) {
            Plateau closestPlateau = mGameEngine.get(Types.PLATEAU)
                    .cast(Plateau.class)
                    .filter(Plateau.unoccupied())
                    .min(Entity.distanceTo(position));

            if (closestPlateau != null) {
                if (mCurrentPlateau == null) {
                    mGameEngine.add(mInsertedTower);
                    mTowerSelector.selectTower(mInsertedTower);
                }

                mCurrentPlateau = closestPlateau;
                mInsertedTower.setPosition(mCurrentPlateau.getPosition());
            } else {
                cancel();
            }
        }
    }

    public void buyTower() {
        if (mGameEngine.isThreadChangeNeeded()) {
            mGameEngine.post(new Message() {
                @Override
                public void execute() {
                    buyTower();
                }
            });
            return;
        }

        if (mInsertedTower != null && mCurrentPlateau != null) {
            mInsertedTower.setEnabled(true);
            mCurrentPlateau.setOccupant(mInsertedTower);

            mScoreBoard.takeCredits(mInsertedTower.getValue());
            mTowerAging.ageTower(mInsertedTower);

            mTowerSelector.selectTower(null);
            hideTowerLevels();

            mCurrentPlateau = null;
            mInsertedTower = null;
        }
    }

    public void cancel() {
        if (mGameEngine.isThreadChangeNeeded()) {
            mGameEngine.post(new Message() {
                @Override
                public void execute() {
                    cancel();
                }
            });
            return;
        }

        if (mInsertedTower != null) {
            mGameEngine.remove(mInsertedTower);

            hideTowerLevels();
            mCurrentPlateau = null;
            mInsertedTower = null;
        }
    }

    private void showTowerLevels() {
        Iterator<Tower> towers = mGameEngine.get(Types.TOWER).cast(Tower.class);

        while (towers.hasNext()) {
            Tower tower = towers.next();
            tower.showLevel();
        }
    }

    private void hideTowerLevels() {
        Iterator<Tower> towers = mGameEngine.get(Types.TOWER).cast(Tower.class);

        while (towers.hasNext()) {
            Tower tower = towers.next();
            tower.hideLevel();
        }
    }

}