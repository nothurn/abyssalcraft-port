package dev.abyssalport.client;

import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.AbyssalZombieEntity;
import net.minecraft.client.model.AbstractZombieModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/** Vanilla-native humanoid geometry/animation, with our own UV skin. */
public final class AbyssalZombieModel extends AbstractZombieModel<AbyssalZombieEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(WastelandContent.id("abyssal_zombie"), "main");
    public AbyssalZombieModel(ModelPart root) { super(root); }
    public static LayerDefinition createLayer() { return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 64); }
    @Override public boolean isAggressive(AbyssalZombieEntity entity) { return entity.isAggressive(); }
}
