package dev.abyssalport.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** Short fading wisps with an authored sprite. Source PEStreamParticleFX damping/
 * lifetime concept, Shinoow, LGPL-3.0; bounded deterministic native lifetime.
 */
public final class PEStreamParticle extends TextureSheetParticle {
    private PEStreamParticle(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,SpriteSet sprites) {
        super(level,x,y,z); xd=vx; yd=vy; zd=vz; friction=.65F;
        hasPhysics=false; lifetime=20; quadSize=.075F; pickSprite(sprites);
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    @Override public int getLightColor(float partialTick) { return 240; }
    @Override public void tick() { super.tick(); setAlpha(Math.max(0,1-age/(float)lifetime)); }
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites=sprites; }
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double vx,double vy,double vz) {
            return new PEStreamParticle(level,x,y,z,vx,vy,vz,sprites);
        }
    }
}
