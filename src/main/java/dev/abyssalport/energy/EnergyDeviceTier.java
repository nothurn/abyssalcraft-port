package dev.abyssalport.energy;

/** Source values from TileEntityTieredEnergy{Collector,Relay,Container},
 * Shinoow, LGPL-3.0. Omothol is specified here, not registered without its progression.
 */
public enum EnergyDeviceTier {
    BASE("", 1000, 500, 10_000, 4, 10, 20),
    OVERWORLD("overworld", 1500, 600, 20_000, 6, 20, 30),
    ABYSSAL_WASTELAND("abyssal_wasteland", 2000, 700, 60_000, 8, 30, 40),
    DREADLANDS("dreadlands", 2500, 800, 240_000, 10, 40, 50),
    OMOTHOL("omothol", 3000, 900, 1_200_000, 12, 50, 60);

    private final String id;
    private final int collectorCapacity, relayCapacity, containerCapacity, range, drain, transfer;
    EnergyDeviceTier(String id, int collectorCapacity, int relayCapacity, int containerCapacity, int range, int drain, int transfer) {
        this.id = id; this.collectorCapacity = collectorCapacity; this.relayCapacity = relayCapacity;
        this.containerCapacity = containerCapacity; this.range = range; this.drain = drain; this.transfer = transfer;
    }
    public String id() { return id; }
    public int collectorCapacity() { return collectorCapacity; }
    public int relayCapacity() { return relayCapacity; }
    public int containerCapacity() { return containerCapacity; }
    public int pedestalCapacity() { return 5000 + ordinal() * 2500; }
    public int range() { return range; }
    public int drain() { return drain; }
    public int transfer() { return transfer; }
}
