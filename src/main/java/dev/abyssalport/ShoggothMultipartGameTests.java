package dev.abyssalport;

import dev.abyssalport.content.ShoggothEcologyContent;
import dev.abyssalport.entity.ShoggothEntity;
import dev.abyssalport.entity.ShoggothEntity.*;
import dev.abyssalport.entity.ShoggothPart;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShoggothMultipartGameTests {
    private ShoggothMultipartGameTests() { }

    @GameTest(template="empty",batch="shoggoth_multipart_geometry")
    public static void NativeTwoSurfaceSizesOffsetsAndReservedIdsFollowEveryFormAndRotation(GameTestHelper helper) {
        var ids=new HashSet<Integer>();
        for(var habitat:Habitat.values()) for(var kind:Kind.values()) {
            var mob=ShoggothEcologyContent.type(kind,habitat).create(helper.getLevel());
            try {
                helper.assertTrue(mob.isMultipartEntity()&&mob.getParts().length==2,"Form does not expose two native hit surfaces");
                helper.assertTrue(ids.add(mob.getId()),"Native parent ID duplicates another form");
                var body=mob.getParts()[0]; var head=mob.getParts()[1];
                helper.assertTrue(body.surface()==ShoggothPart.Surface.BODY&&head.surface()==ShoggothPart.Surface.HEAD,"Hit surfaces reversed");
                helper.assertTrue(body.getParent()==mob&&head.getParent()==mob&&body.is(mob)&&head.is(mob)
                    &&body.getId()==mob.getId()+1&&head.getId()==mob.getId()+2&&ids.add(body.getId())&&ids.add(head.getId()),"Native tracking IDs/parent identity differ");
                helper.assertTrue(!body.shouldBeSaved()&&!head.shouldBeSaved(),"Multipart surfaces opt into independent native persistence");
                for(float yaw:new float[]{0,90,180,270}) {
                    mob.moveTo(12,50,21,yaw,0); double radians=Math.toRadians(yaw),x=-Math.sin(radians),z=Math.cos(radians);
                    double back=kind==Kind.LESSER?.35:kind==Kind.NORMAL?.5:.7,forward=kind==Kind.LESSER?.25:.5,height=kind==Kind.LESSER?.75:kind==Kind.NORMAL?1.2:1.5;
                    helper.assertTrue(body.position().distanceTo(new Vec3(12-x*back,50,21-z*back))<.0001
                        &&head.position().distanceTo(new Vec3(12+x*forward,50+height,21+z*forward))<.0001,"Native hit surfaces fail position/yaw update");
                    helper.assertTrue(Math.abs(body.getBoundingBox().getXsize()-kind.width)<.0001
                        &&Math.abs(body.getBoundingBox().getYsize()-(kind==Kind.LESSER?1.1:kind==Kind.NORMAL?1.4:2.2))<.0001
                        &&Math.abs(head.getBoundingBox().getXsize()-(kind==Kind.LESSER?.8:kind==Kind.NORMAL?1.1:1.6))<.0001
                        &&Math.abs(head.getBoundingBox().getYsize()-(kind==Kind.GREATER?1.2:.6))<.0001,"Source body/head dimensions differ");
                }
                // A native spawn packet reassigns the parent ID before tracking.
                // Exercise that same override without starting a client.
                mob.setId(900000+ids.size()*4);
                helper.assertTrue(body.getId()==mob.getId()+1&&head.getId()==mob.getId()+2,"Packet-assigned IDs leave stale children");
            } finally { mob.discard(); }
        }
        helper.succeed();
    }

    @GameTest(template="empty",batch="shoggoth_multipart_damage")
    public static void BothSurfacesShareParentImmunityDamageVetosAndNativeHurtCooldown(GameTestHelper helper) {
        var level=helper.getLevel(); var arrow=EntityType.ARROW.create(level);
        for(var kind:Kind.values()) {
            var mob=ShoggothEcologyContent.type(kind,Habitat.DREAD).create(level);
            try {
                var body=mob.getParts()[0]; var head=mob.getParts()[1];
                helper.assertTrue(!head.hurt(level.damageSources().arrow(arrow,null),6)&&!body.hurt(level.damageSources().cactus(),4)
                    &&mob.getHealth()==kind.health,"Child surface bypasses parent projectile/cactus immunity");
                Consumer<LivingIncomingDamageEvent> veto=event->{ if(event.getEntity()==mob) event.setCanceled(true); };
                NeoForge.EVENT_BUS.addListener(veto);
                try { helper.assertTrue(!head.hurt(level.damageSources().generic(),3)&&mob.getHealth()==kind.health,"Native parent veto did not reach child surface"); }
                finally { NeoForge.EVENT_BUS.unregister(veto); }
                helper.assertTrue(head.hurt(level.damageSources().generic(),3)&&mob.getHealth()==kind.health-3
                    &&!body.hurt(level.damageSources().generic(),3)&&mob.getHealth()==kind.health-3,"Two surfaces duplicate health damage in one native hurt interval");
                mob.invulnerableTime=0; helper.assertTrue(body.hurt(level.damageSources().generic(),2)&&mob.getHealth()==kind.health-5,"Body cannot pass ordinary native damage");
                mob.discard(); helper.assertTrue(!body.isPickable()&&!head.isPickable()&&!head.hurt(level.damageSources().generic(),1),"Removed parent leaves a live hittable child");
            } finally { mob.discard(); }
        }
        arrow.discard(); helper.succeed();
    }

    @GameTest(template="empty",batch="shoggoth_multipart_tracking",timeoutTicks=100)
    public static void ActuallyTrackedNativePartsCanBeRayPickedAndDisappearWithTheirParent(GameTestHelper helper) {
        var fixture=new GraveyardGameTests.Fixture(helper); var level=fixture.level; var mob=ShoggothEcologyContent.type(Kind.GREATER,Habitat.ABYSSAL).create(level);
        mob.setNoAi(true); mob.setNoGravity(true); mob.moveTo(fixture.node.getX()+.5,fixture.node.getY(),fixture.node.getZ()+.5,0,0);
        try { helper.assertTrue(level.addFreshEntity(mob),"Native multipart parent was not admitted"); }
        catch(RuntimeException error) { mob.discard(); fixture.close(); throw error; }
        helper.runAfterDelay(2,()-> {
            try {
                var head=mob.getParts()[1]; var body=mob.getParts()[0];
                helper.assertTrue(level.getPartEntities().contains(head)&&level.getPartEntities().contains(body),"Native tracking omitted custom hit surfaces");
                Vec3 end=head.getBoundingBox().getCenter(),start=end.add(0,0,3);
                var ray=ProjectileUtil.getEntityHitResult(level,null,start,end,new AABB(start,end).inflate(.02),entity->entity==head&&entity.isPickable());
                helper.assertTrue(ray!=null&&ray.getEntity()==head,"Native world ray cannot select outside-head surface");
                float before=mob.getHealth();
                helper.assertTrue(head.hurt(level.damageSources().generic(),2)&&mob.getHealth()==before-2,"Picked surface is not connected to parent health");
                mob.discard();
                helper.runAfterDelay(2,()-> {
                    try {
                        helper.assertTrue(!level.getPartEntities().contains(head)&&!level.getPartEntities().contains(body)&&!head.isPickable(),"Removed parent leaves stale tracked surfaces"); helper.succeed();
                    } finally { fixture.close(); }
                });
            } catch(RuntimeException error) { mob.discard(); fixture.close(); throw error; }
        });
    }
}
