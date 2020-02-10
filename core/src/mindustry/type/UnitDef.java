package mindustry.type;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

//TODO change to UnitType or Shell or something
public class UnitDef extends UnlockableContent{
    static final float shadowTX = -12, shadowTY = -13, shadowColor = Color.toFloatBits(0, 0, 0, 0.22f);

    public @NonNull Prov<? extends UnitController> defaultController = AIController::new;
    public @NonNull Prov<? extends Unitc> constructor;
    public boolean flying;
    public float speed = 1.1f, boostSpeed = 0.75f, rotateSpeed = 10f, baseRotateSpeed = 10f;
    public float drag = 0.3f, mass = 1f, accel = 0.1f;
    public float health = 200f, range = -1;
    public boolean targetAir = false, targetGround = false;
    public boolean faceTarget = true, isCounted = true;

    public int itemCapacity = 30;
    public int drillTier = -1;
    public float buildPower = 1f, minePower = 1f;

    public Color engineColor = Pal.boostTo;
    public float engineOffset = 5f, engineSize = 2.5f;

    public float hitsize = 6f;
    public float itemOffsetY = 3f;
    public float lightRadius = 60f, lightOpacity = 0.6f;
    public Color lightColor = Pal.powerLight;
    public boolean drawCell = true, drawItems = true;

    public ObjectSet<StatusEffect> immunities = new ObjectSet<>();
    public Sound deathSound = Sounds.bang;

    public Array<Weapon> weapons = new Array<>();
    public TextureRegion baseRegion, legRegion, region, cellRegion, occlusionRegion;

    public UnitDef(String name){
        super(name);

        if(EntityMapping.map(name) != null){
            constructor = EntityMapping.map(name);
        }else{
            constructor = () -> Nulls.unit;
        }
    }

    public UnitController createController(){
        return defaultController.get();
    }

    public Unitc create(Team team){
        Unitc unit = constructor.get();
        unit.team(team);
        unit.type(this);
        return unit;
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayUnit(table, this);
    }

    @CallSuper
    @Override
    public void init(){
        //set up default range
        if(range < 0){
            for(Weapon weapon : weapons){
                range = Math.max(range, weapon.bullet.range());
            }
        }
    }

    @CallSuper
    @Override
    public void load(){
        weapons.each(Weapon::load);
        region = Core.atlas.find(name);
        legRegion = Core.atlas.find(name + "-leg");
        baseRegion = Core.atlas.find(name + "-base");
        cellRegion = Core.atlas.find(name + "-cell", Core.atlas.find("power-cell"));
        occlusionRegion = Core.atlas.find("circle-shadow");
    }

    @Override
    public ContentType getContentType(){
        return ContentType.unit;
    }

    //region drawing

    public void drawShadow(Unitc unit){
        Draw.color(shadowColor);
        Draw.rect(region, unit.x() + shadowTX * unit.elevation(), unit.y() + shadowTY * unit.elevation(), unit.rotation() - 90);
        Draw.color();
    }

    public void drawOcclusion(Unitc unit){
        Draw.color(0, 0, 0, 0.4f);
        float rad = 1.6f;
        float size = Math.max(region.getWidth(), region.getHeight()) * Draw.scl;
        Draw.rect(occlusionRegion, unit, size * rad, size * rad);
        Draw.color();
    }

    public void drawItems(Unitc unit){

        //draw back items
        if(unit.hasItem() && unit.itemTime() > 0.01f){
            float size = (itemSize + Mathf.absin(Time.time(), 5f, 1f)) * unit.itemTime();

            Draw.mixcol(Pal.accent, Mathf.absin(Time.time(), 5f, 0.5f));
            Draw.rect(unit.item().icon(Cicon.medium),
            unit.x() + Angles.trnsx(unit.rotation() + 180f, itemOffsetY),
            unit.y() + Angles.trnsy(unit.rotation() + 180f, itemOffsetY),
            size, size, unit.rotation());

            Draw.mixcol();

            Lines.stroke(1f, Pal.accent);
            Lines.circle(
            unit.x() + Angles.trnsx(unit.rotation() + 180f, itemOffsetY),
            unit.y() + Angles.trnsy(unit.rotation() + 180f, itemOffsetY),
            (3f + Mathf.absin(Time.time(), 5f, 1f)) * unit.itemTime());

            if(unit.isLocal()){
                Fonts.outline.draw(unit.stack().amount + "",
                unit.x() + Angles.trnsx(unit.rotation() + 180f, itemOffsetY),
                unit.y() + Angles.trnsy(unit.rotation() + 180f, itemOffsetY) - 3,
                Pal.accent, 0.25f * unit.itemTime() / Scl.scl(1f), false, Align.center
                );
            }

            Draw.reset();
        }
    }

    public void drawWeapons(Unitc unit){
        Draw.mixcol(Color.white, unit.hitTime());

        for(WeaponMount mount : unit.mounts()){
            Weapon weapon = mount.weapon;

            for(int i : (weapon.mirror ? Mathf.signs : Mathf.one)){
                i *= Mathf.sign(weapon.flipped);

                float rotation = unit.rotation() - 90 + (weapon.rotate ? mount.rotation : 0);
                float trY = weapon.y - (mount.reload / weapon.reload * weapon.recoil) * (weapon.alternate ? Mathf.num(i == Mathf.sign(mount.side)) : 1);
                float width = i > 0 ? -weapon.region.getWidth() : weapon.region.getWidth();

                Draw.rect(weapon.region,
                unit.x() + Angles.trnsx(rotation, weapon.x * i, trY),
                unit.y() + Angles.trnsy(rotation, weapon.x * i, trY),
                width * Draw.scl,
                weapon.region.getHeight() * Draw.scl,
                rotation);
            }
        }

        Draw.reset();
    }

    public void drawBody(Unitc unit){
        Draw.mixcol(Color.white, unit.hitTime());

        Draw.rect(region, unit, unit.rotation() - 90);

        Draw.reset();
    }

    public void drawCell(Unitc unit){
        Draw.color(Color.black, unit.team().color, unit.healthf() + Mathf.absin(Time.time(), Math.max(unit.healthf() * 5f, 1f), 1f - unit.healthf()));
        Draw.rect(cellRegion, unit, unit.rotation() - 90);
        Draw.color();
    }

    public void drawLight(Unitc unit){
        if(lightRadius > 0){
            renderer.lights.add(unit, lightRadius, lightColor, lightOpacity);
        }
    }

    public void drawLegs(Legsc unit){
        Draw.mixcol(Color.white, unit.hitTime());

        float ft = Mathf.sin(unit.walkTime(), 6f, 2f + unit.hitSize() / 15f);

        Floor floor = unit.floorOn();

        if(floor.isLiquid){
            Draw.color(Color.white, floor.color, 0.5f);
        }

        for(int i : Mathf.signs){
            Draw.rect(legRegion,
            unit.x() + Angles.trnsx(unit.baseRotation(), ft * i),
            unit.y() + Angles.trnsy(unit.baseRotation(), ft * i),
            legRegion.getWidth() * i * Draw.scl, legRegion.getHeight() * Draw.scl - Mathf.clamp(ft * i, 0, 2), unit.baseRotation() - 90);
        }

        if(floor.isLiquid){
            Draw.color(Color.white, floor.color, unit.drownTime() * 0.4f);
        }else{
            Draw.color(Color.white);
        }

        Draw.rect(baseRegion, unit, unit.baseRotation() - 90);

        Draw.mixcol();
    }

    //endregion
}