package ch.bfh.anuto.game.objects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.simpleframework.xml.Attribute;

import ch.bfh.anuto.game.DrawObject;
import ch.bfh.anuto.game.GameEngine;
import ch.bfh.anuto.game.GameObject;
import ch.bfh.anuto.game.Layers;
import ch.bfh.anuto.game.TypeIds;
import ch.bfh.anuto.game.data.Path;
import ch.bfh.anuto.util.iterator.Function;
import ch.bfh.anuto.util.math.Vector2;


public abstract class Enemy extends GameObject {

    /*
    ------ Constants ------
     */

    public static final int TYPE_ID = TypeIds.ENEMY;

    private static final float HEALTHBAR_WIDTH = 1.0f;
    private static final float HEALTHBAR_HEIGHT = 0.1f;
    private static final float HEALTHBAR_OFFSET = 0.6f;

    /*
    ------ Healthbar Class ------
     */

    private class HealthBar extends DrawObject {
        private Paint mHealthBarBg;
        private Paint mHealthBarFg;

        public HealthBar() {
            mHealthBarBg = new Paint();
            mHealthBarBg.setColor(Color.BLACK);
            mHealthBarFg = new Paint();
            mHealthBarFg.setColor(Color.GREEN);
        }

        @Override
        public int getLayer() {
            return Layers.ENEMY_HEALTHBAR;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.translate(mPosition.x - HEALTHBAR_WIDTH/2f, mPosition.y + HEALTHBAR_OFFSET);

            canvas.drawRect(0, 0, HEALTHBAR_WIDTH, HEALTHBAR_HEIGHT, mHealthBarBg);
            canvas.drawRect(0, 0, mHealth * HEALTHBAR_WIDTH / mHealthMax, HEALTHBAR_HEIGHT, mHealthBarFg);
        }
    }

    /*
    ------ Static ------
     */

    public static Function<Enemy, Float> health() {
        return new Function<Enemy, Float>() {
            @Override
            public Float apply(Enemy input) {
                return input.mHealth;
            }
        };
    }

    public static Function<Enemy, Float> distanceRemaining() {
        return new Function<Enemy, Float>() {
            @Override
            public Float apply(Enemy input) {
                return input.getDistanceRemaining();
            }
        };
    }

    /*
    ------ Members -----
     */

    @Attribute(name="path")
    private int mPathIndex;

    protected Path mPath = null;
    protected int mWayPointIndex = 0;

    protected float mHealth = 100f;
    protected float mHealthMax = 100f;
    protected float mSpeed = 1.0f;

    protected int mReward = 0;

    private HealthBar mHealthBar;

    /*
    ------ Public Methods ------
     */

    @Override
    public int getTypeId() {
        return TYPE_ID;
    }

    @Override
    public void init() {
        super.init();

        mPath = mGame.getManager().getLevel().getPaths().get(mPathIndex);

        mHealthBar = new HealthBar();
        mGame.add(mHealthBar);
    }

    @Override
    public void clean() {
        super.clean();
        mGame.remove(mHealthBar);
        mGame.getManager().reportEnemyRemoved(this);
    }

    @Override
    public void tick() {
        super.tick();

        if (!hasWayPoint()) {
            mGame.getManager().takeLives(1);
            this.remove();
            return;
        }

        if (getDistanceTo(getWayPoint()) < mSpeed / GameEngine.TARGET_FPS) {
            setPosition(getWayPoint());
            nextWayPoint();
        }
        else {
            moveSpeed(getDirectionTo(getWayPoint()), mSpeed);
        }
    }


    protected Vector2 getWayPoint() {
        return mPath.getWayPoints().get(mWayPointIndex);
    }

    protected void nextWayPoint() {
        mWayPointIndex++;
    }

    protected boolean hasWayPoint() {
        return mPath != null && mPath.getWayPoints().size() > mWayPointIndex;
    }

    public float getDistanceRemaining() {
        if (!hasWayPoint()) {
            return 0;
        }

        float dist = getDistanceTo(getWayPoint());

        for (int i = mWayPointIndex + 1; i < mPath.getWayPoints().size(); i++) {
            Vector2 wThis = mPath.getWayPoints().get(i);
            Vector2 wLast = mPath.getWayPoints().get(i - 1);

            dist += wThis.copy().sub(wLast).len();
        }

        return dist;
    }


    public void damage(float dmg) {
        mHealth -= dmg;

        if (mHealth <= 0) {
            mGame.getManager().giveCredits(mReward);
            this.remove();
        }
    }

    public void heal(float val) {
        mHealth += val;
    }
}
