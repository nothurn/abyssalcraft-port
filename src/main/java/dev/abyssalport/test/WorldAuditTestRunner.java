package dev.abyssalport.test;

import com.mojang.logging.LogUtils;
import dev.abyssalport.AbyssalPort;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestBatchFactory;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.gametest.framework.StructureGridSpawner;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.bus.api.SubscribeEvent;
import org.slf4j.Logger;

/** Opt-in development-only runner for tests requiring real datapack dimensions. */
@EventBusSubscriber(modid = AbyssalPort.MOD_ID)
public final class WorldAuditTestRunner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static MultipleTestTracker tracker;
    private static Integer exitCode;
    private static RestartAuditProbe restartAudit;
    private WorldAuditTestRunner() { }
    private static boolean enabled() { return !FMLLoader.isProduction() && Boolean.getBoolean("abyssalport.gatewayAudit"); }
    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        if (!enabled()) return;
        var world = event.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize();
        if (!world.getFileName().toString().startsWith("gateway-audit-") || !"127.0.0.1".equals(event.getServer().getLocalIp()))
            throw new IllegalStateException("World tests require an isolated gateway-audit-* world bound to loopback");
        String restartPhase = System.getProperty("abyssalport.restartAuditPhase", "");
        if (!restartPhase.isEmpty()) {
            if (!world.getFileName().toString().startsWith("gateway-audit-restart-"))
                throw new IllegalStateException("Restart probe requires its own gateway-audit-restart-* world");
            tracker = null; exitCode = null;
            try {
                restartAudit = new RestartAuditProbe(event.getServer(), restartPhase);
                restartAudit.start();
            } catch (RuntimeException failure) { restartFailed(event.getServer(), failure); }
            return;
        }
        GameTestHooks.registerGametests();
        var level = event.getServer().overworld();
        var functions = GameTestRegistry.getAllTestFunctions().stream()
                .filter(test -> test.structureName().startsWith("abyssalport_worldtest:"))
                .sorted(java.util.Comparator.comparing(net.minecraft.gametest.framework.TestFunction::testName)).toList();
        if (functions.isEmpty()) throw new IllegalStateException("World audit has no selected tests");
        var runner = GameTestRunner.Builder.fromBatches(GameTestBatchFactory.fromTestFunction(functions, level), level)
                .newStructureSpawner(new StructureGridSpawner(new BlockPos(0, 140, 0), 4, false)).build();
        tracker = new MultipleTestTracker(runner.getTestInfos()); exitCode = null;
        LOGGER.info("World audit running {} required integration tests on a normal dedicated world", tracker.getTotalCount());
        runner.start();
    }
    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (enabled() && restartAudit != null && exitCode == null) {
            try {
                if (restartAudit.tick()) {
                    exitCode = 0;
                    LOGGER.info("RESTART AUDIT PHASE COMPLETE: {} passed; halting normally for full native save", restartAudit.phase());
                    event.getServer().halt(false);
                }
            } catch (RuntimeException failure) { restartFailed(event.getServer(), failure); }
            return;
        }
        if (!enabled() || tracker == null || !tracker.isDone() || exitCode != null) return;
        exitCode = tracker.getFailedRequiredCount() + tracker.getFailedOptionalCount();
        LOGGER.info("WORLD AUDIT COMPLETE: {} tests, {} failures", tracker.getTotalCount(), exitCode);
        if (exitCode == 0 && Boolean.getBoolean("abyssalport.auditExitProbe")) {
            LOGGER.info("Intentional development audit exit-code probe: forcing exit 1 after passed tests and full world saving");
            exitCode = 1;
        }
        event.getServer().halt(false);
    }
    private static void restartFailed(net.minecraft.server.MinecraftServer server, RuntimeException failure) {
        exitCode = 1;
        LOGGER.error("RESTART AUDIT FAILED", failure);
        server.halt(false);
    }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        if (enabled() && exitCode != null && exitCode > 0) {
            int result = Math.min(exitCode, 125);
            Thread serverThread = Thread.currentThread();
            // ServerStoppedEvent precedes onServerExit (including any GUI disposal).
            // Finish that thread before Runtime shutdown starts its joining hooks.
            new Thread(() -> {
                try { serverThread.join(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); LOGGER.error("Audit exit interrupted", interrupted); return; }
                System.exit(result);
            }, "Abyssal world audit exit").start();
        }
        tracker = null;
        if (restartAudit != null) { restartAudit.close(); restartAudit = null; }
    }
}
